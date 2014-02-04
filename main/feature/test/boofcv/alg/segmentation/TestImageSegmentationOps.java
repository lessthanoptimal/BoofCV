/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation;

import boofcv.struct.image.ImageSInt32;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImageSegmentationOps {

	@Test
	public void countRegionPixels_single() {
		fail("Implement");
	}

	@Test
	public void countRegionPixels_all() {
		fail("Implement");
	}

	/**
	 * Manually construct input data and see if it has the expected output
	 */
	@Test
	public void regionPixelId_to_Compact() {
		ImageSInt32 graph = new ImageSInt32(4,5);
		ImageSInt32 output = new ImageSInt32(4,5);

		graph.data = new int[]{
				2, 2, 2, 5,
				5, 5, 5, 5,
				2, 2, 2, 2,
				15,15,15,15,
				15,15,15,15};


		GrowQueue_I32 rootNodes = new GrowQueue_I32();
		rootNodes.add(2);
		rootNodes.add(5);
		rootNodes.add(15);


		ImageSegmentationOps.regionPixelId_to_Compact(graph, rootNodes, output);


		ImageSInt32 expected = new ImageSInt32(4,5);
		expected.data = new int[]{
				0, 0, 0, 1,
				1, 1, 1, 1,
				0, 0, 0, 0,
				2, 2, 2, 2,
				2, 2, 2, 2};

		BoofTesting.assertEquals(expected, output, 1e-4);
	}

}
