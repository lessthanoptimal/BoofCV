/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Basic tests for selecting disparity
 *
 * @author Peter Abeles
 */
public abstract class BasicDisparitySelectRectTests <D extends ImageSingleBand> {

	int w=20;
	int h=25;
	int maxDisparity=10;

	D disparity;

	DisparitySelectRect_S32<D> alg;

	protected BasicDisparitySelectRectTests( Class<D> imageType ) {

		disparity = GeneralizedImageOps.createSingleBand(imageType,w,h);

		alg = createAlg();
	}

	public abstract DisparitySelectRect_S32<D> createAlg();

	/**
	 * Give it a hand crafted score with known results for WTA.  See if it produces those results
	 */
	@Test
	public void simpleTest() {

		int y = 3;

		GeneralizedImageOps.fill(disparity,0);
		alg.configure(disparity,maxDisparity,2);


		int scores[] = new int[w*maxDisparity];

		for( int d = 0; d < 10; d++ ) {
			for( int x = 0; x < w; x++ ) {
				scores[w*d+x] = Math.abs(d-5);
			}
		}

		alg.process(y,scores);

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
}
