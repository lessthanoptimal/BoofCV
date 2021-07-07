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
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_F32;
import pabeles.concurrency.GrowArray;

/**
 * <p>Several related algorithms based off the Niblack's [1] paper which are intended for use in thresholding
 * images as a preprocessing step for OCR. All algorithms are based on local image statistics.</p>
 * <ol>
 *     <li>Niblack: T(x,y) = m(x,y) * [ 1 + k * (s(x,y)/R - 1)]</li>
 *     <li>Sauvola: T(x,y) = m(x,y) * [ 1 + k * (s(x,y)/R - 1)]</li>
 *     <li>Wolf-Jolion: T(x,y) = m(x,y) * [ 1 + k * (s(x,y)/R - 1)]</li>
 * </ol>
 * <p>
 * where T(x,y) is the pixel's threshold, m(x,y) is the local mean, s(x,y) is the local deviation,
 * R is dynamic range of standard deviation, and k is a user specified threshold.
 * </p>
 *
 * <p>There are two tuning parameters 'k' a positive number and the the 'radius' of the local region. Recommended
 * values:</p>
 * <ul>
 *     <li>Niblack: k=0.3 and radius=15</li>
 *     <li>Sauvola: k=0.3 and radius=15</li>
 *     <li>Wolf-Jolion: k=0.5 and radius=15</li>
 * </ul>
 *
 * <p>
 *  [1] W.Niblack, An Introduction to Digital Image Processing. Prentice Hall, Englewood Cliffs, (1986).<br>
 *  [2] Sauvola, Jaakko, and Matti Pietikäinen. "Adaptive document image binarization."
 *  Pattern recognition 33.2 (2000): 225-236.<br>
 *  [3] C. Wolf, J-M. Jolion, “Extraction and Recognition of Artificial Text in Multimedia Documents”, Pattern Analysis
 * and Applications, 6(4):309-326, (2003)<br>
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdNiblackFamily implements InputToBinary<GrayF32> {

	/** user specified threshold */
	@Getter @Setter float k;

	/** size of local region */
	@Getter @Setter ConfigLength width;

	/** should it threshold down or up */
	@Getter @Setter boolean down;

	/** Which variant in the family is being applied */
	@Getter Variant variant;

	// storage for intermediate results
	GrayF32 inputPow2 = new GrayF32(1, 1); // I^2
	GrayF32 inputMean = new GrayF32(1, 1); // local mean of I
	GrayF32 inputMeanPow2 = new GrayF32(1, 1); // pow2 of local mean of I
	GrayF32 inputPow2Mean = new GrayF32(1, 1); // local mean of I^2
	GrayF32 stdev = new GrayF32(1, 1); // computed standard deviation

	GrayF32 tmp = new GrayF32(1, 1); // work space
	GrowArray<DogArray_F32> work = new GrowArray<>(DogArray_F32::new);

	// Maximum stdev across entire image
	float maxStdev;
	// Minimum pixel intensity value
	float minItensity;

	// The thresholding operation
	Threshold op;

	/**
	 * Configures the algorithm.
	 *
	 * @param width size of local region. Try 31
	 * @param k User specified threshold adjustment factor. Must be positive. Try 0.3
	 * @param down Threshold down or up
	 */
	public ThresholdNiblackFamily( ConfigLength width, float k, boolean down, Variant variant ) {
		this.k = k;
		this.width = width;
		this.down = down;
		this.variant = variant;

		op = switch (variant) {
			case SAUVOLA -> new Sauvola();
			case NIBLACK -> new Niblack();
			case WOLF_JOLION -> new WolfJolion();
		};
	}

	/**
	 * Converts the input image into a binary image.
	 *
	 * @param input Input image. Not modified.
	 * @param output Output binary image. Modified.
	 */
	@Override
	public void process( GrayF32 input, GrayU8 output ) {
		inputPow2.reshape(input.width, input.height);
		inputMean.reshape(input.width, input.height);
		inputMeanPow2.reshape(input.width, input.height);
		inputPow2Mean.reshape(input.width, input.height);
		stdev.reshape(input.width, input.height);
		tmp.reshape(input.width, input.height);
		inputPow2.reshape(input.width, input.height);

		int radius = width.computeI(Math.min(input.width, input.height))/2;

		// mean of input image = E[X]
		BlurImageOps.mean(input, inputMean, radius, tmp, work);

		// standard deviation = sqrt( E[X^2] + E[X]^2)
		PixelMath.pow2(input, inputPow2);
		BlurImageOps.mean(inputPow2, inputPow2Mean, radius, tmp, work);
		PixelMath.pow2(inputMean, inputMeanPow2);
		PixelMath.subtract(inputPow2Mean, inputMeanPow2, stdev);
		PixelMath.sqrt(stdev, stdev);

		if (variant == Variant.SAUVOLA || variant == Variant.WOLF_JOLION)
			maxStdev = ImageStatistics.max(stdev);

		if (variant == Variant.WOLF_JOLION)
			minItensity = ImageStatistics.min(input);

		applyThresholding(input, output);
	}

	protected void applyThresholding( GrayF32 input, GrayU8 output ) {
		if (down) {
			for (int y = 0; y < input.height; y++) {
				int i = y*stdev.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float threshold = op.compute(inputMean.data[i], stdev.data[i]);
					output.data[indexOut++] = (byte)(input.data[indexIn++] <= threshold ? 1 : 0);
				}
			}
		} else {
			for (int y = 0; y < input.height; y++) {
				int i = y*stdev.width;
				int indexIn = input.startIndex + y*input.stride;
				int indexOut = output.startIndex + y*output.stride;

				for (int x = 0; x < input.width; x++, i++) {
					float threshold = op.compute(inputMean.data[i], stdev.data[i]);
					output.data[indexOut++] = (byte)(input.data[indexIn++] >= threshold ? 1 : 0);
				}
			}
		}
	}

	/**
	 * Threshold operation. Computes the threshold given the mean and standard deviation
	 */
	interface Threshold {
		float compute( float mean, float stdev );
	}

	class Niblack implements Threshold {
		@Override final public float compute( final float mean, final float stdev ) {
			return mean + k*stdev;
		}
	}

	class Sauvola implements Threshold {
		@Override final public float compute( final float mean, final float stdev ) {
			// NOTE: R=maxStdev. Some papers which describe Sauvola have R=128. However in the 1999 paper it says
			// R is equal to the "dynamic range of stdev". Maybe an earlier paper had it as 128?
			return mean*(1.0f + k*(stdev/maxStdev - 1.0f));
		}
	}

	class WolfJolion implements Threshold {
		@Override final public float compute( final float mean, final float stdev ) {
			return mean + k*(stdev/maxStdev - 1.0f)*(mean - minItensity);
		}
	}

	@Override public ImageType<GrayF32> getInputType() {
		return ImageType.SB_F32;
	}

	/** Which variant of this family is computed */
	public enum Variant {
		NIBLACK,
		SAUVOLA,
		WOLF_JOLION
	}
}
