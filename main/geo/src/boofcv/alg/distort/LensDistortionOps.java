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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.distort.SequencePointTransform_F32;
import boofcv.struct.image.ImageSingleBand;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps {

	/**
	 * Removes radial distortion from the image and converts it into normalized image coordinates
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration
	 * @return Distorted pixel to normalized image coordinates
	 */
	public static PointTransform_F64 removeRadialToNorm( IntrinsicParameters param,
														 boolean applyLeftToRight )
	{
		RemoveRadialPtoN_F64 radialDistort = new RemoveRadialPtoN_F64();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( applyLeftToRight ) {
			return new LeftToRightHandedNorm_F64(radialDistort,param.height);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Transform from undistorted image to an image with radial distortion.
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @return Transform from undistorted to distorted image.
	 */
	public static PointTransform_F32 radialTransformInv(IntrinsicParameters param,
														boolean applyLeftToRight )
	{
		AddRadialPtoP_F32 radialDistort = new AddRadialPtoP_F32();
		radialDistort.set(param.fx, param.fy, param.skew, param.cx, param.cy, param.radial);

		if( applyLeftToRight ) {
			PointTransform_F32 l2r = new LeftToRightHanded_F32(param.height);
			return new SequencePointTransform_F32(l2r,radialDistort,l2r);
		} else {
			return radialDistort;
		}
	}

	/**
	 * Creates an {@Link ImageDistort} which removes radial distortion.
	 *
	 * @param param Intrinsic camera parameters
	 * @param applyLeftToRight Set to true if the image coordinate system was adjusted
	 *                         to right handed during calibration.
	 * @param imageType Type of single band image being processed
	 * @return Image distort that removes radial distortion
	 */
	public static <T extends ImageSingleBand> ImageDistort<T>
	removeRadialImage(IntrinsicParameters param,
					  boolean applyLeftToRight ,
					  Class<T> imageType)
	{
		InterpolatePixel<T> interp = FactoryInterpolation.bilinearPixel(imageType);
		ImageBorder<T> border = FactoryImageBorder.general(imageType, BorderType.EXTENDED);

		// only compute the transform once
		ImageDistort<T> ret = FactoryDistort.distortCached(interp, border, imageType);

		PointTransform_F32 transform = radialTransformInv(param, applyLeftToRight );

		ret.setModel(new PointToPixelTransform_F32(transform));

		return ret;
	}
}
