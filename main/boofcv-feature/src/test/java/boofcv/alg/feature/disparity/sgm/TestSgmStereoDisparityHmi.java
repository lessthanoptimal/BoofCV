/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.sgm;

import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestSgmStereoDisparityHmi {
	private int width = 80,height = 60;

	private GrayU8 left  = new GrayU8(width,height);
	private GrayU8 right = new GrayU8(width,height);;

	/**
	 * The entire image should have a disparity of 5. Each pixel if visually distinctive from its neighbors
	 */
	@Test
	void easy_scenario() {
		createStereoPair(5);

		SgmStereoDisparityHmi alg = create();
		alg.process(left,right,0,10);

		// sanity check on internal data structures
		assertEquals(3,alg.getPyrLeft().getLevelsCount());
		assertEquals(3,alg.getPyrRight().getLevelsCount());

		// disparity should be 5 everywhere
		GrayU8 found = alg.getDisparity();
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				assertEquals(5,found.get(x,y));
			}
		}
	}

	void createStereoPair( int d ) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				right.set(x,y, y*10+x);
				left.set(x,y, y*10+x+d);
			}
		}
	}

	SgmStereoDisparityHmi create() {
		StereoMutualInformation stereoMI = new StereoMutualInformation();
		stereoMI.configureHistogram(255,255);
		SgmDisparitySelector selector = new SgmDisparitySelector();
		return new SgmStereoDisparityHmi(20,stereoMI,selector);
	}
}