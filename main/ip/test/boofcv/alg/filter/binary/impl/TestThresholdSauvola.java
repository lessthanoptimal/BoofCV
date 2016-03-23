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

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestThresholdSauvola {

	Random rand = new Random(234);

	/**
	 * Provide it a simple input image with obvious thresholding.  There will be regions of white space
	 * which exceed its radius.
	 */
	@Test
	public void simple() {
		int radius = 5;
		GrayU8 expected = new GrayU8(30,35);

		for (int y = radius; y < expected.height-radius; y++) {
			expected.set(20,y,1);
			expected.set(21,y,1);
			expected.set(22,y,1);
		}

		GrayF32 input = new GrayF32(expected.width,expected.height);
		for (int i = 0; i < input.width * input.height; i++) {
			input.data[i] = expected.data[i] == 0 ? 255 : 0;
		}

		GrayU8 found = new GrayU8(expected.width,expected.height);

		ThresholdSauvola alg = new ThresholdSauvola(radius,0.5f,true);

		alg.process(input,found);

		BoofTesting.assertEqualsInner(expected, found, 0, radius, radius, false);

		alg.setDown(false);
		alg.process(input, found);
		BinaryImageOps.invert(expected, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, radius, radius, false);
	}

	@Test
	public void bruteForce() {
		int radius = 2;
		float k = 0.5f;
		checkBruteForce(10, 12, radius, k, true);
		checkBruteForce(10, 12, radius, k, false);
	}

	private void checkBruteForce(int w, int h, int radius, float k, boolean down) {
		GrayU8 expected = new GrayU8(w,h);
		GrayU8 found = new GrayU8(w,h);
		GrayF32 input = new GrayF32(w,h);
		ImageMiscOps.fillUniform(input, rand, 0, 200);

		GrayF32 mean = new GrayF32(w,h);
		GrayF32 stdev = new GrayF32(w,h);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float m = mean(input, x, y, radius);
				mean.set(x,y,m);
				stdev.set(x,y,stdev(input, m, x, y, radius));
			}
		}

		float R = ImageStatistics.max(stdev);

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float threshold = mean.get(x,y) * (1.0f + k * (stdev.get(x,y) / R - 1.0f));
				int out = down ? (input.get(x,y) <= threshold ? 1 : 0) : (input.get(x,y) >= threshold ? 1 : 0);
				expected.set(x,y,out);
			}
		}

		ThresholdSauvola alg = new ThresholdSauvola(radius,k,down);
		alg.process(input,found);

//		expected.printBinary();
//		System.out.println();
//		found.printBinary();

		BoofTesting.assertEquals(expected, found, 0);
	}

	private float mean(GrayF32 input , int c_x , int c_y , int radius ) {
		int x0 = c_x - radius;
		int x1 = x0 + radius*2 + 1;
		int y0 = c_y - radius;
		int y1 = y0 + radius*2 + 1;

		if( x0 < 0 ) x0 = 0;
		if( y0 < 0 ) y0 = 0;
		if( x1 > input.width ) x1 = input.width;
		if( y1 > input.height ) y1 = input.height;

		float total = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				total += input.get(x,y);
			}
		}

		return total/((x1-x0)*(y1-y0));
	}

	private float stdev(GrayF32 input , float mean, int c_x , int c_y , int radius ) {
		int x0 = c_x - radius;
		int x1 = x0 + radius*2 + 1;
		int y0 = c_y - radius;
		int y1 = y0 + radius*2 + 1;

		if( x0 < 0 ) x0 = 0;
		if( y0 < 0 ) y0 = 0;
		if( x1 > input.width ) x1 = input.width;
		if( y1 > input.height ) y1 = input.height;


		float total = 0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				float d = input.get(x,y) - mean;
				total += d*d;
			}
		}

		return (float)Math.sqrt(total/((x1-x0)*(y1-y0)));
	}
}