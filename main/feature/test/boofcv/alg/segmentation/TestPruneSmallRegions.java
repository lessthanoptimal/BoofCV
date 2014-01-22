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

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPruneSmallRegions {

	/**
	 * Runs everything to remove the small patches. This test hsa been designed to take multiple
	 * passes to complete.
	 */
	@Test
	public void process() {
		ImageUInt8 image = new ImageUInt8(10,9);
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

		ImageSInt32 pixelToRegion = new ImageSInt32(10,9);
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

		ComputeRegionMeanColor<ImageUInt8> mean = new ComputeRegionMeanColor.U8();
		mean.process(image,pixelToRegion,memberCount,regionColor);

		PruneSmallRegions<ImageUInt8> alg = new PruneSmallRegions<ImageUInt8>(3,mean);

		alg.process(image, pixelToRegion, memberCount, regionColor);

		// check the results.  Should only be three regions
		assertEquals(3, memberCount.size);
		assertEquals(3,regionColor.size);

		GrowQueue_I32 memberExpected = new GrowQueue_I32(3);
		memberCount.resize(3);
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

		PruneSmallRegions<ImageFloat32> alg = new PruneSmallRegions(11,null);

		assertTrue(alg.setupPruneList(regionMemberCount));

		assertEquals(2,alg.pruneGraph.size());
		assertEquals(0,alg.pruneGraph.get(0).segment);
		assertEquals(4,alg.pruneGraph.get(1).segment);
		assertEquals(6,alg.pruneGraph.get(0).edges.size);
		assertEquals(6,alg.pruneGraph.get(1).edges.size);

		boolean flags[] = new boolean[]{true,false,false,false,true,false};
		for( int i = 0; i <6; i++ )
			assertEquals(flags[i],alg.segmentPruneFlag.get(i));
		assertEquals(0,alg.segmentToPruneID.get(0));
		assertEquals(1,alg.segmentToPruneID.get(4));
	}

	@Test
	public void findAdjacentRegions_center() {
		int N = 9;
		ImageSInt32 pixelToRegion = new ImageSInt32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,1,
				 1,2,3,4,1,
				 1,5,6,7,1,
				 1,8,8,8,1};

		PruneSmallRegions<ImageFloat32> alg = new PruneSmallRegions(10,null);
		alg.initializeMerge(N);

		alg.segmentPruneFlag.resize(N);
		alg.pruneGraph.reset();
		alg.segmentToPruneID.resize(N);

		alg.segmentPruneFlag.set(2,true);
		alg.segmentPruneFlag.set(5,true);

		alg.segmentToPruneID.set(2,0);
		alg.segmentToPruneID.set(5,1);

		alg.pruneGraph.grow().init(2,N);
		alg.pruneGraph.grow().init(5,N);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected graph is constructed
        //                                0     1   2      3     4    5     6    7     8
		boolean edges2[] = new boolean[]{false,true,false,true,false,true,false,false,false};
		boolean edges5[] = new boolean[]{false,true,true,false,false,false,true,false,true};

		for( int i = 0; i < N; i++ ) {
			assertEquals(edges2[i],alg.pruneGraph.get(0).edges.get(i));
			assertEquals(edges5[i],alg.pruneGraph.get(1).edges.get(i));
		}
	}

	@Test
	public void findAdjacentRegions_right() {
		int N = 9;
		ImageSInt32 pixelToRegion = new ImageSInt32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,2,
				 1,1,1,1,3,
				 1,1,1,7,4,
				 1,1,1,1,5};

		PruneSmallRegions<ImageFloat32> alg = new PruneSmallRegions(10,null);
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

		alg.pruneGraph.grow().init(2,N);
		alg.pruneGraph.grow().init(4,N);
		alg.pruneGraph.grow().init(5,N);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected graph is constructed
		//                                0     1   2      3     4    5     6    7     8
		boolean edges2[] = new boolean[]{false,true,false,true,false,false,false,false,false};
		boolean edges4[] = new boolean[]{false,false,false,true,false,true,false,true,false};
		boolean edges5[] = new boolean[]{false,true,false,false,true,false,false,false,false};

		for( int i = 0; i < N; i++ ) {
			assertEquals(edges2[i],alg.pruneGraph.get(0).edges.get(i));
			assertEquals(edges4[i],alg.pruneGraph.get(1).edges.get(i));
			assertEquals(edges5[i],alg.pruneGraph.get(2).edges.get(i));
		}
	}

	@Test
	public void findAdjacentRegions_bottom() {
		int N = 9;
		ImageSInt32 pixelToRegion = new ImageSInt32(5,4);
		pixelToRegion.data = new int[]
				{1,1,1,1,1,
				 1,1,1,1,1,
				 4,1,1,1,1,
				 2,1,1,3,5};

		PruneSmallRegions<ImageFloat32> alg = new PruneSmallRegions(10,null);
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

		alg.pruneGraph.grow().init(2,N);
		alg.pruneGraph.grow().init(3,N);
		alg.pruneGraph.grow().init(5,N);

		alg.findAdjacentRegions(pixelToRegion);

		// See expected graph is constructed
		//                                0     1   2      3     4    5     6    7     8
		boolean edges2[] = new boolean[]{false,true,false,false,true,false,false,false,false};
		boolean edges3[] = new boolean[]{false,true,false,false,false,true,false,false,false};
		boolean edges5[] = new boolean[]{false,true,false,true,false,false,false,false,false};

		for( int i = 0; i < N; i++ ) {
			assertEquals(edges2[i],alg.pruneGraph.get(0).edges.get(i));
			assertEquals(edges3[i],alg.pruneGraph.get(1).edges.get(i));
			assertEquals(edges5[i],alg.pruneGraph.get(2).edges.get(i));
		}
	}

	@Test
	public void selectMerge() {
		int N = 10;

		PruneSmallRegions alg = new PruneSmallRegions(10,null);
		alg.initializeMerge(N);

		FastQueue<float[]> regionColor = new FastQueue<float[]>(float[].class,false);

		for( int i = 0; i < N; i++ ) {
			regionColor.add( new float[3]);
		}

		// make it so the closest color to 2 is 4
		regionColor.data[2] = new float[]{1,2,3};
		regionColor.data[4] = new float[]{1.1f,2,3};
		regionColor.data[6] = new float[]{100,2,3};

		PruneSmallRegions.Node n = (PruneSmallRegions.Node)alg.pruneGraph.grow();

		// mark 4 and 6 as being connect to 2
		n.init(2,N);
		n.edges.set(4, true);
		n.edges.set(6,true);

		alg.selectMerge(0,regionColor);

		// doesn't matter which one is merged into which
		if( alg.mergeList.get(2) != -1 ) {
			assertEquals(4, alg.mergeList.get(2));
			assertEquals(-1, alg.mergeList.get(4));
		} else {
			assertEquals(2, alg.mergeList.get(4));
			assertEquals(-1, alg.mergeList.get(2));
		}
	}

}
