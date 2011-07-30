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

import gecv.alg.interpolate.InterpolatePixel;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;


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
	 * factor is determined independently using the input and output image's width and height.
	 *
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param interpolation Function used to interpolate pixel values.
	 */
	public static void scale( ImageFloat32 input , ImageFloat32 output ,
							  InterpolatePixel<ImageFloat32> interpolation )
	{
		float ratioW = (float)input.width/(float)output.width;
		float ratioH = (float)input.height/(float)output.height;

		interpolation.setImage(input);

		for( int y = 0; y < output.height; y++ ) {
			int indexDst = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ ) {
				output.data[indexDst++] = interpolation.get(x*ratioW,y*ratioH);
			}
		}
	}

	/**
	 * Rescales the input image and writes the results into the output image.  The scale
	 * factor is determined independently using the input and output image's width and height.
	 *
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param interpolation Function used to interpolate pixel values.
	 */
	public static void scale( ImageUInt8 input , ImageUInt8 output ,
							  InterpolatePixel<ImageUInt8> interpolation )
	{
		float ratioW = (float)input.width/(float)output.width;
		float ratioH = (float)input.height/(float)output.height;

		interpolation.setImage(input);

		for( int y = 0; y < output.height; y++ ) {
			int indexDst = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ ) {
				output.data[indexDst++] = (byte)interpolation.get(x*ratioW,y*ratioH);
			}
		}
	}

	/**
	 * Rescales the input image and writes the results into the output image.  The scale
	 * factor is determined independently using the input and output image's width and height.
	 *
	 * @param input Input image. Not modified.
	 * @param output Rescaled input image. Modified.
	 * @param interpolation Function used to interpolate pixel values.
	 */
	public static void scale( ImageSInt16 input , ImageSInt16 output ,
							  InterpolatePixel<ImageSInt16> interpolation )
	{
		float ratioW = (float)input.width/(float)output.width;
		float ratioH = (float)input.height/(float)output.height;

		interpolation.setImage(input);

		for( int y = 0; y < output.height; y++ ) {
			int indexDst = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ ) {
				output.data[indexDst++] = (short)interpolation.get(x*ratioW,y*ratioH);
			}
		}
	}
}
