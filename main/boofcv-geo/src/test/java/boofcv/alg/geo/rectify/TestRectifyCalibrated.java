/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.rectify;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import static boofcv.alg.geo.PerspectiveOps.renderPixel;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("ConstantConditions")
public class TestRectifyCalibrated extends BoofStandardJUnit {
	/**
	 * Compare results from rectified transform and a set of camera which are already rectified.
	 */
	@Test void compareWithKnown() {
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);

		// transforms are world to camera, but I'm thinking camera to world, which is why invert
		Se3_F64 poseR1 = createPose(0, 0, 0, 0.1, 0, 0.1).invert(null);
		Se3_F64 poseR2 = createPose(0, 0, 0, 1, 0, 0.1).invert(null);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(0, 0.05, 0, 0.1, 0, 0.1).invert(null);
		Se3_F64 poseA2 = createPose(0, -0.1, 0, 1, 0, 0.1).invert(null);

		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K, poseA1, K, poseA2);

		// original camera matrix
		DMatrixRMaj foundP1 = PerspectiveOps.createCameraMatrix(poseA1.getR(), poseA1.getT(), K, null);
		DMatrixRMaj foundP2 = PerspectiveOps.createCameraMatrix(poseA2.getR(), poseA2.getT(), K, null);

		// apply rectification transform
		DMatrixRMaj temp = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(alg.getUndistToRectPixels1(), foundP1, temp);
		foundP1.setTo(temp);
		CommonOps_DDRM.mult(alg.getUndistToRectPixels2(), foundP2, temp);
		foundP2.setTo(temp);

		CommonOps_DDRM.scale(0.1/Math.abs(foundP1.get(2, 3)), foundP1);

		Point3D_F64 X = new Point3D_F64(0, 0, 3);

		// compare results, both should match because of rotation only being around y-axis
		assertEquals(renderPixel(poseR1, K, X, null).x, renderPixel(foundP1, X).x, 1e-5);
		assertEquals(renderPixel(poseR1, K, X, null).y, renderPixel(foundP1, X).y, 1e-5);
		assertEquals(renderPixel(poseR2, K, X, null).x, renderPixel(foundP2, X).x, 1e-5);
		assertEquals(renderPixel(poseR2, K, X, null).y, renderPixel(foundP2, X).y, 1e-5);
	}

	/** rect should be identity */
	@Test void translate0() {
		var K1 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);
		var K2 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);

		var expected1 = new DMatrixRMaj(3, 3, true, 1, 0, 0, 0, 1, 0, 0, 0, 1);
		var expected2 = new DMatrixRMaj(3, 3, true, 1, 0, 0, 0, 1, 0, 0, 0, 1);

		checkKnownRectification(-1, K1, K2, expected1, expected2);
	}

	/** Different focal lengths */
	@Test void translate1() {
		var K1 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 206, 0, 0, 1);
		var K2 = new DMatrixRMaj(3, 3, true, 150, 0, 100, 0, 200, 103, 0, 0, 1);

		double a1 = 0.75, a2 = 1.5;
		var expected1 = new DMatrixRMaj(3, 3, true, a1, 0, 0, 0, a1, 0, 0, 0, 1);
		var expected2 = new DMatrixRMaj(3, 3, true, a2, 0, 0, 0, a2, 0, 0, 0, 1);

		checkKnownRectification(-1, K1, K2, expected1, expected2);
	}

	/** Flip the image */
	@Test void translate2() {
		var K1 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);
		var K2 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);

		var expected1 = new DMatrixRMaj(3, 3, true, -1, 0, 400, 0, -1, 410, 0, 0, 1);
		var expected2 = new DMatrixRMaj(3, 3, true, -1, 0, 400, 0, -1, 410, 0, 0, 1);

		checkKnownRectification(1.0, K1, K2, expected1, expected2);
	}

	/** Flip and scale */
	@Test void translate3() {
		var K1 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 206, 0, 0, 1);
		var K2 = new DMatrixRMaj(3, 3, true, 150, 0, 100, 0, 200, 103, 0, 0, 1);

		double a1 = -0.75, a2 = -1.5;
		var expected1 = new DMatrixRMaj(3, 3, true, a1, 0, 300, 0, a1, 309, 0, 0, 1);
		var expected2 = new DMatrixRMaj(3, 3, true, a2, 0, 300, 0, a2, 309, 0, 0, 1);

		checkKnownRectification(1.0, K1, K2, expected1, expected2);
	}

	private void checkKnownRectification( double tx,
										  DMatrixRMaj k1, DMatrixRMaj k2,
										  DMatrixRMaj expected1, DMatrixRMaj expected2 ) {
		// the second image will be to the left of the first image, so the images will need to be filled
		Se3_F64 poseA1 = SpecialEuclideanOps_F64.axisXyz(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null);
		Se3_F64 poseA2 = SpecialEuclideanOps_F64.axisXyz(tx, 0.0, 0.0, 0.0, 0.0, 0.0, null);

		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(k1, poseA1, k2, poseA2);

		assertTrue(MatrixFeatures_DDRM.isEquals(expected1, alg.undistToRectPixels1, UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_DDRM.isEquals(expected2, alg.undistToRectPixels2, UtilEjml.TEST_F64));
	}

	/**
	 * Check to see if epipoles are at infinity after rectification
	 */
	@Test void checkEpipolarGeometry() {
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(-0.3, 0.05, 0.07, 0.1, 0, 0.1).invert(null);
		Se3_F64 poseA2 = createPose(0.2, -0.1, 0.02, 1, 0, 0.1).invert(null);

		// project epipoles
		Point2D_F64 epi1 = renderPixel(poseA1, K, new Point3D_F64(1, 0, 0.1), null);
		Point2D_F64 epi2 = renderPixel(poseA2, K, new Point3D_F64(0.1, 0, 0.1), null);

		// compute transforms
		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K, poseA1, K, poseA2);

		// apply transform
		Point3D_F64 epi1a = new Point3D_F64();
		GeometryMath_F64.mult(alg.getUndistToRectPixels1(), epi1, epi1a);
		Point3D_F64 epi2a = new Point3D_F64();
		GeometryMath_F64.mult(alg.getUndistToRectPixels2(), epi2, epi2a);

		// see if epipoles are now at infinity
		assertEquals(0, epi1a.getZ(), 1e-8);
		assertEquals(0, epi2a.getZ(), 1e-8);
	}

	/**
	 * See if the transform align an observation to the same y-axis
	 */
	@Test void alignY() {
		// different calibration matrices
		DMatrixRMaj K1 = new DMatrixRMaj(3, 3, true, 300, 0, 200, 0, 400, 205, 0, 0, 1);
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3, true, 180, 0, 195, 0, 370, 210, 0, 0, 1);

		// only rotate around the y-axis so that the rectified coordinate system will have to be
		// the same as the global
		Se3_F64 poseA1 = createPose(-0.3, 0.05, 0.07, 0.1, 0, 0.1);
		Se3_F64 poseA2 = createPose(0.2, -0.1, 0.02, 1, 0, 0.1);
		alignY(K1, poseA1, K2, poseA2);

		// the (Y,Z) coordinates are now different
		poseA1 = createPose(-0.3, 0.05, 0.1, 0.1, 0.05, -0.1);
		poseA2 = createPose(0.2, -0.1, 0.02, 1, -0.4, 0.2);
		alignY(K1, poseA1, K2, poseA2);

		poseA1 = createPose(0, 0, 0, 0, 0, 0);
		poseA2 = createPose(0, 0, 0, -0.2, 0, -0.1);
		alignY(K1, poseA1, K2, poseA2);
	}

	private void alignY( DMatrixRMaj K1, Se3_F64 poseA1, DMatrixRMaj K2, Se3_F64 poseA2 ) {
		// point being observed
		Point3D_F64 X = new Point3D_F64(0, 0, 4);

		// unrectified observation
		Point2D_F64 o1 = renderPixel(poseA1, K1, X, null);
		Point2D_F64 o2 = renderPixel(poseA2, K2, X, null);

		// original observations should not line up
		assertTrue(Math.abs(o1.y - o2.y) > 1e-8);

		// compute transforms
		RectifyCalibrated alg = new RectifyCalibrated();
		alg.process(K1, poseA1, K2, poseA2);

		// apply transform to create rectified observations
		Point2D_F64 r1 = new Point2D_F64();
		Point2D_F64 r2 = new Point2D_F64();

		GeometryMath_F64.mult(alg.getUndistToRectPixels1(), o1, r1);
		GeometryMath_F64.mult(alg.getUndistToRectPixels2(), o2, r2);

		// see if they line up
		assertEquals(r1.y, r2.y, 1e-8);
	}

	private Se3_F64 createPose( double rotX, double rotY, double rotZ, double x, double y, double z ) {
		Se3_F64 ret = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotX, rotY, rotZ, ret.getR());
		ret.getT().setTo(x, y, z);
		return ret;
	}
}
