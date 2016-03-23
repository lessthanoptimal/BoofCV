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

package boofcv.alg.feature.detect.interest;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSiftScaleSpace {

	Random rand = new Random(234);

	/**
	 * Checks to see if the first image in each octave has the expected amount of image blur
	 */
	@Test
	public void checkOctaveBlur() {

		GrayF32 original = new GrayF32(300,340);
		GImageMiscOps.fillUniform(original,rand,0,100);

		GrayF32 expected = new GrayF32(300,340);

		float sigma0 = 1.6f;

		int lastOctave = 5;
		for (int firstOctave = -1; firstOctave < 2; firstOctave++) {

			SiftScaleSpace alg = new SiftScaleSpace(firstOctave,lastOctave,2,sigma0);

			alg.initialize(original);

			for (int i = firstOctave; i <= lastOctave; i++) {
				float sigma = (float)(sigma0*Math.pow(2,i));

				GBlurImageOps.gaussian(original, expected, sigma, -1, null);

				double averageError = compareImage(expected, alg.getImageScale(0), i);

				assertTrue("first "+firstOctave+" oct "+i+" error = "+averageError,averageError<2);
				assertTrue(i < lastOctave == alg.computeNextOctave());
			}
		}
	}

	/**
	 * Test the blur factor at each scale between the octaves
	 */
	@Test
	public void checkScaleBlur() {
		GrayF32 original = new GrayF32(300,340);
		GImageMiscOps.fillUniform(original,rand,0,100);

		GrayF32 expected = new GrayF32(300,340);

		float sigma0 = 1.6f;
		SiftScaleSpace alg = new SiftScaleSpace(0,3,2,sigma0);

		alg.initialize(original);
		assertEquals(5,alg.getNumScaleImages());

		for (int i = 0; i < 5; i++) {
			double sigma = alg.computeSigmaScale(i);

			GBlurImageOps.gaussian(original, expected, sigma, -1, null);

			double averageError = compareImage(expected, alg.getImageScale(i), 0);

			assertTrue(" scale "+i+" error = "+averageError,averageError<2);
		}
	}

	@Test
	public void computeSigmaScale() {
		SiftScaleSpace alg = new SiftScaleSpace(-1,4,3,1.6);
		alg.initialize(new GrayF32(60,50));

		double k = Math.pow(2.0,1.0/3.0);
		for (int i = 0; i < 5; i++) {
			assertEquals(0.8*Math.pow(k,i),alg.computeSigmaScale(i),1e-8);
		}
		alg.computeNextOctave();
		for (int i = 0; i < 5; i++) {
			assertEquals(1.6*Math.pow(k,i),alg.computeSigmaScale(i),1e-8);
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
	private double compareImage(GrayF32 expected, GrayF32 found, int octave) {
		double averageError = 0;
		if( found.width > expected.width ) {
			int scale = (int)Math.pow(2,-octave);

			for (int y = 0; y < expected.height; y++) {
				for (int x = 0; x < expected.width; x++) {
					float f = found.get(x*scale,y*scale);
					float e = expected.get(x,y);
					averageError += Math.abs(e-f);
				}
			}
			averageError /= expected.width*expected.height;
		} else {
			int scale = (int)Math.pow(2,octave);

			for (int y = 0; y < found.height; y++) {
				for (int x = 0; x < found.width; x++) {
					float f = found.get(x,y);
					float e = expected.get(x*scale,y*scale);
					averageError += Math.abs(e-f);
				}
			}
			averageError /= found.width*found.height;
		}
		return averageError;
	}

	/**
	 * The number of octaves would make the input image too small
	 */
	@Test
	public void checkImageTooSmallForOctaves() {
		SiftScaleSpace alg = new SiftScaleSpace(0,5,3,1.6);

		alg.initialize(new GrayF32(20,20));

		assertTrue(alg.computeNextOctave());
		assertTrue(alg.computeNextOctave());
		assertFalse(alg.computeNextOctave());


	}
}