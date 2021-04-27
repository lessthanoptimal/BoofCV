/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.alg.structure.SceneWorkingGraph;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMultiViewIO extends BoofStandardJUnit {

	@Test void save_load_PairwiseImageGraph() {
		for (int trial = 0; trial < 20; trial++) {
			PairwiseImageGraph expected = createPairwise();

			var output = new ByteArrayOutputStream();
			MultiViewIO.save(expected, new OutputStreamWriter(output, UTF_8));

			var input = new ByteArrayInputStream(output.toByteArray());
			PairwiseImageGraph found = MultiViewIO.load(new InputStreamReader(input, UTF_8), (PairwiseImageGraph)null);
			checkIdentical(expected, found);
		}
	}

	private void checkIdentical( PairwiseImageGraph a, PairwiseImageGraph b ) {
		assertEquals(a.edges.size, b.edges.size);
		assertEquals(a.nodes.size, b.nodes.size);
		assertEquals(a.mapNodes.size(), b.mapNodes.size());

		for (int viewIdx = 0; viewIdx < a.nodes.size; viewIdx++) {
			PairwiseImageGraph.View va = a.nodes.get(viewIdx);
			PairwiseImageGraph.View vb = b.nodes.get(viewIdx);

			assertSame(va, a.mapNodes.get(va.id));
			assertSame(vb, b.mapNodes.get(vb.id));

			assertEquals(va.id, vb.id);
			assertEquals(va.totalObservations, vb.totalObservations);
			assertEquals(va.connections.size, vb.connections.size);

			for (int i = 0; i < va.connections.size; i++) {
				PairwiseImageGraph.Motion ma = va.connections.get(i);
				PairwiseImageGraph.Motion mb = vb.connections.get(i);

				// Make sure it didn't declare a new instance of the view
				assertSame(ma.src, a.mapNodes.get(ma.src.id));
				assertSame(mb.dst, b.mapNodes.get(mb.dst.id));

				// Make sure the same instance is in the edges list
				assertTrue(b.edges.contains(mb));

				assertEquals(ma.index, mb.index);
				assertEquals(ma.is3D, mb.is3D);
				assertEquals(ma.src.id, mb.src.id);
				assertEquals(ma.dst.id, mb.dst.id);
				assertEquals(ma.score3D, mb.score3D);
				assertEquals(ma.inliers.size, mb.inliers.size);
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

		ret.nodes.resize(rand.nextInt(10) + 1);
		for (int viewIdx = 0; viewIdx < ret.nodes.size; viewIdx++) {
			PairwiseImageGraph.View v = ret.nodes.get(viewIdx);
			v.id = rand.nextInt() + "";
			v.totalObservations = rand.nextInt(200);
			ret.mapNodes.put(v.id, v);

			// Randomly select connecting nodes from ones which higher indexes to ensure (src,dst) pairs are unique
			var candidates = DogArray_I32.range(viewIdx + 1, ret.nodes.size);
			PrimitiveArrays.shuffle(candidates.data, 0, candidates.size, rand);
			int numConnections = rand.nextInt() + 5;
			for (int i = 0; i < Math.min(numConnections, candidates.size); i++) {
				PairwiseImageGraph.Motion m = ret.edges.grow();
				m.is3D = rand.nextBoolean();
				m.score3D = rand.nextDouble();
				m.src = v;
				m.dst = ret.nodes.get(candidates.get(i));
				m.index = ret.edges.size - 1;
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

	@Test void save_load_SceneWorkingGraph() {
		for (int trial = 0; trial < 20; trial++) {
			PairwiseImageGraph pairwise = createPairwise();
			SceneWorkingGraph expected = createWorkingGraph(pairwise);

			var output = new ByteArrayOutputStream();
			MultiViewIO.save(expected, new OutputStreamWriter(output, UTF_8));

			var input = new ByteArrayInputStream(output.toByteArray());
			SceneWorkingGraph found = MultiViewIO.load(new InputStreamReader(input, UTF_8), pairwise, null);
			checkIdentical(expected, found);
		}
	}

	private void checkIdentical( SceneWorkingGraph a, SceneWorkingGraph b ) {
		assertEquals(a.workingViews.size(), b.workingViews.size());
		assertEquals(a.views.size(), b.views.size());

		for (int viewIdx = 0; viewIdx < a.workingViews.size(); viewIdx++) {
			SceneWorkingGraph.View va = a.workingViews.get(viewIdx);
			SceneWorkingGraph.View vb = b.workingViews.get(viewIdx);

			assertSame(va, a.views.get(va.pview.id));
			assertSame(vb, b.views.get(vb.pview.id));

			assertEquals(va.intrinsic.f, vb.intrinsic.f);
			assertEquals(va.intrinsic.k1, vb.intrinsic.k1);
			assertEquals(va.intrinsic.k2, vb.intrinsic.k2);

			assertEquals(0.0, va.world_to_view.T.distance(vb.world_to_view.T), UtilEjml.EPS);
			assertTrue(MatrixFeatures_DDRM.isEquals(va.world_to_view.R, vb.world_to_view.R, UtilEjml.EPS));

			assertEquals(va.imageDimension.width, vb.imageDimension.width);
			assertEquals(va.imageDimension.height, vb.imageDimension.height);

			assertEquals(va.inliers.views.size, vb.inliers.views.size);
			assertEquals(va.inliers.observations.size, vb.inliers.observations.size);

			final int numViews = va.inliers.views.size;
			for (int i = 0; i < numViews; i++) {
				assertEquals(va.inliers.views.get(i).id, vb.inliers.views.get(i).id);

				DogArray_I32 oa = va.inliers.observations.get(i);
				DogArray_I32 ob = vb.inliers.observations.get(i);
				assertEquals(oa.size, ob.size);
				oa.forIdx(( idx, v ) -> assertEquals(v, ob.get(idx)));
			}
		}
	}

	private SceneWorkingGraph createWorkingGraph( PairwiseImageGraph pairwise ) {
		var ret = new SceneWorkingGraph();

		pairwise.nodes.forIdx(( i, v ) -> ret.addView(v));

		var candidates = DogArray_I32.range(0, pairwise.nodes.size);

		ret.workingViews.forEach(v -> {
			v.intrinsic.f = rand.nextDouble();
			v.intrinsic.k1 = rand.nextDouble();
			v.intrinsic.k2 = rand.nextDouble();

			SpecialEuclideanOps_F64.eulerXyz(
					rand.nextDouble(), rand.nextDouble(), rand.nextDouble(),
					rand.nextDouble(), rand.nextDouble(), rand.nextDouble(), v.world_to_view);

			v.imageDimension.width = rand.nextInt();
			v.imageDimension.height = rand.nextInt();

			RandomMatrices_DDRM.fillUniform(v.projective, rand);

			PrimitiveArrays.shuffle(candidates.data, 0, candidates.size, rand);
			int numViews = rand.nextInt(Math.min(5, candidates.size));
			int numObs = rand.nextInt(20) + 1;
			v.inliers.views.resize(numViews);
			v.inliers.observations.resize(numViews);
			for (int i = 0; i < numViews; i++) {
				v.inliers.views.set(i, pairwise.nodes.get(candidates.get(i)));
				DogArray_I32 obs = v.inliers.observations.get(i);
				obs.resize(numObs);
				for (int j = 0; j < numObs; j++) {
					obs.data[j] = rand.nextInt();
				}
			}
		});

		return ret;
	}

	@Test void save_load_SceneStructureMetric() {
		for (int trial = 0; trial < 20; trial++) {
			SceneStructureMetric expected = createSceneStructureMetric();

			var output = new ByteArrayOutputStream();
			MultiViewIO.save(expected, new OutputStreamWriter(output, UTF_8));

			var input = new ByteArrayInputStream(output.toByteArray());
			SceneStructureMetric found = MultiViewIO.load(new InputStreamReader(input, UTF_8), (SceneStructureMetric)null);
			assertTrue(expected.isIdentical(found, UtilEjml.TEST_F64));
		}
	}

	private SceneStructureMetric createSceneStructureMetric() {
		var ret = new SceneStructureMetric(rand.nextBoolean());

		int numMotions = 1 + rand.nextInt(4);
		ret.initialize(1 + rand.nextInt(4), rand.nextInt(4),
				numMotions, rand.nextInt(4), rand.nextInt(4));

		for (int i = 0; i < numMotions; i++) {
			SceneStructureMetric.Motion m = ret.motions.grow();
			SpecialEuclideanOps_F64.eulerXyz(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), m.motion);
			m.known = rand.nextBoolean();
		}

		for (int i = 0; i < ret.views.size; i++) {
			SceneStructureMetric.View v = ret.views.get(i);
			int parent = rand.nextInt(i + 1) - 1;
			v.parent = parent >= 0 ? ret.views.get(parent) : null;
			v.parent_to_view = rand.nextInt(ret.motions.size);
			v.camera = rand.nextInt(ret.cameras.size);
		}

		for (int i = 0; i < ret.rigids.size; i++) {
			SceneStructureMetric.Rigid r = ret.rigids.get(i);
			r.known = rand.nextBoolean();
			SpecialEuclideanOps_F64.eulerXyz(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), r.object_to_world);
			r.indexFirst = rand.nextInt(10);
			r.points = new SceneStructureCommon.Point[rand.nextInt(4)];
			for (int j = 0; j < r.points.length; j++) {
				r.points[j] = new SceneStructureCommon.Point(ret.isHomogenous() ? 4 : 3);
				randomizePoint(r.points[j]);
			}
		}

		for (int i = 0; i < ret.points.size; i++) {
			randomizePoint(ret.points.data[i]);
		}

		for (int i = 0; i < ret.cameras.size; i++) {
			SceneStructureCommon.Camera c = ret.cameras.get(i);
			c.known = rand.nextBoolean();
			var b = new BundlePinholeSimplified();
			b.f = rand.nextGaussian();
			b.k1 = rand.nextGaussian();
			b.k2 = rand.nextGaussian();
			c.model = b;
		}

		return ret;
	}

	private void randomizePoint( SceneStructureCommon.Point p ) {
		p.views.resize(rand.nextInt(4));
		p.views.forIdx(( iv, v ) -> p.views.set(iv, rand.nextInt(4)));
		for (int i = 0; i < p.coordinate.length; i++) {
			p.coordinate[i] = rand.nextGaussian();
		}
	}
}
