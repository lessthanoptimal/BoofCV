/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageBase;


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
	 * @param interpType Which interpolation algorithm should be used.
	 */
	public static <T extends ImageBase>
	void scale( T input , T output , TypeInterpolate interpType ) {
		Class<T> inputType = (Class<T>)input.getClass();
		InterpolatePixel<T> interp = FactoryInterpolation.createPixel(0, 255, interpType, inputType);

		scale(input,output,interp);
	}

	public static <T extends ImageBase>
	void scale( T input , T output , InterpolatePixel<T> interp ) {
		Class<T> inputType = (Class<T>)input.getClass();

		PixelTransform_F32 model = DistortSupport.transformScale(output, input);
		ImageBorder<T> border = FactoryImageBorder.value(inputType, 0);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,interp, border);

		distorter.apply(input,output);
	}

	/**
	 * <p>
	 * Rotates the image using the specified interpolation type.  The rotation is performed
	 * around the specified center of rotation in the input image.
	 * </p>
	 *
	 * <p>
	 * Input coordinates (x,y) to output coordinate (x',y')<br>
	 * x' = x_c + c*(x-x_c) - s(y - y_c)<br>
	 * y' = y_c + s*(x-x_c) + c(y - y_c)
	 * </p>
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param interpType Which type of interpolation will be used.
	 * @param angleInputToOutput Angle of rotation in radians. From input to output, CCW rotation.
	 */
	public static <T extends ImageBase>
	void rotate( T input , T output , TypeInterpolate interpType , float angleInputToOutput ) {

		Class<T> inputType = (Class<T>)input.getClass();
		InterpolatePixel<T> interp = FactoryInterpolation.createPixel(0, 255, interpType, inputType);

		rotate(input, output, interp, angleInputToOutput);
	}

	/**
	 * <p>
	 * Rotates the image using the specified interpolation.  The rotation is performed
	 * around the specified center of rotation in the input image.
	 * </p>
	 *
	 * <p>
	 * Input coordinates (x,y) to output coordinate (x',y')<br>
	 * x' = x_c + c*(x-x_c) - s(y - y_c)<br>
	 * y' = y_c + s*(x-x_c) + c(y - y_c)
	 * </p>
	 *
	 * @param input Which which is being rotated.
	 * @param output The image in which the output is written to.
	 * @param interp The interpolation algorithm which is to be used.
	 * @param angleInputToOutput Angle of rotation in radians.  From input to output, CCW rotation.
	 */
	public static <T extends ImageBase>
	void rotate( T input , T output , InterpolatePixel<T> interp ,
				 float angleInputToOutput ) {
		Class<T> inputType = (Class<T>)input.getClass();

		float offX = 0;//(output.width+1)%2;
		float offY = 0;//(output.height+1)%2;

		PixelTransform_F32 model = DistortSupport.transformRotate(input.width/2,input.height/2,output.width/2-offX,output.height/2-offY,angleInputToOutput);
		ImageDistort<T> distorter = DistortSupport.createDistort(inputType,model,interp, FactoryImageBorder.value(inputType, 0));

		distorter.apply(input,output);
	}

}
