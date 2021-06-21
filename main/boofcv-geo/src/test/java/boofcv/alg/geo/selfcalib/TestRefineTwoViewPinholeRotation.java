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

package boofcv.alg.geo.selfcalib;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.geo.AssociatedPair;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestRefineTwoViewPinholeRotation extends BoofStandardJUnit {
	Se3_F64 view1_to_view2 = SpecialEuclideanOps_F64.eulerXyz(0, 0, 0, 0.1, -0.05, 0.15, null);

	/**
	 * Give it perfect input and see if it screws things up
	 */
	@Test void perfectInput() {
		CameraPinhole intrinsic1 = new CameraPinhole(400, 410, 0.1, 500, 550, 1000, 1000);
		CameraPinhole intrinsic2 = new CameraPinhole(600, 550, 0.02, 400, 440, 800, 800);

		List<AssociatedPair> pairs = renderPairs(intrinsic1, intrinsic2, 50, 0.0);

		var alg = new RefineTwoViewPinholeRotation();
		// turn off all assumptions
		alg.assumeUnityAspect = false;
		alg.zeroSkew = false;
		alg.assumeSameIntrinsics = false;

		CameraPinhole found1 = new CameraPinhole(intrinsic1);
		CameraPinhole found2 = new CameraPinhole(intrinsic2);
		DMatrixRMaj R = view1_to_view2.R.copy();

		assertTrue(alg.refine(pairs, R, found1, found2));

		// Score shouldn't get worse, but since the input is perfect it might not get better
		assertTrue(alg.errorAfter <= alg.errorBefore);

		// should be very very similar
		assertTrue(found1.isEquals(intrinsic1, 1e-6));
		assertTrue(found2.isEquals(intrinsic2, 1e-6));
		assertTrue(MatrixFeatures_DDRM.isIdentical(view1_to_view2.R, R, 1e-6));
	}

	/**
	 * Intrinsic parameters are not correct, but the model will match. No noise added to observations.
	 */
	@Test void imperfectInput_perfectObservations() {
		CameraPinhole intrinsic1 = new CameraPinhole(400, 400, 0.0, 500, 550, 1000, 1000);
		CameraPinhole intrinsic2 = new CameraPinhole(600, 600, 0.0, 400, 440, 800, 800);

		List<AssociatedPair> pairs = renderPairs(intrinsic1, intrinsic2, 100, 0.0);

		evaluateImperfect(intrinsic1, intrinsic2, pairs);

	}

	/**
	 * Intrinsic parameters are not correct and there's noise added to observations
	 */
	@Test void imperfectInput_noisyObservations() {
		CameraPinhole intrinsic1 = new CameraPinhole(400, 400, 0.0, 500, 550, 1000, 1000);
		CameraPinhole intrinsic2 = new CameraPinhole(600, 600, 0.0, 400, 440, 800, 800);

		List<AssociatedPair> pairs = renderPairs(intrinsic1, intrinsic2, 100, 0.5);

		evaluateImperfect(intrinsic1, intrinsic2, pairs);
	}

	private void evaluateImperfect( CameraPinhole intrinsic1, CameraPinhole intrinsic2, List<AssociatedPair> pairs ) {
		var alg = new RefineTwoViewPinholeRotation();
		// Apply some constraints. Yes this test isn't exhaustive at all.
		alg.assumeUnityAspect = true;
		alg.zeroSkew = true;
		alg.assumeSameIntrinsics = false;
		alg.knownFocalLength = false;

		// Focal length and rotation are going to be way off as a test
		CameraPinhole found1 = new CameraPinhole(200, 200, 0.0, 530, 530, 1000, 1000);
		CameraPinhole found2 = new CameraPinhole(1000, 1000, 0.0, 430, 430, 800, 800);
		DMatrixRMaj R = CommonOps_DDRM.identity(3);

		alg.converge.maxIterations = 40;
//		alg.setVerbose(System.out, null);
		assertTrue(alg.refine(pairs, R, found1, found2));


		// Should be significantly better
		assertTrue(alg.errorAfter*1e5 <= alg.errorBefore);

		// Check constraints
		assertEquals(found1.fx, found1.fy);
		assertEquals(found2.fx, found2.fy);
		assertEquals(0.0, found1.skew);
		assertEquals(0.0, found2.skew);

		// Based on manual inspection there seems to be redundancy in these equations some place that I've
		// not yet dug out. The error is zero but K1 and K2 are significantly different. R is "close"

		// very crude test for focal length
		assertEquals(intrinsic1.fx, found1.fx, 150);
		assertEquals(intrinsic2.fx, found2.fy, 150);

		// Compute the number of radians different the two rotations are
		DMatrixRMaj diffR = CommonOps_DDRM.multTransA(view1_to_view2.R, R, null);
		double angle = ConvertRotation3D_F64.matrixToRodrigues(diffR, null).theta;
		assertEquals(0.0, angle, 0.05);
	}

	public List<AssociatedPair> renderPairs( CameraPinhole intrinsic1, CameraPinhole intrinsic2, int numPoints, double noise ) {
		List<AssociatedPair> pairs = new ArrayList<>();

		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 2), -0.5, 1.0, numPoints, rand);

		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F64 X = cloud.get(i);

			AssociatedPair pair = new AssociatedPair();
			PerspectiveOps.renderPixel(intrinsic1, X, pair.p1);
			if (X.z < 0)
				throw new RuntimeException("Point behind camera");

			SePointOps_F64.transform(view1_to_view2, X, X);
			if (X.z < 0)
				throw new RuntimeException("Point behind camera");
			PerspectiveOps.renderPixel(intrinsic2, X, pair.p2);
			pairs.add(pair);

			if (noise == 0.0)
				continue;

			// Add observation noise
			pair.p1.x += noise*rand.nextGaussian();
			pair.p1.y += noise*rand.nextGaussian();
			pair.p2.x += noise*rand.nextGaussian();
			pair.p2.y += noise*rand.nextGaussian();
		}

		return pairs;
	}
}
