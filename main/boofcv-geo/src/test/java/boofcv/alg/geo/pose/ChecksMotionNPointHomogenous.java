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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.equation.Equation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Common testing code for algorithms which estimate motion from a set of associated observations
 * and known 3D coordinates.
 *
 * @author Peter Abeles
 */
public abstract class ChecksMotionNPointHomogenous extends CommonMotionNPointHomogenous {

	public abstract DMatrixRMaj compute( List<AssociatedPair> obs, List<Point4D_F64> locations );

	/**
	 * Standard test using only the minimum number of observation
	 */
	@Test void minimalObservationTest() {
		standardTest(6);
	}

	/**
	 * Standard test with an over determined system
	 */
	@Test void overdetermined() {
		standardTest(20);
	}

	/**
	 * Standard test with an over determined system
	 */
	@Test void overdeterminedPlanar() {
		planarTest(20);
	}

	/**
	 * Standard test with an over determined system
	 */
	@Test void checkMotion() {
		testNoMotion(20);
	}

	/**
	 * Test a set of random points in general position
	 *
	 * @param N number of observed point objects
	 */
	public void testNoMotion( int N ) {
		DMatrixRMaj P = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.setIdentity(P);

		checkMotion(N, P, false);
	}

	/**
	 * Test a set of random points in general position
	 *
	 * @param N number of observed point objects
	 */
	public void standardTest( int N ) {
		DMatrixRMaj P = createCameraMatrix();

		checkMotion(N, P, false);
	}

	private DMatrixRMaj createCameraMatrix() {
		Equation eq = new Equation();
		eq.process("A=[1,0.1,0.2;0.1,0.9,0.3;0.2,0.3,1.1]");
		eq.process("T=[0.1,0.3,0.5]'");
		eq.process("P=[A,T]");
		return eq.lookupDDRM("P");
	}

	/**
	 * Test a set of random points in general position
	 *
	 * @param N number of observed point objects
	 */
	public void planarTest( int N ) {
		DMatrixRMaj P = createCameraMatrix();

		checkMotion(N, P, true);
	}

	private void checkMotion( int N, DMatrixRMaj cameraMatrix, boolean planar ) {
		generateScene(N, cameraMatrix, planar);

		// extract the motion
		DMatrixRMaj foundP = compute(assocPairs, worldPts);

		// see if the found motion produces the same output as the original motion
		for (int i = 0; i < worldPts.size(); i++) {
			Point4D_F64 X = worldPts.get(i);
			Point2D_F64 x = assocPairs.get(i).p2;

			Point2D_F64 foundPt = PerspectiveOps.renderPixel(foundP, X, (Point2D_F64)null);

			assertEquals(x.x, foundPt.x, 1e-6);
			assertEquals(x.y, foundPt.y, 1e-6);
		}
	}
}
