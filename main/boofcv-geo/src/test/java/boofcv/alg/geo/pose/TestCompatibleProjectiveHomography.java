/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public class TestCompatibleProjectiveHomography extends CommonThreeViewHomogenous {

	List<DMatrixRMaj> camerasB;
	List<Point4D_F64> sceneB;

	@Override
	public void createScene(int numFeatures, boolean planar) {
		super.createScene(numFeatures, planar);

		// Triangulate 3D points in another projective
		TrifocalTensor T = MultiViewOps.createTrifocal(cameras.get(1),cameras.get(2),null);
		DMatrixRMaj P1 = super.cameras.get(0).copy();
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);
		MultiViewOps.extractCameraMatrices(T,P2,P3);

		sceneB = new ArrayList<>();

		TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNView(ConfigTriangulation.GEOMETRIC);
		List<Point2D_F64> observations = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			observations.add(new Point2D_F64());
		}
		camerasB = new ArrayList<>();
		camerasB.add( P1 );
		camerasB.add( P2 );
		camerasB.add( P3 );
		for (int i = 0; i < numFeatures; i++) {
			AssociatedTriple t = triples.get(i);
			observations.get(0).set(t.p1);
			observations.get(1).set(t.p2);
			observations.get(2).set(t.p3);
			Point4D_F64 location = new Point4D_F64();
			assertTrue(triangulator.triangulate(observations,camerasB,location));

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
		createScene(N,false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		assertTrue(alg.fitPoints(worldPts,sceneB,H));

		checkCamerasH(H, 1e-7 );
	}

	@Test
	void fitCameras() {
		createScene(10,false);

		List<DMatrixRMaj> camerasA = new ArrayList<>();
		List<DMatrixRMaj> camerasB = new ArrayList<>(this.camerasB);

		// both views have identity for first view
		DMatrixRMaj P1 = super.cameras.get(0);
		camerasA.add(P1);
		camerasA.add(super.cameras.get(1));
		camerasA.add(super.cameras.get(2));

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		DMatrixRMaj H_inv = new DMatrixRMaj(4,4);
		assertTrue(alg.fitCameras(camerasA,camerasB,H_inv));
		checkCamerasHinv( H_inv, 0.1);

		// should still work with 2 cameras
		camerasA.remove(0);
		camerasB.remove(0);
		assertTrue(alg.fitCameras(camerasA,camerasB,H_inv));
		checkCamerasHinv( H_inv, 0.1);
	}

	@Test
	void fitCameraPoints() {
		fitCameraPoints(2);
		fitCameraPoints(6);
	}

	void fitCameraPoints( int N ) {
		createScene(N,false);

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		for (int i = 0; i < 3; i++) {
			assertTrue(alg.fitCameraPoints(cameras.get(i),camerasB.get(i),worldPts,sceneB,H));
			checkCamerasH(H, 1e-7 );
		}
	}

	@Test
	void refineReprojection() {
		fail("Implement");
	}

	private void checkCamerasH(DMatrixRMaj H, double tol ) {
		DMatrixRMaj H_inv = new DMatrixRMaj(4,4);
		CommonOps_DDRM.invert(H,H_inv);
		checkCamerasHinv(H_inv,tol);
	}

	private void checkCamerasHinv(DMatrixRMaj h_inv, double tol )
	{
		DMatrixRMaj P1 = camerasB.get(0);
		DMatrixRMaj P2 = camerasB.get(1);
		DMatrixRMaj P3 = camerasB.get(2);

		DMatrixRMaj PP = new DMatrixRMaj(3,4);
		for (int i = (tol>0.01?1:0); i < 3; i++) {
			switch(i) {
				case 0:CommonOps_DDRM.mult(P1, h_inv, PP);break;
				case 1:CommonOps_DDRM.mult(P2, h_inv, PP);break;
				case 2:CommonOps_DDRM.mult(P3, h_inv, PP);break;
			}

			DMatrixRMaj A = cameras.get(i);
			commonScale(A,PP);

//			System.out.println("------------");
//			A.print();
//			PP.print();
			assertTrue(MatrixFeatures_DDRM.isIdentical(A,PP,tol));
		}
	}

	public static void commonScale( DMatrixRMaj A , DMatrixRMaj B ) {
		double n = NormOps_DDRM.normF(A);
		CommonOps_DDRM.divide(A,n);

		// find element with largest abs value
		double best = 0;
		int bestIdx = 0;
		int N = A.getNumElements();
		for (int i = 0; i < N; i++) {
			double v = Math.abs(A.data[i]);
			if( v > best ) {
				best = v;
				bestIdx = i;
			}
		}

		n = NormOps_DDRM.normF(B);
		CommonOps_DDRM.divide(B,n);

		if( Math.signum(B.data[bestIdx]) != Math.signum(A.data[bestIdx])) {
			CommonOps_DDRM.scale(-1,A);
		}
	}
}