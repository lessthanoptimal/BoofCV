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

package boofcv.alg.segmentation.ms;

import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestRegionMergeTree {

	@Test
	public void flowIntoRootNode() {
		RegionMergeTree alg = new RegionMergeTree();
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,1,2,2,2,3,5};

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32(7);
		regionMemberCount.size = 7;
		regionMemberCount.data = new int[]{1,2,3,4,5,6,7};

		alg.flowIntoRootNode(regionMemberCount);

		// check member count
		int expectedCount[] = new int[]{1,3,3+4+5+6+7,4,5,6,7};
		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedCount[i],regionMemberCount.data[i]);

		// check mergeList
		int expectedMerge[] = new int[]{1,1,2,2,2,2,2};
		for( int i = 0; i < expectedMerge.length; i++ )
			assertEquals(expectedMerge[i],alg.mergeList.data[i]);

		// check root id
		assertEquals(0,alg.rootID.get(1));
		assertEquals(1,alg.rootID.get(2));
	}

	@Test
	public void setToRootNodeNewID() {
		GrowQueue_I32 regionMemberCount = new GrowQueue_I32(7);
		regionMemberCount.size = 7;
		regionMemberCount.data = new int[]{1,2,3,4,5,6,7};

		RegionMergeTree alg = new RegionMergeTree();
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,1,2,2,2,2,2};

		alg.rootID.resize(7);
		alg.rootID.data = new int[]{0,0,1,0,0,0,0};

		alg.setToRootNodeNewID(regionMemberCount);

		int expectedCount[] = new int[]{2,3};
		int expectedMerge[] = new int[]{0,0,1,1,1,1,1};

		assertEquals(2,regionMemberCount.size);

		for( int i = 0; i < expectedCount.length; i++ ) {
			assertEquals(expectedCount[i],regionMemberCount.data[i]);
		}

		for( int i = 0; i < expectedMerge.length; i++ )
			assertEquals(expectedMerge[i],alg.mergeList.data[i]);

	}

	/**
	 * Tests focused on the quick test.  All these cases should result in no change
	 */
	@Test
	public void markMerge_quick() {

		int expected[] = new int[]{1,1,2,2,2,3,5};

		RegionMergeTree alg = new RegionMergeTree();
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,1,2,2,2,3,5};

		// case 1 - Both are references
		alg.markMerge(3,4);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// case 2 - A is a reference and B is not
		alg.markMerge(3, 2);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// case 3 - B is a reference and A is not
		alg.markMerge(2, 3);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	/**
	 * These cases result in an actual merge
	 */
	@Test
	public void markMergee_merge() {

		int original[] = new int[]{1,1,2,2,2,3,5};

		RegionMergeTree alg = new RegionMergeTree();
		alg.mergeList.resize(7);
		alg.mergeList.data = original.clone();

		// Both are root nodes
		alg.markMerge(1, 2);
		assertEquals(1, alg.mergeList.data[1]);
		assertEquals(1,alg.mergeList.data[2]);

		// both are references
		alg.mergeList.data = original.clone();
		alg.markMerge(0, 3);
		int expected[] = new int[]{1,1,1,1,2,3,5};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// Both point to the same data but aren't equivalent references, so no change
		alg.mergeList.data = original.clone();
		alg.markMerge(3, 6);
		expected = new int[]{1,1,2,2,2,3,2};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		alg.mergeList.data = original.clone();
		alg.markMerge(6, 3);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

}
