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

package boofcv.alg.geo;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.h.CommonHomographyInducedPlane;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.alg.geo.impl.ProjectiveToIdentity;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.PairLineNorm;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.Tuple2;
import org.ddogleg.struct.Tuple3;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrix4x4;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF4;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.ejml.equation.Equation;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.ops.MatrixFeatures_D;
import org.ejml.simple.SimpleMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
class TestMultiViewOps extends BoofStandardJUnit {
	// camera calibration matrix
	DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 60, 0.01, 200, 0, 80, 150, 0, 0, 1);

	// camera locations
	Se3_F64 worldToCam1 = new Se3_F64();
	Se3_F64 worldToCam2, worldToCam3;

	// camera matrix for views 2 and 3
	DMatrixRMaj P2, P3;

	// Fundamental matrix for views 2 and 3
	DMatrixRMaj F2, F3;

	// trifocal tensor for these views
	TrifocalTensor tensor;

	// storage for lines in 3 views
	Vector3D_F64 line1 = new Vector3D_F64();
	Vector3D_F64 line2 = new Vector3D_F64();
	Vector3D_F64 line3 = new Vector3D_F64();

	TestMultiViewOps() {
		worldToCam2 = new Se3_F64();
		worldToCam3 = new Se3_F64();

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.2, 0.001, -0.02, worldToCam2.R);
		worldToCam2.getT().setTo(0.3, 0, 0.05);

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.8, -0.02, 0.003, worldToCam3.R);
		worldToCam3.getT().setTo(0.6, 0.2, -0.02);

		P2 = PerspectiveOps.createCameraMatrix(worldToCam2.R, worldToCam2.T, K, null);
		P3 = PerspectiveOps.createCameraMatrix(worldToCam3.R, worldToCam3.T, K, null);
		tensor = MultiViewOps.createTrifocal(P2, P3, null);
		tensor.normalizeScale();

		F2 = MultiViewOps.createEssential(worldToCam2.getR(), worldToCam2.getT(), null);
		F2 = MultiViewOps.createFundamental(F2, K);
		F3 = MultiViewOps.createEssential(worldToCam3.getR(), worldToCam3.getT(), null);
		F3 = MultiViewOps.createFundamental(F3, K);
	}

	/**
	 * Check the trifocal tensor using its definition
	 */
	@Test void createTrifocal_CameraMatrix2() {
		TrifocalTensor found = MultiViewOps.createTrifocal(P2, P3, null);

		List<Point3D_F64> points = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, -1, -1, -0.1, 0.1, 20, rand);

		// Test it against a constraint
		DMatrixRMaj C = RandomMatrices_DDRM.rectangle(3, 3, -1, 1, rand);
		for (Point3D_F64 X : points) {
			Point2D_F64 x1 = PerspectiveOps.renderPixel(worldToCam1, X, null);
			Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
			Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

			MultiViewOps.constraint(found, x1, x2, x3, C);
			for (int i = 0; i < 9; i++) {
				assertEquals(0, C.data[i], 10*UtilEjml.TEST_F64);
			}
		}
		// test it against its definition
		Equation eq = new Equation(P2, "P2", P3, "P3");
		for (int i = 0; i < 3; i++) {
			eq.alias(i, "i");
			eq.process("ai = P2(:,i)");
			eq.process("b4 = P3(:,3)");
			eq.process("a4 = P2(:,3)");
			eq.process("bi = P3(:,i)");

			eq.process("z = ai*b4' - a4*bi'");
			DMatrixRMaj expected = eq.lookupDDRM("z");

			assertTrue(MatrixFeatures_DDRM.isIdentical(expected, found.getT(i), UtilEjml.TEST_F64));
		}
	}

	/**
	 * Check using trifocal constraint
	 */
	@Test void createTrifocal_CameraMatrix3() {
		DMatrixRMaj P1 = PerspectiveOps.createCameraMatrix(worldToCam1.R, worldToCam1.T, K, null);
		TrifocalTensor found = MultiViewOps.createTrifocal(P1, P2, P3, null);

		assertTrue(CommonOps_DDRM.elementMaxAbs(found.T1) > 1e-4);
		assertTrue(CommonOps_DDRM.elementMaxAbs(found.T2) > 1e-4);
		assertTrue(CommonOps_DDRM.elementMaxAbs(found.T3) > 1e-4);

		List<Point3D_F64> points = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -1, 1, -1, -1, -0.1, 0.1, 20, rand);

		// Test it against a constraint
		DMatrixRMaj C = RandomMatrices_DDRM.rectangle(3, 3, -1, 1, rand);
		for (Point3D_F64 X : points) {
			Point2D_F64 x1 = PerspectiveOps.renderPixel(worldToCam1, K, X, null);
			Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
			Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

			MultiViewOps.constraint(found, x1, x2, x3, C);
			for (int i = 0; i < 9; i++) {
				assertEquals(0, C.data[i], UtilEjml.TEST_F64);
			}
		}
	}

	/**
	 * Check the trifocal tensor using its definition
	 */
	@Test void createTrifocal_SE() {

		TrifocalTensor found = MultiViewOps.createTrifocal(worldToCam2, worldToCam3, null);

		SimpleMatrix R2 = SimpleMatrix.wrap(worldToCam2.getR());
		SimpleMatrix R3 = SimpleMatrix.wrap(worldToCam3.getR());
		SimpleMatrix b4 = new SimpleMatrix(3, 1);
		SimpleMatrix a4 = new SimpleMatrix(3, 1);

		b4.set(0, worldToCam3.getX());
		b4.set(1, worldToCam3.getY());
		b4.set(2, worldToCam3.getZ());

		a4.set(0, worldToCam2.getX());
		a4.set(1, worldToCam2.getY());
		a4.set(2, worldToCam2.getZ());

		for (int i = 0; i < 3; i++) {
			SimpleMatrix ai = R2.extractVector(false, i);
			SimpleMatrix bi = R3.extractVector(false, i);

			SimpleMatrix expected = ai.mult(b4.transpose()).minus(a4.mult(bi.transpose()));

			assertTrue(MatrixFeatures_DDRM.isIdentical(expected.getDDRM(), found.getT(i), 1e-8));
		}
	}

	@Test void constraint_Trifocal_lll() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		Vector3D_F64 found = MultiViewOps.constraint(tensor, line1, line2, line3, null);

		assertEquals(0, found.x, 1e-12);
		assertEquals(0, found.y, 1e-12);
		assertEquals(0, found.z, 1e-12);
	}

	@Test void constraint_Trifocal_pll() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);

		double found = MultiViewOps.constraint(tensor, x1, line2, line3);

		assertEquals(0, found, 1e-12);
	}

	@Test void constraint_Trifocal_plp() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		Vector3D_F64 found = MultiViewOps.constraint(tensor, x1, line2, x3, null);

		assertEquals(0, found.x, 1e-12);
		assertEquals(0, found.y, 1e-12);
		assertEquals(0, found.z, 1e-12);
	}

	@Test void constraint_Trifocal_ppl() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);

		Vector3D_F64 found = MultiViewOps.constraint(tensor, x1, x2, line3, null);

		assertEquals(0, found.x, 1e-12);
		assertEquals(0, found.y, 1e-12);
		assertEquals(0, found.z, 1e-12);
	}

	@Test void constraint_Trifocal_ppp() {
		// Point in 3D space being observed
		Point3D_F64 X = new Point3D_F64(0.1, 0.5, 3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
		Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		// check the constraint
		DMatrixRMaj A = MultiViewOps.constraint(tensor, p1, p2, p3, null);

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(0, A.get(i, j), 1e-11);
			}
		}
	}

	@Test void constraint_epipolar() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);

		DMatrixRMaj E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E, K);

		assertEquals(0, MultiViewOps.constraint(F, p1, p2), 1e-8);
	}

	@Test void constraint_homography() {

		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0, 0, 1);
		Point3D_F64 X = new Point3D_F64(0.1, -0.4, d);


		DMatrixRMaj H = MultiViewOps.createHomography(worldToCam2.getR(), worldToCam2.getT(), d, N);

		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, X, null);

		Point2D_F64 found = MultiViewOps.constraintHomography(H, p1, null);

		assertEquals(p2.x, found.x, 1e-8);
		assertEquals(p2.y, found.y, 1e-8);
	}

	@Test void inducedHomography13() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 p3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		DMatrixRMaj H13 = MultiViewOps.inducedHomography13(tensor, line2, null);

		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H13, p1, found);

		assertEquals(p3.x, found.x, 1e-8);
		assertEquals(p3.y, found.y, 1e-8);
	}

	@Test void inducedHomography12() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);

		DMatrixRMaj H12 = MultiViewOps.inducedHomography12(tensor, line3, null);

		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H12, p1, found);

		assertEquals(p2.x, found.x, 1e-8);
		assertEquals(p2.y, found.y, 1e-8);
	}

	@Test void fundamentalToHomography3Pts() {
		var common = new CommonHomographyInducedPlane();

		DMatrixRMaj H = MultiViewOps.fundamentalToHomography3Pts(common.F, common.p1, common.p2, common.p3);

		// TODO Fix the algorithm to improve numerical stability. See code for comments
		common.checkHomography(H, 20);
	}

	@Test void fundamentalToHomographyLinePt() {
		var common = new CommonHomographyInducedPlane();

		PairLineNorm l1 = CommonHomographyInducedPlane.convert(common.p1, common.p2);

		DMatrixRMaj H = MultiViewOps.fundamentalToHomographyLinePt(common.F, l1, common.p3);

		common.checkHomography(H, UtilEjml.TEST_F64);
	}

	@Test void fundamentalToHomography2Lines() {
		var common = new CommonHomographyInducedPlane();

		PairLineNorm l1 = CommonHomographyInducedPlane.convert(common.p1, common.p2);
		PairLineNorm l2 = CommonHomographyInducedPlane.convert(common.p1, common.p3);

		DMatrixRMaj H = MultiViewOps.fundamentalToHomography2Lines(common.F, l1, l2);

		common.checkHomography(H, UtilEjml.TEST_F64);
	}

	/**
	 * Compute lines in each view using epipolar geometry that include point X. The first view is
	 * in normalized image coordinates
	 */
	private void computeLines( Point3D_F64 X, Vector3D_F64 line1, Vector3D_F64 line2, Vector3D_F64 line3 ) {
		Point3D_F64 X2 = X.copy();
		X2.y += 1;

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		line1.setTo(computeLine(X, X2, new Se3_F64(), null));
		line2.setTo(computeLine(X, X2, worldToCam2, K));
		line3.setTo(computeLine(X, X2, worldToCam3, K));
	}

	private Vector3D_F64 computeLine( Point3D_F64 X1, Point3D_F64 X2, Se3_F64 worldToCam, DMatrixRMaj K ) {
		Point2D_F64 a = PerspectiveOps.renderPixel(worldToCam, K, X1, null);
		Point2D_F64 b = PerspectiveOps.renderPixel(worldToCam, K, X2, null);

		Vector3D_F64 v1 = new Vector3D_F64(b.x - a.x, b.y - a.y, 0);
		Vector3D_F64 v2 = new Vector3D_F64(a.x, a.y, 1);
		Vector3D_F64 norm = new Vector3D_F64();

		GeometryMath_F64.cross(v1, v2, norm);

		norm.normalize();

		return norm;
	}

	@Test void extractEpipoles_threeview() {
		Point3D_F64 found2 = new Point3D_F64();
		Point3D_F64 found3 = new Point3D_F64();

		TrifocalTensor input = tensor.copy();

		MultiViewOps.extractEpipoles(input, found2, found3);

		// make sure the input was not modified
		for (int i = 0; i < 3; i++)
			assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(i), input.getT(i), 1e-8));

		Point3D_F64 space = new Point3D_F64();

		// check to see if it is the left-null space of their respective Fundamental matrices
		GeometryMath_F64.multTran(F2, found2, space);
		assertEquals(0, space.norm(), 1e-8);

		GeometryMath_F64.multTran(F3, found3, space);
		assertEquals(0, space.norm(), 1e-8);
	}

	@Test void extractFundamental_threeview() {
		DMatrixRMaj found2 = new DMatrixRMaj(3, 3);
		DMatrixRMaj found3 = new DMatrixRMaj(3, 3);

		TrifocalTensor input = tensor.copy();
		MultiViewOps.trifocalToFundamental(input, found2, found3);

		// make sure the input was not modified
		for (int i = 0; i < 3; i++)
			assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(i), input.getT(i), 1e-8));

		CommonOps_DDRM.scale(1.0/CommonOps_DDRM.elementMaxAbs(found2), found2);
		CommonOps_DDRM.scale(1.0/CommonOps_DDRM.elementMaxAbs(found3), found3);

		Point3D_F64 X = new Point3D_F64(0.1, 0.05, 2);

		// remember the first view is assumed to have a projection matrix of [I|0]
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		assertEquals(0, MultiViewOps.constraint(found2, x1, x2), 1e-8);
		assertEquals(0, MultiViewOps.constraint(found3, x1, x3), 1e-8);
	}

	@Test void extractCameraMatrices() {
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

		TrifocalTensor input = tensor.copy();
		MultiViewOps.trifocalToCameraMatrices(input, P2, P3);

		// make sure the input was not modified
		for (int i = 0; i < 3; i++)
			assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(i), input.getT(i), 1e-8));

		// Using found camera matrices render the point's location
		Point3D_F64 X = new Point3D_F64(0.1, 0.05, 2);

		Point2D_F64 x1 = new Point2D_F64(X.x/X.z, X.y/X.z);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(P2, X);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(P3, X);

		// validate correctness by testing a constraint on the points
		DMatrixRMaj A = new DMatrixRMaj(3, 3);
		MultiViewOps.constraint(tensor, x1, x2, x3, A);

		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				assertEquals(0, A.get(i, j), 1e-7);
			}
		}
	}

	@Test void createEssential() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.05, -0.04, 0.1, null);
		Vector3D_F64 T = new Vector3D_F64(2, 1, -3);
		T.normalize();

		DMatrixRMaj E = MultiViewOps.createEssential(R, T, null);

		// Test using the following theorem:  x2^T*E*x1 = 0
		Point3D_F64 X = new Point3D_F64(0.1, 0.1, 2);

		Point2D_F64 x0 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(R, T), X, null);

		double val = GeometryMath_F64.innerProd(x1, E, x0);
		assertEquals(0, val, 1e-8);
	}

	@Test void computeFundamental() {
		DMatrixRMaj E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E, K);

		Point3D_F64 X = new Point3D_F64(0.1, -0.1, 2.5);
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);

		assertEquals(0, MultiViewOps.constraint(F, p1, p2), 1e-8);
	}

	@Test void computeFundamental2() {
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3, true, 80, 0.02, 190, 0, 30, 170, 0, 0, 1);

		DMatrixRMaj E = MultiViewOps.createEssential(worldToCam2.R, worldToCam2.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E, K, K2);

		Point3D_F64 X = new Point3D_F64(0.1, -0.1, 2.5);
		Point2D_F64 p1 = PerspectiveOps.renderPixel(new Se3_F64(), K, X, null);
		Point2D_F64 p2 = PerspectiveOps.renderPixel(worldToCam2, K2, X, null);

		assertEquals(0, MultiViewOps.constraint(F, p1, p2), 1e-8);
	}

	@Test void createHomography_calibrated() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.01, 0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1, 1, 0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0, 0, 1);

		DMatrixRMaj H = MultiViewOps.createHomography(R, T, d, N);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1, 0.2, d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z, P.y/P.z);
		SePointOps_F64.transform(new Se3_F64(R, T), P, P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z, P.y/P.z);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x, found.x, 1e-8);
		assertEquals(x1.y, found.y, 1e-8);
	}

	@Test void createHomography_uncalibrated() {
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 0.1, 0.001, 200, 0, 0.2, 250, 0, 0, 1);
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.01, 0.2, null);
		Vector3D_F64 T = new Vector3D_F64(1, 1, 0.1);
		T.normalize();
		double d = 2;
		Vector3D_F64 N = new Vector3D_F64(0, 0, 1);

		DMatrixRMaj H = MultiViewOps.createHomography(R, T, d, N, K);

		// Test using the following theorem:  x2 = H*x1
		Point3D_F64 P = new Point3D_F64(0.1, 0.2, d); // a point on the plane

		Point2D_F64 x0 = new Point2D_F64(P.x/P.z, P.y/P.z);
		GeometryMath_F64.mult(K, x0, x0);
		SePointOps_F64.transform(new Se3_F64(R, T), P, P);
		Point2D_F64 x1 = new Point2D_F64(P.x/P.z, P.y/P.z);
		GeometryMath_F64.mult(K, x1, x1);
		Point2D_F64 found = new Point2D_F64();

		GeometryMath_F64.mult(H, x0, found);
		assertEquals(x1.x, found.x, 1e-8);
		assertEquals(x1.y, found.y, 1e-8);
	}

	@Test void extractEpipoles_stereo() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(400, 400, 0.1, 410, 399);
		for (int i = 0; i < 100; i++) {
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, rotX, rotY, rotZ, null);
			Vector3D_F64 T = new Vector3D_F64(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian());

			DMatrixRMaj E = MultiViewOps.createEssential(R, T, null);

			assertTrue(NormOps_DDRM.normF(E) != 0);

			Point3D_F64 e1 = new Point3D_F64();
			Point3D_F64 e2 = new Point3D_F64();

			MultiViewOps.extractEpipoles(E, e1, e2);

			Point3D_F64 temp = new Point3D_F64();

			GeometryMath_F64.mult(E, e1, temp);
			assertEquals(0, temp.norm(), 1e-8);

			GeometryMath_F64.multTran(E, e2, temp);
			assertEquals(0, temp.norm(), 1e-8);

			DMatrixRMaj F = MultiViewOps.createFundamental(E, K);
			MultiViewOps.extractEpipoles(F, e1, e2);
			GeometryMath_F64.mult(F, e1, temp);
			assertEquals(0, temp.norm(), 1e-8);
			GeometryMath_F64.multTran(F, e2, temp);
			assertEquals(0, temp.norm(), 1e-8);
		}
	}

	@Test void fundamentalToProjective_Two() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		Se3_F64 T = SpecialEuclideanOps_F64.eulerXyz(0.5, 0.7, -0.3, EulerType.XYZ, 1, 2, -0.5, null);

		DMatrixRMaj E = MultiViewOps.createEssential(T.R, T.T, null);
		DMatrixRMaj F = MultiViewOps.createFundamental(E, K);

		Point3D_F64 e1 = new Point3D_F64();
		Point3D_F64 e2 = new Point3D_F64();

		CommonOps_DDRM.scale(-2.0/F.get(0, 1), F);
		MultiViewOps.extractEpipoles(F, e1, e2);

		DMatrixRMaj P = MultiViewOps.fundamentalToProjective(F, e2, new Vector3D_F64(1, 1, 1), 2);

		// recompose the fundamental matrix using the special equation for canonical cameras
		DMatrixRMaj foundF = new DMatrixRMaj(3, 3);
		DMatrixRMaj crossEpi = new DMatrixRMaj(3, 3);

		GeometryMath_F64.crossMatrix(e2, crossEpi);

		DMatrixRMaj M = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.extract(P, 0, 3, 0, 3, M, 0, 0);
		CommonOps_DDRM.mult(crossEpi, M, foundF);

		// see if they are equal up to a scale factor
		CommonOps_DDRM.scale(1.0/foundF.get(0, 1), foundF);
		CommonOps_DDRM.scale(1.0/F.get(0, 1), F);

		assertTrue(MatrixFeatures_DDRM.isIdentical(F, foundF, 1e-8));
	}

	@Test void projectiveToFundamental_Two() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 400, 405);
		Se3_F64 M11 = new Se3_F64();
		Se3_F64 M12 = SpecialEuclideanOps_F64.eulerXyz(-1, 0, -0.2, EulerType.XYZ, 0, -.2, 0, null);

		DMatrixRMaj P1 = PerspectiveOps.createCameraMatrix(M11.R, M11.T, K, null);
		DMatrixRMaj P2 = PerspectiveOps.createCameraMatrix(M12.R, M12.T, K, null);

		DMatrixRMaj F21 = MultiViewOps.projectiveToFundamental(P1, P2, null);

		// test by seeing if a skew symmetric matrix is produced with the formula below
		Equation eq = new Equation(P1, "P1", P2, "P2", F21, "F21");
		eq.process("A = P2'*F21*P1");
		DMatrixRMaj A = eq.lookupDDRM("A");

		for (int row = 0; row < 4; row++) {
			for (int col = row + 1; col < 4; col++) {
				assertEquals(A.get(row, col), -A.get(col, row), 1e-8);
			}
		}
	}

	@Test void projectiveToFundamental_One() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 400, 405);
		Se3_F64 M11 = new Se3_F64();
		Se3_F64 M12 = SpecialEuclideanOps_F64.eulerXyz(-1, 0, -0.2, EulerType.XYZ, 0, -.2, 0, null);

		DMatrixRMaj P1 = PerspectiveOps.createCameraMatrix(M11.R, M11.T, K, null);
		DMatrixRMaj P2 = PerspectiveOps.createCameraMatrix(M12.R, M12.T, K, null);

		// Force P1 to be identity
		DMatrixRMaj H = new DMatrixRMaj(4, 4);
		ProjectiveToIdentity p2i = new ProjectiveToIdentity();
		assertTrue(p2i.process(P1));
		p2i.computeH(H);
		CommonOps_DDRM.mult(P1.copy(), H, P1);
		CommonOps_DDRM.mult(P2.copy(), H, P2);

		// Compare results against two camera variant
		DMatrixRMaj F21_expected = MultiViewOps.projectiveToFundamental(P1, P2, null);
		DMatrixRMaj F21 = MultiViewOps.projectiveToFundamental(P2, null);

		assertTrue(MatrixFeatures_DDRM.isIdentical(F21_expected, F21, UtilEjml.TEST_F64));
	}

	@Test void fundamentalToEssential_one() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);

		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx, Ty, Tz, rotX, rotY, rotZ, null);

			DMatrixRMaj E = MultiViewOps.createEssential(m.R, m.T, null);
			DMatrixRMaj F = MultiViewOps.createFundamental(E, K);

			DMatrixRMaj found = MultiViewOps.fundamentalToEssential(F, K, null);

			double scale = NormOps_DDRM.normF(E)/NormOps_DDRM.normF(found);
			CommonOps_DDRM.scale(scale, found);

			assertTrue(MatrixFeatures_DDRM.isIdentical(E, found, UtilEjml.TEST_F64));
		}
	}

	@Test void fundamentalToEssential_two() {
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(250, 310, 0.5, 120, 200);

		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx, Ty, Tz, rotX, rotY, rotZ, null);

			DMatrixRMaj E = MultiViewOps.createEssential(m.R, m.T, null);
			DMatrixRMaj F = MultiViewOps.createFundamental(E, K1, K2);

			DMatrixRMaj found = MultiViewOps.fundamentalToEssential(F, K1, K2, null);

			double scale = NormOps_DDRM.normF(E)/NormOps_DDRM.normF(found);
			CommonOps_DDRM.scale(scale, found);

			assertTrue(MatrixFeatures_DDRM.isIdentical(E, found, UtilEjml.TEST_F64));
		}
	}

	@Test void fundamentalToProjective_Three() {
		// carefully constructed transforms to ensure that the three views are not colinear
		// and have reasonable epipoles.
		// needing to be this careful highlights why the trifocal tensor is a better choice...
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 400, 405);
		Se3_F64 M12 = SpecialEuclideanOps_F64.eulerXyz(-1, 0, -0.2, EulerType.XYZ, 0, -.2, 0, null);
		Se3_F64 M13 = SpecialEuclideanOps_F64.eulerXyz(1, 0, -0.2, EulerType.XYZ, 0, .2, 0, null);
		Se3_F64 M23 = M12.invert(null).concat(M13, null);

		DMatrixRMaj F21 = MultiViewOps.createFundamental(M12.R, M12.T, K, K, null);
		DMatrixRMaj F31 = MultiViewOps.createFundamental(M13.R, M13.T, K, K, null);
		DMatrixRMaj F32 = MultiViewOps.createFundamental(M23.R, M23.T, K, K, null);

		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

		MultiViewOps.fundamentalToProjective(F21, F31, F32, P2, P3);

		// Test using the definition of consistency from "An Invitation to 3-D Vision"
		DMatrixRMaj foundF32 = new DMatrixRMaj(3, 3);
		MultiViewOps.projectiveToFundamental(P2, P3, foundF32);

		// resolve scale ambiguity
		CommonOps_DDRM.scale(1.0/NormOps_DDRM.normF(F32), F32);
		CommonOps_DDRM.scale(1.0/NormOps_DDRM.normF(foundF32), foundF32);

