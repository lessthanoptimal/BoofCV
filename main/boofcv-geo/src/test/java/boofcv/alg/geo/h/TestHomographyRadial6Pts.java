/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.division.LensDistortionDivision;
import boofcv.alg.geo.MultiViewOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.CameraDivision;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.equation.Equation;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestHomographyRadial6Pts extends BoofStandardJUnit {
	double width = 100;
	double height = 80;

	DMatrixRMaj H_truth = new DMatrixRMaj(3, 3);
	DMatrixRMaj H_distorted = new DMatrixRMaj(3, 3);
	double radial1 = 1e-5, radial2 = -2e-6;
	DogArray<AssociatedPair> distorted = new DogArray<>(AssociatedPair::new);
	DogArray<AssociatedPair> undistorted = new DogArray<>(AssociatedPair::new);

	/** Test it in scenarios with no noise added */
	@Test void perfect() {
		randomTrials(0, 6);
	}

	/** Add a little bit of noise and see if it blows up */
	@Test void smallNoise() {
		randomTrials(0.1, 6);
		randomTrials(0.1, 8);

		// it was impossible to get every trial to pass, even with generous thresholds. This might be due to
		// instability caused by dealing with pixels and very small radial distortion values. Normalized
		// coordinates would work better, probably. The randomly generated scene might also be poor
	}

	/** See if it works with more than 6 points */
	@Test void extraPoints() {
		randomTrials(0, 7);
		randomTrials(0, 8);
		randomTrials(0, 12);
	}

	void randomTrials( double noiseSigma, int numPoints ) {
		var alg = new HomographyRadial6Pts();

		int passed = 0;
		for (int trial = 0; trial < 20; trial++) {
			// radial distortion values seem to need to be very small...
			radial1 = (rand.nextDouble() - 0.5)*1e-5;
			radial2 = (rand.nextDouble() - 0.5)*1e-5;
//			System.out.println("radial: " + radial1 + " " + radial2);

			randomScenario(numPoints, noiseSigma);

			assertTrue(alg.process(distorted.toList()), "trial=" + trial);
			var results = alg.getFound();

			if (numPoints > 6)
				assertEquals(1, results.size);
			else if (noiseSigma == 0.0)
				assertEquals(2, results.size);

			HomographyRadial6Pts.Result best = null;
			double bestError = Double.MAX_VALUE;
			for (var r : results.toList()) {
				double error = HomographyRadial6Pts.computeResidualError(distorted.toList(), r);
//				System.out.println("   " + error);
				if (error >= bestError)
					continue;
				bestError = error;
				best = r;
			}
			Objects.requireNonNull(best);

			// See if the error is excessive or not
			if (bestError <= 5*noiseSigma + UtilEjml.TEST_F64) {
				passed++;
			}

			if (noiseSigma == 0.0)
				checkConstraints(best.H, best.radial1, best.radial2);
		}

		// See if most of them passed. Difficult in situations with noise
		if (noiseSigma == 0.0)
			assertEquals(20, passed);
		else
			assertTrue(passed >= 15);
	}

	@Test void linearCrossConstraint() {
		randomScenario(6, 0.0);

		var alg = new HomographyRadial6Pts();
		assertTrue(alg.linearCrossConstraint(distorted.toList()));

		// This should be a good solution
		assertTrue(alg.getCrossSingularRatio() > 1);

		// Recompute A just in case it was modified by the solver
		alg.constructCrossConstraints(distorted.toList());

		// Test it by verifying the two vectors are in the null space of A
		var found = new DMatrixRMaj(1, 1);
		CommonOps_DDRM.mult(alg.A, alg.null1, found);
		assertTrue(MatrixFeatures_DDRM.isZeros(found, UtilEjml.TEST_F64));
		CommonOps_DDRM.mult(alg.A, alg.null2, found);
		assertTrue(MatrixFeatures_DDRM.isZeros(found, UtilEjml.TEST_F64));
	}

	@Test void constructCrossConstraints() {
		randomScenario(10, 0.0);

		var alg = new HomographyRadial6Pts();
		alg.constructCrossConstraints(distorted.toList());

		// See if truth is in the null space of the constraint matrix
		var V = new DMatrixRMaj(8, 1);
		System.arraycopy(H_truth.data, 0, V.data, 0, 6);
		V.data[6] = radial1*H_truth.get(0, 2);
		V.data[7] = radial1*H_truth.get(1, 2);

		// Check to see if 'V' is in the null space. If so, found will be all zeros
		var found = new DMatrixRMaj(1, 1);
		CommonOps_DDRM.mult(alg.A, V, found);

		assertTrue(MatrixFeatures_DDRM.isZeros(found, UtilEjml.TEST_F64));
	}

	@Test void solveQuadraticRelationship() {
		randomScenario(6, 0.0);

		var alg = new HomographyRadial6Pts();
		assertTrue(alg.linearCrossConstraint(distorted.toList()));
		assertTrue(alg.solveQuadraticRelationship());

		// test it by seeing if it produces a value of zero when fed back into equation (5) from paper
		DMatrixRMaj null1 = alg.null1;
		DMatrixRMaj null2 = alg.null2;
		for (var hypo : alg.hypotheses.toList()) {
			for (int i = 0; i < distorted.size; i++) {
				AssociatedPair a = distorted.get(i);

				double h11 = hypo.gamma*null1.data[0] + null2.data[0];
				double h12 = hypo.gamma*null1.data[1] + null2.data[1];
				double h13 = hypo.gamma*null1.data[2] + null2.data[2];
				double h21 = hypo.gamma*null1.data[3] + null2.data[3];
				double h22 = hypo.gamma*null1.data[4] + null2.data[4];
				double h23 = hypo.gamma*null1.data[5] + null2.data[5];

				double sum = 0.0;
				sum += (-a.p2.y*h11 + a.p2.x*h21)*a.p1.x;
				sum += (-a.p2.y*h12 + a.p2.x*h22)*a.p1.y;
				sum += (-a.p2.y*h13 + a.p2.x*h23)*(1 + hypo.lambda*a.p1.normSq());

				assertEquals(0.0, sum, UtilEjml.TEST_F64);
			}
		}
	}

	public void randomScenario( int numPoints, double noise ) {
		double focalLength = 500;
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 0.02, -0.05, null);
		Vector3D_F64 T = new Vector3D_F64(0.1, 0.1, 0.001);

		// intrinsics with (cx,cy) = 0 and skew = 0
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, focalLength, 0, 0, 0, focalLength, 0, 0, 0, 1);
		H_truth = MultiViewOps.createHomography(R, T, 1.0, new Vector3D_F64(0.1, 0.001, -0.8), K);

		// Applies lens distortion to pixels
		Point2Transform2_F64 distorter1 = new LensDistortionDivision(new CameraDivision().fsetRadial(radial1)).distort_F64(false, false);
		Point2Transform2_F64 distorter2 = new LensDistortionDivision(new CameraDivision().fsetRadial(radial2)).distort_F64(false, false);

		distorted.resetResize(numPoints);
		undistorted.resetResize(numPoints);
		for (int i = 0; i < numPoints; i++) {
			// Randomly generate undistorted observations
			AssociatedPair a = undistorted.get(i);
			a.p1.x = (rand.nextDouble() - 0.5)*width;
			a.p1.y = (rand.nextDouble() - 0.5)*height;

			// Generate observation in other view using truth
			GeometryMath_F64.mult(H_truth, a.p1, a.p2);

			// Add lens distortion
			AssociatedPair b = distorted.get(i);
			distorter1.compute(a.p1.x, a.p1.y, b.p1);
			distorter2.compute(a.p2.x, a.p2.y, b.p2);
		}

		if (noise > 0) {
			distorted.forEach(p -> {
				p.p1.x += rand.nextGaussian()*noise;
				p.p1.y += rand.nextGaussian()*noise;
				p.p2.x += rand.nextGaussian()*noise;
				p.p2.y += rand.nextGaussian()*noise;
			});
		} else {
			// Sanity check
			checkConstraints(H_truth, radial1, radial2);
		}

		assertTrue(FactoryMultiView.homographyTLS().process(distorted.toList(), H_distorted));
	}

	/** Directly apply constraints to the solution and see if you get a zero vector */
	public void checkConstraints( DMatrixRMaj H, double l1, double l2 ) {
		for (int i = 0; i < distorted.size; i++) {
			AssociatedPair a = distorted.get(i);
			double w1 = 1.0 + l1*a.p1.normSq();
			double w2 = 1.0 + l2*a.p2.normSq();

			var eq = new Equation();
			eq.alias(H, "H");
			eq.alias(a.p1.x, "x1", a.p1.y, "y1", w1, "w1");
			eq.alias(a.p2.x, "x2", a.p2.y, "y2", w2, "w2");
			eq.process("C=[0, -w2, y2; w2, 0, -x2;-y2, x2, 0]");
			eq.process("N=[x1 y1 w1]'");
			eq.process("s=C*H*N");
			assertTrue(MatrixFeatures_DDRM.isZeros(eq.lookupDDRM("s"), 1e-6), "failed on point " + i);
		}
	}
}