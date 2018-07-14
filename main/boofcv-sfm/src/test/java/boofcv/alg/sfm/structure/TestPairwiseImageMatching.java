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

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPairwiseImageMatching extends GenericSceneStructureChecks {
	CameraPinholeRadial intrinsic = new CameraPinholeRadial(300,300,0,250,200,500,400);

	@Test
	public void fullyConnected_calibrated() {

		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = new PairwiseImageMatching(detector);
		alg.getConfigRansac().maxIterations = 100;

		PairwiseImageGraph graph = computeGraphScenario0(detector, alg);

		assertEquals(5,graph.nodes.size());
		assertEquals(4+3+2+1,graph.edges.size());

		for (int i = 0; i < graph.nodes.size(); i++) {
			PairwiseImageGraph.CameraView n = graph.nodes.get(i);
			assertEquals(4,n.connections.size());
			assertTrue(n.features3D.length <= 400 && n.features3D.length >= 300);

			for( PairwiseImageGraph.CameraMotion m : n.connections ) {
				assertTrue(MatrixFeatures_DDRM.isIdentity(m.a_to_b.R,1e-8));
				assertTrue(m.a_to_b.T.x > 0.99);
			}
		}
	}

	private PairwiseImageGraph computeGraphScenario0(MockDetector detector, PairwiseImageMatching alg) {
		String cameraName = "camera";

		Map<String, Point2Transform2_F64> camerasPixelToNorm = new HashMap<>();
		Map<String, CameraPinhole> camerasIntrinsc = new HashMap<>();

		camerasPixelToNorm.put(cameraName, new LensDistortionRadialTangential(intrinsic).undistort_F64(true,false));
		camerasIntrinsc.put(cameraName,intrinsic);

		for (int i = 0; i < 5; i++) {
			Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.setEulerXYZ(0,0,0,-0.5*i,0,0,null);

			detector.cameraToWorld.set(cameraToWorld);
			alg.addImage(new GrayF32(intrinsic.width,intrinsic.height),cameraName,camerasPixelToNorm.get(cameraName));
		}

		assertTrue(alg.process(camerasPixelToNorm,camerasIntrinsc));

		return alg.getGraph();
	}

	/**
	 * The graph will not be fully connected in this scenario
	 */
	@Test
	public void withIslands() {
		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = new PairwiseImageMatching(detector);
		alg.getConfigRansac().maxIterations = 100;
		String cameraName = "camera";

		Map<String, Point2Transform2_F64> camerasPixelToNorm = new HashMap<>();
		Map<String, CameraPinhole> camerasIntrinsc = new HashMap<>();

		camerasPixelToNorm.put(cameraName, new LensDistortionRadialTangential(intrinsic).undistort_F64(true,false));
		camerasIntrinsc.put(cameraName,intrinsic);

		// there will be two independent set of views in the graph
		for (int i = 0; i < 7; i++) {
			double x = i < 5 ? 0 : 50+0.5*5;
			Se3_F64 cameraToWorld = SpecialEuclideanOps_F64.setEulerXYZ(0,0,0,x-0.5*i,0,0,null);
			detector.cameraToWorld.set(cameraToWorld);
			alg.addImage(new GrayF32(intrinsic.width,intrinsic.height),cameraName,camerasPixelToNorm.get(cameraName));
		}

		assertTrue(alg.process(camerasPixelToNorm,camerasIntrinsc));

		PairwiseImageGraph graph = alg.getGraph();

		assertEquals(7,graph.nodes.size());
		assertEquals(4+3+2+1+1,graph.edges.size());

		for (int i = 0; i < 5; i++) {
			PairwiseImageGraph.CameraView n = graph.nodes.get(i);
			assertEquals(4,n.connections.size());
			assertTrue(n.features3D.length <= 400 && n.features3D.length >= 300);

			for( PairwiseImageGraph.CameraMotion m : n.connections ) {
				assertTrue(MatrixFeatures_DDRM.isIdentity(m.a_to_b.R,1e-8));
				assertTrue(m.a_to_b.T.x > 0.99);
			}
		}

		for (int i = 5; i < 7; i++) {
			PairwiseImageGraph.CameraView n = graph.nodes.get(i);
			assertEquals(1,n.connections.size());
			assertTrue(n.features3D.length <= 400 && n.features3D.length >= 300);

			for( PairwiseImageGraph.CameraMotion m : n.connections ) {
				assertTrue(MatrixFeatures_DDRM.isIdentity(m.a_to_b.R,1e-8));
				assertTrue(m.a_to_b.T.x > 0.99);
			}
		}
	}

	@Test
	public void fitEpipolar() {
		createWorld(2,3);

		List<Point3D_F64> worldPoints = new ArrayList<>();
		findViewable(new int[]{0,1},worldPoints);
		Se3_F64 camera_a_to_b = cameraAtoB(0,1);

		List<Point2D_F64> pointsA = new ArrayList<>();
		List<Point2D_F64> pointsB = new ArrayList<>();
		renderObservations(0,false,worldPoints,pointsA);
		renderObservations(1,false,worldPoints,pointsB);

		FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class,true);
		for (int i = 0; i < pointsA.size(); i++) {
			matches.grow().setAssociation(i,i,0);
		}

		PairwiseImageMatching<?> alg = new PairwiseImageMatching(new MockDetector());
		alg.calibrated = true;
		alg.declareModelFitting();

		PairwiseImageGraph.CameraMotion edge = new PairwiseImageGraph.CameraMotion();
		alg.fitEpipolar(matches,pointsA,pointsB,alg.ransacEssential,edge);

		assertTrue(edge.associated.size() >= matches.size*0.95 );
		assertFalse(matches.contains(edge.associated.get(0))); // it should be a copy and not have the same instance

		Se3_F64 found_a_to_b = alg.ransacEssential.getModelParameters();

		camera_a_to_b.T.normalize();
		found_a_to_b.T.normalize();

		assertTrue( camera_a_to_b.T.distance(found_a_to_b.T) < 1e-4 );
		assertTrue(MatrixFeatures_DDRM.isIdentical(camera_a_to_b.R,found_a_to_b.R,1e-3));
	}

	@Test
	public void reset() {
		MockDetector detector = new MockDetector();
		PairwiseImageMatching alg = new PairwiseImageMatching(detector);
		alg.getConfigRansac().maxIterations = 100;

		PairwiseImageGraph graph0 = computeGraphScenario0(detector, alg);
		alg.reset();
		PairwiseImageGraph graph1 = computeGraphScenario0(detector, alg);

		assertTrue(graph0 != graph1);
		assertEquals(graph0.nodes.size(),graph1.nodes.size());
		assertEquals(graph0.edges.size(),graph1.edges.size());
	}

	public class MockDetector implements DetectDescribePoint<GrayF32,TupleDesc_F64>
	{
		List<Point3D_F64> locations3D = new ArrayList<>();
		List<TupleDesc_F64> descriptions = new ArrayList<>();

		Se3_F64 cameraToWorld = new Se3_F64();

		GrowQueue_I32 visible = new GrowQueue_I32();
		FastQueue<Point2D_F64> pixels = new FastQueue<>(Point2D_F64.class,true);

		public MockDetector() {
			// Two sets of points so that there can be a gap in the views
			locations3D.addAll(UtilPoint3D_F64.random(new Point3D_F64(0,0,5),
					-2,2,-2,2,-2,2,400,rand));
			locations3D.addAll(UtilPoint3D_F64.random(new Point3D_F64(50,0,5),
					-2,2,-2,2,-2,2,400,rand));

			for (int i = 0; i < locations3D.size(); i++) {
				descriptions.add( new TupleDesc_F64(new double[]{rand.nextGaussian(),rand.nextGaussian()}));
			}
		}

		@Override
		public TupleDesc_F64 getDescription(int index) {
			return descriptions.get(visible.get(index));
		}

		@Override
		public TupleDesc_F64 createDescription() {
			return new TupleDesc_F64(2);
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
		public boolean hasScale() {
			return false;
		}

		@Override
		public boolean hasOrientation() {
			return false;
		}

		@Override
		public int getNumberOfFeatures() {
			return visible.size;
		}

		@Override
		public Point2D_F64 getLocation(int featureIndex) {
			return pixels.get(featureIndex);
		}

		@Override
		public double getRadius(int featureIndex) {
			return 0;
		}

		@Override
		public double getOrientation(int featureIndex) {
			return 0;
		}
	}

}