/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSiftScaleSpace2 {

	Random rand = new Random(234);

	/**
	 * Checks to see if the first image in each octave has the expected amount of image blur
	 */
	@Test
	public void checkOctaveBlur() {

		ImageFloat32 original = new ImageFloat32(300,340);
		GImageMiscOps.fillUniform(original,rand,0,100);

		ImageFloat32 expected = new ImageFloat32(300,340);

		float sigma0 = 1.6f;

		int lastOctave = 5;
		for (int firstOctave = -1; firstOctave < 2; firstOctave++) {

			SiftScaleSpace2 alg = new SiftScaleSpace2(firstOctave,lastOctave,2,sigma0);

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
		ImageFloat32 original = new ImageFloat32(300,340);
		GImageMiscOps.fillUniform(original,rand,0,100);

		ImageFloat32 expected = new ImageFloat32(300,340);

		float sigma0 = 1.6f;
		SiftScaleSpace2 alg = new SiftScaleSpace2(0,3,2,sigma0);

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
		SiftScaleSpace2 alg = new SiftScaleSpace2(-1,4,3,1.6);
		alg.initialize(new ImageFloat32(60,50));

		double k = Math.pow(2.0,1.0/3.0);
		for (int i = 0; i < 5; i++) {
			assertEquals(0.8*Math.pow(k,i),alg.computeSigmaScale(i),1e-8);
		}
		alg.computeNextOctave();
		for (int i = 0; i < 5; i++) {
			assertEquals(1.6*Math.pow(k,i),alg.computeSigmaScale(i),1e-8);
		}

	}

	@Test
	public void scaleImageUp() {
		ImageFloat32 input = new ImageFloat32(15, 8);
		ImageFloat32 output = new ImageFloat32(1, 1);
		ImageMiscOps.fillUniform(input, rand, -10, 10);

		SiftScaleSpace2 alg = new SiftScaleSpace2(0,4,3,1.6);

		alg.scaleImageUp(input,output,2);

		assertEquals(30,output.width );
		assertEquals(16,output.height);

		InterpolatePixelS<ImageFloat32> interp = FactoryInterpolation.
				createPixelS(0,255, TypeInterpolate.BILINEAR, BorderType.EXTENDED,ImageFloat32.class);

		interp.setImage(input);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				float inputY = y/2.0f;
				float inputX = x/2.0f;

				float expected = interp.get(inputX,inputY);
				float found = output.get(x,y);

				assertEquals(expected,found,1e-4);
			}
		}
	}

	@Test
	public void scaleDown2() {
		int sizes [] = new int[]{30,31};

		for( int width : sizes ) {
			int height = width*2/3 + width%2;
			ImageFloat32 input = new ImageFloat32(width, height);
			ImageFloat32 output = new ImageFloat32(1, 1);

			ImageMiscOps.fillUniform(input, rand, -10, 10);
			SiftScaleSpace2.scaleDown2(input, output);

			assertEquals(width/2, output.width);
			assertEquals(height/2, output.height);

			for (int y = 0; y < output.height; y++) {
				for (int x = 0; x < output.width; x++) {
					assertEquals(input.get(x * 2, y * 2), output.get(x, y), 1e-4);
				}
			}
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
	private double compareImage(ImageFloat32 expected, ImageFloat32 found, int octave) {
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
}