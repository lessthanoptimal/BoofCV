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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

/**
 * Operations for manipulating lens distortion which do not have F32 and F64 equivalents.
 *
 * @author Peter Abeles
 * @see LensDistortionOps_F32
 * @see LensDistortionOps_F64
 */
public class LensDistortionOps {
	/**
	 * Creates a distortion for modifying the input image from one camera model into another camera model. If
	 * requested the camera model can be further modified to ensure certain visibility requirements are meet
	 * and the adjusted camera model will be returned.
	 *
	 * @param type How it should modify the image model to ensure visibility of pixels.
	 * @param borderType How the image border is handled
	 * @param original The original camera model
	 * @param desired The desired camera model
	 * @param modified (Optional) The desired camera model after being rescaled. Can be null.
	 * @param imageType Type of image.
	 * @return Image distortion from original camera model to the modified one.
	 */
	public static <T extends ImageBase<T>, O extends CameraPinhole, D extends CameraPinhole>
	ImageDistort<T, T> changeCameraModel( AdjustmentType type, BorderType borderType,
										  O original,
										  D desired,
										  @Nullable D modified,
										  ImageType<T> imageType ) {
		Class bandType = imageType.getImageClass();
		boolean skip = borderType == BorderType.SKIP;

		// it has to process the border at some point, so if skip is requested just skip stuff truly outside the image
		if (skip)
			borderType = BorderType.EXTENDED;

		InterpolatePixelS interp = FactoryInterpolation.createPixelS(0, 255, InterpolationType.BILINEAR, borderType, bandType);

		Point2Transform2_F32 undistToDist = LensDistortionOps_F32.transformChangeModel(type, original, desired, true, modified);

		ImageDistort<T, T> distort = FactoryDistort.distort(true, interp, imageType);

		distort.setModel(new PointToPixelTransform_F32(undistToDist));
		distort.setRenderAll(!skip);

		return distort;
	}
}
