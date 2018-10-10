/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure;

import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.sfm.structure.MetricSceneGraph.ViewState;
import boofcv.alg.sfm.structure.PairwiseImageGraph.Camera;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestMetricSceneGraph {

	private Random rand = new Random(234);

	@Test
	void consturctor() {
		PairwiseImageGraph pairwise = createPairwise();

		MetricSceneGraph graph = new MetricSceneGraph(pairwise);

		assertEquals(2,graph.cameras.size());
		assertEquals(5,graph.nodes.size());
		assertEquals(10,graph.edges.size());

		Camera camMoo = graph.cameras.get("moo");
		Camera camNoo = graph.cameras.get("noo");

		assertNotNull(camMoo);
		assertNotNull(camNoo);

		for (int i = 0; i < graph.nodes.size(); i++) {
			MetricSceneGraph.View v = graph.nodes.get(i);
			assertEquals(i,v.index);
			assertEquals(ViewState.UNPROCESSED,v.state);
			assertTrue(v.observationNorm.size>0);
			assertEquals(v.observationNorm.size,v.features3D.length);
		}

		for (int i = 0; i < graph.edges.size(); i++) {
			MetricSceneGraph.Motion v = graph.edges.get(i);
			assertEquals(i,v.index);
			assertTrue(0 != v.associated.size());
			assertSame(graph.nodes.get(i/2),v.viewSrc);
			assertSame(graph.nodes.get((i/2+1)%5),v.viewDst);

			assertSame(v.viewDst,v.destination(v.viewSrc));
			assertSame(v.viewSrc,v.destination(v.viewDst));
		}
	}

	private PairwiseImageGraph createPairwise() {
		PairwiseImageGraph pairwise = new PairwiseImageGraph();

		pairwise.addCamera(createCamera("moo"));
		pairwise.addCamera(createCamera("noo"));

		for (int i = 0; i < 5; i++) {
			FastQueue<TupleDesc> descs =(FastQueue) UtilFeature.createQueueF64(8);
			pairwise.nodes.add( new PairwiseImageGraph.View(i,descs) );

			PairwiseImageGraph.View v = pairwise.nodes.get(i);
			v.index = i;

			if( i%2 == 0 )
				v.camera = pairwise.cameras.get("moo");
			else
				v.camera = pairwise.cameras.get("noo");

			int N = rand.nextInt(20)+5;
			for (int j = 0; j < N; j++) {
				v.descriptions.grow();
				v.observationNorm.grow().set(rand.nextGaussian(),rand.nextGaussian());
				v.observationPixels.grow().set(rand.nextDouble(),rand.nextDouble());
			}
		}

		for (int i = 0; i < 10; i++) {
			PairwiseImageGraph.Motion e = new PairwiseImageGraph.Motion();
			pairwise.edges.add( e );

			e.index = i;
			e.viewSrc = pairwise.nodes.get(i/2);
			e.viewDst = pairwise.nodes.get((i/2+1)%5);
			e.metric = true;
			e.associated.add( new AssociatedIndex(0,1,0.1));
			e.associated.add( new AssociatedIndex(3,3,0.1));
			e.F = new DMatrixRMaj(3,3);
		}
		return pairwise;
	}

	private static Camera createCamera( String name ) {
		CameraPinhole model = new CameraPinhole();
		Point2Transform2_F64 p2n = new LensDistortionPinhole(model).distort_F64(true,false);

		return new Camera(name,p2n,model);
	}

	@Test
	void motion_srcToDst() {
		MetricSceneGraph.Motion m = new MetricSceneGraph.Motion();
		m.viewSrc = new MetricSceneGraph.View();
		m.viewDst = new MetricSceneGraph.View();
		m.a_to_b = SpecialEuclideanOps_F64.eulerXyz(0,0,1,0,0,0,null);
	}

	@Test
	void motion_destination() {
		MetricSceneGraph.Motion m = new MetricSceneGraph.Motion();
		m.viewSrc = new MetricSceneGraph.View();
		m.viewDst = new MetricSceneGraph.View();

		assertSame(m.viewDst,m.destination(m.viewSrc));
		assertSame(m.viewSrc,m.destination(m.viewDst));

		try {
			MetricSceneGraph.View v = new MetricSceneGraph.View();
			m.destination(v);
			fail("exception should have been thrown");
		} catch( RuntimeException ignore){}
	}
}