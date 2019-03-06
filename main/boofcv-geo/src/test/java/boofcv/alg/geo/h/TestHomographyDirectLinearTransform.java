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

package boofcv.alg.geo.h;

import boofcv.struct.geo.AssociatedPair;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Peter Abeles
 */
class TestHomographyDirectLinearTransform extends CommonHomographyChecks
{
	@Test
	void perfect2D_calibrated() {
		// test the minimum number of points
		checkHomography(4, false, new HomographyDirectLinearTransform(false));
		// test with extra points
		checkHomography(10, false, new HomographyDirectLinearTransform(false));
	}

	@Test
	void perfect2D_pixels() {
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
	private void checkHomography(int N, boolean isPixels, HomographyDirectLinearTransform alg) {
		createScene(N,isPixels);

		// compute essential
		assertTrue(alg.process(pairs2D,solution));

		checkSolution();
	}

	@Test
	void perfect_3D() {
		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(false);
		check3D(4,alg);
		check3D(10,alg);
		alg = new HomographyDirectLinearTransform(true);
		check3D(4,alg);
		check3D(10,alg);
	}

	/**
	 * Create a set of points perfectly on a plane and provide perfect observations of them
	 *
	 * @param N Number of observed points.
	 * @param alg Algorithm being evaluated
	 */
	private void check3D(int N, HomographyDirectLinearTransform alg) {
		createScene(N,true);
		assertTrue(alg.process(null,pairs3D,null,solution));
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

	@Test
	void perfect_Conic() {
		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(false);
		checkConics(3,alg);
		checkConics(10,alg);
		alg = new HomographyDirectLinearTransform(true);
		checkConics(3,alg);
		checkConics(10,alg);
	}

	private void checkConics(int N, HomographyDirectLinearTransform alg) {
		fail("implement");
	}

	@Test
	void perfect_AllToGether() {
		fail("implement");
	}
}
