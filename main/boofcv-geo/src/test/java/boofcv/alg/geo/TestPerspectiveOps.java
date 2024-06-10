/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.UtilAngle;
import georegression.struct.EulerType;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.homography.UtilHomography_F64;
import georegression.struct.point.*;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.affine.AffinePointOps_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.MatrixFeatures_D;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestPerspectiveOps extends BoofStandardJUnit {
	@Test void approximatePinhole() {
		CameraPinhole original = PerspectiveOps.createIntrinsic(640, 480, 75, 70, null);

		LensDistortionPinhole distortion = new LensDistortionPinhole(original);

		CameraPinhole found = PerspectiveOps.approximatePinhole(distortion.undistort_F64(true, false),
				original.width, original.height);

		assertEquals(original.width, found.width);
		assertEquals(original.width, found.width);
		assertEquals(original.skew, found.skew, UtilEjml.TEST_F64);
		assertEquals(original.fx, found.fx, 1);
		assertEquals(original.fy, found.fy, 1);
	}

	@Test void guessIntrinsic_two() {

		double hfov = 30;
		double vfov = 35;

		CameraPinhole found = PerspectiveOps.createIntrinsic(640, 480, hfov, vfov, null);

		assertEquals(UtilAngle.degreeToRadian(hfov), 2.0*Math.atan(found.cx/found.fx), 1e-6);
		assertEquals(UtilAngle.degreeToRadian(vfov), 2.0*Math.atan(found.cy/found.fy), 1e-6);
	}

	@Test void guessIntrinsic_one() {

		double hfov = 30;

		CameraPinholeBrown found = PerspectiveOps.createIntrinsic(640, 480, hfov, null);

		assertEquals(UtilAngle.degreeToRadian(hfov), 2.0*Math.atan(found.cx/found.fx), 1e-6);
		assertEquals(found.fx, found.fy, 1e-6);
	}

	@Test void scaleIntrinsic() {
		Point3D_F64 X = new Point3D_F64(0.1, 0.3, 2);

		CameraPinholeBrown param = new CameraPinholeBrown(200, 300, 2, 250, 260, 200, 300);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(param, (DMatrixRMaj)null);

		// find the pixel location in the unscaled image
		Point2D_F64 a = PerspectiveOps.renderPixel(new Se3_F64(), K, X, null);

		PerspectiveOps.scaleIntrinsic(param, 0.5);
		K = PerspectiveOps.pinholeToMatrix(param, (DMatrixRMaj)null);

		// find the pixel location in the scaled image
		Point2D_F64 b = PerspectiveOps.renderPixel(new Se3_F64(), K, X, null);

		assertEquals(a.x*0.5, b.x, 1e-8);
		assertEquals(a.y*0.5, b.y, 1e-8);
	}

	@Test void adjustIntrinsic() {

		DMatrixRMaj B = new DMatrixRMaj(3, 3, true, 2, 0, 1, 0, 3, 2, 0, 0, 1);

		CameraPinholeBrown param = new CameraPinholeBrown(200, 300, 2, 250, 260, 200, 300).fsetRadial(0.1, 0.3);
		CameraPinholeBrown found = PerspectiveOps.adjustIntrinsic(param, B, null);

		DMatrixRMaj A = PerspectiveOps.pinholeToMatrix(param, (DMatrixRMaj)null);

		DMatrixRMaj expected = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(B, A, expected);

		assertArrayEquals(param.radial, found.radial, 1e-8);
		DMatrixRMaj foundM = PerspectiveOps.pinholeToMatrix(found, (DMatrixRMaj)null);

		assertTrue(MatrixFeatures_DDRM.isIdentical(expected, foundM, 1e-8));
	}

	@Test void pinholeToMatrix_params_D() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(1.0, 2, 3, 4, 5);

		assertEquals(1, K.get(0, 0), UtilEjml.TEST_F64);
		assertEquals(2, K.get(1, 1), UtilEjml.TEST_F64);
		assertEquals(3, K.get(0, 1), UtilEjml.TEST_F64);
		assertEquals(4, K.get(0, 2), UtilEjml.TEST_F64);
		assertEquals(5, K.get(1, 2), UtilEjml.TEST_F64);
		assertEquals(1, K.get(2, 2), UtilEjml.TEST_F64);
	}

	@Test void pinholeToMatrix_params_F() {
		FMatrixRMaj K = PerspectiveOps.pinholeToMatrix(1.0f, 2f, 3f, 4, 5);

		assertEquals(1, K.get(0, 0), UtilEjml.TEST_F32);
		assertEquals(2, K.get(1, 1), UtilEjml.TEST_F32);
		assertEquals(3, K.get(0, 1), UtilEjml.TEST_F32);
		assertEquals(4, K.get(0, 2), UtilEjml.TEST_F32);
		assertEquals(5, K.get(1, 2), UtilEjml.TEST_F32);
		assertEquals(1, K.get(2, 2), UtilEjml.TEST_F32);
	}

	@Test void pinholeToMatrix_class_D() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(new CameraPinhole(1.0, 2, 3, 4, 5, 400, 500), (DMatrixRMaj)null);

		assertEquals(1, K.get(0, 0), UtilEjml.TEST_F64);
		assertEquals(2, K.get(1, 1), UtilEjml.TEST_F64);
		assertEquals(3, K.get(0, 1), UtilEjml.TEST_F64);
		assertEquals(4, K.get(0, 2), UtilEjml.TEST_F64);
		assertEquals(5, K.get(1, 2), UtilEjml.TEST_F64);
		assertEquals(1, K.get(2, 2), UtilEjml.TEST_F64);
	}

	@Test void pinholeToMatrix_class_F() {
		FMatrixRMaj K = PerspectiveOps.pinholeToMatrix(new CameraPinhole(1.0, 2, 3, 4, 5, 400, 500), (FMatrixRMaj)null);

		assertEquals(1, K.get(0, 0), UtilEjml.TEST_F32);
		assertEquals(2, K.get(1, 1), UtilEjml.TEST_F32);
		assertEquals(3, K.get(0, 1), UtilEjml.TEST_F32);
		assertEquals(4, K.get(0, 2), UtilEjml.TEST_F32);
		assertEquals(5, K.get(1, 2), UtilEjml.TEST_F32);
		assertEquals(1, K.get(2, 2), UtilEjml.TEST_F32);
	}

	@Test void matrixToPinhole_D() {
		double fx = 1;
		double fy = 2;
		double skew = 3;
		double cx = 4;
		double cy = 5;

		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, fx, skew, cx, 0, fy, cy, 0, 0, 1);
		CameraPinhole ret = PerspectiveOps.matrixToPinhole(K, 100, 200, null);

		assertEquals(ret.fx, fx, 1.1);
		assertEquals(ret.fy, fy);
		assertEquals(ret.skew, skew);
		assertEquals(ret.cx, cx);
		assertEquals(ret.cy, cy);
		assertEquals(100, ret.width);
		assertEquals(200, ret.height);
	}

	@Test void matrixToPinhole_F() {
		float fx = 1;
		float fy = 2;
		float skew = 3;
		float cx = 4;
		float cy = 5;

		FMatrixRMaj K = new FMatrixRMaj(3, 3, true, fx, skew, cx, 0, fy, cy, 0, 0, 1);
		CameraPinhole ret = PerspectiveOps.matrixToPinhole(K, 100, 200, null);

		assertEquals(ret.fx, fx);
		assertEquals(ret.fy, fy);
		assertEquals(ret.skew, skew);
		assertEquals(ret.cx, cx);
		assertEquals(ret.cy, cy);
		assertEquals(100, ret.width);
		assertEquals(200, ret.height);
	}

	/**
	 * Test using a known pinhole model which fits its assumptions perfectly
	 */
	@Test void estimatePinhole() {
		CameraPinhole expected = new CameraPinhole(500, 550, 0, 600, 700, 1200, 1400);
		Point2Transform2_F64 pixelToNorm = new LensDistortionPinhole(expected).distort_F64(true, false);

		CameraPinhole found = PerspectiveOps.estimatePinhole(pixelToNorm, expected.width, expected.height);

		assertEquals(expected.fx, found.fx, UtilEjml.TEST_F64);
		assertEquals(expected.fy, found.fy, UtilEjml.TEST_F64);
		assertEquals(expected.cx, found.cx, UtilEjml.TEST_F64);
		assertEquals(expected.cy, found.cy, UtilEjml.TEST_F64);
		assertEquals(expected.skew, found.skew, UtilEjml.TEST_F64);
		assertEquals(expected.width, found.width);
		assertEquals(expected.height, found.height);
	}

	@Test void convertNormToPixel_intrinsic_F64() {
		CameraPinholeBrown intrinsic = new CameraPinholeBrown(100, 150, 0.1, 120, 209, 500, 600);

		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);

		Point2D_F64 norm = new Point2D_F64(-0.1, 0.25);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K, norm, expected);

		Point2D_F64 found = PerspectiveOps.convertNormToPixel(intrinsic, norm.x, norm.y, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void convertNormToPixel_matrix() {
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 100, 0.1, 120, 0, 150, 209, 0, 0, 1);

		Point2D_F64 norm = new Point2D_F64(-0.1, 0.25);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K, norm, expected);

		Point2D_F64 found = PerspectiveOps.convertNormToPixel(K, norm, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void convertPixelToNorm_intrinsic_F64() {
		CameraPinholeBrown intrinsic = new CameraPinholeBrown
				(100, 150, 0.1, 120, 209, 500, 600).fsetRadial(0.1, -0.05);

		Point2Transform2_F64 p2n = LensDistortionFactory.narrow(intrinsic).undistort_F64(true, false);

		Point2D_F64 pixel = new Point2D_F64(100, 120);
		Point2D_F64 expected = new Point2D_F64();

		p2n.compute(pixel.x, pixel.y, expected);

		Point2D_F64 found = PerspectiveOps.convertPixelToNorm(intrinsic, pixel, null);

		assertEquals(expected.x, found.x, UtilEjml.TEST_F64);
		assertEquals(expected.y, found.y, UtilEjml.TEST_F64);
	}

	@Test void convertPixelToNorm_intrinsic_F32() {
		CameraPinholeBrown intrinsic = new CameraPinholeBrown
				(100, 150, 0.1, 120, 209, 500, 600).fsetRadial(0.1, -0.05);

		Point2Transform2_F32 p2n = LensDistortionFactory.narrow(intrinsic).undistort_F32(true, false);

		Point2D_F32 pixel = new Point2D_F32(100, 120);
		Point2D_F32 expected = new Point2D_F32();

		p2n.compute(pixel.x, pixel.y, expected);

		Point2D_F32 found = PerspectiveOps.convertPixelToNorm(intrinsic, pixel, null);

		assertEquals(expected.x, found.x, UtilEjml.TEST_F32);
		assertEquals(expected.y, found.y, UtilEjml.TEST_F32);
	}

	@Test void convertPixelToNorm_matrix() {
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 100, 0.1, 120, 0, 150, 209, 0, 0, 1);
		DMatrixRMaj K_inv = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.invert(K, K_inv);

		Point2D_F64 pixel = new Point2D_F64(100, 120);
		Point2D_F64 expected = new Point2D_F64();

		GeometryMath_F64.mult(K_inv, pixel, expected);

		Point2D_F64 found = PerspectiveOps.convertPixelToNorm(K, pixel, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void renderPixel_SE() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		Se3_F64 worldToCamera = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.05, 0.03, worldToCamera.getR());
		worldToCamera.getT().setTo(0.2, 0.01, -0.03);

		DMatrixRMaj K = RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand);

		Point3D_F64 X_cam = SePointOps_F64.transform(worldToCamera, X, null);
		Point2D_F64 found;

		// calibrated case
		found = PerspectiveOps.renderPixel(worldToCamera, X, null);
		assertEquals(X_cam.x/X_cam.z, found.x, 1e-8);
		assertEquals(X_cam.y/X_cam.z, found.y, 1e-8);

		// uncalibrated case
		Point2D_F64 expected = new Point2D_F64();
		expected.x = X_cam.x/X_cam.z;
		expected.y = X_cam.y/X_cam.z;
		GeometryMath_F64.mult(K, expected, expected);

		found = PerspectiveOps.renderPixel(worldToCamera, K, X, null);
		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void renderPixel_intrinsic() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		CameraPinhole intrinsic = new CameraPinhole(100, 150, 0.1, 120, 209, 500, 600);

		double normX = X.x/X.z;
		double normY = X.y/X.z;

		Point2D_F64 expected = new Point2D_F64();
		PerspectiveOps.convertNormToPixel(intrinsic, normX, normY, expected);

		Point2D_F64 found = PerspectiveOps.renderPixel(intrinsic, X, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void renderPixel_SE3_intrinsic() {
		Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.01, -0.03, 0.1, -0.05, 0.03, null);

		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);
		Point3D_F64 X_cam = SePointOps_F64.transform(worldToCamera, X, null);

		CameraPinhole intrinsic = new CameraPinhole(100, 150, 0.1, 120, 209, 500, 600);

		Point2D_F64 expected = new Point2D_F64();
		expected.x = X_cam.x/X_cam.z;
		expected.y = X_cam.y/X_cam.z;
		expected.x = intrinsic.fx*expected.x + intrinsic.cx + intrinsic.skew*expected.y;
		expected.y = intrinsic.fy*expected.y + intrinsic.cy;

		Point2D_F64 found = PerspectiveOps.renderPixel(worldToCamera, intrinsic, X, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void renderPixel_SE3_K_matrix() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.01, -0.03, 0.1, -0.05, 0.03, null);

		CameraPinhole intrinsic = new CameraPinhole(100, 150, 0.1, 120, 209, 500, 600);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);

		Point2D_F64 expected = PerspectiveOps.renderPixel(worldToCamera, intrinsic, X, null);
		Point2D_F64 found = PerspectiveOps.renderPixel(worldToCamera, K, X, null);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void renderPixel_SE3_homogenous() {
		var X3 = new Point3D_F64(0.1, -0.05, 3);
		var X4 = new Point4D_F64(0.1, -0.05, 3, 1);
		X4.scale(1.2);

		Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.01, -0.03, 0.1, -0.05, 0.03, null);

		CameraPinhole intrinsic = new CameraPinhole(100, 150, 0.1, 120, 209, 500, 600);

		{
			Point2D_F64 expected = PerspectiveOps.renderPixel(worldToCamera, intrinsic, X3, null);
			Point2D_F64 found = PerspectiveOps.renderPixel(worldToCamera, intrinsic, X4, null);

			assertEquals(expected.x, found.x, 1e-8);
			assertEquals(expected.y, found.y, 1e-8);
		}

		// test to see if it can handle points at infinity. Translational component shouldn't matter
		Se3_F64 noTranslate = worldToCamera.copy();
		noTranslate.T.setTo(0, 0, 0);

		{
			X4.w = 0.0;
			Point2D_F64 expected = PerspectiveOps.renderPixel(noTranslate, intrinsic, X3, null);
			Point2D_F64 found = PerspectiveOps.renderPixel(worldToCamera, intrinsic, X4, null);

			assertEquals(expected.x, found.x, 1e-8);
			assertEquals(expected.y, found.y, 1e-8);
		}
	}

	@Test void renderPixel_cameramatrix() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 3);

		Se3_F64 worldToCamera = SpecialEuclideanOps_F64.eulerXyz(0.2, 0.01, -0.03, 0.1, -0.05, 0.03, null);

		DMatrixRMaj K = RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand);

		Point2D_F64 expected = PerspectiveOps.renderPixel(worldToCamera, K, X, null);

		DMatrixRMaj P = PerspectiveOps.createCameraMatrix(worldToCamera.R, worldToCamera.T, K, null);
		Point2D_F64 found = PerspectiveOps.renderPixel(P, X);

		assertEquals(expected.x, found.x, 1e-8);
		assertEquals(expected.y, found.y, 1e-8);
	}

	@Test void splitAssociated_pair() {
		List<AssociatedPair> list = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			AssociatedPair p = new AssociatedPair();

			p.p2.setTo(rand.nextDouble()*5, rand.nextDouble()*5);
			p.p1.setTo(rand.nextDouble()*5, rand.nextDouble()*5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list, list1, list2);

		assertEquals(list.size(), list1.size());
		assertEquals(list.size(), list2.size());

		for (int i = 0; i < list.size(); i++) {
			assertSame(list.get(i).p1, list1.get(i));
			assertSame(list.get(i).p2, list2.get(i));
		}
	}

	@Test void splitAssociated_triple() {
		List<AssociatedTriple> list = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			AssociatedTriple p = new AssociatedTriple();

			p.p1.setTo(rand.nextDouble()*5, rand.nextDouble()*5);
			p.p2.setTo(rand.nextDouble()*5, rand.nextDouble()*5);
			p.p3.setTo(rand.nextDouble()*5, rand.nextDouble()*5);

			list.add(p);
		}

		List<Point2D_F64> list1 = new ArrayList<>();
		List<Point2D_F64> list2 = new ArrayList<>();
		List<Point2D_F64> list3 = new ArrayList<>();

		PerspectiveOps.splitAssociated(list, list1, list2, list3);

		assertEquals(list.size(), list1.size());
		assertEquals(list.size(), list2.size());
		assertEquals(list.size(), list3.size());

		for (int i = 0; i < list.size(); i++) {
			assertSame(list.get(i).p1, list1.get(i));
			assertSame(list.get(i).p2, list2.get(i));
			assertSame(list.get(i).p3, list3.get(i));
		}
	}

	@Test void createCameraMatrix() {
		SimpleMatrix R = SimpleMatrix.random_DDRM(3, 3, -1, 1, rand);
		Vector3D_F64 T = new Vector3D_F64(2, 3, -4);
		SimpleMatrix K = SimpleMatrix.wrap(RandomMatrices_DDRM.triangularUpper(3, 0, -1, 1, rand));

		SimpleMatrix T_ = new SimpleMatrix(3, 1, true, new double[]{T.x, T.y, T.z});

		// test calibrated camera
		DMatrixRMaj found = PerspectiveOps.createCameraMatrix(R.getDDRM(), T, null, null);
		for (int i = 0; i < 3; i++) {
			assertEquals(found.get(i, 3), T_.get(i), 1e-8);
			for (int j = 0; j < 3; j++) {
				assertEquals(found.get(i, j), R.get(i, j), 1e-8);
			}
		}

		// test uncalibrated camera
		found = PerspectiveOps.createCameraMatrix(R.getDDRM(), T, K.getDDRM(), null);

		SimpleMatrix expectedR = K.mult(R);
		SimpleMatrix expectedT = K.mult(T_);

		for (int i = 0; i < 3; i++) {
			assertEquals(found.get(i, 3), expectedT.get(i), 1e-8);
			for (int j = 0; j < 3; j++) {
				assertEquals(found.get(i, j), expectedR.get(i, j), 1e-8);
			}
		}
	}

	@Test void computeHFov() {
		CameraPinhole intrinsic = new CameraPinhole(500, 600, 0, 500, 500, 1000, 1000);

		assertEquals(2*Math.atan(1.0), PerspectiveOps.computeHFov(intrinsic), UtilEjml.TEST_F64);
	}

	@Test void computeVFov() {
		CameraPinhole intrinsic = new CameraPinhole(500, 600, 0, 500, 500, 1000, 1000);

		assertEquals(2*Math.atan(500/600.0), PerspectiveOps.computeVFov(intrinsic), UtilEjml.TEST_F64);
	}

	@Test void multTranA_triple_dense() {
		DMatrixRMaj A = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj B = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj C = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj D = RandomMatrices_DDRM.rectangle(3, 3, rand);

		Equation eq = new Equation(A, "A", B, "B", C, "C");
		eq.process("D=A'*B*C");
		DMatrixRMaj expected = eq.lookupDDRM("D");

		PerspectiveOps.multTranA(A, B, C, D);

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, D, UtilEjml.TEST_F64));
	}

	@Test void multTranC_triple_dense() {
		DMatrixRMaj A = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj B = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj C = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrixRMaj D = RandomMatrices_DDRM.rectangle(3, 3, rand);

		Equation eq = new Equation(A, "A", B, "B", C, "C");
		eq.process("D=A*B*C'");
		DMatrixRMaj expected = eq.lookupDDRM("D");

		PerspectiveOps.multTranC(A, B, C, D);

		assertTrue(MatrixFeatures_DDRM.isEquals(expected, D, UtilEjml.TEST_F64));
	}

	DMatrix3x3 random3x3() {
		DMatrixRMaj A = RandomMatrices_DDRM.rectangle(3, 3, rand);
		DMatrix3x3 f = new DMatrix3x3();
		f.setTo(A);
		return f;
	}

	@Test void multTranA_triple_fixed() {
		DMatrix3x3 A = random3x3();
		DMatrix3x3 B = random3x3();
		DMatrix3x3 C = random3x3();
		DMatrix3x3 D = random3x3();

		Equation eq = new Equation(A, "A", B, "B", C, "C");
		eq.process("D=A'*B*C");
		DMatrixRMaj expected = eq.lookupDDRM("D");

		PerspectiveOps.multTranA(A, B, C, D);

		assertTrue(MatrixFeatures_D.isEquals(expected, D));
	}

	@Test void multTranC_triple_fixed() {
		DMatrix3x3 A = random3x3();
		DMatrix3x3 B = random3x3();
		DMatrix3x3 C = random3x3();
		DMatrix3x3 D = random3x3();

		Equation eq = new Equation(A, "A", B, "B", C, "C");
		eq.process("D=A*B*C'");
		DMatrixRMaj expected = eq.lookupDDRM("D");

		PerspectiveOps.multTranC(A, B, C, D);

		assertTrue(MatrixFeatures_D.isEquals(expected, D));
	}

	@Test void inplaceAdjustCameraMatrix() {
		DMatrixRMaj P = RandomMatrices_DDRM.rectangle(3, 4, rand);

		double sx = 0.3, sy = 0.7, tx = 0.9, ty = -0.5;
		double[][] _A = {{sx, 0, tx}, {0, sy, ty}, {0, 0, 1}};
		DMatrixRMaj A = new DMatrixRMaj(_A);

		DMatrixRMaj expected = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(A, P, expected);

		PerspectiveOps.inplaceAdjustCameraMatrix(sx, sy, tx, ty, P);

		assertTrue(MatrixFeatures_DDRM.isIdentical(expected, P, UtilEjml.TEST_F64));
	}

	@Test void invariantCrossLine_2D() {
		// Could probably just create an arbitrary H, but this is more realistic
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 1, 0.3, null);
		DMatrixRMaj K = new DMatrixRMaj(new double[][]{{500, 0, 500}, {0, 500, 500}, {0, 0, 1}});
		DMatrixRMaj H = MultiViewOps.createHomography(R, new Vector3D_F64(-0.6, 1.1, 0.0), 1, new Vector3D_F64(0, 0, 1), K);
		Homography2D_F64 homography = UtilHomography_F64.convert(H, null);

		var points = new ArrayList<Point2D_F64>();
		double x0 = 4, y0 = 10;
		double dx = 1.5, dy = 3.6;
		points.add(new Point2D_F64(x0, y0));
		points.add(new Point2D_F64(x0 + 10*dx, y0 + 10*dy));
		points.add(new Point2D_F64(x0 + 30*dx, y0 + 30*dy));
		points.add(new Point2D_F64(x0 + 40*dx, y0 + 40*dy));

		double expected = PerspectiveOps.invariantCrossLine(
				points.get(0), points.get(1), points.get(2), points.get(3));

		for (var p : points) {
			HomographyPointOps_F64.transform(homography, p.x, p.y, p);
		}

		double found = PerspectiveOps.invariantCrossLine(
				points.get(0), points.get(1), points.get(2), points.get(3));

		// if this is really an invariant it should be identical
		assertEquals(expected, found, UtilEjml.TEST_F64_SQ);
	}

	@Test void invariantCrossLine_3D() {
		Se3_F64 motion = SpecialEuclideanOps_F64.eulerXyz(10, 15, -8, 0.1, -0.05, 0.9, null);

		var points = new ArrayList<Point3D_F64>();
		double x0 = 4, y0 = 10, z0 = 40;
		double dx = 1.5, dy = 3.6, dz = 0.1;
		points.add(new Point3D_F64(x0, y0, z0));
		points.add(new Point3D_F64(x0 + 10*dx, y0 + 10*dy, z0 + 10 + dz));
		points.add(new Point3D_F64(x0 + 30*dx, y0 + 30*dy, z0 + 30 + dz));
		points.add(new Point3D_F64(x0 + 40*dx, y0 + 40*dy, z0 + 40 + dz));

		double expected = PerspectiveOps.invariantCrossLine(
				points.get(0), points.get(1), points.get(2), points.get(3));

		for (var p : points) {
			SePointOps_F64.transform(motion, p, p);
		}

		double found = PerspectiveOps.invariantCrossLine(
				points.get(0), points.get(1), points.get(2), points.get(3));

		// if this is really an invariant it should be identical
		assertEquals(expected, found, UtilEjml.TEST_F64_SQ);
	}

	@Test void invariantCrossRatio() {
		// Could probably just create an arbitrary H, but this is more realistic
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 1, 0.3, null);
		DMatrixRMaj K = new DMatrixRMaj(new double[][]{{500, 0, 500}, {0, 500, 500}, {0, 0, 1}});
		DMatrixRMaj H = MultiViewOps.createHomography(R, new Vector3D_F64(-0.6, 1.1, 0.0), 1, new Vector3D_F64(0, 0, 1), K);
		Homography2D_F64 homography = UtilHomography_F64.convert(H, null);

		var points = new ArrayList<Point2D_F64>();
		points.add(new Point2D_F64(6, 9));
		points.add(new Point2D_F64(8, 0));
		points.add(new Point2D_F64(0, 20));
		points.add(new Point2D_F64(10, 16));
		points.add(new Point2D_F64(12, 6.5));

		double expected = PerspectiveOps.invariantCrossRatio(
				points.get(0), points.get(1), points.get(2), points.get(3), points.get(4));

		for (var p : points) {
			HomographyPointOps_F64.transform(homography, p.x, p.y, p);
		}

		double found = PerspectiveOps.invariantCrossRatio(
				points.get(0), points.get(1), points.get(2), points.get(3), points.get(4));

		// if this is really an invariant it should be identical
		assertEquals(expected, found, UtilEjml.TEST_F64_SQ);
	}

	@Test void invariantAffine() {
		var affine = new Affine2D_F64(0.9, 0.2, -0.1, 1.1, -0.4, 10);

		var points = new ArrayList<Point2D_F64>();
		points.add(new Point2D_F64(6, 9));
		points.add(new Point2D_F64(8, 0));
		points.add(new Point2D_F64(0, 20));
		points.add(new Point2D_F64(10, 16));

		double expected = PerspectiveOps.invariantAffine(points.get(0), points.get(1), points.get(2), points.get(3));

		for (var p : points) {
			AffinePointOps_F64.transform(affine, p.x, p.y, p);
		}

		double found = PerspectiveOps.invariantAffine(points.get(0), points.get(1), points.get(2), points.get(3));

		// if this is really an invariant it should be identical
		assertEquals(expected, found, UtilEjml.TEST_F64_SQ);
	}

	@Test void homogenousTo3dPositiveZ() {
		Point3D_F64 found = new Point3D_F64();
		PerspectiveOps.homogenousTo3dPositiveZ(new Point4D_F64(4, 6, 8, 2), 1e8, 1e-8, found);
		assertEquals(0.0, found.distance(2, 3, 4), UtilEjml.TEST_F64);

		// positive constraint is only enforced for points at infinity
		PerspectiveOps.homogenousTo3dPositiveZ(new Point4D_F64(4, 6, 8, -2), 1e8, 1e-8, found);
		assertEquals(0.0, found.distance(-2, -3, -4), UtilEjml.TEST_F64);

		// point at infinity. chose to put it in front with positive z
		double r = 1e8;
		double d = Math.sqrt(4.0*4.0 + 6.0*6.0 + 8.0*8.0);
		PerspectiveOps.homogenousTo3dPositiveZ(new Point4D_F64(-4, -6, -8, 0), r, 1e-8, found);
		assertEquals(0.0, found.distance(4*r/d, 6*r/d, 8*r/d), UtilEjml.TEST_F64);
	}

	@Test void distance3DvsH() {
		Point3D_F64 a = new Point3D_F64(0, 0, 3);
		Point4D_F64 b = new Point4D_F64(0, 0, 20, 1.0);

		assertEquals(17.0, PerspectiveOps.distance3DvsH(a, b, 1e-8));
		b.scale(-1e-10);
		assertEquals(17.0, PerspectiveOps.distance3DvsH(a, b, 1e-8));
		b.w = 0;
		assertEquals(Double.POSITIVE_INFINITY, PerspectiveOps.distance3DvsH(a, b, 1e-8));
	}

	@Test void isBehindCamera_homogenous() {
		// Easy numerically
		isBehindCamera_homogenous(1.0);

		// Check for underflow and overflow issues
		isBehindCamera_homogenous(1e20);
		isBehindCamera_homogenous(1e-20);

		// Check an edge case
		assertTrue(PerspectiveOps.isBehindCamera(new Point4D_F64(0, 0, 0, 0)));

		// Mix very large and small numbers
		checkBehindSwapSign(new Point4D_F64(0, 0, -1e20, 1e-20), true);
		checkBehindSwapSign(new Point4D_F64(0, 0, 1e-20, 1e20), false);
		checkBehindSwapSign(new Point4D_F64(0, 0, -1e200, 1e-200), true);
		checkBehindSwapSign(new Point4D_F64(0, 0, 1e-200, 1e200), false);
		checkBehindSwapSign(new Point4D_F64(0, 0, -1, Double.POSITIVE_INFINITY), true);
		checkBehindSwapSign(new Point4D_F64(0, 0, 1, Double.POSITIVE_INFINITY), false);
	}

	void isBehindCamera_homogenous( double v ) {
		// Standard scenarios
		checkBehindSwapSign(new Point4D_F64(0, 0, -v, v), true);
		checkBehindSwapSign(new Point4D_F64(0, 0, v, v), false);

		// Test points on the x-y plane
		checkBehindSwapSign(new Point4D_F64(0, 0, 0, v), true);
		checkBehindSwapSign(new Point4D_F64(0, 0, 0, -v), true);
		checkBehindSwapSign(new Point4D_F64(v, 2*v, 0, -v), true);

		// Check points at infinity
		assertTrue(PerspectiveOps.isBehindCamera(new Point4D_F64(0, 0, -v, 0)));
		assertTrue(PerspectiveOps.isBehindCamera(new Point4D_F64(2*v, -v, -v, 0)));
		assertFalse(PerspectiveOps.isBehindCamera(new Point4D_F64(0, 0, v, 0)));
		assertFalse(PerspectiveOps.isBehindCamera(new Point4D_F64(2*v, -v, v, 0)));
	}

	void checkBehindSwapSign( Point4D_F64 p, boolean expected ) {
		assertEquals(expected, PerspectiveOps.isBehindCamera(p));
		p.scale(-1);
		assertEquals(expected, PerspectiveOps.isBehindCamera(p));
	}

	@Test void invertCalibrationMatrix() {
		CameraPinhole pinhole = new CameraPinhole(400, 450, 0.2, 500, 470, 0, 0);
		DMatrixRMaj K = new DMatrixRMaj(3, 3);
		PerspectiveOps.pinholeToMatrix(pinhole, K);
		DMatrixRMaj K_inv = new DMatrixRMaj(3, 3);
		assertTrue(CommonOps_DDRM.invert(K, K_inv));

		DMatrixRMaj K_found = PerspectiveOps.invertCalibrationMatrix(K, null);
		assertTrue(MatrixFeatures_DDRM.isIdentical(K_inv, K_found, 1e-6));

		// Test recycling a matrix
		K_found.fill(2.5);
		PerspectiveOps.invertCalibrationMatrix(K, K_found);
		assertTrue(MatrixFeatures_DDRM.isIdentical(K_inv, K_found, 1e-6));
	}

	/**
	 * Compare it to a cartesian point. Should be the same since the only difference is scale.
	 */
	@Test void rotateH() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 1, -0.5, 0.7, null);

		var p3 = new Point3D_F64(1, 2, 3);
		var p4 = new Point4D_F64(1, 2, 3, 0.5);

		GeometryMath_F64.mult(R, p3, p3);
		PerspectiveOps.rotateH(R, p4, p4);

		assertEquals(p3.x, p4.x, UtilEjml.TEST_F64);
		assertEquals(p3.y, p4.y, UtilEjml.TEST_F64);
		assertEquals(p3.z, p4.z, UtilEjml.TEST_F64);
	}

	@Test void rotateTranH() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 1, -0.5, 0.7, null);

		var p4 = new Point4D_F64(1, 2, 3, 0.5);

		PerspectiveOps.rotateH(R, p4, p4);
		PerspectiveOps.rotateInvH(R, p4, p4);

		assertEquals(1, p4.x, UtilEjml.TEST_F64);
		assertEquals(2, p4.y, UtilEjml.TEST_F64);
		assertEquals(3, p4.z, UtilEjml.TEST_F64);
	}

	@Test void pointAt() {
		var points = new ArrayList<Point3D_F64>();
		// arbitrary point
		points.add(new Point3D_F64(1, -0.5, 2));
		points.add(new Point3D_F64(-1, 0.5, -2));

		// Pathological cases
		points.add(new Point3D_F64(0, 0, 2));
		points.add(new Point3D_F64(0, 2, 0));
		points.add(new Point3D_F64(2, 0, 0));
		points.add(new Point3D_F64(0, 0, -2));
		points.add(new Point3D_F64(0, -2, 0));
		points.add(new Point3D_F64(-2, 0, 0));

		var R = new DMatrixRMaj(3, 3);

		for (var point : points) {
			PerspectiveOps.pointAt(point.x, point.y, point.z, R);

			// when put into the camera's coordinate system the point should be dead center and in front of the camera
			GeometryMath_F64.multTran(R, point, point);
			assertEquals(0.0, point.x, UtilEjml.TEST_F64);
			assertEquals(0.0, point.y, UtilEjml.TEST_F64);
			assertTrue(point.z > 0);
		}
	}

	/**
	 * In these scenarios the rotation matrix should have all positive elements along the diagonal since there
	 * should be no flipping
	 */
	@Test void pointAt_positive_diagonal() {
		var points = new ArrayList<Point3D_F64>();

		points.add(new Point3D_F64(-1.3877787807814457E-17, 1.3877787807814457E-17, 3.344561447900182));
		points.add(new Point3D_F64(-1.3877787807814457E-17, 0.0, 3.344561447900182));
		points.add(new Point3D_F64(9.540979117872439E-17, -3.469446951953614E-18, 3.3445614479001797));
		points.add(new Point3D_F64(0.0, 5.551115123125783E-17, 3.3445614479001833));
		points.add(new Point3D_F64(0.0, -2.7755575615628914E-17, 3.3445614479001833));

		var R = new DMatrixRMaj(3, 3);

		for (var point : points) {
			PerspectiveOps.pointAt(point.x, point.y, point.z, R);

			for (int i = 0; i < 3; i++) {
				assertTrue(R.get(i,i) > 0);
			}
		}
	}
}
