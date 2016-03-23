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

package boofcv.alg.segmentation.watershed;

import boofcv.struct.image.GrayS32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestRemoveWatersheds {

	/**
	 * Simple case.  Still will require multiple passes for all the pixels to be assigned.
	 */
	@Test
	public void basic() {
		GrayS32 segmented = new GrayS32(5,7);
		segmented.data = new int[] {
				-1,-1,-1,-1,-1,
				-1, 1, 1, 1,-1,
				-1, 1, 0, 0,-1,
				-1, 2, 2, 2,-1,
				-1, 0, 0, 0,-1,
				-1, 0, 0, 0,-1,
				-1,-1,-1,-1,-1};

		// technically it could be assigned other values and still be a valid solution
		// this expected image is created knowing the exact internal algorithm
		GrayS32 expected = new GrayS32(5,7);
		expected.data = new int[] {
				-1,-1,-1,-1,-1,
				-1, 0, 0, 0,-1,
				-1, 0, 0, 0,-1,
				-1, 1, 1, 1,-1,
				-1, 1, 1, 1,-1,
				-1, 1, 1, 1,-1,
				-1,-1,-1,-1,-1};

		RemoveWatersheds alg = new RemoveWatersheds();

		alg.remove(segmented);

		BoofTesting.assertEquals(expected, segmented, 0);
	}

}
