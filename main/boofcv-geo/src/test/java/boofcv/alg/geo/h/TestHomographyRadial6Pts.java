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

import boofcv.alg.geo.MultiViewOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestHomographyRadial6Pts extends BoofStandardJUnit {

	double width = 100;
	double height = 80;

	DMatrixRMaj H_truth = new DMatrixRMaj(3, 3);
	DMatrixRMaj H_distorted = new DMatrixRMaj(3, 3);
	double radial1 = 0.01, radial2 = 0.02;
	DogArray<AssociatedPair> distorted = new DogArray<>(AssociatedPair::new);
	DogArray<AssociatedPair> undistorted = new DogArray<>(AssociatedPair::new);

	/** test everything in a scenario with no lens distortion */
	@Test void distortion_none() {
		radial1 = 0.00;
		radial2 = 0.00;
		randomScenario(10);

		var foundA = new HomographyRadial6Pts.Results();
		var foundB = new HomographyRadial6Pts.Results();
		var alg = new HomographyRadial6Pts();
		assertTrue(alg.process(distorted.toList(), foundA, foundB));

		System.out.println("error " + computeError(foundA.H, foundA.l1, foundA.l2));
		System.out.println("error " + computeError(foundB.H, foundB.l1, foundB.l2));


		NormOps_DDRM.normalizeF(H_truth);
		NormOps_DDRM.normalizeF(foundA.H);
		NormOps_DDRM.normalizeF(foundB.H);
		H_truth.print();
		foundA.H.print();
		foundB.H.print();
	}

	private double computeError( DMatrixRMaj H, double l1, double l2 ) {
		double error = 0.0;

		var undistorted = new AssociatedPair();
		var found = new Point2D_F64();
		for (AssociatedPair a : distorted.toList()) {
			undistorted.setTo(a);

			// Remove lens distortion
			undistorted.p1.scale(1.0/(1.0 + l1*a.p1.normSq()));
			undistorted.p2.scale(1.0/(1.0 + l2*a.p2.normSq()));

			GeometryMath_F64.mult(H, undistorted.p1, found);
			error += found.distance(undistorted.p2);
		}
		return error / distorted.size;
	}

	/** Each view will have unique image distortion that isn't zero or too large */
	@Test void distortion_mild() {
		fail("Implement");
	}

	@Test void constructCrossConstraints() {
		radial1 = 0.0;
		radial2 = 0.01;
		randomScenario(10);

		var alg = new HomographyRadial6Pts();
		alg.constructCrossConstraints(distorted.toList());

		// See if truth is in the null space of the constraint matrix
		var V = new DMatrixRMaj(8, 1);
		System.arraycopy(H_distorted.data, 0, V.data, 0, 6);
		V.data[6] = radial1*H_distorted.get(0, 2);
		V.data[7] = radial1*H_distorted.get(1, 2);

		// Check to see if 'V' is in the null space. If so, found will be all zeros
		var found = new DMatrixRMaj(1, 1);
		CommonOps_DDRM.mult(alg.A, V, found);

		alg.A.print();
		V.print();
		found.print();
		for (int i = 0; i < found.getNumElements(); i++) {
			assertEquals(0.0, found.get(i), UtilEjml.TEST_F64);
		}
	}

	@Test void solveQuadraticRelationship() {
		fail("Implement");
	}

	@Test void solveForRemaining() {
		fail("Implement");
	}

	public void randomScenario( int numPoints ) {
		DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0.1, 0.02, -0.05, null);
		Vector3D_F64 T = new Vector3D_F64(0.1, 0.1, 0.001);

		// intrinsics with (cx,cy) = 0 and skew = 0
		DMatrixRMaj K = new DMatrixRMaj(3, 3, true, 200, 0, 0, 0, 200, 0, 0, 0, 1);
		H_distorted = MultiViewOps.createHomography(R, T, 1.0, new Vector3D_F64(0.1, 0.001, -0.8), K);

		distorted.resetResize(numPoints);
		undistorted.resetResize(numPoints);
		for (int i = 0; i < numPoints; i++) {
			// Randomly generate distorted observations
			AssociatedPair a = distorted.get(i);

			// Randomly select a point. This will be undistorted coordinate
			a.p1.x = (rand.nextDouble() - 0.5)*width;
			a.p1.y = (rand.nextDouble() - 0.5)*height;

			// Generate observation in other view using truth
			GeometryMath_F64.mult(H_distorted, a.p1, a.p2);

			// Create set of undistorted observations
			AssociatedPair b = undistorted.get(i).setTo(a);
			b.p1.scale(1.0/(1.0 + radial1*a.p1.normSq()));
			b.p2.scale(1.0/(1.0 + radial2*a.p2.normSq()));
		}

		assertTrue(FactoryMultiView.homographyTLS().process(undistorted.toList(), H_truth));
	}
}