//		F32.print();
//		foundF32.print();
		assertTrue(MatrixFeatures_DDRM.isIdentical(F32, foundF32, UtilEjml.TEST_F64_SQ));
	}

	@Test void projectiveToIdentityH() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		Se3_F64 T = SpecialEuclideanOps_F64.eulerXyz(0.5, 0.7, -0.3, EulerType.XYZ, 1, 2, -0.5, null);
		DMatrixRMaj P = PerspectiveOps.createCameraMatrix(T.R, T.T, K, null);

		DMatrixRMaj H = new DMatrixRMaj(1, 1);
		MultiViewOps.projectiveToIdentityH(P, H);

		assertEquals(4, H.numRows);
		assertEquals(4, H.numCols);

		DMatrixRMaj found = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(P, H, found);

		assertTrue(MatrixFeatures_DDRM.isIdentity(found, UtilEjml.TEST_F64));
	}

	@Test void fundamentalCompatible3() {
		// carefully constructed transforms to ensure that the three views are not colinear
		// and have reasonable epipoles
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 400, 405);
		Se3_F64 M12 = SpecialEuclideanOps_F64.eulerXyz(-1, 0, -0.2, EulerType.XYZ, 0, -.2, 0, null);
		Se3_F64 M13 = SpecialEuclideanOps_F64.eulerXyz(1, 0, -0.2, EulerType.XYZ, 0, .2, 0, null);
		Se3_F64 M23 = M12.invert(null).concat(M13, null);

		DMatrixRMaj F21 = MultiViewOps.createFundamental(M12.R, M12.T, K, K, null);
		DMatrixRMaj F31 = MultiViewOps.createFundamental(M13.R, M13.T, K, K, null);
		DMatrixRMaj F32 = MultiViewOps.createFundamental(M23.R, M23.T, K, K, null);

		assertTrue(MultiViewOps.fundamentalCompatible3(F21, F31, F32, 1e-12));

		// F is only defined up to a scale
		CommonOps_DDRM.scale(10, F31);
		assertTrue(MultiViewOps.fundamentalCompatible3(F21, F31, F32, 1e-12));

		CommonOps_DDRM.scale(-5, F32);
		assertTrue(MultiViewOps.fundamentalCompatible3(F21, F31, F32, 1e-12));

		// Recompute F after applying a projective transform. They should no longer be compatible
		DMatrixRMaj A = CommonOps_DDRM.diag(0.5, -0.1, 200);
		DMatrixRMaj K2 = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(A, K, K2);
		F31 = MultiViewOps.createFundamental(M13.R, M13.T, K2, K2, null);
		assertFalse(MultiViewOps.fundamentalCompatible3(F21, F31, F32, 1e-12));
	}

	@Test void decomposeMetricCamera() {
		// compute an arbitrary projection matrix from known values
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);

		// try a bunch of different matrices to try to exercise all possible options
		for (int i = 0; i < 50; i++) {
			Se3_F64 worldToView = SpecialEuclideanOps_F64.eulerXyz(
					rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian(), null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(worldToView.R, worldToView.T, K, null);

			// The camera matrix is often only recovered up to a scale factor
			CommonOps_DDRM.scale(rand.nextGaussian(), P);

			// decompose the projection matrix
			DMatrixRMaj foundK = new DMatrixRMaj(3, 3);
			Se3_F64 foundWorldToView = new Se3_F64();
			MultiViewOps.decomposeMetricCamera(P, foundK, foundWorldToView);

			// When you recombine everything it should produce the same camera matrix
			var foundP = PerspectiveOps.createCameraMatrix(foundWorldToView.R, foundWorldToView.T, foundK, null);
			double scale = MultiViewOps.findScale(foundP, P);
			CommonOps_DDRM.scale(scale, foundP);
			assertTrue(MatrixFeatures_DDRM.isIdentical(foundP, P, UtilEjml.TEST_F64));

			// see if it extract the input
			assertEquals(1, CommonOps_DDRM.det(foundWorldToView.R), UtilEjml.TEST_F64);
			assertTrue(MatrixFeatures_DDRM.isIdentical(K, foundK, UtilEjml.TEST_F64));
			assertTrue(MatrixFeatures_DDRM.isIdentical(worldToView.R, foundWorldToView.R, UtilEjml.TEST_F64));

			// make sure it didn't change the scale of the decomposed T
			// this is very important when decomposing cameras which had a common projective frame
			assertEquals(0.0, worldToView.T.distance(foundWorldToView.T), UtilEjml.TEST_F64);
		}
	}

	@Test void decomposeEssential() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 1, 2, -0.5, null);
		Vector3D_F64 T = new Vector3D_F64(0.5, 0.7, -0.3);

		DMatrixRMaj E = MultiViewOps.createEssential(R, T, null);

		List<Se3_F64> found = MultiViewOps.decomposeEssential(E);

		// the scale factor is lost
		T.normalize();

		int numMatched = 0;

		for (Se3_F64 m : found) {
			DMatrixRMaj A = new DMatrixRMaj(3, 3);

			CommonOps_DDRM.multTransA(R, m.getR(), A);

			if (!MatrixFeatures_DDRM.isIdentity(A, 1e-8)) {
				continue;
			}

			Vector3D_F64 foundT = m.getT();
			foundT.normalize();

			if (foundT.isIdentical(T, 1e-8))
				numMatched++;
		}

		assertEquals(1, numMatched);
	}

	@Test void decomposeHomography() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.2, -0.06, -0.05, null);
		Vector3D_F64 T = new Vector3D_F64(2, 1, -3);

		double d = 2.5;
		Vector3D_F64 N = new Vector3D_F64(0.68, 0.2, -0.06);
		N.normalize();

		DMatrixRMaj H = MultiViewOps.createHomography(R, T, d, N);

		List<Tuple2<Se3_F64, Vector3D_F64>> found = MultiViewOps.decomposeHomography(H);

		assertEquals(4, found.size());

		List<Se3_F64> solutionsSE = new ArrayList<>();
		List<Vector3D_F64> solutionsN = new ArrayList<>();

		for (Tuple2<Se3_F64, Vector3D_F64> t : found) {
			solutionsSE.add(t.d0);
			solutionsN.add(t.d1);
		}

		TestDecomposeHomography.checkHasOriginal(solutionsSE, solutionsN, R, T, d, N);
	}

	@Test void transfer_1_to_3_PL() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		Point3D_F64 found = MultiViewOps.transfer_1_to_3(tensor, x1, line2, null);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x3.x, found.x, UtilEjml.TEST_F64);
		assertEquals(x3.y, found.y, UtilEjml.TEST_F64);
	}

	@Test void transfer_1_to_3_PP() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		Point3D_F64 found = MultiViewOps.transfer_1_to_3(tensor, x1, x2, null);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x3.x, found.x, UtilEjml.TEST_F64);
		assertEquals(x3.y, found.y, UtilEjml.TEST_F64);
	}

	@Test void transfer_1_to_2_PL() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		computeLines(X, line1, line2, line3);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);

		Point3D_F64 found = MultiViewOps.transfer_1_to_2(tensor, x1, line3, null);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x2.x, found.x, UtilEjml.TEST_F64);
		assertEquals(x2.y, found.y, UtilEjml.TEST_F64);
	}

	@Test void transfer_1_to_2_PP() {
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 2);

		// When the tensor was constructed the first view was assumed to be [I|0], which
		// is why normalized image coordinates are used for the first view
		Point2D_F64 x1 = PerspectiveOps.renderPixel(new Se3_F64(), X, null);
		Point2D_F64 x2 = PerspectiveOps.renderPixel(worldToCam2, K, X, null);
		Point2D_F64 x3 = PerspectiveOps.renderPixel(worldToCam3, K, X, null);

		Point3D_F64 found = MultiViewOps.transfer_1_to_2(tensor, x1, x3, null);

		found.x /= found.z;
		found.y /= found.z;

		assertEquals(x2.x, found.x, UtilEjml.TEST_F64);
		assertEquals(x2.y, found.y, UtilEjml.TEST_F64);
	}

	@Test void projectiveToMetric() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		DMatrixRMaj foundK = new DMatrixRMaj(3, 3);
		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx, Ty, Tz, rotX, rotY, rotZ, null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R, m.T, K, null);
			CommonOps_DDRM.scale(0.9, P, P); // mess up the scale of P

			Equation eq = new Equation(P, "P", K, "K");
			eq.process("p=[-0.9,0.1,0.7]'").
					process("H=[K zeros(3,1);-p'*K 1]").
					process("P=P*H").process("H_inv=inv(H)");

			DMatrixRMaj H_inv = eq.lookupDDRM("H_inv");

			Se3_F64 found = new Se3_F64();

			MultiViewOps.projectiveToMetric(P, H_inv, found, foundK);

			assertTrue(MatrixFeatures_DDRM.isEquals(K, foundK, UtilEjml.TEST_F64));
			assertEquals(0, m.T.distance(found.T), UtilEjml.TEST_F64);
		}
	}

	@Test void projectiveToMetricKnownK() {
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(200, 250, 0.0, 100, 110);
		for (int i = 0; i < 50; i++) {
			double Tx = rand.nextGaussian();
			double Ty = rand.nextGaussian();
			double Tz = rand.nextGaussian();
			double rotX = rand.nextGaussian();
			double rotY = rand.nextGaussian();
			double rotZ = rand.nextGaussian();

			Se3_F64 m = SpecialEuclideanOps_F64.eulerXyz(Tx, Ty, Tz, rotX, rotY, rotZ, null);

			DMatrixRMaj P = PerspectiveOps.createCameraMatrix(m.R, m.T, K, null);
			CommonOps_DDRM.scale(0.9, P, P); // mess up the scale of P

			Equation eq = new Equation(P, "P", K, "K");
			eq.process("p=[-0.9,0.1,0.7]'").
					process("H=[K zeros(3,1);-p'*K 1]").
					process("P=P*H").process("H_inv=inv(H)");

			DMatrixRMaj H_inv = eq.lookupDDRM("H_inv");

			Se3_F64 found = new Se3_F64();

			assertTrue(MultiViewOps.projectiveToMetricKnownK(P, H_inv, K, found));

			assertEquals(0, m.T.distance(found.T), UtilEjml.TEST_F64);
		}
	}

	/**
	 * Give it a valid Q but have the sign be inverted
	 */
	@Test void enforceAbsoluteQuadraticConstraints_negative() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K");
		eq.process("H=[K [0;0;0]; u 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");
		DMatrixRMaj Q = eq.lookupDDRM("Q");
		DMatrix4x4 Q_neg = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q, Q_neg);
		CommonOps_DDF4.scale(-0.045, Q_neg); // negative and non-standard scale

		MultiViewOps.enforceAbsoluteQuadraticConstraints(Q_neg, false, false);

		// change scale so that the test tolerance is reasonable
		CommonOps_DDRM.scale(1.0/Math.abs(Q.get(3, 3)), Q);
		CommonOps_DDF4.scale(1.0/Math.abs(Q_neg.a44), Q_neg);

		assertTrue(MatrixFeatures_D.isIdentical(Q, Q_neg, UtilEjml.TEST_F64));
	}

	/**
	 * Give it a valid Q but request that zeros be constrained
	 */
	@Test void enforceAbsoluteQuadraticConstraints_zeros() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("H=[K [0;0;0]; -p'*K 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");
		DMatrixRMaj Q = eq.lookupDDRM("Q");

		DMatrix4x4 Q_in = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q, Q_in);

		assertTrue(MultiViewOps.enforceAbsoluteQuadraticConstraints(Q_in, true, true));

		eq.process("K=[300 0 0;0 310 0; 0 0 1]");
		eq.process("H=[K [0;0;0]; -p'*K 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");

		// change scale so that the test tolerance is reasonable
		CommonOps_DDRM.scale(1.0/Math.abs(Q.get(3, 3)), Q);
		CommonOps_DDF4.scale(1.0/Math.abs(Q_in.a44), Q_in);

		assertTrue(MatrixFeatures_D.isIdentical(Q, Q_in, UtilEjml.TEST_F64));
	}

	@Test void decomposeAbsDualQuadratic() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("w=K*K'");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K");
		eq.process("H=[K [0;0;0]; u 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");
		DMatrixRMaj Q = eq.lookupDDRM("Q");
		DMatrix4x4 Q_in = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q, Q_in);
		CommonOps_DDF4.scale(0.0394, Q_in);


		DMatrix3x3 w = new DMatrix3x3();
		DMatrix3 p = new DMatrix3();

		assertTrue(MultiViewOps.decomposeAbsDualQuadratic(Q_in, w, p));

		assertTrue(MatrixFeatures_D.isIdentical(eq.lookupDDRM("w"), w, UtilEjml.TEST_F64));
		assertTrue(MatrixFeatures_D.isIdentical(eq.lookupDDRM("p"), p, UtilEjml.TEST_F64));
	}

	@Test void absoluteQuadraticToH() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K");
		eq.process("H=[K [0;0;0]; u 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");
		DMatrixRMaj Q = eq.lookupDDRM("Q");
		DMatrix4x4 Q_in = new DMatrix4x4();
		DConvertMatrixStruct.convert(Q, Q_in);

		DMatrixRMaj H = eq.lookupDDRM("H");
		DMatrixRMaj foundH = new DMatrixRMaj(4, 4);

		assertTrue(MultiViewOps.absoluteQuadraticToH(Q_in, foundH));

		assertTrue(MatrixFeatures_D.isIdentical(H, foundH, UtilEjml.TEST_F64));
	}

	@Test void rectifyHToAbsoluteQuadratic() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K");
		eq.process("H=[K [0;0;0]; u 1]");
		eq.process("Q=H*diag([1 1 1 0])*H'");

		DMatrixRMaj Q = eq.lookupDDRM("Q");

		DMatrixRMaj H = eq.lookupDDRM("H");
		DMatrixRMaj foundQ = new DMatrixRMaj(4, 4);

		MultiViewOps.rectifyHToAbsoluteQuadratic(H, foundQ);

		assertTrue(MatrixFeatures_D.isIdentical(Q, foundQ, UtilEjml.TEST_F64));
	}

	@Test void canonicalRectifyingHomographyFromKPinf() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K");
		eq.process("H=[K [0;0;0]; u 1]");

		DMatrixRMaj K = eq.lookupDDRM("K");
		DMatrixRMaj p = eq.lookupDDRM("p");
		Point3D_F64 _p = new Point3D_F64(p.get(0), p.get(1), p.get(2));

		DMatrixRMaj found = new DMatrixRMaj(1, 1); // wrong size intentionally

		MultiViewOps.canonicalRectifyingHomographyFromKPinf(K, _p, found);
		assertTrue(MatrixFeatures_DDRM.isEquals(eq.lookupDDRM("H"), found, UtilEjml.TEST_F64));
	}

	@Test void intrinsicFromAbsoluteQuadratic() {
		Equation eq = new Equation();
		eq.process("K1=[300 1 50;0 310 60; 0 0 1]");
		eq.process("K2=[310 1 75;0 310 60; 0 0 1]");
		eq.process("p=rand(3,1)");
		eq.process("u=-p'*K1");
		eq.process("H=[K1 [0;0;0]; u 1]");
		eq.process("P=K2*[eye(3) [1;2;3]]"); // very simplistic P
		eq.process("P=P*inv(H)"); // back to projective
		eq.process("Q=-1.1*H*diag([1 1 1 0])*H'");

		DMatrixRMaj P = eq.lookupDDRM("P");
		DMatrixRMaj Q = eq.lookupDDRM("Q");
		CameraPinhole intrinsic = new CameraPinhole();

		MultiViewOps.intrinsicFromAbsoluteQuadratic(Q, P, intrinsic);

		assertEquals(310, intrinsic.fx, 1e-6);
		assertEquals(310, intrinsic.fy, 1e-6);
		assertEquals(75, intrinsic.cx, 1e-6);
		assertEquals(60, intrinsic.cy, 1e-6);
		assertEquals(1, intrinsic.skew, 1e-6);
	}

	@Test void decomposeDiac() {
		Equation eq = new Equation();
		eq.process("K=[300 1 50;0 310 60; 0 0 1]");
		eq.process("w=1.5*K*K'");
		DMatrixRMaj w = eq.lookupDDRM("w");

		CameraPinhole intrinsic = new CameraPinhole();

		MultiViewOps.decomposeDiac(w, intrinsic);

		assertEquals(300, intrinsic.fx);
		assertEquals(310, intrinsic.fy);
		assertEquals(50, intrinsic.cx);
		assertEquals(60, intrinsic.cy);
		assertEquals(1, intrinsic.skew);
	}

	@Test void split2() {
		List<AssociatedPair> triples = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			triples.add(new AssociatedPair());
		}

		Tuple2<List<Point2D_F64>, List<Point2D_F64>> found = MultiViewOps.split2(triples);

		assertEquals(triples.size(), found.d0.size());
		assertEquals(triples.size(), found.d1.size());

		for (int i = 0; i < triples.size(); i++) {
			AssociatedPair t = triples.get(i);
			assertSame(t.p1, found.d0.get(i));
			assertSame(t.p2, found.d1.get(i));
		}
	}

	@Test void split3() {
		List<AssociatedTriple> triples = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			triples.add(new AssociatedTriple());
		}

		Tuple3<List<Point2D_F64>, List<Point2D_F64>, List<Point2D_F64>> found = MultiViewOps.split3(triples);

		assertEquals(triples.size(), found.d0.size());
		assertEquals(triples.size(), found.d1.size());
		assertEquals(triples.size(), found.d2.size());

		for (int i = 0; i < triples.size(); i++) {
			AssociatedTriple t = triples.get(i);
			assertSame(t.p1, found.d0.get(i));
			assertSame(t.p2, found.d1.get(i));
			assertSame(t.p3, found.d2.get(i));
		}
	}

	/**
	 * Construct a scene with perfect observations. See if triangulation returns the same points
	 */
	@Test void triangulatePoints() {
		CameraPinhole intrinsic = new CameraPinhole(500, 500, 0, 500, 500, 1000, 1000);

		List<Point3D_F64> points = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -0.5, 0.5, 100, rand);
		Se3_F64 view0_to_view1 = SpecialEuclideanOps_F64.eulerXyz(0.3, 0, 0, 0.1, -0.1, 0, null);

		SceneStructureMetric structure = new SceneStructureMetric(false);
		SceneObservations observations = new SceneObservations();

		observations.initialize(2);
		structure.initialize(1, 2, points.size());

		structure.setCamera(0, true, intrinsic);
		structure.setView(0, 0, true, new Se3_F64());
		structure.setView(1, 0, false, view0_to_view1);

		SceneObservations.View v0 = observations.getView(0);
		SceneObservations.View v1 = observations.getView(1);

		for (int i = 0; i < points.size(); i++) {
			Point3D_F64 X = points.get(i);

			Point2D_F64 p0 = PerspectiveOps.renderPixel(intrinsic, X, null);
			Point2D_F64 p1 = PerspectiveOps.renderPixel(view0_to_view1, intrinsic, X, null);

			v0.add(i, (float)p0.x, (float)p0.y);
			v1.add(i, (float)p1.x, (float)p1.y);
			structure.connectPointToView(i, 0);
			structure.connectPointToView(i, 1);
		}

		MultiViewOps.triangulatePoints(structure, observations);

		Point3D_F64 X = new Point3D_F64();
		for (int i = 0; i < points.size(); i++) {
			structure.getPoints().get(i).get(X);

			assertEquals(0, points.get(i).distance(X), UtilEjml.TEST_F64_SQ);
		}
	}

	@Test void findScale() {
		double expected = -1.8;
		DMatrixRMaj a = RandomMatrices_DDRM.rectangleGaussian(5, 5, 0, 2, rand);
		DMatrixRMaj b = a.copy();
		CommonOps_DDRM.scale(expected, b);

		double found = MultiViewOps.findScale(a, b);
		assertEquals(expected, found, UtilEjml.TEST_F64);
	}

	@Test void splits3Lists() {
		DogArray<AssociatedTriple> triples = new DogArray<>(AssociatedTriple::new);
		for (int i = 0; i < 8; i++) {
			triples.grow().setTo(
					rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian());
		}

		List<List<Point2D_F64>> found = MultiViewOps.splits3Lists(triples.toList(), null);
		checkSplit3Lists(triples, found);

		// see if it properly resets the list
		found.get(1).remove(3);
		MultiViewOps.splits3Lists(triples.toList(), found);
		checkSplit3Lists(triples, found);
	}

	private void checkSplit3Lists( DogArray<AssociatedTriple> triples, List<List<Point2D_F64>> found ) {
		assertEquals(3, found.size());
		for (int i = 0; i < 3; i++) {
			List<Point2D_F64> list = found.get(i);
			assertEquals(triples.size, list.size());
			for (int j = 0; j < list.size(); j++) {
				assertSame(triples.get(j).get(i), list.get(j));
			}
		}
	}

	@Test void convertTr_pair() {
		var triples = new DogArray<>(AssociatedTriple::new);
		for (int i = 0; i < 8; i++) {
			triples.grow().setTo(
					rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian(),
					rand.nextGaussian(), rand.nextGaussian());
		}

		var found = new DogArray<>(AssociatedPair::new);
		MultiViewOps.convertTr(triples.toList(), 0, 1, found);

		assertEquals(triples.size, found.size);
		triples.forIdx(( i, t ) -> assertEquals(0.0, t.p1.distance(found.get(i).p1), UtilEjml.TEST_F64));
		triples.forIdx(( i, t ) -> assertEquals(0.0, t.p2.distance(found.get(i).p2), UtilEjml.TEST_F64));

		MultiViewOps.convertTr(triples.toList(), 2, 1, found);

		assertEquals(triples.size, found.size);
		triples.forIdx(( i, t ) -> assertEquals(0.0, t.p3.distance(found.get(i).p1), UtilEjml.TEST_F64));
		triples.forIdx(( i, t ) -> assertEquals(0.0, t.p2.distance(found.get(i).p2), UtilEjml.TEST_F64));
	}

	@Test void scenePointsToPixels() {
		var helper = new BundleSceneHelper(false, 10);

		Point3D_F64 expectedX = new Point3D_F64();
		Point2D_F64 expectedPx = new Point2D_F64();
		MultiViewOps.scenePointsToPixels(helper.scene, 1, ( idx, coor, pixel ) -> {
			Point3D_F64 worldPt = helper.cloud3.get(idx);
			PerspectiveOps.renderPixel(helper.world_to_view1, helper.intrinsic, worldPt, expectedPx);
			assertEquals(0.0, expectedPx.distance(pixel), UtilEjml.TEST_F64);

			helper.world_to_view1.transform(worldPt, expectedX);
			assertEquals(0.0, expectedX.distance(coor), UtilEjml.TEST_F64);
			helper.counter++;
		});
		assertEquals(10, helper.counter);
	}

	@Test void sceneToCloud3() {
		for (boolean homogenous : new boolean[]{false, true}) {
			var helper = new BundleSceneHelper(homogenous, 10);
			MultiViewOps.sceneToCloud3(helper.scene, 1e-8, ( idx, coor ) -> {
				assertEquals(0.0, helper.cloud3.get(idx).distance(coor), UtilEjml.TEST_F64);
				helper.counter++;
			});
			assertEquals(10, helper.counter);
		}
	}

	@Test void sceneToCloudH() {
		for (boolean homogenous : new boolean[]{false, true}) {
			var helper = new BundleSceneHelper(homogenous, 10);
			MultiViewOps.sceneToCloudH(helper.scene, ( idx, coor ) -> {
				double distance = PerspectiveOps.distance3DvsH(helper.cloud3.get(idx), coor, 1e-8);
				assertEquals(0.0, distance, UtilEjml.TEST_F64);
				helper.counter++;
			});
			assertEquals(10, helper.counter);
		}
	}

	@Test void compatibleHomography() {
		var common = new CommonHomographyInducedPlane();

		// Compute the homography from the point pairs.
		// tried to do it directly but apparently I don't understand how to do that
		DMatrixRMaj H = new DMatrixRMaj(3, 3);
		new HomographyDirectLinearTransform(true).process(common.getPairs(), H);

		// This should be a perfect fit
		assertEquals(0.0, MultiViewOps.compatibleHomography(common.F, H), UtilEjml.TEST_F64);

		// Make it no longer compatible
		H.data[1] += 0.1;
		assertNotEquals(0.0, MultiViewOps.compatibleHomography(common.F, H), UtilEjml.TEST_F64);
	}

	@Test void homographyToFundamental() {
		// Create an arbitrary scene
		CameraPinhole intrinsic = new CameraPinhole(500, 500, 0, 250, 250, 500, 500);
		DMatrixRMaj K = PerspectiveOps.pinholeToMatrix(intrinsic, (DMatrixRMaj)null);
		Se3_F64 leftToRight = SpecialEuclideanOps_F64.eulerXyz(0.5, 0.05, 0, -0.02, 0.05, 0.01, null);
		Se3_F64 homographyToLeft = new Se3_F64();
		homographyToLeft.T.setTo(0, 0, 2);

		// create a bunch of points which are off the plane
		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1.5),
				-1, 1, -1, 1, -0.2, 0.2, 50, rand);

		// Create a homography for
		DMatrixRMaj H = MultiViewOps.createHomography(homographyToLeft.R, homographyToLeft.T, 1.0, new Vector3D_F64(0, 0, 1), K);

		// Create observation
		List<AssociatedPair> observations = new ArrayList<>();
		for (Point3D_F64 X : cloud) {
			var p = new AssociatedPair();
			PerspectiveOps.renderPixel(intrinsic, X, p.p1);
			PerspectiveOps.renderPixel(leftToRight, intrinsic, X, p.p2);
			observations.add(p);
		}

		var foundF = new DMatrixRMaj(3, 3);
		assertTrue(MultiViewOps.homographyToFundamental(H, observations, foundF));

		// make sure it's not all zeros
		assertTrue(NormOps_DDRM.normF(foundF) > 0.5);
		// they should be compatible
		assertEquals(0.0, MultiViewOps.compatibleHomography(foundF, H), UtilEjml.TEST_F64);
	}

	/**
	 * Tests the constructed homography matrix using its properties
	 */
	@Test void homographyFromRotation() {
		// pure rotation
		Se3_F64 left_to_right = SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0.2, 0, 0, null);

		// Two distinct cameras
		CameraPinhole intrinsic1 = new CameraPinhole(500, 500, 0, 250, 250, 500, 500);
		CameraPinhole intrinsic2 = new CameraPinhole(1000, 1100, 0, 300, 310, 500, 500);
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(intrinsic1, (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(intrinsic2, (DMatrixRMaj)null);

		// Point in front of both views
		Point3D_F64 X = new Point3D_F64(0.1, -0.05, 1.2);

		// Compute the expected value and input
		Point2D_F64 pixel1 = PerspectiveOps.renderPixel(intrinsic1, X, null);
		Point2D_F64 pixel2 = PerspectiveOps.renderPixel(left_to_right, intrinsic2, X, null);

		// Call the function being tested
		DMatrixRMaj H21 = MultiViewOps.homographyFromRotation(left_to_right.R, K1, K2, null);

		// See it transferred the point
		Point2D_F64 found = new Point2D_F64();
		GeometryMath_F64.mult(H21,pixel1, found);

		assertEquals(0.0, found.distance(pixel2), 1e-6);
	}

	private class BundleSceneHelper {
		Se3_F64 world_to_view0;
		Se3_F64 world_to_view1;
		SceneStructureMetric scene;
		CameraPinhole intrinsic;
		List<Point3D_F64> cloud3;
		int counter;

		public BundleSceneHelper( boolean homogenous, int numPoints ) {
			world_to_view0 = SpecialEuclideanOps_F64.eulerXyz(0, 0.1, -0.2, 0.02, -0.04, 0.03, null);
			world_to_view1 = SpecialEuclideanOps_F64.eulerXyz(0.3, 0.1, 0.2, 0.01, -0.06, 0.02, null);

			intrinsic = new CameraPinhole(300, 350, 0, 300, 300, 600, 600);
			cloud3 = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2),
					-1, 1, -1, 1, -0.1, 0.1, numPoints, rand);

			scene = new SceneStructureMetric(homogenous);
			scene.initialize(1, 3, numPoints);
			scene.setCamera(0, true, intrinsic);
			scene.setView(0, 0, true, world_to_view0);
			scene.setView(1, 0, true, world_to_view1);
			for (int i = 0; i < cloud3.size(); i++) {
				Point3D_F64 p = cloud3.get(i);
				if (homogenous) {
					double w = rand.nextDouble() + 0.1;
					scene.setPoint(i, p.x*w, p.y*w, p.z*w, w);
				} else {
					scene.setPoint(i, p.x, p.y, p.z);
				}
			}
		}
	}
}
