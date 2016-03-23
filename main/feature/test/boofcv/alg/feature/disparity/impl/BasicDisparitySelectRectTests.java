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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Basic tests for selecting disparity
 *
 * @author Peter Abeles
 */
public abstract class BasicDisparitySelectRectTests <ArrayData , D extends ImageGray> {

	Class<ArrayData> arrayType;

	int w=20;
	int h=25;
	int maxDisparity=10;

	D disparity;

	DisparitySelect<ArrayData,D> alg;

	protected BasicDisparitySelectRectTests( Class<ArrayData> arrayType , Class<D> disparityType ) {

		this.arrayType = arrayType;
		disparity = GeneralizedImageOps.createSingleBand(disparityType,w,h);

		alg = createAlg();
	}

	public abstract DisparitySelect<ArrayData,D> createAlg();

	public void allTests() {
		simpleTest();
		minDisparity();
	}

	/**
	 * Give it a hand crafted score with known results for WTA.  See if it produces those results
	 */
	@Test
	public void simpleTest() {

		int y = 3;

		GImageMiscOps.fill(disparity, 0);
		alg.configure(disparity,0,maxDisparity,2);

		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = Math.abs(d-5);
			}
		}

		ArrayData s = copyToCorrectType(scores);

		alg.process(y,s);

		// make sure image borders are zero
		assertEquals(0, GeneralizedImageOps.get(disparity, 0, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, 1, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, w-2, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, w-1, y), 1e-8);

		// should ramp up to 5 here
		for( int i = 0; i < 5; i++ )
			assertEquals(i, GeneralizedImageOps.get(disparity, i+2, y), 1e-8);
		// should be at 5 for the remainder
		for( int i = 5; i < w-4; i++ )
			assertEquals(5, GeneralizedImageOps.get(disparity, i+2, y), 1e-8);
	}

	private ArrayData copyToCorrectType( int scores[] ) {

		if( arrayType == int[].class )
			return (ArrayData)scores;

		float[] ret = new float[ scores.length ];

		for( int i = 0; i < scores.length; i++ ) {
			ret[i] = scores[i];
		}

		return (ArrayData)ret;
	}

	/**
	 * Set the minimum disparity to a non zero number and see if it has the expected behavior
	 */
	@Test
	public void minDisparity() {
		int minDisparity = 2;
		int maxDisparity = 10;
		int r = 2;

		int range = maxDisparity-minDisparity;

		int y = 3;

		GImageMiscOps.fill(disparity,0);
		alg.configure(disparity,minDisparity,maxDisparity,r);

		int scores[] = new int[w*range];

		for( int d = 0; d < range; d++ ) {
			for( int x = 0; x < w-(r*2+1); x++ ) {
				scores[w*d+x] = Math.abs(d-5);
			}
		}

		alg.process(y,copyToCorrectType(scores));

		// make sure image borders are zero
		for( int i = 0; i < 2+minDisparity; i++ )
			assertEquals(0, GeneralizedImageOps.get(disparity, i, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, w-2, y), 1e-8);
		assertEquals(0, GeneralizedImageOps.get(disparity, w-1, y), 1e-8);

		// should ramp up to 7 starting at 2
		for( int i = 0; i < 5; i++ )
			assertEquals(i+2, minDisparity+GeneralizedImageOps.get(disparity, i+2+minDisparity, y), 1e-8);
		// should be at 7 for the remainder
		for( int i = 5; i < w-4-minDisparity; i++ )
			assertEquals(7, minDisparity+GeneralizedImageOps.get(disparity, i+2+minDisparity, y), 1e-8);
	}
}
