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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.DogArray;

import java.util.Objects;

/**
 * Precomputes the gradient for all scales in the scale-space and saves them in a list. Since it saves the entire
 * scale space it can take up a bit of memory, but allows quick random look up of images.
 *
 * @author Peter Abeles
 */
public class UnrollSiftScaleSpaceGradient {
	// storage for all possible scales
	DogArray<ImageScale> scales = new DogArray<>(ImageScale::new);

	// used to compute the image gradient
	ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.three(GrayF32.class, null);

	private int numScales;

	/** Initializes data structures given the scale-space */
	public void initialize( SiftScaleSpace scaleSpace ) {
		numScales = scaleSpace.getNumScales();
		int numScales = scaleSpace.getNumScales()*scaleSpace.getTotalOctaves();
		scales.reserve(numScales);
		scales.reset();

		for (int octaveIdx = 0; octaveIdx < scaleSpace.octaves.length; octaveIdx++) {
			int octave = octaveIdx + scaleSpace.firstOctave;

			for (int i = 0; i < scaleSpace.getNumScales(); i++) {
				double sigma = scaleSpace.computeSigmaScale(octave, i);
				double pixelCurrentToInput = scaleSpace.pixelScaleCurrentToInput(octave);

				ImageScale scale = scales.grow();
				scale.imageToInput = pixelCurrentToInput;
				scale.sigma = sigma;
			}
		}
	}

	/**
	 * Sets the input image. Scale-space is computed and unrolled from this image
	 */
	public void process( SiftScaleSpace scaleSpace ) {
		int scaleIdx = 0;
		for (int octaveIdx = 0; octaveIdx < scaleSpace.octaves.length; octaveIdx++) {
			// Don't process what is invalid
			if (scaleSpace.isOctaveTooSmall(octaveIdx))
				break;

			SiftScaleSpace.Octave o = scaleSpace.octaves[octaveIdx];

			for (int i = 0; i < scaleSpace.getNumScales(); i++, scaleIdx++) {
				// See comment in ImageScale for why there's an offset below
				GrayF32 scaleImage = o.scales[i + 1];
				ImageScale scale = scales.get(scaleIdx);
				gradient.process(scaleImage, scale.derivX, scale.derivY);
			}
		}
	}

	/**
	 * Looks up the image which is closest specified sigma
	 */
	public ImageScale lookup( double sigma ) {
		ImageScale best = null;
		double bestValue = Double.MAX_VALUE;

		for (int i = 0; i < scales.size(); i++) {
			ImageScale image = scales.get(i);
			double difference = Math.abs(sigma - image.sigma);
			if (difference < bestValue) {
				bestValue = difference;
				best = image;
			}
		}
		return Objects.requireNonNull(best);
	}

	public GrayF32 getDerivX( byte octaveIdx, byte scaleIdx ) {
		return scales.get(octaveIdx*numScales + scaleIdx).derivX;
	}

	public GrayF32 getDerivY( byte octaveIdx, byte scaleIdx ) {
		return scales.get(octaveIdx*numScales + scaleIdx).derivY;
	}

	/**
	 * The gradient for an image in the scale space.
	 *
	 * NOTE: The gradient for index i is computed from scaleIdx=i+1 to reflect the DoG image that the
	 * feature was detected inside of.
	 */
	public static class ImageScale {
		public GrayF32 derivX = new GrayF32(1, 1);
		public GrayF32 derivY = new GrayF32(1, 1);
		public double imageToInput;
		public double sigma;
	}
}
