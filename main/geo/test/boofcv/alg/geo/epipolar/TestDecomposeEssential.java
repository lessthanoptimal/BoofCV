/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.epipolar;

import georegression.geometry.RotationMatrixGenerator;
import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDecomposeEssential {

	/**
	 * Check the decomposition against a known input.  See if the solutions have the expected
	 * properties and at least one matches the input.
	 */
	@Test
	public void checkAgainstKnown() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.4,0.5,null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);

		DenseMatrix64F E = UtilEpipolar.computeEssential(R,T);

		DecomposeEssential alg = new DecomposeEssential();
		alg.decompose(E);

		List<Se3_F64> solutions = alg.getSolutions();

		assertEquals(4,solutions.size());

		checkUnique(solutions);

		checkHasOriginal(solutions,E);
	}

	/**
	 * Checks to see if the same solution is returned when invoked multiple times
	 */
	@Test
	public void multipleCalls() {
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(0.1,-0.4,0.5,null);
		Vector3D_F64 T = new Vector3D_F64(2,1,-3);

		DenseMatrix64F E = UtilEpipolar.computeEssential(R,T);

		DecomposeEssential alg = new DecomposeEssential();
		// call it twice and see if it breaks
		alg.decompose(E);
		alg.decompose(E);

		List<Se3_F64> solutions = alg.getSolutions();

		assertEquals(4,solutions.size());

		checkUnique(solutions);

		checkHasOriginal(solutions, E);
	}

	/**
	 * Makes sure each solution returned is unique by transforming a point.
	 */
	public static void checkUnique( List<Se3_F64> solutions ) {

		Point3D_F64 orig = new Point3D_F64(1,2,3);

		for( int i = 0; i < solutions.size(); i++ ) {
			Point3D_F64 found = SePointOps_F64.transform(solutions.get(i),orig,null);

			for( int j = i+1; j < solutions.size(); j++ ) {
				Point3D_F64 alt = SePointOps_F64.transform(solutions.get(j),orig,null);

				GeometryUnitTest.assertNotEquals(found,alt,1e-4);
			}
		}
	}

	/**
	 * See if an equivalent to the input matrix exists
	 */
	private void checkHasOriginal( List<Se3_F64> solutions , DenseMatrix64F origE ) {

		int numMatches = 0;
		for( Se3_F64 se : solutions ) {
			DenseMatrix64F foundE = UtilEpipolar.computeEssential(se.getR(),se.getT());

			if(MatrixFeatures.isIdentical(origE,foundE,1e-4))
				numMatches++;
		}

		assertEquals(2,numMatches);
	}
}
