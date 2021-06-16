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

package boofcv.alg.geo.robust;

import boofcv.abst.geo.Triangulate2ViewsMetricH;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.ClosestPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.line.LineParametric3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.junit.jupiter.api.Test;

import static georegression.geometry.ConvertRotation3D_F64.eulerToMatrix;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDistanceSe3SymmetricSq extends BoofStandardJUnit {

	Triangulate2ViewsMetricH triangulate = FactoryMultiView.triangulate2ViewMetricH(
			new ConfigTriangulation(ConfigTriangulation.Type.GEOMETRIC));
	DistanceSe3SymmetricSq alg;

	public TestDistanceSe3SymmetricSq() {
		alg = new DistanceSe3SymmetricSq(triangulate);
		alg.setIntrinsic(0, new CameraPinhole(1, 1, 0, 0, 0, 0, 0));
		alg.setIntrinsic(1, new CameraPinhole(1, 1, 0, 0, 0, 0, 0));
	}

	/**
	 * Nominal case with an easily computed solution.
	 */
	@Test void perfect_nominal() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		keyToCurr.getT().setTo(0.1, -0.1, 0.01);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(0, alg.distance(obs), 1e-8);
	}

	/**
	 * Pure rotation with perfect observations. This is undefined. The intersection of the two rays will be at
	 * the camera's origin.
	 */
	@Test void pureRotation_perfectObservations() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.0, 0.05, 0.00, null));

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(Double.MAX_VALUE, alg.distance(obs), UtilEjml.TEST_F64);
	}

	/**
	 * Translation will be very small here. That should be enough that it doesn't intersect at the origin and
	 * a pixel value can be found
	 */
	@Test void nearlyPureRotation() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.0, 0.05, 0.00, null));
		keyToCurr.getT().setTo(1e-4, 0.0, 0.0);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 1e4);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(0.0, alg.distance(obs), UtilEjml.TEST_F64);
	}

	@Test void noisy() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		keyToCurr.getT().setTo(0.1, -0.1, 0.01);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z + 1;
		obs.p2.y = X.y/X.z + 1;

		alg.setModel(keyToCurr);
		assertTrue(alg.distance(obs) > 1e-8);
	}

	@Test void behindCamera() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.05, -0.03, 0.02, null));
		keyToCurr.getT().setTo(0.1, -0.1, 0.01);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, -3);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(Double.MAX_VALUE, alg.distance(obs));
	}

	/**
	 * The point will be very close to infinity but behind the camera
	 */
	@Test void behind_nearlyPureRotation() {
		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getR().setTo(eulerToMatrix(EulerType.XYZ, 0.0, 0.05, 0.00, null));
		keyToCurr.getT().setTo(1e-4, 0.0, 0.0);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, -1e4);

		AssociatedPair obs = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		alg.setModel(keyToCurr);
		assertEquals(Double.MAX_VALUE, alg.distance(obs));
	}

	/**
	 * Manually compute the error using a calibration matrix and see if they match
	 */
	@Test void intrinsicParameters() {
		// intrinsic camera calibration matrix
		DMatrixRMaj K1 = new DMatrixRMaj(3, 3, true, 100, 0.01, 200, 0, 150, 200, 0, 0, 1);
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3, true, 105, 0.021, 180, 0, 155, 210, 0, 0, 1);
		DMatrixRMaj K1_inv = new DMatrixRMaj(3, 3);
		DMatrixRMaj K2_inv = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.invert(K1, K1_inv);
		CommonOps_DDRM.invert(K2, K2_inv);

		Se3_F64 keyToCurr = new Se3_F64();
		keyToCurr.getT().setTo(0.1, -0.1, 0.01);

		Point4D_F64 X = new Point4D_F64(0.02, -0.05, 3, 1.0);

		AssociatedPair obs = new AssociatedPair();
		AssociatedPair obsP = new AssociatedPair();

		obs.p1.x = X.x/X.z;
		obs.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		obs.p2.x = X.x/X.z;
		obs.p2.y = X.y/X.z;

		// translate into pixels
		GeometryMath_F64.mult(K1, obs.p1, obsP.p1);
		GeometryMath_F64.mult(K2, obs.p2, obsP.p2);

		// add some noise
		obsP.p1.x += 0.25;
		obsP.p1.y += 0.25;
		obsP.p2.x -= 0.25;
		obsP.p2.y -= 0.25;

		// convert noisy into normalized coordinates
		GeometryMath_F64.mult(K1_inv, obsP.p1, obsP.p1);
		GeometryMath_F64.mult(K2_inv, obsP.p2, obsP.p2);

		// triangulate the point's position given noisy data
		LineParametric3D_F64 rayA = new LineParametric3D_F64();
		LineParametric3D_F64 rayB = new LineParametric3D_F64();
		rayA.slope.setTo(obsP.p1.x, obsP.p1.y, 1);
		rayB.p.setTo(-0.1, 0.1, -0.01);
		rayB.slope.setTo(obsP.p2.x, obsP.p2.y, 1);

		ClosestPoint3D_F64.closestPoint(rayA, rayB, X);

		// compute predicted given noisy triangulation
		AssociatedPair ugh = new AssociatedPair();
		ugh.p1.x = X.x/X.z;
		ugh.p1.y = X.y/X.z;

		SePointOps_F64.transform(keyToCurr, X, X);

		ugh.p2.x = X.x/X.z;
		ugh.p2.y = X.y/X.z;

		// convert everything into pixels
		GeometryMath_F64.mult(K1, ugh.p1, ugh.p1);
		GeometryMath_F64.mult(K2, ugh.p2, ugh.p2);
		GeometryMath_F64.mult(K1, obsP.p1, obsP.p1);
		GeometryMath_F64.mult(K2, obsP.p2, obsP.p2);

		double dx1 = ugh.p1.x - obsP.p1.x;
		double dy1 = ugh.p1.y - obsP.p1.y;
		double dx2 = ugh.p2.x - obsP.p2.x;
		double dy2 = ugh.p2.y - obsP.p2.y;

		double error = dx1*dx1 + dy1*dy1 + dx2*dx2 + dy2*dy2;

		// convert noisy back into normalized coordinates
		GeometryMath_F64.mult(K1_inv, obsP.p1, obsP.p1);
		GeometryMath_F64.mult(K2_inv, obsP.p2, obsP.p2);

		DistanceSe3SymmetricSq alg = new DistanceSe3SymmetricSq(triangulate);
		alg.setIntrinsic(0, PerspectiveOps.matrixToPinhole(K1, 0, 0, null));
		alg.setIntrinsic(1, PerspectiveOps.matrixToPinhole(K2, 0, 0, null));
		alg.setModel(keyToCurr);
		assertEquals(error, alg.distance(obsP), 1e-8);
	}
}
