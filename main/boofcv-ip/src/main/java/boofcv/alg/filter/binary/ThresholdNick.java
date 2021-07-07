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

package boofcv.alg.filter.binary;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray_F32;
import pabeles.concurrency.GrowArray;
//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Based off the NICK algorithm described in [1] this is a thresholding algorithm intended for use on
 * low quality ancient documents. A sliding windows approach is employed and is inspired by Niblack. It's
 * designed to better handled "white" and ligh page images by shifting the threshold down.
 * </p>
 *
 * <p>
 * [1] Khurshid, Khurram, et al. "Comparison of Niblack inspired Binarization methods for ancient documents."
 * Document Recognition and Retrieval XVI. Vol. 7247. International Society for Optics and Photonics, 2009.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdNick implements InputToBinary<GrayF32> {

	// user specified threshold. Niblack factor
	// [1] recommends -0.1 to -0.2
	float k;
	// size of local region
	ConfigLength width;
	// should it threshold down or up
	boolean down;

	// storage for intermediate results
	GrayF32 imageI2 = new GrayF32(1, 1); // I^2
	GrayF32 meanImage = new GrayF32(1, 1); // local mean of I
	GrayF32 meanI2 = new GrayF32(1, 1);

	GrayF32 tmp = new GrayF32(1, 1); // work space
	GrowArray<DogArray_F32> work = new GrowArray<>(DogArray_F32::new);

	/**
	 * Configures the algorithm.
	 *
	 * @param width size of local region. Try 31
	 * @param k The Niblack factor. Recommend -0.1 to -0.2
	 * @param down Threshold down or up
	 */
	public ThresholdNick( ConfigLength width, float k, boolean down ) {
		this.k = k;
		this.width = width;
		this.down = down;
	}

	/**
	 * Converts the input image into a binary image.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output binary image. Modified.
	 */
	@Override
	public void process( GrayF32 input, GrayU8 output ) {
		output.reshape(input.width, input.height);
		imageI2.reshape(input.width, input.height);
		meanImage.reshape(input.width, input.height);
		meanI2.reshape(input.width, input.height);
		tmp.reshape(input.width, input.height);
		imageI2.reshape(input.width, input.height);

		int radius = width.computeI(Math.min(input.width, input.height))/2;

		float NP = (radius*2 + 1)*(radius*2 + 1);

		// mean of input image = E[X]
		BlurImageOps.mean(input, meanImage, radius, tmp, work);

		// Compute I^2
		PixelMath.pow2(input, imageI2);

		// Compute local mean of I^2
		BlurImageOps.mean(imageI2, meanI2, radius, tmp, work);

		if (down) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for (int y = 0; y < input.height; y++) {
				int i = y*meanI2.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float mean = meanImage.data[i];
					float A = meanI2.data[i] - (mean*mean/NP);

					// threshold = mean + k*sqrt( A )
					float threshold = mean + k*(float)Math.sqrt(A);
					output.data[indexOut++] = (byte)(input.data[indexIn++] <= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
			for (int y = 0; y < input.height; y++) {
				int i = y*meanI2.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float mean = meanImage.data[i];
					float A = meanI2.data[i] - (mean*mean/NP);

					// threshold = mean + k*sqrt( A )
					float threshold = mean + k*(float)Math.sqrt(A);
					output.data[indexOut++] = (byte)(input.data[indexIn++] >= threshold ? 1 : 0);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	@Override
	public ImageType<GrayF32> getInputType() {
		return ImageType.SB_F32;
	}

	public float getK() {
		return k;
	}

	public void setK( float k ) {
		this.k = k;
	}

	public ConfigLength getWidth() {
		return width;
	}

	public void setWidth( ConfigLength width ) {
		this.width = width;
	}

	public boolean isDown() {
		return down;
	}

	public void setDown( boolean down ) {
		this.down = down;
	}
}
