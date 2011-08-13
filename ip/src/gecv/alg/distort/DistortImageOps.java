/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.distort;

import gecv.alg.distort.impl.DistortSupport;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.alg.interpolate.TypeInterpolate;
import gecv.struct.distort.PixelTransform;
import gecv.struct.image.ImageBase;


/**
 * <p>
 * Provides common function for distorting images.
 * </p>
 *
 * @author Peter Abeles
 */
public class DistortImageOps {

	/**
	 * Rescales the input image and writes the results into the output image.  The scale
	 * factor is determined independently of the width and height.
	 *
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param type Which interpolation algorithm should be used.
	 */
	public static <T extends ImageBase>
	void scale( T input , T output , TypeInterpolate type ) {
		Class<T> inputType = (Class<T>)input.getClass();

		PixelTransform model = DistortSupport.transformScale(output, input);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,type);

		distorter.apply(input,output);
	}

	public static <T extends ImageBase>
	void scale( T input , T output , InterpolatePixel<T> interp ) {
		Class<T> inputType = (Class<T>)input.getClass();

		PixelTransform model = DistortSupport.transformScale(output, input);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,interp);

		distorter.apply(input,output);
	}

	/**
	 * Rotates the image using the specified interpolation type.  The rotation is performed
	 * around the specified center of rotation in the input image.
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param type Which type of interpolation will be used.
	 * @param centerX Center of rotation in input image coordinates.
	 * @param centerY Center of rotation in input image coordinates.
	 * @param angle Angle of rotation in radians.
	 */
	public static <T extends ImageBase>
	void rotate( T input , T output , TypeInterpolate type ,
				 float centerX , float centerY , float angle ) {
		Class<T> inputType = (Class<T>)input.getClass();

		PixelTransform model = DistortSupport.transformRotate(centerX,centerY,angle);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,type);

		distorter.apply(input,output);
	}

		/**
	 * Rotates the image using the specified interpolation.  The rotation is performed
	 * around the specified center of rotation in the input image.
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param interp The interpolation algorithm which is to be used.
	 * @param centerX Center of rotation in input image coordinates.
	 * @param centerY Center of rotation in input image coordinates.
	 * @param angle Angle of rotation in radians.
	 */
	public static <T extends ImageBase>
	void rotate( T input , T output , InterpolatePixel<T> interp ,
				 float centerX , float centerY , float angle ) {
		Class<T> inputType = (Class<T>)input.getClass();

		PixelTransform model = DistortSupport.transformRotate(centerX,centerY,angle);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,interp);

		distorter.apply(input,output);
	}

}
