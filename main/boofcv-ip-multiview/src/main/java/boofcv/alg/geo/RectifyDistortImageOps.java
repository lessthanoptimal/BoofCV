/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.geo;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.*;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

/**
 * <p>
 * Operations related to rectifying stereo image pairs. Provides functions for 1) creating rectification calculation
 * algorithms, 2) rectification transforms, and 3) image distortion for rectification.
 * </p>
 *
 * <p>
 * Definition of transformed coordinate systems:
 * <dl>
 *     <dt>Pixel<dd>Original image coordinates in pixels.
 *     <dt>Rect<dd>Rectified image coordinates in pixels. Lens distortion has been removed.
 *     <dt>RectNorm<dd>Rectified image coordinates in normalized coordinates.
 * </dl>
 * </p>
 *
 * @author Peter Abeles
 */
public class RectifyDistortImageOps {

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its rectification matrix.
	 * Lens distortion is assumed to have been previously removed.
	 *
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageGray<T>> ImageDistort<T,T>
	rectifyImage( FMatrixRMaj rectify , BorderType borderType, Class<T> imageType)
	{
		boolean skip = borderType == BorderType.SKIP;
		if( skip ) {
			borderType = BorderType.EXTENDED;
		}
		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, borderType);

		FMatrixRMaj rectifyInv = new FMatrixRMaj(3,3);
		CommonOps_FDRM.invert(rectify,rectifyInv);
		PointTransformHomography_F32 rectifyTran = new PointTransformHomography_F32(rectifyInv);

		// don't bother caching the results since it is likely to only be applied once and is cheap to compute
		ImageDistort<T,T> ret = FactoryDistort.distortSB(false, interp, imageType);
		ret.setRenderAll(!skip);

		ret.setModel(new PointToPixelTransform_F32(rectifyTran));

		return ret;
	}

	/**
	 * Creates an {@link ImageDistort} for rectifying an image given its radial distortion and
	 * rectification matrix.
	 *
	 * @param param Intrinsic parameters.
	 * @param rectify Transform for rectifying the image.
	 * @param imageType Type of single band image the transform is to be applied to.
	 * @return ImageDistort for rectifying the image.
	 */
	public static <T extends ImageBase<T>> ImageDistort<T,T>
	rectifyImage(CameraPinholeBrown param, FMatrixRMaj rectify , BorderType borderType, ImageType<T> imageType)
	{
		boolean skip = borderType == BorderType.SKIP;
		if( skip ) {
			borderType = BorderType.EXTENDED;
		}
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0,255, InterpolationType.BILINEAR,borderType,imageType);

		// only compute the transform once
		ImageDistort<T,T> ret = FactoryDistort.distort(true, interp, imageType);
		ret.setRenderAll(!skip);

		Point2Transform2_F32 transform = RectifyImageOps.transformRectToPixel(param, rectify);

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
	}

	/**
	 * Applies a mask which indicates which pixels had mappings to the unrectified image. Pixels which were
	 * outside of the original image will be set to 255. The border is extended because the sharp edge
	 * in the rectified image can cause in incorrect match between image features.
	 *
	 * @param disparity (Input) disparity
	 * @param mask (Input) mask. 1 = mapping to unrectified. 0 = no mapping
	 * @param radius How much the border is extended by
	 */
	public static void applyMask(GrayF32 disparity , GrayU8 mask , int radius ) {
		if( disparity.isSubimage() || mask.isSubimage() )
			throw new RuntimeException("Input is subimage. Currently not support but no reason why it can't be. Ask for it");

		int N = disparity.width*disparity.height;
		for (int i = 0; i < N; i++) {
			if( mask.data[i] == 0 ) {
				disparity.data[i] = 255;
			}
		}

		// TODO make this more efficient and correct. Update unit test
		if( radius > 0 ) {
			int r = radius;
			for (int y = r; y < mask.height - r-1; y++) {
				int indexMsk = y * mask.stride + r;
				for (int x = r; x < mask.width - r-1; x++, indexMsk++) {
					int deltaX = mask.data[indexMsk] - mask.data[indexMsk + 1];
					int deltaY = mask.data[indexMsk] - mask.data[indexMsk + mask.stride];

					if ( deltaX != 0 || deltaY != 0) {
						// because of how the border is detected it has a bias when going from up to down
						if( deltaX < 0 )
							deltaX = 0;
						if( deltaY < 0 )
							deltaY = 0;
						for (int i = -r; i <= r; i++) {
							for (int j = -r; j <= r; j++) {
								disparity.set(deltaX+x + j, deltaY+y + i, 255);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Applies a mask which indicates which pixels had mappings to the unrectified image. Pixels which were
	 * outside of the original image will be set to 255. The border is extended because the sharp edge
	 * can confuse disparity algorithms.
	 *
	 * @param disparity (Input) disparity
	 * @param mask (Input) mask. 1 = mapping to unrectified. 0 = no mapping
	 * @param radius How much the border is extended by
	 */
	public static void applyMask(GrayU8 disparity , GrayU8 mask , int radius ) {
		if( disparity.isSubimage() || mask.isSubimage() )
			throw new RuntimeException("Input is subimage. Currently not support but no reason why it can't be. Ask for it");

		int N = disparity.width*disparity.height;
		for (int i = 0; i < N; i++) {
			if( mask.data[i] == 0 ) {
				disparity.data[i] = (byte)255;
			}
		}

		// TODO make this more efficient and correct. Update unit test
		if( radius > 0 ) {
			int r = radius;
			for (int y = r; y < mask.height - r-1; y++) {
				int indexMsk = y * mask.stride + r;
				for (int x = r; x < mask.width - r-1; x++, indexMsk++) {
					int deltaX = mask.data[indexMsk] - mask.data[indexMsk + 1];
					int deltaY = mask.data[indexMsk] - mask.data[indexMsk + mask.stride];

					if ( deltaX != 0 || deltaY != 0) {
						// because of how the border is detected it has a bias when going from up to down
						if( deltaX < 0 )
							deltaX = 0;
						if( deltaY < 0 )
							deltaY = 0;
						for (int i = -r; i <= r; i++) {
							for (int j = -r; j <= r; j++) {
								disparity.set(deltaX+x + j, deltaY+y + i, 255);
							}
						}
					}
				}
			}
		}
	}
}
