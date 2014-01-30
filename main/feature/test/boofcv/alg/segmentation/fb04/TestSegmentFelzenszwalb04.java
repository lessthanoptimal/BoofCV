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

package boofcv.alg.segmentation.fb04;

import boofcv.struct.image.ImageSInt32;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSegmentFelzenszwalb04 {

	@Test
	public void process() {
		fail("implement");
	}

	@Test
	public void mergeRegions() {
		fail("implement");
	}

	@Test
	public void mergeSmallRegions() {
		fail("implement");
	}

	@Test
	public void find() {
		SegmentFelzenszwalb04 alg = new SegmentFelzenszwalb04(300,20,null);

		alg.graph = new ImageSInt32(4,5);
		alg.graph.data = new int[]{
				2, 0, 2, 5,
				3, 5, 4, 6,
				2, 2, 2, 1,
				15,15,15,15,
				15,15,15,15};

		assertEquals(2, alg.find(11));
		assertEquals(2,alg.graph.data[11]);
		assertEquals(0,alg.graph.data[1]);

		assertEquals(2,alg.find(2));
	}

	@Test
	public void computeOutput() {

		SegmentFelzenszwalb04 alg = new SegmentFelzenszwalb04(300,20,null);

		alg.graph = new ImageSInt32(4,5);
		alg.graph.data = new int[]{
				2, 0, 2, 5,
				3, 5, 4, 6,
				2, 2, 2, 1,
				15,15,15,15,
				15,15,15,15};

		for( int i = 0; i < alg.graph.data.length; i++ )
			alg.regionSize.add(i+1);

		alg.computeOutput();

		GrowQueue_I32 regionId = alg.getRegionId();
		assertEquals(3,regionId.size);
		assertEquals(2,regionId.get(0));
		assertEquals(5,regionId.get(1));
		assertEquals(15,regionId.get(2));

		GrowQueue_I32 outputRegionSize = alg.getRegionSizes();
		assertEquals(3,outputRegionSize.size);
		assertEquals(3,outputRegionSize.get(0));
		assertEquals(6,outputRegionSize.get(1));
		assertEquals(16,outputRegionSize.get(2));

		ImageSInt32 expected = new ImageSInt32(4,5);
		expected.data = new int[]{
				2, 2, 2, 5,
				5, 5, 5, 5,
				2, 2, 2, 2,
				15,15,15,15,
				15,15,15,15};

		BoofTesting.assertEquals(expected, alg.graph, 1e-4);
	}
}
