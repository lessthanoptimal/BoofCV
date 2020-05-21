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

package boofcv.alg.sfm.structure;

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.detdesc.DetectDescribePointAbstract;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestPairwiseImageMatching extends GenericSceneStructureChecks {
	CameraPinholeBrown intrinsic = new CameraPinholeBrown(300,300,0,250,200,500,400);

	@Test
	public void fullyConnected_calibrated() {

		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = create(detector);
		alg.getConfigRansac().iterations = 100;

		PairwiseImageGraph graph = computeGraphScenario0(detector, alg);

		assertEquals(5,graph.nodes.size());
		assertEquals(4+3+2+1,graph.edges.size());

		for (int i = 0; i < graph.nodes.size(); i++) {
			PairwiseImageGraph.View n = graph.nodes.get(i);

			assertEquals(4,n.connections.size());
			assertTrue(n.observationNorm.size <= 400 && n.observationNorm.size >= 300);
			assertEquals(n.observationPixels.size,n.observationNorm.size);
		}

		for (int i = 0; i < graph.edges.size(); i++) {
			PairwiseImageGraph.Motion e = graph.edges.get(i);
			assertTrue(e.metric);
		}
	}

	private PairwiseImageGraph computeGraphScenario0(MockDetector detector, PairwiseImageMatching alg) {
		String cameraName = "camera";

		Point2Transform2_F64 p2n = new LensDistortionBrown(intrinsic).undistort_F64(true,false);
		alg.addCamera( cameraName , p2n , intrinsic );

		for (int i = 0; i < 5; i++) {
			Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-0.5*i,0,0,0,0,0,null);

			detector.cameraToWorld.set(cameraToWorld);
			alg.addImage(new GrayF32(intrinsic.width,intrinsic.height),cameraName);
		}

		assertTrue(alg.process());

		return alg.getGraph();
	}

	@Test
	public void fullyConnected_uncalibrated() {

		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = create(detector);
		alg.getConfigRansac().iterations = 100;

		PairwiseImageGraph graph = computeGraphScenario1(detector, alg);

		assertEquals(5,graph.nodes.size());
		assertEquals(4+3+2+1,graph.edges.size());

		for (int i = 0; i < graph.nodes.size(); i++) {
			PairwiseImageGraph.View n = graph.nodes.get(i);
			assertEquals(4,n.connections.size());
			assertEquals( 0 , n.observationNorm.size);
			assertTrue(n.observationPixels.size <= 400 && n.observationPixels.size >= 300);
		}

		for (int i = 0; i < graph.edges.size(); i++) {
			PairwiseImageGraph.Motion e = graph.edges.get(i);
			assertFalse(e.metric);
		}
	}

	private PairwiseImageGraph computeGraphScenario1(MockDetector detector, PairwiseImageMatching alg) {
		String cameraName = "camera";


		alg.addCamera(cameraName);

		for (int i = 0; i < 5; i++) {
			Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(-0.5*i,0,0,0,0,0,null);

			detector.cameraToWorld.set(cameraToWorld);
			alg.addImage(new GrayF32(intrinsic.width,intrinsic.height),cameraName);
		}

		assertTrue(alg.process());

		return alg.getGraph();
	}

	/**
	 * The graph will not be fully connected in this scenario. There are two independent islands
	 */
	@Test
	public void withIslands() {
		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = create(detector);
		alg.getConfigRansac().iterations = 100;
		String cameraName = "camera";

		Map<String, Point2Transform2_F64> camerasPixelToNorm = new HashMap<>();

		camerasPixelToNorm.put(cameraName, new LensDistortionBrown(intrinsic).undistort_F64(true,false));
		alg.addCamera(cameraName,camerasPixelToNorm.get(cameraName),intrinsic);

		// there will be two independent set of views in the graph
		for (int i = 0; i < 7; i++) {
			double x = i < 5 ? 0 : 10000+0.5*5;
			Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.eulerXyz(x-0.5*i,0,0,0,0,0,null);
			detector.cameraToWorld.set(cameraToWorld);
			alg.addImage(new GrayF32(intrinsic.width,intrinsic.height),cameraName);
		}

		assertTrue(alg.process());

		PairwiseImageGraph graph = alg.getGraph();

		assertEquals(7,graph.nodes.size());
		assertEquals(4+3+2+1+1,graph.edges.size());

		for (int i = 0; i < 5; i++) {
			PairwiseImageGraph.View n = graph.nodes.get(i);
			assertEquals(4,n.connections.size());
			assertTrue(n.observationNorm.size <= 400 && n.observationNorm.size >= 300);
		}

		for (int i = 5; i < 7; i++) {
			PairwiseImageGraph.View n = graph.nodes.get(i);
			assertEquals(1,n.connections.size());
			assertTrue(n.observationNorm.size <= 400 && n.observationNorm.size >= 300);
		}
	}

	@Test
	public void fitEpipolar() {
		createWorld(2,3);

		List<Point3D_F64> worldPoints = new ArrayList<>();
		findViewable(new int[]{0,1},worldPoints);

		List<Point2D_F64> pointsA = new ArrayList<>();
		List<Point2D_F64> pointsB = new ArrayList<>();
		renderObservations(0,true,worldPoints,pointsA);
		renderObservations(1,true,worldPoints,pointsB);

		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex::new);
		for (int i = 0; i < pointsA.size(); i++) {
			matches.grow().setAssociation(i,i,0);
		}

		PairwiseImageMatching alg = create(new MockDetector());
		alg.declareModelFitting();

		PairwiseImageGraph.Motion edge = new PairwiseImageGraph.Motion();
		alg.fitEpipolar(matches,pointsA,pointsB,alg.ransacFundamental,edge);

		assertTrue(edge.associated.size() >= matches.size*0.95 );
		assertFalse(matches.contains(edge.associated.get(0))); // it should be a copy and not have the same instance

		// see if it computed the matrix correctly
		for (int i = 0; i < edge.associated.size(); i++) {
			AssociatedIndex a = edge.associated.get(i);
			Point2D_F64 p1 = pointsA.get(a.src);
			Point2D_F64 p2 = pointsB.get(a.dst);

			assertEquals(0,MultiViewOps.constraint(edge.F,p1,p2),0.001);
		}
	}

	@Test
	public void reset() {
		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = create(detector);
		alg.getConfigRansac().iterations = 100;

		PairwiseImageGraph graph0 = computeGraphScenario0(detector, alg);
		alg.reset();
		PairwiseImageGraph graph1 = computeGraphScenario0(detector, alg);

		assertTrue(graph0 != graph1);
		assertEquals(graph0.nodes.size(),graph1.nodes.size());
		assertEquals(graph0.edges.size(),graph1.edges.size());
	}

	public PairwiseImageMatching create( MockDetector detector ) {
		ScoreAssociation scorer = FactoryAssociation.defaultScore(detector.getDescriptionType());
		AssociateDescription<TupleDesc> associate =
				FactoryAssociation.greedy(new ConfigAssociateGreedy(true,0.5),scorer);
		return new PairwiseImageMatching(detector,associate);
	}

	public class MockDetector extends DetectDescribePointAbstract<GrayF32,TupleDesc_F64>
	{
		List<Point3D_F64> locations3D = new ArrayList<>();
		List<TupleDesc_F64> descriptions = new ArrayList<>();

		Se3_F64 cameraToWorld = new Se3_F64();

		GrowQueue_I32 visible = new GrowQueue_I32();
		FastQueue<Point2D_F64> pixels = new FastQueue<>(Point2D_F64::new);

		public MockDetector() {
			// Two sets of points so that there can be a gap in the views
			locations3D.addAll(UtilPoint3D_F64.random(new Point3D_F64(0,0,5),
					-2,2,400,rand));
			locations3D.addAll(UtilPoint3D_F64.random(new Point3D_F64(10000,0,5),
					-2,2,400,rand));

			for (int i = 0; i < locations3D.size(); i++) {
				descriptions.add( new TupleDesc_F64(new double[]{
						5*rand.nextGaussian(),5*rand.nextGaussian(),5*rand.nextGaussian()}));
			}
		}

		@Override
		public TupleDesc_F64 getDescription(int index) {
			return descriptions.get(visible.get(index));
		}

		@Override
		public ImageType<GrayF32> getInputType() {
			return ImageType.SB_F32;
		}

		@Override
		public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(3);
		}

		@Override
		public Class getDescriptionType() {
			return TupleDesc_F64.class;
		}

		@Override
		public void detect(GrayF32 input) {
			visible.reset();
			pixels.reset();
			Se3_F64 worldToCamera = cameraToWorld.invert(null);
			Point2D_F64 pixel = new Point2D_F64();

			WorldToCameraToPixel w2p = new WorldToCameraToPixel();
			w2p.configure(intrinsic,worldToCamera);

			for (int i = 0; i < locations3D.size(); i++) {
				Point3D_F64 w = locations3D.get(i);

				if( !w2p.transform(w,pixel) )
					continue;
				if( pixel.x < 0 || pixel.y < 0 || pixel.x >= input.width-1 || pixel.y >= input.height-1 )
					continue;

				visible.add(i);
				pixels.grow().set(pixel);
			}
		}
		@Override
		public int getNumberOfFeatures() {
			return visible.size;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return pixels.get(featureIndex);
		}
	}

}