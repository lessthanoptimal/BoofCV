/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDecomposeHomography {

	DMatrixRMaj R = ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0.1, -0.04, 0.08, null);
	Vector3D_F64 T = new Vector3D_F64(2,1,-3);

	double d = 2;
	Vector3D_F64 N = new Vector3D_F64(1,0.1,-0.1);

	public TestDecomposeHomography() {
		N.normalize();
	}

	@Test
	public void checkAgainstKnown() {
		DMatrixRMaj H = MultiViewOps.createHomography(R, T, d, N);

		DecomposeHomography alg = new DecomposeHomography();

		// There's a scale ambiguity. Let's see if it handles it correctly
		CommonOps_DDRM.scale(2.4,H);

		alg.decompose(H);

		List<Se3_F64> foundSE = alg.getSolutionsSE();
		List<Vector3D_F64> foundN = alg.getSolutionsN();

		assertEquals(4,foundSE.size());
		assertEquals(4,foundN.size());

		TestDecomposeEssential.checkUnique(foundSE);
		checkHasOriginal(foundSE,foundN,R,T,d,N);
	}

	/**
	 * Checks to see if the same solution is returned when invoked multiple times
	 */
	@Test
	public void multipleCalls() {
		DMatrixRMaj H = MultiViewOps.createHomography(R, T, d, N);

		DecomposeHomography alg = new DecomposeHomography();
		// call it twice and see if things break
		alg.decompose(H);
		alg.decompose(H);

		List<Se3_F64> foundSE = alg.getSolutionsSE();
		List<Vector3D_F64> foundN = alg.getSolutionsN();

		TestDecomposeEssential.checkUnique(foundSE);
		checkHasOriginal(foundSE,foundN,R,T,d,N);
	}

	/*
	 * See if an equivalent to the input matrix exists
	 */
	public static void checkHasOriginal( List<Se3_F64> solutionsSE , List<Vector3D_F64> solutionsN ,
										 DMatrixRMaj R, Vector3D_F64 T, double d , Vector3D_F64 N ) {

		int numMatches = 0;
		for( int i = 0; i < 4; i++ ) {
			Se3_F64 foundSE = solutionsSE.get(i);
			Vector3D_F64 foundN = solutionsN.get(i);

			if(!MatrixFeatures_DDRM.isIdentical(foundSE.getR(), R, 1e-4)) break;

			if( Math.abs(T.x/d - foundSE.getT().x) > 1e-8 ) break;
			if( Math.abs(T.y/d - foundSE.getT().y) > 1e-8 ) break;
			if( Math.abs(T.z/d - foundSE.getT().z) > 1e-8 ) break;

			if( Math.abs(N.x - foundN.x) > 1e-8 ) break;
			if( Math.abs(N.y - foundN.y) > 1e-8 ) break;
			if( Math.abs(N.z - foundN.z) > 1e-8 ) break;

			numMatches++;
		}

		assertEquals(1,numMatches);
	}
}
