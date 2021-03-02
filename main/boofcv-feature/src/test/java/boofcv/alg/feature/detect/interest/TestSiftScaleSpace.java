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

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSiftScaleSpace extends BoofStandardJUnit {
	/**
	 * In a full resolution image apply the blur that each image in the scale space should
	 * have applied to it and compare the two images. The full resolution one will be sub-sampled
	 */
	@Test void checkBlurAppliedToImages() {
		GrayF32 original = new GrayF32(300, 340);
		GImageMiscOps.fillUniform(original, rand, 0, 100);

		GrayF32 expected = new GrayF32(300, 340);

		float sigma0 = 1.6f;

		int lastOctave = 3;
		int numScales = 2;

		// Change the first active in the scale spaces
		for (int firstOctave = -1; firstOctave <= 1; firstOctave++) {
			var alg = new SiftScaleSpace(firstOctave, lastOctave, numScales, sigma0);
			alg.process(original);
			for (int octaveIdx = 0; octaveIdx < alg.getTotalOctaves(); octaveIdx++) {
				int octave = octaveIdx + firstOctave;
				SiftScaleSpace.Octave o = alg.octaves[octaveIdx];

				// Look at the amount of blur in scale image at the first octave
				for (int scaleIdx = 0; scaleIdx < o.scales.length; scaleIdx++) {
					float sigma = (float)(sigma0*Math.pow(2.0, octave + scaleIdx/(double)numScales));

					GBlurImageOps.gaussian(original, expected, sigma, -1, null);

					double averageError = compareImage(expected, o.scales[scaleIdx], octave);

					assertTrue(averageError < 2, "first "+firstOctave+" octave " + octave + " scale " + scaleIdx + " error = " + averageError);
				}
			}
		}
	}

	@Test void computeSigmaScale() {
		SiftScaleSpace alg = new SiftScaleSpace(-1, 4, 3, 1.6);
		alg.process(new GrayF32(60, 50));

		double k = Math.pow(2.0, 1.0/3.0);
		for (int scaleIdx = 0; scaleIdx < 5; scaleIdx++) {
			assertEquals(0.8*Math.pow(k, scaleIdx), alg.computeSigmaScale(-1, scaleIdx), 1e-8);
			assertEquals(1.6*Math.pow(k, scaleIdx), alg.computeSigmaScale(0, scaleIdx), 1e-8);
		}
	}

	/**
	 * Computes the average abs difference across the two images
	 *
	 * @param expected Input image
	 * @param found Found at expected resolution for the octave
	 * @param octave Which octave its at
	 * @return average error across the smaller image
	 */
	private double compareImage( GrayF32 expected, GrayF32 found, int octave ) {
		double averageError = 0;
		if (found.width > expected.width) {
			int scale = (int)Math.pow(2, -octave);

			for (int y = 0; y < expected.height; y++) {
				for (int x = 0; x < expected.width; x++) {
					float f = found.get(x*scale, y*scale);
					float e = expected.unsafe_get(x, y);
					averageError += Math.abs(e - f);
				}
			}
			averageError /= expected.width*expected.height;
		} else {
			double scale = Math.pow(2, octave);

			int total = 0;
			for (int y = 0; y < found.height; y++) {
				int yy = (int)(y*scale);
				for (int x = 0; x < found.width; x++) {
					int xx = (int)(x*scale);
					if (!expected.isInBounds(xx,yy))
						continue;
					float f = found.unsafe_get(x, y);
					float e = expected.get(xx,yy);
					averageError += Math.abs(e - f);
					total++;
				}
			}
			averageError /= total;
		}
		return averageError;
	}

	/**
	 * The number of octaves would make the input image too small. See if it blows up.
	 */
	@Test void checkImageTooSmallForOctaves() {
		SiftScaleSpace alg = new SiftScaleSpace(0, 5, 3, 1.6);

		alg.process(new GrayF32(20, 20));
	}
}
