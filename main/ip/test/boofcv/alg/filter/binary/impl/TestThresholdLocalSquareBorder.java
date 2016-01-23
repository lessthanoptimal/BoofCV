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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestThresholdLocalSquareBorder {

	Random rand = new Random(100);

	@Test
	public void compareToNaive() {
		int numPixelValues = 100;
		int regionWidths[] = new int[]{4,5,10,13};
		int minSpread = 75;

		ImageUInt8 input = new ImageUInt8(100,121);
		ImageMiscOps.fillUniform(input,rand,0,numPixelValues);

		boolean direction[] = new boolean[]{true,false};

		ImageUInt8 expected = new ImageUInt8(input.width,input.height);
		ImageUInt8 found = new ImageUInt8(input.width,input.height);

		for( boolean d : direction ) {
			for( int regionWidth : regionWidths ) {
				ThresholdLocalSquareBorderNaive naive = new ThresholdLocalSquareBorderNaive(d, regionWidth, numPixelValues, minSpread, 0.05, 0.95);
				ThresholdLocalSquareBorder alg = new ThresholdLocalSquareBorder(d, regionWidth, numPixelValues, minSpread, 0.05, 0.95);

				naive.process(input, expected);
				alg.process(input, found);

//			BoofTesting.printDiffBinary(expected,found);
				BoofTesting.assertEquals(expected, found, 0);
			}
		}
	}

	@Test
	public void subimage() {
		int numPixelValues = 100;
		int regionWidths[] = new int[]{4,5,10,13};
		int minSpread = 75;

		ImageUInt8 input = new ImageUInt8(100,121);
		ImageMiscOps.fillUniform(input,rand,0,numPixelValues);

		boolean direction[] = new boolean[]{true,false};

		ImageUInt8 found = new ImageUInt8(input.width,input.height);

		for( boolean d : direction ) {
			for( int regionWidth : regionWidths ) {
				BoofTesting.checkSubImage(this,"subimage",true,d,numPixelValues, regionWidth, minSpread,input,found);
			}
		}
	}

	public void subimage( boolean direction, int numPixelValues, int regionWidth , int minSpread , ImageUInt8 input , ImageUInt8 found ) {
		ThresholdLocalSquareBorder alg = new ThresholdLocalSquareBorder(direction, regionWidth, numPixelValues, minSpread, 0.05, 0.95);
		alg.process(input, found);

		ImageUInt8 expected = new ImageUInt8(input.width,input.height);
		ThresholdLocalSquareBorderNaive naive = new ThresholdLocalSquareBorderNaive(direction, regionWidth, numPixelValues, minSpread, 0.05, 0.95);
		naive.process(input, expected);

//		BoofTesting.printDiffBinary(expected,found);
		BoofTesting.assertEquals(expected, found, 0);
	}

	@Test
	public void initializeHistogram() {
		fail("implement");
	}

	@Test
	public void updateHistogramRight() {
		fail("implement");
	}

	@Test
	public void updateHistogramDown() {
		fail("implement");
	}

	@Test
	public void findPercentiles() {
		fail("implement");
	}

	/**
	 * Make sure it handles the case correctly when the upper and lower percentiles should be the same index value
	 */
	@Test
	public void findPercentiles_sameValue() {
		fail("implement");
	}

}
