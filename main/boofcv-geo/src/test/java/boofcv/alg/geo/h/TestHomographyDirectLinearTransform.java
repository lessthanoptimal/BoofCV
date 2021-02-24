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

package boofcv.alg.geo.h;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedPairConic;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.UtilCurves_F64;
import georegression.geometry.UtilEllipse_F64;
import georegression.struct.EulerType;
import georegression.struct.curve.ConicGeneral_F64;
import georegression.struct.curve.EllipseQuadratic_F64;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.ejml.ops.DConvertMatrixStruct;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestHomographyDirectLinearTransform extends CommonHomographyChecks {
	@Test void perfect2D_calibrated() {
		// test the minimum number of points
		checkHomography(4, false, new HomographyDirectLinearTransform(false));
		// test with extra points
		checkHomography(10, false, new HomographyDirectLinearTransform(false));
	}

	@Test void perfect2D_pixels() {
		checkHomography(4, true, new HomographyDirectLinearTransform(true));
		checkHomography(10, true, new HomographyDirectLinearTransform(true));
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param isPixels Pixel or calibrated coordinates
	 * @param alg Algorithm being evaluated
	 */
	private void checkHomography( int N, boolean isPixels, HomographyDirectLinearTransform alg ) {
		createScene(N, isPixels);

		// compute essential
		assertTrue(alg.process(pairs2D, solution));

		checkSolution();
	}

	@Test void perfect_3D() {
		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(false);
		check3D(4, alg);
		check3D(10, alg);
		alg = new HomographyDirectLinearTransform(true);
		check3D(4, alg);
		check3D(10, alg);
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param alg Algorithm being evaluated
	 */
	private void check3D( int N, HomographyDirectLinearTransform alg ) {
		createScene(N, true);
		assertTrue(alg.process(null, pairs3D, null, solution));
		checkSolution();
	}

	private void checkSolution() {
		// validate by homography transfer
		// sanity check, H is not zero
		assertTrue(NormOps_DDRM.normF(solution) > 0.001);

		// see if it follows the epipolar constraint
		for (AssociatedPair p : pairs2D) {
			Point2D_F64 a = GeometryMath_F64.mult(solution, p.p1, new Point2D_F64());
			double diff = a.distance(p.p2);
			assertEquals(0, diff, 1e-8);
		}
	}

	@Test void perfect_Conic() {
		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(false);
		checkConics(3, alg);
		checkConics(10, alg);
		alg = new HomographyDirectLinearTransform(true);
		checkConics(3, alg);
		checkConics(10, alg);
	}

	private void checkConics( int N, HomographyDirectLinearTransform alg ) {

		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.2, 0.05, null);
		DMatrixRMaj H = MultiViewOps.createHomography(R, new Vector3D_F64(), 1, new Vector3D_F64(0, 0, -1), K);

		List<AssociatedPairConic> pairs = createConicPairs(N, H);

		DMatrixRMaj found = new DMatrixRMaj(3, 3);
		alg.process(null, null, pairs, found);

		// remove scale ambuguity
		CommonOps_DDRM.scale(1.0/NormOps_DDRM.normF(H), H);
		CommonOps_DDRM.scale(1.0/NormOps_DDRM.normF(found), found);

		if (Math.signum(H.get(0, 0)) != Math.signum(found.get(0, 0))) {
			CommonOps_DDRM.scale(-1, found);
		}

		assertTrue(MatrixFeatures_DDRM.isIdentical(H, found, UtilEjml.TEST_F64));
	}

	private List<AssociatedPairConic> createConicPairs( int N, DMatrixRMaj H ) {
		DMatrix3x3 Hinv = new DMatrix3x3();
		CommonOps_DDF3.invert(DConvertMatrixStruct.convert(H, (DMatrix3x3)null), Hinv);
		DMatrix3x3 C = new DMatrix3x3();

		List<AssociatedPairConic> pairs = new ArrayList<>();
		EllipseQuadratic_F64 ellipseQ = new EllipseQuadratic_F64();
		for (int i = 0; i < N; i++) {
			EllipseRotated_F64 ellipseR = new EllipseRotated_F64(rand.nextGaussian()*5, rand.nextGaussian()*5,
					0.5 + rand.nextDouble()*2, 0.5, rand.nextGaussian()*1.5);
			UtilEllipse_F64.convert(ellipseR, ellipseQ);
			AssociatedPairConic pair = new AssociatedPairConic();
			pair.p1.setTo(ellipseQ);

			UtilCurves_F64.convert(pair.p1, C);
			PerspectiveOps.multTranA(Hinv, C, Hinv, C);
			UtilCurves_F64.convert(C, pair.p2);

			pairs.add(pair);
		}
		return pairs;
	}

	@Test void perfect_AllToGether() {
		perfect_AllToGether(10, new HomographyDirectLinearTransform(false));
		perfect_AllToGether(10, new HomographyDirectLinearTransform(true));
	}

	void perfect_AllToGether( int N, HomographyDirectLinearTransform alg ) {
		// pure rotation so that the H computed below acts as expected
		motion = SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0.05, -0.1, 0.08, null);
		createScene(N, alg.isNormalize());

		DMatrixRMaj H = computeH(alg.isNormalize());
		List<AssociatedPairConic> conics = createConicPairs(N, H);

		assertTrue(alg.process(pairs2D, pairs3D, conics, solution));
		checkSolution();
	}

	/**
	 * Test the constraint added from a pair of conics and seeing of the generating homography results in a null
	 * vector
	 */
	@Test void addConicPairConstraints() {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, -0.2, 0.05, null);
		DMatrixRMaj H = MultiViewOps.createHomography(R, new Vector3D_F64(), 1, new Vector3D_F64(0, 0, -1), K);
		DMatrix3x3 H3 = new DMatrix3x3();
		DMatrix3x3 Hinv = new DMatrix3x3();
		DMatrix3x3 C = new DMatrix3x3();

		DConvertMatrixStruct.convert(H, H3);
		CommonOps_DDF3.invert(H3, Hinv);

		AssociatedPairConic pair1 = new AssociatedPairConic();
		AssociatedPairConic pair2 = new AssociatedPairConic();


		// Two arbitrary ellipse conics. Using ellipses since I know they are not degenerate
		EllipseRotated_F64 ellipseR = new EllipseRotated_F64(2, 3, 1.5, 0.5, 0.2);
		EllipseQuadratic_F64 ellipseQ = new EllipseQuadratic_F64();
		UtilEllipse_F64.convert(ellipseR, ellipseQ);
		pair1.p1 = new ConicGeneral_F64(ellipseQ);
		ellipseR = new EllipseRotated_F64(20, -2, 2.0, 0.75, -0.6);
		UtilEllipse_F64.convert(ellipseR, ellipseQ);
		pair2.p1 = new ConicGeneral_F64(ellipseQ);

		// Find the conic in view 2 by applying C' = inv(H)^T * C * inv(H)
		UtilCurves_F64.convert(pair1.p1, C);
		PerspectiveOps.multTranA(Hinv, C, Hinv, C);
		UtilCurves_F64.convert(C, pair1.p2);
		UtilCurves_F64.convert(pair2.p1, C);
		PerspectiveOps.multTranA(Hinv, C, Hinv, C);
		UtilCurves_F64.convert(C, pair2.p2);

		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(false);

		// construct constraint matrix
		DMatrixRMaj A = new DMatrixRMaj(9, 9);
		assertEquals(9, alg.addConicPairConstraints(pair1, pair2, A, 0));

		// Test that A*H = 0
		H.numRows = 9;
		H.numCols = 1; // convert into a vector
		DMatrixRMaj b = new DMatrixRMaj(9, 1);
		CommonOps_DDRM.mult(A, H, b);

		assertEquals(0, CommonOps_DDRM.elementSum(b), UtilEjml.TEST_F64);
	}
}
