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

package boofcv.alg.segmentation.fh04;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.segmentation.fh04.impl.FhEdgeWeights4_U8;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSegmentFelzenszwalbHuttenlocher04 {

	Random rand = new Random(234);

	/**
	 * Test it on a trivial segmentation problem
	 */
	@Test
	public void process() {
		GrayU8 image = new GrayU8(20,25);
		ImageMiscOps.fillRectangle(image,100,0,0,10,25);
		GrayS32 output = new GrayS32(20,25);

		// normal images
		process(image, output);

		// sub-images
		process(BoofTesting.createSubImageOf(image), output);
	}

	private void process(GrayU8 image, GrayS32 output) {
		ImageMiscOps.fillUniform(output,rand,0,100);

		FhEdgeWeights<GrayU8> edgeWeights = new FhEdgeWeights4_U8();
		SegmentFelzenszwalbHuttenlocher04<GrayU8> alg = new SegmentFelzenszwalbHuttenlocher04<>(200,10,edgeWeights);

		alg.process(image,output);

		int valA = output.get(0,0);
		int valB = output.get(19,0);

		assertTrue(valA != valB);
		for( int y = 0; y < 25; y++ ) {
			for( int x =0; x < 10; x++ )
				assertEquals(valA, output.get(x, y));
			for( int x =10; x < 20; x++ )
				assertEquals(valB,output.get(x,y));
		}
	}

	@Test
	public void mergeRegions() {

		// K is zero to make it easier to figure out if two edges should be merged or not
		SegmentFelzenszwalbHuttenlocher04 alg = new SegmentFelzenszwalbHuttenlocher04(0,10,null);

		// add edges.  Design it such that order is important and to make sure the equality checks
		// are done correctly
		alg.edges.add( edge(1, 0, 20));
		alg.edges.add( edge(2, 0, 25));
		alg.edges.add( edge(14, 0, 40));
		alg.edges.add( edge(3,4,20));
		alg.edges.add( edge(5,4,20));
		alg.edges.add( edge(10,11,20));
		alg.edges.add( edge(12,11,5));
		alg.edges.add( edge(13,11,5));

		// randomize their order
		Collections.shuffle(alg.edges.toList(),rand);
		// NOTE the order after sorting is undefined.  So the checks below could be incorrect if they are processed
		// in a different order

		alg.graph = new GrayS32(4,5);
		alg.graph.data = new int[]{
				0, 1, 2, 3,
				4, 5, 6, 7,
				8, 9, 10,11,
				12,13,14,15,
				16,17,18,19};

		alg.regionSize.reset();
		alg.threshold.reset();
		for( int i = 0; i < 20; i++ ) {
			alg.regionSize.add(i);
			alg.threshold.add(1000); // high value so that all first matches are accepted
		}

		// make sure that the regions are merged correctly
		alg.mergeRegions();

		// check the graph
		assertEquals(1,alg.graph.data[0]);
		assertEquals(1,alg.graph.data[1]);
		assertEquals(2,alg.graph.data[2]);
		assertEquals(5,alg.graph.data[3]);
		assertEquals(5,alg.graph.data[4]);
		assertEquals(5,alg.graph.data[5]);
		assertEquals(10,alg.graph.data[10]);
		assertEquals(13,alg.graph.data[11]);
		assertEquals(13,alg.graph.data[12]);
		assertEquals(13,alg.graph.data[13]);
		assertEquals(14,alg.graph.data[14]);

		// see if thresholds were updated as expected
		assertEquals(20,alg.threshold.data[1],1e-4f);
		assertEquals(20,alg.threshold.data[5],1e-4f);
		assertEquals(1000,alg.threshold.data[10],1e-4f);
		assertEquals(5,alg.threshold.data[13],1e-4f);
		assertEquals(1000,alg.threshold.data[14],1e-4f);

		// ditto for size
		assertEquals(1,alg.regionSize.data[1]);
		assertEquals(2,alg.regionSize.data[2]);
		assertEquals(12,alg.regionSize.data[5]);
		assertEquals(10,alg.regionSize.data[10]);
		assertEquals(11+12+13,alg.regionSize.data[13]);
		assertEquals(14, alg.regionSize.data[14]);
	}

	@Test
	public void mergeSmallRegions() {
		SegmentFelzenszwalbHuttenlocher04 alg = new SegmentFelzenszwalbHuttenlocher04(0,10,null);

		alg.regionSize.resize(20);
		alg.regionSize.set(2, 10);
		alg.regionSize.set(5, 20);
		alg.regionSize.set(15, 9);

		// regions: 2,5,15
		alg.graph = new GrayS32(4,5);
		alg.graph.data = new int[]{
				2, 0, 2, 5,
				3, 5, 4, 6,
				2, 2, 2, 1,
				15,15,15,15,
				15,15,15,15};

		alg.edgesNotMatched.add( new SegmentFelzenszwalbHuttenlocher04.Edge(1,5));
		alg.edgesNotMatched.add( new SegmentFelzenszwalbHuttenlocher04.Edge(12,8));

		alg.mergeSmallRegions();

		// see if the regions were merged
		assertEquals(15,alg.find(15));
		assertEquals(15, alg.find(2));
		assertEquals(5,alg.find(5));
	}

	@Test
	public void find() {
		SegmentFelzenszwalbHuttenlocher04 alg = new SegmentFelzenszwalbHuttenlocher04(300,20,null);

		alg.graph = new GrayS32(4,5);
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

		SegmentFelzenszwalbHuttenlocher04 alg = new SegmentFelzenszwalbHuttenlocher04(300,20,null);

		alg.graph = new GrayS32(4,5);
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

		GrayS32 expected = new GrayS32(4,5);
		expected.data = new int[]{
				2, 2, 2, 5,
				5, 5, 5, 5,
				2, 2, 2, 2,
				15,15,15,15,
				15,15,15,15};

		BoofTesting.assertEquals(expected, alg.graph, 1e-4);
	}

	private SegmentFelzenszwalbHuttenlocher04.Edge edge( int indexA , int indexB , float weight ) {
		SegmentFelzenszwalbHuttenlocher04.Edge e = new SegmentFelzenszwalbHuttenlocher04.Edge();
		e.indexA = indexA;
		e.indexB = indexB;
		e.sortValue = weight;
		return e;
	}
}
