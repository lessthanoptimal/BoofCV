/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.tld;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTldTracker {

	/**
	 * Basic sanity check on the pyramid it selects
	 */
	@Test
	public void selectPyramidScale() {
		int minSize = (5*2+1)*5;
		int[] scales = TldTracker.selectPyramidScale(640,480,minSize);

		assertTrue(scales.length > 3);

		for( int i = 0; i < scales.length; i++ ) {
			int w = 640/scales[i];
			int h = 6480/scales[i];

			assertTrue(w>minSize);
			assertTrue(h>minSize);
		}
	}

}
