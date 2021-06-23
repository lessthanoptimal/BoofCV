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

package boofcv.alg.geo.pose;

import boofcv.abst.geo.TriangulateNViewsProjective;
import boofcv.alg.geo.MultiViewOps;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCompatibleProjectiveHomography extends CommonThreeViewHomogenous {

	List<DMatrixRMaj> camerasB;
	List<Point4D_F64> sceneB;

	@Override
	public void createScene( int numFeatures, boolean planar ) {
		super.createScene(numFeatures, planar);

		// Triangulate 3D points in another projective
		TrifocalTensor T = MultiViewOps.createTrifocal(cameras.get(0), cameras.get(1), cameras.get(2), null);
		DMatrixRMaj P1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);
		MultiViewOps.trifocalToCameraMatrices(T, P2, P3);
		CommonOps_DDRM.setIdentity(P1);

		TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNViewProj(ConfigTriangulation.GEOMETRIC());
		List<Point2D_F64> observations = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			observations.add(new Point2D_F64());
		}
		camerasB = new ArrayList<>();
		camerasB.add(P1);
		camerasB.add(P2);
		camerasB.add(P3);

		sceneB = new ArrayList<>();
		for (int i = 0; i < numFeatures; i++) {
			AssociatedTriple t = triples.get(i);
			observations.get(0).setTo(t.p1);
			observations.get(1).setTo(t.p2);
			observations.get(2).setTo(t.p3);
			Point4D_F64 location = new Point4D_F64();
			assertTrue(triangulator.triangulate(observations, camerasB, location));

			sceneB.add(location);
		}
	}

	@Test
	void fitPoints() {
		// minimum number of points
		fitPoints(5);
		// extra points
		fitPoints(20);
	}

	void fitPoints( int N ) {
		createScene(N, false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		assertTrue(alg.fitPoints(worldPts, sceneB, H));

		checkCamerasH(H, 1e-7);
		checkWorldPointsH(H, 1e-7);
	}

	@Test
	void fitCameras() {
		// planar not makes no difference since points are not considered
		createScene(0, true);

		List<DMatrixRMaj> camerasA = new ArrayList<>(super.cameras);
		List<DMatrixRMaj> camerasB = new ArrayList<>(this.camerasB);

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		// Test with three pairs of cameras
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		assertTrue(alg.fitCameras(camerasA, camerasB, H));
		checkCamerasH(H, 1e-7);

		// Should work with two cameras, the minimum case
		camerasA.remove(0);
		camerasB.remove(0);
		assertTrue(alg.fitCameras(camerasA, camerasB, H));

		checkCamerasH(H, 1e-7);
		checkWorldPointsH(H, 1e-7);
	}

	/**
	 * A known issue is when there are two views and one of them has a zeros column
	 */
	@Test
	void fitCameras_zeroColumn() {
		createScene(0, true);

		// view 0 will have a camera with the edge case. both A and B will have zeros in right most column, col=3
		List<DMatrixRMaj> camerasA = new ArrayList<>(super.cameras);
		List<DMatrixRMaj> camerasB = new ArrayList<>(this.camerasB);
		camerasA.remove(2);
		camerasB.remove(2);

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		// Test in both directions
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		assertTrue(alg.fitCameras(camerasA, camerasB, H));
		checkCamerasH(camerasA, camerasB, H, 1e-7);

		assertTrue(alg.fitCameras(camerasB, camerasA, H));
		checkCamerasH(camerasB, camerasA, H, 1e-7);
	}

	/**
	 * Feed in the same set of camera matrices and see if an identity matrix is returned
	 */
	@Test
	void fitCameras_identity() {
		createScene(0, true);

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		assertTrue(alg.fitCameras(cameras, cameras, H));
		assertTrue(MatrixFeatures_DDRM.isIdentity(H, UtilEjml.TEST_F64));
		assertTrue(alg.fitCameras(camerasB, camerasB, H));
		assertTrue(MatrixFeatures_DDRM.isIdentity(H, UtilEjml.TEST_F64));

		// try it with two cameras only now
		cameras.remove(2);
		camerasB.remove(2);
		assertTrue(alg.fitCameras(cameras, cameras, H));
		assertTrue(MatrixFeatures_DDRM.isIdentity(H, UtilEjml.TEST_F64));
		assertTrue(alg.fitCameras(camerasB, camerasB, H));
		assertTrue(MatrixFeatures_DDRM.isIdentity(H, UtilEjml.TEST_F64));
	}

	/**
	 * Checks to see if the columns in H are the null space and that independent scales of the camera matrices
	 * do not matter.
	 */
	@Test
	void cameraCrossProductMatrix() {
		DMatrixRMaj H = RandomMatrices_DDRM.rectangle(4, 4, rand);
		DMatrixRMaj A1 = RandomMatrices_DDRM.rectangle(3, 4, rand);
		DMatrixRMaj A2 = RandomMatrices_DDRM.rectangle(3, 4, rand);

		DMatrixRMaj B1 = new DMatrixRMaj(3, 4);
		DMatrixRMaj B2 = new DMatrixRMaj(3, 4);

		CommonOps_DDRM.mult(A1, H, B1);
		CommonOps_DDRM.mult(A2, H, B2);

		// make sure the scale of B doesn't matter
		CommonOps_DDRM.scale(-1.2, B1);
		CommonOps_DDRM.scale(2.5, B2);

		DMatrixRMaj h = new DMatrixRMaj(4, 1);
		for (int col = 0; col < 4; col++) {
			CommonOps_DDRM.extractColumn(H, col, h);
			CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

			alg.A.reshape(6, 4);
			alg.cameraCrossProductMatrix(A1, B1, col, 0);
			alg.cameraCrossProductMatrix(A2, B2, col, 3);

			DMatrixRMaj ns = new DMatrixRMaj(6, 1);

			CommonOps_DDRM.mult(alg.A, h, ns);
			assertTrue(MatrixFeatures_DDRM.isZeros(ns, UtilEjml.TEST_F64));
		}
	}

	/**
	 * Test more situations with random matrices
	 */
	@Test
	void fitCameras_RandomH() {
		createScene(0, false);

		DMatrixRMaj H = RandomMatrices_DDRM.rectangle(4, 4, rand);

		List<DMatrixRMaj> camerasA = new ArrayList<>(super.cameras);
		List<DMatrixRMaj> camerasB = new ArrayList<>();

		for (DMatrixRMaj P : camerasA) {
			DMatrixRMaj PP = new DMatrixRMaj(3, 4);
			CommonOps_DDRM.mult(P, H, PP);
			camerasB.add(PP);
		}

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		assertTrue(alg.fitCameras(camerasA, camerasB, H));

		for (int i = 0; i < camerasA.size(); i++) {
			DMatrixRMaj A = camerasA.get(i);
			DMatrixRMaj BB = new DMatrixRMaj(3, 4);
			CommonOps_DDRM.mult(A, H, BB);

			DMatrixRMaj B = camerasB.get(i);
			commonScale(B, BB);
			assertTrue(MatrixFeatures_DDRM.isIdentical(B, BB, 1e-6));
		}
	}

	@Test
	void fitCameraPoints() {
//		fitCameraPoints(2); // two should be the minimum but isn't working
		fitCameraPoints(4);
		fitCameraPoints(20);
	}

	void fitCameraPoints( int N ) {
		createScene(N, false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		for (int i = 0; i < 3; i++) {
			assertTrue(alg.fitCameraPoints(cameras.get(i), camerasB.get(i), worldPts, sceneB, H));
			checkCamerasH(H, 1e-7);
		}
	}

	@Test
	void refineWorld() {
		createScene(20, false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		List<Point3D_F64> world3a = new ArrayList<>();
		List<Point3D_F64> world3b = new ArrayList<>();

		for (int i = 0; i < worldPts.size(); i++) {
			Point4D_F64 a = worldPts.get(i);
			Point4D_F64 b = sceneB.get(i);

			world3a.add(new Point3D_F64(a.x/a.w, a.y/a.w, a.z/a.w));
			world3b.add(new Point3D_F64(b.x/b.w, b.y/b.w, b.z/b.w));
		}

		// Get the initial value of H
		assertTrue(alg.fitPoints(worldPts, sceneB, H));

		// break the model
		H.data[3] += 0.1;
		H.data[7] -= 0.8;

		// Refine it
//		alg.lm.setVerbose(System.out,0);
		alg.refineWorld(world3a, world3b, H);

		checkCamerasH(H, 1e-5);
	}

	@Test
	void refineReprojection() {
		createScene(20, false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		// Get the initial value of H
		assertTrue(alg.fitPoints(worldPts, sceneB, H));

		// break the model
		H.data[3] += 0.1;

		// Refine it
//		alg.lm.setVerbose(System.out,0);
		alg.refineReprojection(cameras, worldPts, sceneB, H);

		checkCamerasH(H, 1e-5);
	}

	private void checkCamerasH( DMatrixRMaj H, double tol ) {
		checkCamerasH(cameras, camerasB, H, tol);
	}

	private void checkCamerasH( List<DMatrixRMaj> camerasA, List<DMatrixRMaj> camerasB, DMatrixRMaj H, double tol ) {
		assertEquals(camerasA.size(), camerasB.size());

		DMatrixRMaj A = new DMatrixRMaj(3, 4);
		for (int i = 0; i < camerasA.size(); i++) {
			// Check P*H = P'
			CommonOps_DDRM.mult(camerasA.get(i), H, A);
			DMatrixRMaj B = camerasB.get(i);
			commonScale(/* A= */B, /* B= */A);

			assertTrue(MatrixFeatures_DDRM.isIdentical(B, A, tol));
		}
	}

	private void checkWorldPointsH( DMatrixRMaj H, double tol ) {
		Point4D_F64 found = new Point4D_F64();
		for (int i = 0; i < triples.size(); i++) {

			Point4D_F64 X = worldPts.get(i);
			Point4D_F64 Xb = sceneB.get(i);

			GeometryMath_F64.mult(H, Xb, found);

			X.scale(1.0/X.norm());
			found.scale(1.0/found.norm());

			double error = X.distance(found);
			found.scale(-1);
			error = Math.min(error, X.distance(found));

			assertEquals(0.0, error, tol);
		}
	}

	public static void commonScale( DMatrixRMaj A, DMatrixRMaj B ) {
		double n = NormOps_DDRM.normF(A);
		CommonOps_DDRM.divide(A, n);

		// find element with largest abs value
		double best = 0;
		int bestIdx = 0;
		int N = A.getNumElements();
		for (int i = 0; i < N; i++) {
			double v = Math.abs(A.data[i]);
			if (v > best) {
				best = v;
				bestIdx = i;
			}
		}

		n = NormOps_DDRM.normF(B);
		CommonOps_DDRM.divide(B, n);

		if (Math.signum(B.data[bestIdx]) != Math.signum(A.data[bestIdx])) {
			CommonOps_DDRM.scale(-1, B);
		}
	}
}
