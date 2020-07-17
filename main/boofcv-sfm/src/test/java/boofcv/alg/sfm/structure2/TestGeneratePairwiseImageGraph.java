/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.plane.PlaneNormal3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestGeneratePairwiseImageGraph {

	Random rand = new Random(234);

	/**
	 * See if it gracefully handles 0 to 1 images
	 */
	@Test
	void process_0_to_1() {
		GeneratePairwiseImageGraph alg = new GeneratePairwiseImageGraph();

		for( int numViews = 0; numViews < 2; numViews++ ) {
			MockLookupSimilarImages similar = new MockLookupSimilarImages(numViews,123123);
			alg.process(similar);

			PairwiseImageGraph2 graph = alg.getGraph();
			assertEquals(numViews,graph.nodes.size);
			assertEquals(0,graph.edges.size);
		}
	}

	/**
	 * A fully connected scene with 3D structure
	 */
	@Test
	void process_connected() {
		GeneratePairwiseImageGraph alg = new GeneratePairwiseImageGraph();

		MockLookupSimilarImages similar = new MockLookupSimilarImages(4,123123);
		alg.process(similar);

		PairwiseImageGraph2 graph = alg.getGraph();
		assertEquals(4,graph.nodes.size);
		assertEquals(4,graph.mapNodes.size());
		for (int i = 0; i < graph.nodes.size; i++) {
			PairwiseImageGraph2.View v = graph.nodes.get(i);
			assertTrue(v.totalObservations >50);
			assertEquals(3,v.connections.size);
			assertNotNull(graph.mapNodes.get(v.id));
		}
		assertEquals(6,graph.edges.size);

		for (int i = 0; i < graph.edges.size; i++) {
			PairwiseImageGraph2.Motion a = alg.graph.edges.get(i);
			assertTrue(a.is3D);

			// each edge pair should be unique
			for (int j = i+1; j < graph.edges.size; j++) {
				PairwiseImageGraph2.Motion b = alg.graph.edges.get(j);

				if( a.src.id.equals(b.src.id) && a.dst.id.equals(b.dst.id))
					fail("duplicate1! "+a.src.id+" "+a.dst.id);
				if( a.dst.id.equals(b.src.id) && a.src.id.equals(b.dst.id))
					fail("duplicate2! "+a.src.id+" "+a.dst.id);
			}
		}
	}

	@Test
	void createEdge_3D() {
		GeneratePairwiseImageGraph alg = new GeneratePairwiseImageGraph();
		alg.graph.createNode("moo");
		alg.graph.createNode("foo");

		FastQueue<AssociatedPair> associated = createAssociations(100,false,false);
		FastQueue<AssociatedIndex> associtedIdx = new FastQueue<>(AssociatedIndex::new);
		for (int i = 0; i < associated.size; i++) {
			associtedIdx.grow().setAssociation(i,i,1);
		}

		alg.createEdge("moo","foo",associated,associtedIdx);

		assertEquals(1,alg.graph.edges.size);
		PairwiseImageGraph2.Motion found = alg.graph.edges.get(0);

		assertTrue(found.is3D);
		assertTrue(found.inliers.size>85);
		assertTrue(found.countF>85);
		assertTrue(found.countH<20);
		assertEquals("moo",found.src.id);
		assertEquals("foo",found.dst.id);
	}

	@Test
	void createEdge_Rotation() {
		GeneratePairwiseImageGraph alg = new GeneratePairwiseImageGraph();
		alg.graph.createNode("moo");
		alg.graph.createNode("foo");

		FastQueue<AssociatedPair> associated = createAssociations(100,false,true);
		FastQueue<AssociatedIndex> associtedIdx = new FastQueue<>(AssociatedIndex::new);
		for (int i = 0; i < associated.size; i++) {
			associtedIdx.grow().setAssociation(i,i,1);
		}

		alg.createEdge("moo","foo",associated,associtedIdx);

		assertEquals(1,alg.graph.edges.size);
		PairwiseImageGraph2.Motion found = alg.graph.edges.get(0);

		assertFalse(found.is3D);
		assertTrue(found.inliers.size>85);
//		assertTrue(found.countF>85);
		assertTrue(found.countH>85);
		assertEquals("moo",found.src.id);
		assertEquals("foo",found.dst.id);
	}

	@Test
	void createEdge_Planar() {
		GeneratePairwiseImageGraph alg = new GeneratePairwiseImageGraph();
		alg.graph.createNode("moo");
		alg.graph.createNode("foo");

		FastQueue<AssociatedPair> associated = createAssociations(100,true,false);
		FastQueue<AssociatedIndex> associtedIdx = new FastQueue<>(AssociatedIndex::new);
		for (int i = 0; i < associated.size; i++) {
			associtedIdx.grow().setAssociation(i,i,1);
		}

		alg.createEdge("moo","foo",associated,associtedIdx);

		assertEquals(1,alg.graph.edges.size);
		PairwiseImageGraph2.Motion found = alg.graph.edges.get(0);

		assertFalse(found.is3D);
		assertTrue(found.inliers.size>85);
		// both models should match it well
//		assertTrue(found.countF>85);
		assertTrue(found.countH>85);
		assertEquals("moo",found.src.id);
		assertEquals("foo",found.dst.id);
	}

	private FastQueue<AssociatedPair> createAssociations(int N , boolean planar , boolean pureRotation ) {
		CameraPinhole intrinsic = new CameraPinhole(400,410,0,500,500,1000,1000);
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(0.3,0,0.01,-0.04,-2e-3,0.4,null);

		if( pureRotation )
			view0_to_view1.T.set(0,0,0);

		List<Point3D_F64> feats3D;

		if( planar ) {
			PlaneNormal3D_F64 plane = new PlaneNormal3D_F64(0,0,1,0.001,0.02,1);
			feats3D = UtilPoint3D_F64.random(plane, 0.5, N, rand);
		} else {
			feats3D = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1), -0.5, 0.5, N, rand);
		}

		FastQueue<AssociatedPair> associated = new FastQueue<>(AssociatedPair::new);

		for (int i = 0; i < feats3D.size(); i++) {
			Point3D_F64 X = feats3D.get(i);
			AssociatedPair a = associated.grow();

			a.p1.set(PerspectiveOps.renderPixel(intrinsic,X, null));
			a.p2.set(PerspectiveOps.renderPixel(view0_to_view1,intrinsic,X, null));

			// add a little bit of noise so that it isn't perfect
			a.p1.x += rand.nextGaussian()*0.5;
			a.p1.y += rand.nextGaussian()*0.5;
			a.p2.x += rand.nextGaussian()*0.5;
			a.p2.y += rand.nextGaussian()*0.5;
		}

		return associated;
	}
}