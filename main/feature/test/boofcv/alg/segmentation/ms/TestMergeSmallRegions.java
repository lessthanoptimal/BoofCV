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

package boofcv.alg.segmentation.ms;

import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMergeSmallRegions {

	/**
	 * Runs everything to remove the small patches. This test hsa been designed to take multiple
	 * passes to complete.
	 */
	@Test
	public void process() {
		GrayU8 image = new GrayU8(10,9);
		image.data = new byte[]{
				0,0,0,5,5,5,0,0,0,0,
				0,0,0,5,5,5,7,8,0,0,
				0,0,0,5,6,5,0,0,0,0,
				0,0,0,5,5,5,0,0,0,0,
				0,0,0,5,5,5,0,0,0,7,
				0,0,0,5,5,4,4,0,0,8,
				0,0,0,0,0,0,0,0,9,9,
				0,0,0,0,0,0,9,9,9,9,
				0,0,0,0,0,0,9,9,6,7};

		GrayS32 pixelToRegion = new GrayS32(10,9);
		pixelToRegion.data = new int[]{
				0,0,0,1,1,1,0,0,0,0,
				0,0,0,1,1,1,4,5,0,0,
				0,0,0,1,2,1,0,0,0,0,
				0,0,0,1,1,1,0,0,0,0,
				0,0,0,1,1,1,0,0,0,9,
				0,0,0,1,1,3,3,0,0,9,
				0,0,0,0,0,0,0,0,6,6,
				0,0,0,0,0,0,6,6,6,6,
				0,0,0,0,0,0,6,6,7,8};


		GrowQueue_I32 memberCount = new GrowQueue_I32();
		memberCount.resize(10);
		for( int i = 0; i < pixelToRegion.data.length; i++ ) {
			memberCount.data[pixelToRegion.data[i]]++;
		}

		FastQueue<float[]> regionColor = new FastQueue<float[]>(float[].class,true) {
			protected float[] createInstance() {return new float[ 1 ];}
		};
		regionColor.resize(10);

		ComputeRegionMeanColor<GrayU8> mean = new ComputeRegionMeanColor.U8();
		mean.process(image,pixelToRegion,memberCount,regionColor);

		MergeSmallRegions<GrayU8> alg = new MergeSmallRegions<>(3, ConnectRule.FOUR,mean);

		alg.process(image, pixelToRegion, memberCount, regionColor);

		// check the results.  Should only be three regions
		assertEquals(3,memberCount.size);
		assertEquals(3,regionColor.size);

		GrowQueue_I32 memberExpected = new GrowQueue_I32(3);
		memberExpected.resize(3);
		for( int i = 0; i < pixelToRegion.data.length; i++ ) {
			memberExpected.data[pixelToRegion.data[i]]++;
		}

		for( int i = 0; i < 3; i++ )
			assertEquals(memberExpected.get(i),memberCount.get(i));

		// simple sanity check
		assertTrue(memberExpected.get(0)>memberExpected.get(1));
	}

	@Test
	public void setupPruneList() {
		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.size = 6;
		regionMemberCount.data = new int[]{10,11,20,20,10,20};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(11,ConnectRule.FOUR,null);

		assertTrue(alg.setupPruneList(regionMemberCount));

		assertEquals(2,alg.pruneGraph.size());
		assertEquals(0,alg.pruneGraph.get(0).segment);
		assertEquals(4,alg.pruneGraph.get(1).segment);
		assertEquals(0,alg.pruneGraph.get(0).edges.size);
		assertEquals(0,alg.pruneGraph.get(1).edges.size);

		boolean flags[] = new boolean[]{true,false,false,false,true,false};
		for( int i = 0; i <6; i++ )
			assertEquals(flags[i],alg.segmentPruneFlag.get(i));
		assertEquals(0,alg.segmentToPruneID.get(0));
		assertEquals(1,alg.segmentToPruneID.get(4));
	}

	@Test
	public void findAdjacentRegions_center() {
		int N = 9;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,1,
				 1,2,3,4,1,
				 1,5,6,7,1,
				 1,8,8,8,1};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(2,true);
		alg.segmentPruneFlag.set(5,true);

		alg.segmentToPruneID.set(2,0);
		alg.segmentToPruneID.set(5,1);

		alg.pruneGraph.grow().init(2);
		alg.pruneGraph.grow().init(5);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected if the graph is constructed
		int edges2[] = new int[]{1,3,5};
		int edges5[] = new int[]{1,2,6,8};

		checkNode(alg, edges2,0);
		checkNode(alg, edges5,1);
	}

	@Test
	public void findAdjacentRegions_right() {
		int N = 9;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,2,
				 1,1,1,1,3,
				 1,1,1,7,4,
				 1,1,1,1,5};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(2,true);
		alg.segmentPruneFlag.set(4,true);
		alg.segmentPruneFlag.set(5,true);

		alg.segmentToPruneID.set(2,0);
		alg.segmentToPruneID.set(4,1);
		alg.segmentToPruneID.set(5,2);

		alg.pruneGraph.grow().init(2);
		alg.pruneGraph.grow().init(4);
		alg.pruneGraph.grow().init(5);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected if the graph is constructed
		int edges2[] = new int[]{1,3};
		int edges4[] = new int[]{3,5,7};
		int edges5[] = new int[]{1,4};

		checkNode(alg, edges2,0);
		checkNode(alg, edges4,1);
		checkNode(alg, edges5,2);
	}

	@Test
	public void findAdjacentRegions_bottom() {
		int N = 9;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,1,
				 1,1,1,1,1,
				 4,1,1,1,1,
				 2,1,1,3,5};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(2,true);
		alg.segmentPruneFlag.set(3,true);
		alg.segmentPruneFlag.set(5,true);

		alg.segmentToPruneID.set(2,0);
		alg.segmentToPruneID.set(3,1);
		alg.segmentToPruneID.set(5,2);

		alg.pruneGraph.grow().init(2);
		alg.pruneGraph.grow().init(3);
		alg.pruneGraph.grow().init(5);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected if the graph is constructed
		int edges2[] = new int[]{1,4};
		int edges3[] = new int[]{1,5};
		int edges5[] = new int[]{1,3};

		checkNode(alg, edges2,0);
		checkNode(alg, edges3,1);
		checkNode(alg, edges5,2);
	}

	@Test
	public void selectMerge() {
		int N = 10;

		MergeSmallRegions alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		FastQueue<float[]> regionColor = new FastQueue<>(float[].class, false);

		for( int i = 0; i < N; i++ ) {
			regionColor.add( new float[3]);
		}

		// make it so the closest color to 2 is 4
		regionColor.data[2] = new float[]{1,2,3};
		regionColor.data[4] = new float[]{1.1f,2,3};
		regionColor.data[6] = new float[]{100,2,3};

		MergeSmallRegions.Node n = (MergeSmallRegions.Node)alg.pruneGraph.grow();

		// mark 4 and 6 as being connect to 2
		n.init(2);
		n.edges.add(4);
		n.edges.add(6);

		alg.selectMerge(0,regionColor);

		// doesn't matter which one is merged into which
		if( alg.mergeList.get(4) == 4 ) {
			assertEquals(4, alg.mergeList.get(2));
			assertEquals(4, alg.mergeList.get(4));
		} else {
			assertEquals(2, alg.mergeList.get(4));
			assertEquals(2, alg.mergeList.get(2));
		}
	}

	private void checkNode(MergeSmallRegions<GrayF32> alg, int[] edges, int pruneId) {
		assertEquals(edges.length,alg.pruneGraph.get(pruneId).edges.size);
		for( int i = 0; i < edges.length; i++ ) {
			assertTrue(alg.pruneGraph.get(pruneId).isConnected(edges[i]));
		}
	}

	/**
	 * Make sure a connect-4 rule is correctly enforced
	 */
	@Test
	public void adjacentInner4() {
		int N = 9;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{1,2,3,0,0,
				 8,9,4,0,0,
				 7,6,5,0,0,
				 0,0,0,0,0};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N+1);

		alg.segmentPruneFlag.set(1,true);
		alg.segmentPruneFlag.set(9,true);

		alg.segmentToPruneID.set(1,0);
		alg.segmentToPruneID.set(9,1);

		alg.pruneGraph.grow().init(1);
		alg.pruneGraph.grow().init(9);

		alg.adjacentInner4(pixelToRegion);

		// See expected if the graph is constructed
		int edges1[] = new int[]{2,8};
		int edges9[] = new int[]{2,4,6,8};

		checkNode(alg, edges1,0);
		checkNode(alg, edges9,1);
	}

	@Test
	public void adjacentInner8() {
		int N = 13;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{1,2,3,10,0,
				 8,9,4,11,0,
				 7,6,5,12,0,
				 0,0,0,0,0};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.EIGHT,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(1,true);
		alg.segmentPruneFlag.set(2,true);
		alg.segmentPruneFlag.set(4,true);

		alg.segmentToPruneID.set(1,0);
		alg.segmentToPruneID.set(2,1);
		alg.segmentToPruneID.set(4,2);

		alg.pruneGraph.grow().init(1);
		alg.pruneGraph.grow().init(2);
		alg.pruneGraph.grow().init(4);

		alg.adjacentInner8(pixelToRegion);

		// See expected if the graph is constructed
		int edges1[] = new int[]{};
		int edges2[] = new int[]{3,4,9,8};
		int edges4[] = new int[]{2,3,10,11,12,5,6,9};

		checkNode(alg, edges1,0);
		checkNode(alg, edges2,1);
		checkNode(alg, edges4,2);
	}

	@Test
	public void adjacentBorder8() {
		int N = 13;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{0,0,0, 0,0,
				 8,2,0, 9,4,
				 7,1,3, 5,11,
				 0,0,6,10,12};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.EIGHT,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(8,true);
		alg.segmentPruneFlag.set(4,true);
		alg.segmentPruneFlag.set(12,true);

		alg.segmentToPruneID.set(8,0);
		alg.segmentToPruneID.set(4,1);
		alg.segmentToPruneID.set(12,2);

		alg.pruneGraph.grow().init(8);
		alg.pruneGraph.grow().init(4);
		alg.pruneGraph.grow().init(12);

		alg.adjacentBorder(pixelToRegion);

		// See expected if the graph is constructed
		int edges8[] = new int[]{0,2,1,7};
		int edges4[] = new int[]{0,9,11,5};
		int edges12[] = new int[]{5,10,11};

		checkNode(alg, edges8,0);
		checkNode(alg, edges4,1);
		checkNode(alg, edges12,2);
	}

	@Test
	public void adjacentBorder4() {
		int N = 13;
		GrayS32 pixelToRegion = new GrayS32(5,4);
		pixelToRegion.data = new int[]
				{0,0,0, 0,0,
				 8,2,0, 9,4,
				 7,1,3, 5,11,
				 0,0,6,10,12};

		MergeSmallRegions<GrayF32> alg = new MergeSmallRegions(10,ConnectRule.FOUR,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(8,true);
		alg.segmentPruneFlag.set(4,true);
		alg.segmentPruneFlag.set(12,true);

		alg.segmentToPruneID.set(8,0);
		alg.segmentToPruneID.set(4,1);
		alg.segmentToPruneID.set(12,2);

		alg.pruneGraph.grow().init(8);
		alg.pruneGraph.grow().init(4);
		alg.pruneGraph.grow().init(12);

		alg.adjacentBorder(pixelToRegion);

		// See expected if the graph is constructed
		int edges8[] = new int[]{};
		int edges4[] = new int[]{0,9,11};
		int edges12[] = new int[]{10,11};

		checkNode(alg, edges8,0);
		checkNode(alg, edges4,1);
		checkNode(alg, edges12,2);
	}

}
