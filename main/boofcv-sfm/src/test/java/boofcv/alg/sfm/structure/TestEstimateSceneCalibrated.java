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

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.structure.MetricSceneGraph.Feature3D;
import boofcv.alg.sfm.structure.MetricSceneGraph.Motion;
import boofcv.alg.sfm.structure.MetricSceneGraph.View;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static boofcv.abst.geo.bundle.GenericBundleAdjustmentMetricChecks.checkReprojectionError;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEstimateSceneCalibrated extends GenericSceneStructureChecks {

	Random rand = new Random(234);
	CameraPinhole pinhole = new CameraPinhole(400,400,0,500,500,1000,1000);

	@Test
	public void perfectScene() {
		PairwiseImageGraph pairwise = createPerfectImageGraph();

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
//		alg.setVerbose(System.out);
		alg.process(pairwise);

		SceneObservations observations = alg.getObservations();
		SceneStructureMetric structure = alg.getSceneStructure();

		assertEquals(1,structure.cameras.length);
		assertEquals(5,structure.views.length);
		assertTrue(structure.points.length>495);

		checkReprojectionError(structure,observations,1e-4);
	}

	private PairwiseImageGraph createPerfectImageGraph() {
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0,0,1.5),
				-1.5,1.5,-0.5,0.5,-0.2,0.2,500,rand);

		PairwiseImageGraph pairwise = new PairwiseImageGraph();

		LensDistortionPinhole distortion = new LensDistortionPinhole(pinhole);
		Point2Transform2_F64 n2n = distortion.distort_F64(false,false);

		pairwise.addCamera(new PairwiseImageGraph.Camera("one",distortion.undistort_F64(true,false),pinhole));

		List<Se3_F64> listViewToWorld = new ArrayList<>();


		Point3D_F64 Xv = new Point3D_F64(); // 3D point in view reference frame
		Point2D_F64 n = new Point2D_F64();  // normalized image coordinate
		Point2D_F64 p = new Point2D_F64();  // pixel coordinate

		for (int viewidx = 0; viewidx < 5; viewidx++) {
			Se3_F64 viewToWorld = new Se3_F64();
			viewToWorld.set(-1+2.0*viewidx/4.0,0,0, EulerType.XYZ,rand.nextGaussian()*0.1,0,0);
			listViewToWorld.add(viewToWorld);

			FastQueue descs = UtilFeature.createQueueF64(3);
			PairwiseImageGraph.View view = new PairwiseImageGraph.View(viewidx,descs);

			// add visible features to each view
			for (int i = 0; i < cloud.size(); i++) {
				Point3D_F64 X = cloud.get(i);
				viewToWorld.transformReverse(X,Xv);
				n2n.compute(Xv.x/Xv.z, Xv.y/Xv.z, n);
				PerspectiveOps.convertNormToPixel(pinhole,n.x,n.y,p);

				if( !pinhole.inside(p.x,p.y) )
					continue;

				((TupleDesc_F64)view.descriptions.grow()).set(X.x,X.y,X.z);
				view.observationNorm.grow().set(n);
				view.observationPixels.grow().set(p);
			}

			view.camera = pairwise.cameras.get("one");
			pairwise.nodes.add(view);
		}

		// Connect every node to
		for (int i = 0; i < pairwise.nodes.size(); i++) {
			for (int j = i+1; j < pairwise.nodes.size(); j++) {
				PairwiseImageGraph.Motion m = new PairwiseImageGraph.Motion();
				m.viewSrc = pairwise.nodes.get(i);
				m.viewDst = pairwise.nodes.get(j);
				m.metric = true;
				m.index= pairwise.edges.size();
				pairwise.edges.add(m);
				m.viewSrc.connections.add(m);
				m.viewDst.connections.add(m);

				Se3_F64 src_to_dst = new Se3_F64();
				listViewToWorld.get(i).concat(listViewToWorld.get(j).invert(null),src_to_dst);
				m.F = MultiViewOps.createEssential(src_to_dst.R,src_to_dst.T, null);

				// find common features. Match as pairs
				matchCommon(m.viewSrc,m.viewDst,m.associated);

				// randomly remove a few to make everything not perfect
				for (int k = 0; k < 5; k++) {
					m.associated.remove( rand.nextInt(m.associated.size()));
				}
			}
		}
		return pairwise;
	}

	private void matchCommon( PairwiseImageGraph.View viewA , PairwiseImageGraph.View viewB,
							   List<AssociatedIndex> matches )
	{
		for (int i = 0; i < viewA.descriptions.size; i++) {
			TupleDesc_F64 a = (TupleDesc_F64)viewA.descriptions.get(i);

			for (int j = 0; j < viewB.descriptions.size; j++) {
				TupleDesc_F64 b = (TupleDesc_F64)viewB.descriptions.get(j);

				if(DescriptorDistance.euclidean(a,b) <= UtilEjml.EPS ) {
					matches.add( new AssociatedIndex(i,j,0));
					break;
				}
			}
		}
	}

	@Test
	public void decomposeEssential() {
		int N = 20;

		for (int trial = 0; trial < 10; trial++) {
			final Motion edge = new Motion();
			edge.viewSrc = new View();
			edge.viewDst = new View();
			edge.viewSrc.observationNorm = new FastQueue<>(Point2D_F64.class,true);
			edge.viewDst.observationNorm = new FastQueue<>(Point2D_F64.class,true);
			edge.associated = new ArrayList<>();

			double rotX = rand.nextGaussian()*0.02;
			double rotY = rand.nextGaussian()*0.02;
			Se3_F64 a_to_b = SpecialEuclideanOps_F64.eulerXyz(rand.nextGaussian(),0,0,rotX,rotY,0,null);

			List<Point3D_F64> sceneX = UtilPoint3D_F64.random(new Point3D_F64(0,0,1),-0.5,0.5,N,rand);
			edge.viewDst.observationNorm.grow();
			for (int i = 0; i < sceneX.size(); i++) {
				Point3D_F64 X = sceneX.get(i);

				edge.viewSrc.observationNorm.grow().set(X.x/X.z, X.y/X.z);
				a_to_b.transform(X,X);
				edge.viewDst.observationNorm.grow().set(X.x/X.z, X.y/X.z);

				edge.associated.add( new AssociatedIndex(i,i+1,0));
			}

			edge.F = MultiViewOps.createEssential(a_to_b.R,a_to_b.T, null);
			CommonOps_DDRM.scale(2.0,edge.F); // accurate up to a scale invariance

			EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
			alg.decomposeEssential(edge);

			// set to same scale
			edge.a_to_b.T.divide(edge.a_to_b.T.norm());
			a_to_b.T.divide(a_to_b.T.norm());

			Se3_F64 a_to_a = edge.a_to_b.concat(a_to_b.invert(null),null);

			assertTrue(MatrixFeatures_DDRM.isIdentity(a_to_a.R, UtilEjml.TEST_F64));
		}
	}

	@Test
	public void medianTriangulationAngle() {
		int N = 20;
		
		final Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();
		edge.viewSrc.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.viewDst.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.associated = new ArrayList<>();
		edge.a_to_b = SpecialEuclideanOps_F64.eulerXyz(-1,0,0,0,0,0,null);

		Point3D_F64 Xa = new Point3D_F64(0,0,1);
		Point3D_F64 Xb = new Point3D_F64();
		edge.a_to_b.transform(Xa,Xb);

		for (int i = 0; i < N; i++) {
			edge.viewSrc.observationNorm.grow().set(Xa.x/Xa.z, Xa.y/Xa.z);
			edge.viewDst.observationNorm.grow().set(Xb.x/Xb.z, Xb.y/Xb.z);
			edge.associated.add( new AssociatedIndex(i,i,0));
		}

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		double found = alg.medianTriangulationAngle(edge);

		assertEquals(Math.PI/4.0, found, UtilEjml.TEST_F64);

		// add a little bit of noise
		edge.viewSrc.observationNorm.get(1).set(-2,1);
		edge.viewSrc.observationNorm.get(6).set(-2,1);
		found = alg.medianTriangulationAngle(edge);
		assertEquals(Math.PI/4.0, found, UtilEjml.TEST_F64);
	}

	@Test
	public void addTriangulatedStereoFeatures() {
		int N = 10;

		// create one good point and all the others will have an angle which is too small
		final Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();
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
		final Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();
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
		int N = 40;
		Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();

		// define the camera's motion between the two views
		Se3_F64 world_to_a = SpecialEuclideanOps_F64.eulerXyz(0,0.5,0,0,0,0.05,null);

		edge.a_to_b.set(0.5,0,0,EulerType.XYZ,0.05,0,0);

		edge.viewSrc.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.viewDst.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.associated = new ArrayList<>();

		CameraPinhole intrinsic = new CameraPinhole(400,400,0,500,500,1000,1000);
		edge.viewSrc.camera = new PairwiseImageGraph.Camera("moo",null,intrinsic);
		edge.viewDst.camera = edge.viewSrc.camera;
		edge.viewSrc.features3D = new Feature3D[N];
		edge.viewDst.features3D = new Feature3D[N];
		edge.viewSrc.connections.add(edge);
		edge.viewDst.connections.add(edge);

		// make it so that its observations are not skipped
		edge.viewDst.state = MetricSceneGraph.ViewState.PROCESSED;

		for (int i = 0; i < N; i++) {
			Point3D_F64 X = new Point3D_F64(rand.nextGaussian(),rand.nextGaussian(),2);

			Feature3D f3 = new Feature3D();
			f3.worldPt.set(X);

			world_to_a.transform(X,X);
			edge.viewSrc.features3D[i] = f3;
			edge.viewSrc.observationNorm.grow().set(X.x/X.z,X.y/X.z);

			edge.a_to_b.transform(X,X);
			edge.viewDst.features3D[i] = f3;
			edge.viewDst.observationNorm.grow().set(X.x/X.z,X.y/X.z);

			edge.associated.add( new AssociatedIndex(i,i,0.1));
		}


		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		alg.declareModelFitting();

		assertTrue(alg.determinePose(edge.viewSrc));

		Se3_F64 a_to_a = edge.viewSrc.viewToWorld.concat(world_to_a,null);
		assertEquals(0,a_to_a.T.norm(), 1e-8);
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
	public void defineCoordinateSystem() {
		int N = 10;

		// create one good point and all the others will have an angle which is too small
		final Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();
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
			void addTriangulatedFeaturesForAllEdges(View v) {
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
			double scoreNodeAsOrigin(View node) {
				return node.index;
			}
		};
		alg.graph = new MetricSceneGraph(new PairwiseImageGraph());

		for (int i = 0; i < 4; i++) {
			View node = new View();
			node.index = i;
			alg.graph.nodes.add( node );
		}

		assertSame( alg.graph.nodes.get(3), alg.selectOriginNode() );
	}

	@Test
	public void selectCoordinateBase() {
		View view = new View();

		for (int i = 0; i < 4; i++) {
			Motion m = new Motion();
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

		int N = 20;

		// create one good point and all the others will have an angle which is too small
		Motion edge = new Motion();
		edge.viewSrc = new View();
		edge.viewDst = new View();
		edge.viewSrc.camera = edge.viewDst.camera = camera("camera", pinhole);

		edge.viewSrc.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.viewDst.observationNorm = new FastQueue<>(Point2D_F64.class,true);
		edge.associated = new ArrayList<>();

		edge.a_to_b.set(rand.nextGaussian(),0,0,EulerType.XYZ, 0.05, 0.1,0);

		// Define the Points
		List<Point3D_F64> sceneX = UtilPoint3D_F64.random(new Point3D_F64(0,0,1),-0.5,0.5,N,rand);
		edge.viewDst.observationNorm.grow(); // offset by 1 to prevent indexes from being identical
		for (int i = 0; i < sceneX.size(); i++) {
			Point3D_F64 X = sceneX.get(i);
			Point3D_F64 Xb = new Point3D_F64();

			edge.viewSrc.observationNorm.grow().set(X.x/X.z, X.y/X.z);
			edge.a_to_b.transform(X,Xb);
			edge.viewDst.observationNorm.grow().set(Xb.x/Xb.z, Xb.y/Xb.z);

			edge.associated.add( new AssociatedIndex(i,i+1,0));
		}

		EstimateSceneCalibrated alg = new EstimateSceneCalibrated();
		alg.triangulateStereoEdges(edge);

		assertEquals(N,edge.stereoTriangulations.size());

		for (int i = 0; i < N; i++) {
			Point3D_F64 X = sceneX.get(i);

			Feature3D f = edge.stereoTriangulations.get(i);

			assertEquals(2,f.obsIdx.size());
			assertEquals(2,f.views.size());
			assertEquals(i,f.obsIdx.get(0));
			assertEquals(i+1,f.obsIdx.get(1));
			assertEquals(edge.viewSrc,f.views.get(0));
			assertEquals(edge.viewDst,f.views.get(1));
			assertEquals(0,X.distance(f.worldPt), UtilEjml.TEST_F64);
		}
	}

	private static PairwiseImageGraph.Camera camera( String name , CameraPinhole intrinsic ) {
		return new PairwiseImageGraph.Camera("camera",
				new LensDistortionPinhole(intrinsic).undistort_F64(true,false),intrinsic);
	}

}