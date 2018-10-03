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

import boofcv.alg.sfm.structure.MetricSceneGraph.CameraMotion;
import boofcv.alg.sfm.structure.MetricSceneGraph.CameraView;
import boofcv.alg.sfm.structure.MetricSceneGraph.Feature3D;
import boofcv.struct.feature.AssociatedIndex;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEstimateSceneCalibrated extends GenericSceneStructureChecks {

	@Test
	public void perfectScene_calibrated() {
		fail("Implement");
	}

	@Test
	public void decomposeEssential() {
		fail("Implement");
	}

	@Test
	public void medianTriangulationAngle() {
		fail("Implement");
	}

	@Test
	public void addTriangulatedStereoFeatures() {
		int N = 10;

		// create one good point and all the others will have an angle which is too small
		final CameraMotion edge = new CameraMotion();
		edge.viewSrc = new CameraView();
		edge.viewDst = new CameraView();
		edge.viewSrc.features3D = new Feature3D[N];
		edge.viewDst.features3D = new Feature3D[N];
		edge.triangulationAngle = 0;

		double scale = 1.5;

		for (int i = 0; i < N; i++) {
			Feature3D f = new Feature3D();
			f.worldPt.set(i,i,i);
			f.views.add(edge.viewSrc);
			f.obsIdx.add(i);
			f.views.add(edge.viewDst);
			f.obsIdx.add(i);
			f.triangulationAngle = 10; // this will prevent it from triangulating again. I'm too lazy to deal with that
			edge.stereoTriangulations.add(f);
		}
		edge.a_to_b.T.set(0,0,scale);

		// make two of them known
		edge.viewDst.features3D[8] = edge.stereoTriangulations.get(1);
		edge.viewDst.features3D[9] = edge.stereoTriangulations.get(3);

		edge.viewDst.viewToWorld.T.set(2,0,0);
		edge.viewDst.connections.add(edge);

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		alg.graph = new MetricSceneGraph(new PairwiseImageGraph());

		alg.addTriangulatedStereoFeatures(edge.viewDst,edge,1.0/scale);

		// check to see if the transform to world was correctly computed
		assertEquals(0,edge.viewDst.viewToWorld.T.distance(2,0,0), UtilEjml.TEST_F64);
		assertEquals(0,edge.viewSrc.viewToWorld.T.distance(2,0,1), UtilEjml.TEST_F64);

		// see if it moved all the points into the feature list
		assertEquals(N-2,alg.graph.features3D.size()); // two are already known
		assertEquals(0,edge.stereoTriangulations.size());

		// see if scale was applied and transform from a_to_b since dst is the origin
		for (int i = 0; i < N-2; i++) {
			Feature3D f = alg.graph.features3D.get(i);
			assertEquals(0, f.worldPt.distance(2+i/scale,i/scale,(i+scale)/scale), UtilEjml.TEST_F64);

			assertSame(edge.viewSrc.features3D[i], f);
			assertSame(edge.viewDst.features3D[i], f);

			assertTrue(f.views.contains(edge.viewSrc));
			assertTrue(f.views.contains(edge.viewDst));
		}

		assertSame(MetricSceneGraph.ViewState.UNPROCESSED,edge.viewSrc.state);
		assertSame(MetricSceneGraph.ViewState.UNPROCESSED,edge.viewDst.state);
	}

	@Test
	public void determineScale() {
		determineScale(20,false);
		determineScale(19,true);
	}

	public void determineScale( int N , boolean expectException ){

		double scale = 1.5;

		// create one good point and all the others will have an angle which is too small
		final CameraMotion edge = new CameraMotion();
		edge.viewSrc = new CameraView();
		edge.viewDst = new CameraView();
		edge.viewSrc.features3D = new Feature3D[N];
		edge.viewDst.features3D = new Feature3D[N];

		for (int i = 0; i < N; i++) {
			Feature3D a = new Feature3D();
			Feature3D b = new Feature3D();

			double v = i+1;
			a.worldPt.set(v,v,i);
			b.worldPt.set(v/scale,v/scale,i/scale);

			a.obsIdx.add(i);
			a.obsIdx.add(i);
			b.obsIdx.add(i);
			b.obsIdx.add(i);

			edge.viewDst.features3D[i] = a;
			edge.stereoTriangulations.add(b);
		}

		try {
			double found = EstimateSceneCalibrated.determineScale(edge.viewDst, edge);
			assertEquals(scale, found, UtilEjml.TEST_F64);
			assertFalse(expectException);
		} catch( Exception ignore ){}
	}

	@Test
	public void determinePose() {
		fail("Implement");
	}

	@Test
	public void triangulateNoLocation() {
		fail("Implement");
	}

	@Test
	public void triangulationAngle() {
		Point2D_F64 normA = new Point2D_F64(-0.5,0);
		Point2D_F64 normB = new Point2D_F64(0.5,0);
		Se3_F64 a_to_b = new Se3_F64();

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();

		assertEquals(2.0*Math.atan(0.5), alg.triangulationAngle(normA,normB,a_to_b), UtilEjml.TEST_F64);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,0.1,0,a_to_b.R);
		GeometryMath_F64.multTran(a_to_b.R,normA,normA);

		assertEquals(2.0*Math.atan(0.5), alg.triangulationAngle(normA,normB,a_to_b), UtilEjml.TEST_F64);
	}

	@Test
	public void addUnvistedToStack() {
		fail("Implement");
	}

	@Test
	public void defineCoordinateSystem() {
		int N = 10;

		// create one good point and all the others will have an angle which is too small
		final CameraMotion edge = new CameraMotion();
		edge.viewSrc = new CameraView();
		edge.viewDst = new CameraView();
		edge.viewSrc.features3D = new Feature3D[N];
		edge.viewDst.features3D = new Feature3D[N];

		double scale = 1.5;

		for (int i = 0; i < N; i++) {
			Feature3D f = new Feature3D();
			f.worldPt.set(i,i,i);
			f.views.add(edge.viewSrc);
			f.views.add(edge.viewDst);
			f.obsIdx.add(i);
			f.obsIdx.add(i);
			edge.stereoTriangulations.add(f);
		}
		edge.a_to_b.T.set(0,0,scale);

		edge.viewDst.connections.add(edge);

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated() {
			// override this method since it's tested elsewhere
			@Override
			void addTriangulatedFeaturesForAllEdges(CameraView v) {
				assertTrue(v==edge.viewSrc||v == edge.viewDst);
			}
		};
		alg.graph = new MetricSceneGraph(new PairwiseImageGraph());

		alg.defineCoordinateSystem(edge.viewDst,edge);

		// see if it moved all the points into the feature list
		assertEquals(N,alg.graph.features3D.size());
		assertEquals(0,edge.stereoTriangulations.size());

		// see if scale was applied and transform from a_to_b since dst is the origin
		for (int i = 0; i < N; i++) {
			Feature3D f = alg.graph.features3D.get(i);
			assertEquals(0, f.worldPt.distance(i/scale,i/scale,(i+scale)/scale), UtilEjml.TEST_F64);

			assertSame(edge.viewSrc.features3D[i], f);
			assertSame(edge.viewDst.features3D[i], f);

			assertTrue(f.views.contains(edge.viewSrc));
			assertTrue(f.views.contains(edge.viewDst));
		}

		assertSame(MetricSceneGraph.ViewState.PROCESSED,edge.viewSrc.state);
		assertSame(MetricSceneGraph.ViewState.PROCESSED,edge.viewDst.state);

		// check to see if the set the transform to world correctly
		assertEquals(1,edge.viewSrc.viewToWorld.T.z);
		assertEquals(0,edge.viewDst.viewToWorld.T.z); // this should be the origin

	}

	@Test
	public void selectOriginNode() {
		// hack the score so that we know which one will be the best
		EstimateSceneCalibrated alg = new EstimateSceneCalibrated() {
			@Override
			double scoreNodeAsOrigin(CameraView node) {
				return node.index;
			}
		};
		alg.graph = new MetricSceneGraph(new PairwiseImageGraph());

		for (int i = 0; i < 4; i++) {
			CameraView node = new CameraView();
			node.index = i;
			alg.graph.nodes.add( node );
		}

		assertSame( alg.graph.nodes.get(3), alg.selectOriginNode() );
	}

	@Test
	public void selectCoordinateBase() {
		CameraView view = new CameraView();

		for (int i = 0; i < 4; i++) {
			CameraMotion m = new CameraMotion();
			m.triangulationAngle = 0.1;
			m.associated = new ArrayList<>();
			m.associated.add( new AssociatedIndex() );
			view.connections.add( m );
		}

		// since the number of associated is the same, increasing the angle will make the score higher than the rest
		view.connections.get(1).triangulationAngle = 1.5;

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		assertSame(view.connections.get(1), alg.selectCoordinateBase(view));
	}

	@Test
	public void triangulateMetricStereoEdges() {

		// create one good point and all the others will have an angle which is too small
		CameraMotion edge = new CameraMotion();
		edge.viewSrc = new CameraView();
		edge.viewDst = new CameraView();

		edge.viewSrc.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.viewDst.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.associated = new ArrayList<>();

		for (int i = 0; i < 5; i++) {
			edge.viewSrc.observationNorm.grow();
			edge.viewDst.observationNorm.grow();

			// have the indexes be different to make sure it's doing the look up correctly
			edge.associated.add( new AssociatedIndex(i,i+1,0.1));
		}
		edge.viewDst.observationNorm.grow();

		// define the camera's motion between the two views
		edge.a_to_b.set(0.5,0,0,EulerType.XYZ,0.05,0,0);

		// make the one point which will be triangulated
		Point3D_F64 X = new Point3D_F64(0.1,0.9,1.5); // src
		Point3D_F64 Y = new Point3D_F64();                      // dst
		edge.a_to_b.transform(X,Y);
		edge.viewSrc.observationNorm.get(1).set(X.x/X.z,X.y/X.z);
		edge.viewDst.observationNorm.get(2).set(Y.x/Y.z,Y.y/Y.z);

		// make things more interesting by applying a non-identity transformation

		for (int i = 0; i < 5; i++) {
			Point2D_F64 n = edge.viewSrc.observationNorm.get(i);
			if( i != 1 )
				GeometryMath_F64.multTran(edge.a_to_b.R,n,n);
		}

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		alg.triangulateMetricStereoEdges(edge);

		assertEquals(1,edge.stereoTriangulations.size());

		Feature3D f = edge.stereoTriangulations.get(0);

		assertEquals(2,f.obsIdx.size());
		assertEquals(2,f.views.size());
		assertEquals(1,f.obsIdx.get(0));
		assertEquals(2,f.obsIdx.get(1));
		assertEquals(edge.viewSrc,f.views.get(0));
		assertEquals(edge.viewDst,f.views.get(1));
		assertEquals(0,X.distance(f.worldPt), UtilEjml.TEST_F64);
	}

}