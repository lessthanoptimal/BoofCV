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

package boofcv.io.geo;

import boofcv.alg.sfm.structure.PairwiseImageGraph;
import boofcv.alg.sfm.structure.SceneWorkingGraph;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofTesting;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMultiViewIO {
	Random rand = BoofTesting.createRandom(234);

	@Test
	void save_load_PairwiseImageGraph() {
		for (int trial = 0; trial < 20; trial++) {
			PairwiseImageGraph expected = createPairwise();

			var output = new ByteArrayOutputStream();
			MultiViewIO.save(expected,new OutputStreamWriter(output));

			var input = new ByteArrayInputStream(output.toByteArray());
			PairwiseImageGraph found = MultiViewIO.load(new InputStreamReader(input),(PairwiseImageGraph)null);
			checkIdentical(expected,found);
		}
	}

	private void checkIdentical( PairwiseImageGraph a , PairwiseImageGraph b ) {
		assertEquals(a.edges.size,b.edges.size);
		assertEquals(a.nodes.size,b.nodes.size);
		assertEquals(a.mapNodes.size(),b.mapNodes.size());

		for ( int viewIdx = 0;viewIdx < a.nodes.size; viewIdx++ ) {
			PairwiseImageGraph.View va = a.nodes.get(viewIdx);
			PairwiseImageGraph.View vb = b.nodes.get(viewIdx);

			assertSame(va,a.mapNodes.get(va.id));
			assertSame(vb,b.mapNodes.get(vb.id));

			assertEquals(va.id,vb.id);
			assertEquals(va.totalObservations,vb.totalObservations);
			assertEquals(va.connections.size, vb.connections.size);

			for (int i = 0; i < va.connections.size; i++) {
				PairwiseImageGraph.Motion ma = va.connections.get(i);
				PairwiseImageGraph.Motion mb = vb.connections.get(i);

				// Make sure it didn't declare a new instance of the view
				assertSame(ma.src,a.mapNodes.get(ma.src.id));
				assertSame(mb.dst,b.mapNodes.get(mb.dst.id));

				// Make sure the same instance is in the edges list
				assertTrue(b.edges.contains(mb));

				assertEquals(ma.index,mb.index);
				assertEquals(ma.is3D,mb.is3D);
				assertEquals(ma.src.id,mb.src.id);
				assertEquals(ma.dst.id,mb.dst.id);
				assertEquals(ma.countF,mb.countF);
				assertEquals(ma.countH,mb.countH);
				assertTrue(MatrixFeatures_DDRM.isEquals(ma.F,mb.F, UtilEjml.EPS));
				assertEquals(ma.inliers.size,mb.inliers.size);
				for (int j = 0; j < ma.inliers.size; j++) {
					AssociatedIndex ia = ma.inliers.get(j);
					AssociatedIndex ib = mb.inliers.get(j);

					assertEquals(ia.src, ib.src);
					assertEquals(ia.dst, ib.dst);
				}
			}
		}
	}

	private PairwiseImageGraph createPairwise() {
		var ret = new PairwiseImageGraph();

		ret.nodes.resize(rand.nextInt(10)+1);
		for ( int viewIdx = 0;viewIdx < ret.nodes.size; viewIdx++ ) {
			PairwiseImageGraph.View v = ret.nodes.get(viewIdx);
			v.id = rand.nextInt()+"";
			v.totalObservations = rand.nextInt(200);
			ret.mapNodes.put(v.id,v);

			// Randomly select connecting nodes from ones which higher indexes to ensure (src,dst) pairs are unique
			var candidates = GrowQueue_I32.range(viewIdx+1,ret.nodes.size);
			PrimitiveArrays.shuffle(candidates.data,0,candidates.size,rand);
			int numConnections = rand.nextInt()+5;
			for (int i = 0; i < Math.min(numConnections, candidates.size); i++) {
				PairwiseImageGraph.Motion m = ret.edges.grow();
				m.is3D = rand.nextBoolean();
				m.src = v;
				m.dst = ret.nodes.get(candidates.get(i));
				m.index = ret.edges.size-1;
				m.countF = rand.nextInt();
				m.countH = rand.nextInt();
				RandomMatrices_DDRM.fillUniform(m.F,rand);
				m.inliers.resize(rand.nextInt(20));
				for (int j = 0; j < m.inliers.size; j++) {
					AssociatedIndex a = m.inliers.get(j);
					a.src = rand.nextInt();
					a.dst = rand.nextInt();
				}
				v.connections.add(m);
				m.dst.connections.add(m);
			}
		}

		return ret;
	}

	@Test
	void save_load_SceneWorkingGraph() {
		for (int trial = 0; trial < 20; trial++) {
			PairwiseImageGraph pairwise = createPairwise();
			SceneWorkingGraph expected = createWorkingGraph(pairwise);

			var output = new ByteArrayOutputStream();
			MultiViewIO.save(expected,new OutputStreamWriter(output));

			var input = new ByteArrayInputStream(output.toByteArray());
			SceneWorkingGraph found = MultiViewIO.load(new InputStreamReader(input),pairwise,null);
			checkIdentical(expected,found);
		}
	}

	private void checkIdentical(SceneWorkingGraph a , SceneWorkingGraph b ) {
		assertEquals(a.viewList.size(), b.viewList.size());
		assertEquals(a.views.size(), b.views.size());

		for (int viewIdx = 0; viewIdx < a.viewList.size(); viewIdx++) {
			SceneWorkingGraph.View va = a.viewList.get(viewIdx);
			SceneWorkingGraph.View vb = b.viewList.get(viewIdx);

			assertSame(va,a.views.get(va.pview.id));
			assertSame(vb,b.views.get(vb.pview.id));

			assertEquals(va.intrinsic.f, vb.intrinsic.f);
			assertEquals(va.intrinsic.k1, vb.intrinsic.k1);
			assertEquals(va.intrinsic.k2, vb.intrinsic.k2);

			assertEquals(0.0,va.world_to_view.T.distance(vb.world_to_view.T),UtilEjml.EPS);
			assertTrue(MatrixFeatures_DDRM.isEquals(va.world_to_view.R,vb.world_to_view.R,UtilEjml.EPS));

			assertEquals(va.imageDimension.width, vb.imageDimension.width);
			assertEquals(va.imageDimension.height, vb.imageDimension.height);

			assertEquals(va.inliers.views.size, vb.inliers.views.size);
			assertEquals(va.inliers.observations.size, vb.inliers.observations.size);

			final int numViews = va.inliers.views.size;
			for (int i = 0; i < numViews; i++) {
				assertEquals(va.inliers.views.get(i).id, vb.inliers.views.get(i).id);

				GrowQueue_I32 oa = va.inliers.observations.get(i);
				GrowQueue_I32 ob = vb.inliers.observations.get(i);
				assertEquals(oa.size, ob.size);
				oa.forIdx((idx,v)->assertEquals(v,ob.get(idx)));
			}
		}
	}

	private SceneWorkingGraph createWorkingGraph( PairwiseImageGraph pairwise) {
		var ret = new SceneWorkingGraph();

		pairwise.nodes.forIdx((i,v)->ret.addView(v));

		var candidates = GrowQueue_I32.range(0,pairwise.nodes.size);

		ret.viewList.forEach(v->{
			v.intrinsic.f = rand.nextDouble();
			v.intrinsic.k1 = rand.nextDouble();
			v.intrinsic.k2 = rand.nextDouble();

			SpecialEuclideanOps_F64.eulerXyz(
					rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
					rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),v.world_to_view);

			v.imageDimension.width = rand.nextInt();
			v.imageDimension.height = rand.nextInt();

			RandomMatrices_DDRM.fillUniform(v.projective,rand);

			PrimitiveArrays.shuffle(candidates.data,0,candidates.size,rand);
			int numViews = rand.nextInt(Math.min(5,candidates.size));
			int numObs = rand.nextInt(20)+1;
			v.inliers.views.resize(numViews);
			v.inliers.observations.resize(numViews);
			for (int i = 0; i < numViews; i++) {
				v.inliers.views.set(i,pairwise.nodes.get(candidates.get(i)));
				GrowQueue_I32 obs = v.inliers.observations.get(i);
				obs.resize(numObs);
				for (int j = 0; j < numObs; j++) {
					obs.data[j] = rand.nextInt();
				}
			}
		});

		return ret;
	}
}