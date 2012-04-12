/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.*;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.geo.rectify.RectifyFundamental;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.SequencePointTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * Operations related to rectifying stereo image pairs.
 *
 * @author Peter Abeles
 */
public class RectifyImageOps {

	public static RectifyCalibrated createCalibrated() {
		return new RectifyCalibrated();
	}

	public static RectifyFundamental createFundamental() {
		return new RectifyFundamental();
	}

	/**
	 * Creates a transform that goes from rectified to original pixel coordinates.
	 * Rectification includes removal of lens distortion.  Used for rendering rectified images.
	 *
	 * @param param Intrinsic parameters.
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param rectify Transform for rectifying the image.
	 * @return Inverse rectification transform.
	 */
	public static PointTransform_F32 rectifyTransformInv(IntrinsicParameters param,
														 boolean applyLeftToRight ,
														 DenseMatrix64F rectify )
	{
		AddRadialPtoP_F32 radialDistort = new AddRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		DenseMatrix64F rectifyInv = new DenseMatrix64F(3,3);
		CommonOps.invert(rectify,rectifyInv);
		PointTransformHomography_F32 rectifyDistort = new PointTransformHomography_F32(rectifyInv);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,rectifyDistort,radialDistort,l2r);
		} else {
			return new SequencePointTransform_F32(rectifyDistort,radialDistort);
		}
	}

	public static <T extends ImageSingleBand> ImageDistort<T>
	rectifyImage(IntrinsicParameters param,
				 boolean applyLeftToRight ,
				 DenseMatrix64F rectify , Class<T> imageType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);

		// only compute the transform once
		ImageDistort<T> ret = FactoryDistort.distortCached(interp,border,imageType);

		PointTransform_F32 transform = rectifyTransformInv(param, applyLeftToRight , rectify);

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
	}
}
