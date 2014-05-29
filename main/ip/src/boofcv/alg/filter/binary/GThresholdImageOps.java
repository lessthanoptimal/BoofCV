/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.struct.image.*;


/**
 * Weakly typed version of {@link ThresholdImageOps}.
 *
 * @author Peter Abeles
 */
public class GThresholdImageOps {

	/**
	 * Applies a global threshold across the whole image.  If 'down' is true, then pixels with values <=
	 * to 'threshold' are set to 1 and the others set to 0.  If 'down' is false, then pixels with values >=
	 * to 'threshold' are set to 1 and the others set to 0.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Binary output image. If null a new image will be declared. Modified.
	 * @param threshold threshold value.
	 * @param down If true then the inequality <= is used, otherwise if false then >= is used.
	 * @return Output image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 threshold( T input , ImageUInt8 output ,
						  double threshold , boolean down )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.threshold((ImageFloat32)input,output,(float)threshold,down);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.threshold((ImageUInt8)input,output,(int)threshold,down);
		} else if( input instanceof ImageUInt16) {
			return ThresholdImageOps.threshold((ImageUInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt16) {
			return ThresholdImageOps.threshold((ImageSInt16)input,output,(int)threshold,down);
		} else if( input instanceof ImageSInt32 ) {
			return ThresholdImageOps.threshold((ImageSInt32)input,output,(int)threshold,down);
		} else if( input instanceof ImageFloat64 ) {
			return ThresholdImageOps.threshold((ImageFloat64)input,output,threshold,down);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using an adaptive threshold that is computed using a local square region centered
	 * on each pixel.  The threshold is equal to the average value of the surrounding pixels plus the bias.
	 * If down is true then b(x,y) = I(x,y) <= T(x,y) + bias ? 1 : 0.  Otherwise
	 * b(x,y) = I(x,y) >= T(x,y) + bias ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results.  If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image.  If null it will be declared internally.
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace.  Can be null
	 * @param work2 (Optional) Internal workspace.  Can be null
	 * @return Thresholded image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 adaptiveSquare( T input , ImageUInt8 output ,
							   int radius , double bias , boolean down, T work1 , T work2 )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.adaptiveSquare((ImageFloat32) input, output, radius, (float) bias, down,
					(ImageFloat32) work1, (ImageFloat32) work2);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.adaptiveSquare((ImageUInt8) input, output, radius, (int) bias, down,
					(ImageUInt8) work1, (ImageUInt8) work2);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Thresholds the image using an adaptive threshold that is computed using a local square region centered
	 * on each pixel.  The threshold is equal to the gaussian weighted sum of the surrounding pixels plus the bias.
	 * If down is true then b(x,y) = I(x,y) <= T(x,y) + bias ? 1 : 0.  Otherwise
	 * b(x,y) = I(x,y) >= T(x,y) + bias ? 0 : 1
	 * </p>
	 *
	 * <p>
	 * NOTE: Internally, images are declared to store intermediate results.  If more control is needed over memory
	 * call the type specific function.
	 * </p>
	 *
	 * @param input Input image.
	 * @param output (optional) Output binary image.  If null it will be declared internally.
	 * @param radius Radius of square region.
	 * @param bias Bias used to adjust threshold
	 * @param down Should it threshold up or down.
	 * @param work1 (Optional) Internal workspace.  Can be null
	 * @param work2 (Optional) Internal workspace.  Can be null
	 * @return Thresholded image.
	 */
	public static <T extends ImageSingleBand>
	ImageUInt8 adaptiveGaussian( T input , ImageUInt8 output ,
								 int radius , double bias , boolean down ,
								 T work1 , T work2 )
	{
		if( input instanceof ImageFloat32 ) {
			return ThresholdImageOps.adaptiveGaussian((ImageFloat32) input, output, radius, (float) bias, down,
					(ImageFloat32) work1, (ImageFloat32) work2);
		} else if( input instanceof ImageUInt8 ) {
			return ThresholdImageOps.adaptiveGaussian((ImageUInt8) input, output, radius, (int) bias, down,
					(ImageUInt8) work1, (ImageUInt8) work2);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getSimpleName());
		}
	}
}
