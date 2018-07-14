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

import boofcv.abst.geo.bundle.BundleAdjustmentSceneStructure;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraMotion;
import boofcv.alg.sfm.structure.PairwiseImageGraph.CameraView;
import boofcv.alg.sfm.structure.PairwiseImageGraph.Feature3D;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.feature.AssociatedIndex;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEstimateSceneUnordered extends GenericSceneStructureChecks {

	Se3_F64 transformAtoB = new Se3_F64();

	@Test
	public void perfectScene_calibrated() {
		MockPairwiseImageMatching mock = new MockPairwiseImageMatching();
		EstimateSceneUnordered alg = new EstimateSceneUnordered(mock);

		alg.camerasPixelToNorm.put(mock.cameraName,new LensDistortionPinhole(mock.intrinsic).undistort_F64(true,false));
		alg.camerasIntrinsc.put(mock.cameraName,mock.intrinsic);
		alg.cameraToIndex.put(mock.cameraName,0);
		alg.calibrated = true;

		assertTrue(alg.estimate());

		BundleAdjustmentSceneStructure found = alg.getSceneStructure();

		assertEquals(1,found.cameras.length);
		assertEquals(3,found.views.length);
		assertTrue(found.points.length > 350); // prefect data and all the points are in view

		assertTrue(mock.equivalent(0,1,found.views[0].worldToView,found.views[1].worldToView));
		assertTrue(mock.equivalent(0,2,found.views[0].worldToView,found.views[2].worldToView));
	}

	@Test
	public void addTriangulatedStereoFeatures() {
		addTriangulatedStereoFeatures(true);
		addTriangulatedStereoFeatures(false);
	}

	private void addTriangulatedStereoFeatures( boolean a_to_b ) {
		CameraView src = new CameraView(0,null);
		CameraView dst = new CameraView(0,null);
		CameraMotion edge = new CameraMotion();
		double scale = 100;
		int N = 1;

		createSceneEdge(a_to_b, N, src, dst, edge, scale);

		// The other view will have its pose set to identity
		CameraView other = a_to_b ? dst : src;
		CameraView base = a_to_b ? src : dst;

		assertTrue(MatrixFeatures_DDRM.isIdentity(other.viewToWorld.R,UtilEjml.TEST_F64));
		assertTrue(other.viewToWorld.T.distance(0,0,0) <= UtilEjml.TEST_F64);

		Se3_F64 baseToWorld = base.viewToWorld.copy();

		// Expected 3D location of all points in world
		List<Point3D_F64> worldPoints = new ArrayList<>();
		// These will be filled in later with already known features
		for (int i = 0; i < src.features3D.length; i++) {
			if( a_to_b ) {
				worldPoints.add( src.features3D[i].worldPt.copy() );
			} else {
				worldPoints.add( dst.features3D[i].worldPt.copy() );
			}
			src.features3D[i] = null;
			dst.features3D[i] = null;
		}

		EstimateSceneUnordered alg = new EstimateSceneUnordered();
		alg.graph = new PairwiseImageGraph();
		alg.addTriangulatedStereoFeatures(base,edge,scale);

		if( a_to_b ) {
			assertTrue(SpecialEuclideanOps_F64.isIdentical(src.viewToWorld,baseToWorld,1e-6,1e-4));
			Se3_F64 dstToWorld = new Se3_F64();
			this.transformAtoB.invert(null).concat(src.viewToWorld,dstToWorld);
			assertTrue(SpecialEuclideanOps_F64.isIdentical(dst.viewToWorld,dstToWorld,1e-6,1e-4));
		} else {
			assertTrue(SpecialEuclideanOps_F64.isIdentical(dst.viewToWorld,baseToWorld,1e-6,1e-4));
			Se3_F64 srcToWorld = new Se3_F64();
			this.transformAtoB.concat(dst.viewToWorld,srcToWorld);
			assertTrue(SpecialEuclideanOps_F64.isIdentical(src.viewToWorld,srcToWorld,1e-6,1e-4));
		}

		assertEquals(N,alg.graph.features3D.size());
		for (int i = 0; i < N; i++) {
			assertNotNull(src.features3D[i]);
			assertNotNull(dst.features3D[i]);
		}

		for (int i = 0; i < N; i++) {
			assertTrue(worldPoints.get(i).distance2(alg.graph.features3D.get(i).worldPt) < 1e-6);
		}
	}

	@Test
	public void determineScale() throws Exception {
		determineScale(true);
		determineScale(false);
	}

	private void determineScale(boolean a_to_b) throws Exception {
		int N = 100;
		CameraView src = new CameraView(0,null);
		CameraView dst = new CameraView(0,null);
		CameraMotion edge = new CameraMotion();

		double scale = 2;

		createSceneEdge(a_to_b, N, src, dst, edge, scale);

		double found = EstimateSceneUnordered.determineScale(a_to_b?src:dst,edge);

		assertEquals(scale,found,UtilEjml.TEST_F64);
	}

	private void createSceneEdge(boolean a_to_b, int numPoints, CameraView src, CameraView dst,
								 CameraMotion edge, double scale ) {
		Se3_F64 baseToWorld = new Se3_F64();
		baseToWorld.set(5,7,1,EulerType.XYZ,1,-0.3,2.1);

		src.features3D = new Feature3D[numPoints];
		dst.features3D = new Feature3D[numPoints];

		edge.a_to_b.set(0.1,-0.2,-0.5,EulerType.XYZ,0.1,0.05,-0.03);
		transformAtoB.set(edge.a_to_b);
		edge.viewSrc = src;
		edge.viewDst = dst;
		if( a_to_b ) {
			src.viewToWorld.set(baseToWorld);
		} else {
			dst.viewToWorld.set(baseToWorld);
		}

		for (int i = 0; i < numPoints; i++) {
			// Point in base frame
			Point3D_F64 baseP = new Point3D_F64(rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian()+3);

			// save the feature in world frame
			Feature3D world3D = new Feature3D();
			baseToWorld.transform(baseP,world3D.worldPt);
			if( a_to_b )
				src.features3D[i] = world3D;
			else
				dst.features3D[i] = world3D;

			// save this point as one computed using stereo
			Feature3D stereo3D = new Feature3D();
			if( a_to_b )
				stereo3D.worldPt.set(baseP);
			else
				edge.a_to_b.transformReverse(baseP,stereo3D.worldPt);

			stereo3D.views.add(src);
			stereo3D.views.add(dst);
			stereo3D.obsIdx.add(i);
			stereo3D.obsIdx.add(i);

			edge.stereoTriangulations.add(stereo3D);
			edge.associated.add( new AssociatedIndex(i,i,0));
		}

		for ( Feature3D f : edge.stereoTriangulations ) {
			f.worldPt.scale(1.0/scale);
		}

		edge.a_to_b.T.scale(1.0/scale);
	}

	public class MockPairwiseImageMatching extends PairwiseImageMatching {

		String cameraName = "camera";
		List<Se3_F64> worldToCamera = new ArrayList<>();
		List<Point3D_F64> worldPoints = new ArrayList<>();
		CameraPinhole intrinsic = new CameraPinhole(300,300,0,250,200,500,400);
		PairwiseImageGraph graph = new PairwiseImageGraph();

		@Override
		public boolean process(Map camerasPixelToNorm, Map camerasIntrinsc) {
			return true;
		}

		@Override
		public PairwiseImageGraph getGraph() {
			int N = 400;

			// two of the frames have significant translation. The 3rd is very bad for stereo
			worldToCamera.add(SpecialEuclideanOps_F64.setEulerXYZ(0.06,0.05,-0.04,0.5,1,2.4,null));
			worldToCamera.add(SpecialEuclideanOps_F64.setEulerXYZ(0.03,0.025,-0.04,0.0,-0.1,3,null));
			worldToCamera.add(SpecialEuclideanOps_F64.setEulerXYZ(0.031,0.0251,-0.04,0.0,-0.1,3,null));

			graph.nodes.add(new CameraView(0,null));
			graph.nodes.add(new CameraView(1,null));
			graph.nodes.add(new CameraView(2,null));

			graph.edges.add( new CameraMotion());
			graph.edges.add( new CameraMotion());
			graph.edges.add( new CameraMotion());

			initializeEdge(graph.edges.get(0),0,1);
			initializeEdge(graph.edges.get(1),0,2);
			initializeEdge(graph.edges.get(2),1,2);

			WorldToCameraToPixel w2p = new WorldToCameraToPixel();
			Point2D_F64 pixel = new Point2D_F64();
			Point2D_F64 norm = new Point2D_F64();
			Point3D_F64 center = new Point3D_F64(0,0,4);
			for (int i = 0; i < N; i++) {
				Point3D_F64 p3 = UtilPoint3D_F64.noiseNormal(center,1,1,1,rand,null);

				boolean visible[] = new boolean[graph.nodes.size()];
				for( int j = 0; j < graph.nodes.size(); j++ ) {
					CameraView v = graph.nodes.get(j);
					w2p.configure(intrinsic,worldToCamera.get(v.index));
					if( w2p.transform(p3,pixel,norm) &&
							pixel.x >= 0 && pixel.y >= 0 && pixel.x < intrinsic.width && pixel.y < intrinsic.height )
					{
						v.observationPixels.add( pixel.copy() );
						v.observationNorm.add( norm.copy() );
						visible[j] = true;
					}
				}
				if( visible[0] && visible[1]) {
					addObservation(graph.edges.get(0),0,1,p3);
				}
				if( visible[0] && visible[2]) {
					addObservation(graph.edges.get(1),0,2,p3);
				}
				if( visible[1] && visible[2]) {
					addObservation(graph.edges.get(2),1,2,p3);
				}

				worldPoints.add(p3);
			}

			for (int i = 0; i < graph.nodes.size(); i++) {
				CameraView v = graph.nodes.get(i);
				v.camera = cameraName;
				v.features3D = new Feature3D[v.observationNorm.size];
			}


			return graph;
		}

		public boolean equivalent( int indexA , int indexB , Se3_F64 foundWorldToViewA , Se3_F64 foundWorldToViewB )
		{
			Se3_F64 expectedA2B = worldToCamera.get(indexA).invert(null).concat(worldToCamera.get(indexB),null);
			Se3_F64 foundA2B = foundWorldToViewA.invert(null).concat(foundWorldToViewB,null);

			double scale = expectedA2B.T.norm()/foundA2B.T.norm();
			if( Math.abs(expectedA2B.T.z-scale*foundA2B.T.z) > 1e-8 )
				scale *= -1;
			foundA2B.T.scale(scale);

			return SpecialEuclideanOps_F64.isIdentical(expectedA2B,foundA2B,1e-4,1e-4);
		}

		private void initializeEdge( CameraMotion e ,  int v0 , int v1 ) {
			e.viewSrc = graph.nodes.get(v0);
			e.viewDst = graph.nodes.get(v1);
			worldToCamera.get(v0).invert(null).concat(worldToCamera.get(v1),e.a_to_b);
			e.viewSrc.connections.add(e);
			e.viewDst.connections.add(e);

		}
		private void addObservation(CameraMotion e ,  int v0 , int v1 , Point3D_F64 worldPt ) {
			Feature3D f = new Feature3D();
			worldToCamera.get(v0).transform(worldPt,f.worldPt);

			e.associated.add( new AssociatedIndex(
					graph.nodes.get(v0).observationNorm.size-1,
					graph.nodes.get(v1).observationNorm.size-1,0));
//			e.stereoTriangulations.add(f);
		}
	}
}