/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMergeRegionMeanShiftGray {
	@Test
	public void basicAll() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,1,
				 2,0,0,1,
				 2,0,1,1,
				 0,0,3,1};

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4};
		regionMemberCount.size = 4;

		GrowQueue_F32 regionColor = new GrowQueue_F32(3);
		regionColor.add(5);
		regionColor.add(1);
		regionColor.add(6);
		regionColor.add(4);

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
			assertEquals(expectedColor[i],regionColor.data[i],1e-4f);
	}

	@Test
	public void compactRegionId() {
		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4,5};
		regionMemberCount.size = 5;

		GrowQueue_F32 regionColor = new GrowQueue_F32();
		regionColor.data = new float[]{2,3,4,5,6};
		regionColor.size = 5;

		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(1);
		alg.mergeList = new GrowQueue_I32();
		alg.mergeList.data = new int[]{1,-1,1,-1,3};
		alg.mergeList.size = 5;

		alg.compactRegionId(regionMemberCount,regionColor);

		int expectedMergeList[] = new int[]{0,-1,0,-1,1};
		for( int i = 0; i < expectedMergeList.length; i++ )
			assertEquals(expectedMergeList[i],alg.mergeList.data[i]);

		int expectedCount[] = new int[]{2,4};
		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedCount[i],regionMemberCount.data[i]);

		float expectedColor[] = new float[]{3,5};
		for( int i = 0; i < expectedCount.length; i++ )
			assertEquals(expectedColor[i],regionColor.data[i],1e-4f);
	}

	@Test
	public void updateMemberCount() {
		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.data = new int[]{1,2,3,4,5};
		regionMemberCount.size = 5;

		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(1);
		alg.mergeList = new GrowQueue_I32();
		alg.mergeList.data = new int[]{1,-1,1,-1,3};
		alg.mergeList.size = 5;

		alg.updateMemberCount(regionMemberCount);

		int expected[] = new int[]{1,6,3,9,5};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],regionMemberCount.data[i]);
	}

	/**
	 * This test just sees if the inner part of the image is processed.
	 */
	@Test
	public void createMergeList() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,1,
				 2,0,0,1,
				 2,0,1,1,
				 0,0,3,1};

		GrowQueue_F32 regionColor = new GrowQueue_F32(3);
		regionColor.add(5);
		regionColor.add(1);
		regionColor.add(6);
		regionColor.add(10);

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
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(1);

		ImageSInt32 pixelToRegion = new ImageSInt32(4,4);
		pixelToRegion.data = new int[]
				{0,0,0,0,
				 0,0,0,2,
				 0,0,0,1,
				 0,0,3,1};

		GrowQueue_F32 regionColor = new GrowQueue_F32(3);
		regionColor.add(5);
		regionColor.add(1);
		regionColor.add(2);
		regionColor.add(4);

		alg.createMergeList(pixelToRegion,regionColor);
		int expected[] = new int[]{-1,2,-1,0};
		for( int i = 0; i < expected.length; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	@Test
	public void checkMerge_both() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(5);

		alg.mergeList.size = 6;

		alg.mergeList.data = new int[]{-1,0,-1,4,-1,0};
		alg.checkMerge(1, 3);
		int expected[] = new int[]{-1,0,-1,0,0,0};

		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		alg.mergeList.data = new int[]{-1,0,-1,4,-1,0};
		alg.checkMerge(3, 1);
		expected = new int[]{4,4,-1,4,-1,4};

		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	@Test
	public void checkMerge_justA() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(5);

		alg.mergeList.size = 6;

		// straight forward example
		alg.mergeList.data = new int[]{-1,0,-1,-1,-1,0};
		int expected[] = new int[]{-1,0,-1,0,-1,0};

		alg.checkMerge(1, 3);
		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// A already references B
		alg.mergeList.data = new int[]{-1,3,-1,-1,-1,0};
		expected = new int[]{-1,3,-1,-1,-1,0};
		alg.checkMerge(1, 3);
		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	@Test
	public void checkMerge_justB() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(5);

		alg.mergeList.size = 6;

		// straight forward example
		alg.mergeList.data = new int[]{-1,0,-1,-1,-1,0};
		int expected[] = new int[]{-1,0,-1,0,-1,0};

		alg.checkMerge(3, 1);
		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);

		// B already references A
		alg.mergeList.data = new int[]{-1,3,-1,-1,-1,0};
		expected = new int[]{-1,3,-1,-1,-1,0};
		alg.checkMerge(3, 1);
		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}

	@Test
	public void checkMerge_neither() {
		MergeRegionMeanShiftGray alg = new MergeRegionMeanShiftGray(5);

		alg.mergeList.size = 6;

		alg.mergeList.data = new int[]{-1,-1,-1,-1,-1,-1};
		int expected[] = new int[]{-1,3,-1,-1,-1,-1};

		alg.checkMerge(3, 1);
		for( int i = 0; i < 6; i++ )
			assertEquals(expected[i],alg.mergeList.data[i]);
	}
}
