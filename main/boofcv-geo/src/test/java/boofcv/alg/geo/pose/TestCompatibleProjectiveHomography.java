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

	@Test
	void fitPoints() {
		// minimum number of points
		fitPoints(5);
		// extra points
		fitPoints(20);
	}

	void fitPoints( int N ) {
		createScene(N,false);

		// Triangulate 3D points in another projective
		TrifocalTensor T = MultiViewOps.createTrifocal(cameras.get(1),cameras.get(2),null);
		DMatrixRMaj P1 = super.cameras.get(0);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);
		MultiViewOps.extractCameraMatrices(T,P2,P3);

		List<Point4D_F64> sceneB = new ArrayList<>();

		TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNView(ConfigTriangulation.GEOMETRIC);
		List<Point2D_F64> observations = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			observations.add(new Point2D_F64());
		}
		List<DMatrixRMaj> camerasB = new ArrayList<>();
		camerasB.add( P1 );
		camerasB.add( P2 );
		camerasB.add( P3 );
		for (int i = 0; i < N; i++) {
			AssociatedTriple t = triples.get(i);
			observations.get(0).set(t.p1);
			observations.get(1).set(t.p2);
			observations.get(2).set(t.p3);
			Point4D_F64 location = new Point4D_F64();
			assertTrue(triangulator.triangulate(observations,camerasB,location));

			sceneB.add(location);
		}

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		assertTrue(alg.fitPoints(worldPts,sceneB,H));

		DMatrixRMaj H_inv = new DMatrixRMaj(4,4);
		CommonOps_DDRM.invert(H,H_inv);

		// test the solution
		checkCameras(P1, P2, P3, H_inv, 1e-4);
	}

	@Test
	void fitCameras() {
		createScene(10,false);

		// get equivalent cameras but in a different projective frame
//		TrifocalTensor T = new TrifocalTensor();
//		Estimate1ofTrifocalTensor estimateTri = FactoryMultiView.trifocal_1(null);
//		estimateTri.process(triples,T);

		TrifocalTensor T = MultiViewOps.createTrifocal(cameras.get(1),cameras.get(2),null);

		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);

		MultiViewOps.extractCameraMatrices(T,P2,P3);

//		Point4D_F64 X = worldPts.get(0);
//		Point2D_F64 pixel = new Point2D_F64();
//		PerspectiveOps.renderPixel(P2,X,pixel);
//		pixel.print();
//		PerspectiveOps.renderPixel(cameras.get(1),X,pixel);
//		pixel.print();

		List<DMatrixRMaj> camerasA = new ArrayList<>();
		List<DMatrixRMaj> camerasB = new ArrayList<>();

		// both views have identity for first view
		DMatrixRMaj P1 = super.cameras.get(0);
//		camerasA.add(P1);
		camerasA.add(super.cameras.get(1));
		camerasA.add(super.cameras.get(2));

//		camerasB.add(P1);
		camerasB.add(P2);
		camerasB.add(P3);

		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();

		DMatrixRMaj H = new DMatrixRMaj(4,4);
		assertTrue(alg.fitCameras(camerasA,camerasB,H));

		checkCameras(P1, P2, P3, H, 0.1);
	}

	@Test
	void fitCameraPoints() {
//		fitCameraPoints(2);
		fitCameraPoints(6);
	}

	void fitCameraPoints( int N ) {
		createScene(N,false);

		// Triangulate 3D points in another projective
		TrifocalTensor T = MultiViewOps.createTrifocal(cameras.get(1),cameras.get(2),null);
		DMatrixRMaj P1 = super.cameras.get(0);
		DMatrixRMaj P2 = new DMatrixRMaj(3,4);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4);
		MultiViewOps.extractCameraMatrices(T,P2,P3);

		List<Point4D_F64> sceneB = new ArrayList<>();

		TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNView(ConfigTriangulation.GEOMETRIC);
		List<Point2D_F64> observations = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			observations.add(new Point2D_F64());
		}
		List<DMatrixRMaj> camerasB = new ArrayList<>();
		camerasB.add( P1 );
		camerasB.add( P2 );
		camerasB.add( P3 );
		for (int i = 0; i < N; i++) {
			AssociatedTriple t = triples.get(i);
			observations.get(0).set(t.p1);
			observations.get(1).set(t.p2);
			observations.get(2).set(t.p3);
			Point4D_F64 location = new Point4D_F64();
			assertTrue(triangulator.triangulate(observations,camerasB,location));

			sceneB.add(location);
		}

		// Find the homography
		DMatrixRMaj H = new DMatrixRMaj(4,4);
		CompatibleProjectiveHomography alg = new CompatibleProjectiveHomography();
		assertTrue(alg.fitCameraPoints(cameras.get(1),P2,worldPts,sceneB,H));

		DMatrixRMaj H_inv = new DMatrixRMaj(4,4);
		CommonOps_DDRM.invert(H,H_inv);

		// test the solution
		checkCameras(P1, P2, P3, H_inv, 1e-4 );
	}

	@Test
	void refineReprojection() {
		fail("Implement");
	}

	private void checkCameras(DMatrixRMaj p1, DMatrixRMaj p2, DMatrixRMaj p3, DMatrixRMaj h_inv, double tol ) {
		DMatrixRMaj PP = new DMatrixRMaj(3,4);
		for (int i = (tol>0.01?1:0); i < 3; i++) {
			switch(i) {
				case 0:CommonOps_DDRM.mult(p1, h_inv, PP);break;
				case 1:CommonOps_DDRM.mult(p2, h_inv, PP);break;
				case 2:CommonOps_DDRM.mult(p3, h_inv, PP);break;
			}

			DMatrixRMaj A = cameras.get(i);
			commonScale(A,PP);

			A.print();
			PP.print();
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