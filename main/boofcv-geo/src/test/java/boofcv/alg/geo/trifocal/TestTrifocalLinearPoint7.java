/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.trifocal;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedTriple;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestTrifocalLinearPoint7 extends CommonTrifocalChecks {

	/**
	 * Check the linear constraint matrix by seeing if the correct solution is in the null space
	 */
	@Test
	public void checkLinearSystem() {

		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		// construct in pixel coordinates for ease
		alg.N1 = new NormalizationPoint2D(0,1,0,1);
		alg.N2 = new NormalizationPoint2D(0,1,0,1);
		alg.N3 = new NormalizationPoint2D(0,1,0,1);

		alg.createLinearSystem(observationsSpecial);  // TOOO change back

		DMatrixRMaj A = alg.A;

		DMatrixRMaj X = new DMatrixRMaj(27,1);
		for( int i = 0; i < 9; i++ ) {
			X.data[i] = tensor.T1.get(i);
			X.data[i+9] = tensor.T2.get(i);
			X.data[i+18] = tensor.T3.get(i);
		}

		DMatrixRMaj Y = new DMatrixRMaj(A.numRows,1);

		CommonOps_DDRM.mult(A,X,Y);

		for( int i = 0; i < Y.numRows; i++ ) {
			assertEquals(0,Y.get(i),1e-7);
		}
	}

	@Test
	public void fullTest() {
		TrifocalLinearPoint7 alg = new TrifocalLinearPoint7();

		assertTrue(alg.process(observations,found));

		// validate the solution by using a constraint
		for( AssociatedTriple a : observations ) {
			DMatrixRMaj A = MultiViewOps.constraint(found,a.p1,a.p2,a.p3,null);

			assertEquals(0,NormOps_DDRM.normF(A),1e-7);
		}
	}
}
