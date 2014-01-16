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
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
* @author Peter Abeles
*/
public class TestMergeRegionMeanShift {
	@Test
	public void basicAll() {
		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,1,
				 2,0,0,1,
				 2,0,1,1,
				 0,0,3,1};

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4};
		regionMemberCount.size = 4;

		FastQueue<float[]> regionColor = createList(5,1,6,4);

		alg.merge(pixelToRegion,regionMemberCount,regionColor);

		ImageSInt32 expectedP2R = new ImageSInt32(4,4);
		expectedP2R.data = new int[]
				{0,0,0,1,
				 0,0,0,1,
				 0,0,1,1,
				 0,0,0,1};

		int expectedCount[] = new int[]{8,2};
		float expectedColor[] = new float[]{5,1};

		for( int i = 0; i < expectedP2R.data.length; i++ )
			assertEquals(expectedP2R.data[i],pixelToRegion.data[i]);

		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedCount[i],regionMemberCount.data[i]);

		for( int i = 0; i < expectedColor.length; i++ )
			assertEquals(expectedColor[i],regionColor.data[i][0],1e-4f);
	}

	@Test
	public void updateMemberCount() {
		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4,5};
		regionMemberCount.size = 5;

		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);
		alg.mergeList = new GrowQueue_I32();
		alg.mergeList.data = new int[]{1,-1,1,-1,3};
		alg.mergeList.size = 5;

		alg.flowIntoRootNode(regionMemberCount);

		int expected[] = new int[]{1,6,3,9,5};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],regionMemberCount.data[i]);
	}

	/**
	 * This test just sees if the inner part of the image is processed.
	 */
	@Test
	public void createMergeList() {
		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,1,
				 2,0,0,1,
				 2,0,1,1,
				 0,0,3,1};

		FastQueue<float[]> regionColor = createList(5,1,6,10);

		alg.createMergeList(pixelToRegion,regionColor);
		int expected[] = new int[]{-1,-1,0,-1};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	/**
	 * Specifically design a test case that will fail if right and bottom borders are not handled correctly
	 */
	@Test
	public void createMergeList_border() {
		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,0,
				 0,0,0,2,
				 0,0,0,1,
				 0,0,3,1};

		FastQueue<float[]> regionColor = createList(5,1,2,4);

		alg.createMergeList(pixelToRegion,regionColor);
		int expected[] = new int[]{-1,2,-1,0};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	@Test
	public void flowIntoRootNode() {
		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,-1,-1,2,2,3,5};

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32(7);
		regionMemberCount.size = 7;
		regionMemberCount.data = new int[]{1,2,3,4,5,6,7};

		alg.flowIntoRootNode(regionMemberCount);

		// check member count
		int expectedCount[] = new int[]{1,3,3+4+5+6+7,4,5,6,7};
		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedCount[i],regionMemberCount.data[i]);

		// check mergeList
		int expectedMerge[] = new int[]{1,-1,-1,2,2,2,2};
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

		FastQueue<float[]> regionColor = createList(3,2,4,7,4,5,4);

		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,-1,-1,2,2,2,2};

		alg.rootID.resize(7);
		alg.rootID.data = new int[]{0,0,1,0,0,0,0};

		alg.setToRootNodeNewID(regionMemberCount,regionColor);

		int expectedCount[] = new int[]{2,3};
		int expectedMerge[] = new int[]{0,0,1,1,1,1,1};
		float expectedColor[] = new float[]{2,4};

		assertEquals(2,regionMemberCount.size);
		assertEquals(2,regionColor.size);

		for( int i = 0; i < expectedCount.length; i++ ) {
			assertEquals(expectedCount[i],regionMemberCount.data[i]);
			assertEquals(expectedColor[i],regionColor.data[i][0],1e-4f);
		}

		for( int i = 0; i < expectedMerge.length; i++ )
			assertEquals(expectedMerge[i],alg.mergeList.data[i]);

	}

	/**
	 * Tests focused on the quick test.  All these cases should result in no change
	 */
	@Test
	public void checkMerge_quick() {

		int expected[] = new int[]{1,-1,-1,2,2,3,5};

		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);
		alg.mergeList.resize(7);
		alg.mergeList.data = new int[]{1,-1,-1,2,2,3,5};

		// case 1 - Both are references
		alg.checkMerge(3,4);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// case 2 - A is a reference and B is not
		alg.checkMerge(3,2);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// case 3 - B is a reference and A is not
		alg.checkMerge(2,3);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	/**
	 * These cases result in an actual merge
	 */
	@Test
	public void checkMerge_merge() {

		int original[] = new int[]{1,-1,-1,2,2,3,5};

		MergeRegionMeanShift alg = new MergeRegionMeanShift(1,1);
		alg.mergeList.resize(7);
		alg.mergeList.data = original.clone();

		// Both are root nodes
		alg.checkMerge(1,2);
		assertEquals(-1, alg.mergeList.data[1]);
		assertEquals(1,alg.mergeList.data[2]);

		// both are references
		alg.mergeList.data = original.clone();
		alg.checkMerge(0,3);
		int expected[] = new int[]{1,-1,1,1,2,3,5};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// Both point to the same data but aren't equivalent references, so no change
		alg.mergeList.data = original.clone();
		alg.checkMerge(3,6);
		expected = new int[]{1,-1,-1,2,2,3,2};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		alg.mergeList.data = original.clone();
		alg.checkMerge(6,3);
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	private FastQueue<float[]> createList( int ...colors ) {
		FastQueue<float[]> ret = new FastQueue<float[]>(float[].class,true) {
			@Override
			protected float[] createInstance() {
				return new float[1];
			}
		};

		for( int i = 0; i < colors.length; i++ ) {
			ret.grow()[0] = colors[i];
		}
		return ret;
	}
}
