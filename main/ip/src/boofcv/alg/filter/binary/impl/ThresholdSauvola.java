/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;

/**
 * <p>
 * Intended for use as a preprocessing step in OCR it computes a binary image from an input gray image.  It's
 * an adaptive algorithm and uses the local mean and standard deviation, as is shown in the equation below:<br>
 * T(x,y) = m(x,y) * [ 1 + k * (s(x,y)/R - 1)]<br>
 * where T(x,y) is the pixel's threshold, m(x,y) is the local mean, s(x,y) is the local deviation,
 * R is dynamic range of standard deviation, and k is a user specified threshold.
 * </p>
 *
 * <p>
 * There are two tuning parameters 'k' a positive number and the the 'radius' of the local region.  Recommended
 * values are k=0.3 and radius=15.  These were found by tuning against a set of text.
 * </p>
 *
 * @author Peter Abeles
 */
public class ThresholdSauvola {

	// user specified threshold
	float k;
	// size of local region
	int radius;
	// should it threshold down or up
	boolean down;

	// storage for intermediate results
	GrayF32 inputPow2 = new GrayF32(1,1); // I^2
	GrayF32 inputMean = new GrayF32(1,1); // local mean of I
	GrayF32 inputMeanPow2 = new GrayF32(1,1); // pow2 of local mean of I
	GrayF32 inputPow2Mean = new GrayF32(1,1); // local mean of I^2
	GrayF32 stdev = new GrayF32(1,1); // computed standard deviation

	GrayF32 tmp = new GrayF32(1,1); // work space

	/**
	 * Configures the algorithm.
	 * @param radius size of local radius.  Try 15
	 * @param k User specified threshold adjustment factor.  Must be positive. Try 0.3
	 * @param down Threshold down or up
	 */
	public ThresholdSauvola(int radius, float k, boolean down) {
		this.k = k;
		this.radius = radius;
		this.down = down;
	}

	/**
	 * Converts the input image into a binary image.
	 *
	 * @param input Input image.  Not modified.
	 * @param output Output binary image.  Modified.
	 */
	public void process(GrayF32 input , GrayU8 output ) {
		inputPow2.reshape(input.width,input.height);
		inputMean.reshape(input.width,input.height);
		inputMeanPow2.reshape(input.width,input.height);
		inputPow2Mean.reshape(input.width,input.height);
		stdev.reshape(input.width,input.height);
		tmp.reshape(input.width,input.height);
		inputPow2.reshape(input.width,input.height);

		// mean of input image = E[X]
		BlurImageOps.mean(input, inputMean, radius, tmp);

		// standard deviation = sqrt( E[X^2] + E[X]^2)
		PixelMath.pow2(input, inputPow2);
		BlurImageOps.mean(inputPow2,inputPow2Mean,radius,tmp);
		PixelMath.pow2(inputMean,inputMeanPow2);
		PixelMath.subtract(inputPow2Mean, inputMeanPow2, stdev);
		PixelMath.sqrt(stdev, stdev);

		float R = ImageStatistics.max(stdev);

		if( down ) {
			for (int y = 0; y < input.height; y++) {
				int i = y * stdev.width;
				int indexIn = input.startIndex + y * input.stride;
				int indexOut = output.startIndex + y * output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					// threshold = mean.*(1 + k * ((deviation/R)-1));
					float threshold = inputMean.data[i] * (1.0f + k * (stdev.data[i] / R - 1.0f));
					output.data[indexOut++] = (byte) (input.data[indexIn++] <= threshold ? 1 : 0);
				}
			}
		} else {
			for (int y = 0; y < input.height; y++) {
				int i = y * stdev.width;
				int indexIn = input.startIndex + y * input.stride;
				int indexOut = output.startIndex + y * output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					// threshold = mean.*(1 + k * ((deviation/R)-1));
					float threshold = inputMean.data[i] * (1.0f + k * (stdev.data[i] / R - 1.0f));
					output.data[indexOut++] = (byte) (input.data[indexIn++] >= threshold ? 1 : 0);
				}
			}
		}
	}

	public float getK() {
		return k;
	}

	public void setK(float k) {
		this.k = k;
	}

	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public boolean isDown() {
		return down;
	}

	public void setDown(boolean down) {
		this.down = down;
	}
}
