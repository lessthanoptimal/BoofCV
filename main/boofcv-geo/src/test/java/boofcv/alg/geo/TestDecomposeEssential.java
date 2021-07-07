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

import boofcv.testing.BoofStandardJUnit;
import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDecomposeEssential extends BoofStandardJUnit {

	/**
	 * Check the decomposition against a known input. See if the solutions have the expected
	 * properties and at least one matches the input.
	 */
	@Test void checkAgainstKnown() {
		List<Se3_F64> testCases = new ArrayList<>();
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(0.1,-0.4,0.5,2,1,-3,null));
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(-0.1,0.4,-0.5,2,1,-3,null));
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(0.1,-0.4,0.5,-2,-1,3,null));
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(-0.1,0.4,-0.5,-2,-1,3,null));
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(0,0,0.1,0,0,0,null));
		testCases.add(SpecialEuclideanOps_F64.eulerXyz(1,0.1,-2,-0.1,0,0.05,null).invert(null));

		DecomposeEssential alg = new DecomposeEssential();

		for( Se3_F64 expected : testCases ) {
			DMatrixRMaj E = MultiViewOps.createEssential(expected.R, expected.T, null);

			alg.decompose(E);

			List<Se3_F64> solutions = alg.getSolutions();

			assertEquals(4, solutions.size());

			checkUnique(solutions);

			double length = expected.T.norm();
			assertEquals(alg.getTranslationLength(), length, UtilEjml.TEST_F64);

			checkHasOriginal(solutions, expected);
		}
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
	private void checkHasOriginal( List<Se3_F64> solutions , Se3_F64 expected ) {

		expected.T.divide(expected.T.norm());

		int numMatches = 0;
		for( Se3_F64 se : solutions ) {
			if(MatrixFeatures_DDRM.isIdentical(expected.R,se.getR(),1e-4)) {
				if( expected.T.distance(se.getT()) < 1e-4 )
					numMatches++;
			}
		}

		assertEquals(1,numMatches);
	}
}
