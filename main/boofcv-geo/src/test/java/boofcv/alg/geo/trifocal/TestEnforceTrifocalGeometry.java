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
import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEnforceTrifocalGeometry extends CommonTrifocalChecks {

	/**
	 * Construct a tensor from two arbitrary camera matrices.  Then provide the same inputs
	 * to the algorithm and see if the matrix is constructed correctly.
	 */
	@Test
	public void checkMatrixE() {
		DMatrixRMaj P2 = new DMatrixRMaj(3,4,true,1,2,3,4,5,6,7,8,9,10,11,12);
		DMatrixRMaj P3 = new DMatrixRMaj(3,4,true,10,20,30,40,50,60,70,80,90,100,110,120);

		Point3D_F64 e2 = new Point3D_F64(P2.get(0,3),P2.get(1,3),P2.get(2,3));
		Point3D_F64 e3 = new Point3D_F64(P3.get(0,3),P3.get(1,3),P3.get(2,3));

		TrifocalTensor tensor = MultiViewOps.createTrifocal(P2,P3,null);

		EnforceTrifocalGeometry alg = new EnforceTrifocalGeometry();

		alg.constructE(e2,e3);

		DMatrixRMaj X = new DMatrixRMaj(18,1);

		for( int i = 0; i < 9; i++ ) {
			X.data[i] = P2.get(i/3,i%3);
			X.data[i+9] = P3.get(i/3,i%3);
		}

		DMatrixRMaj vectorT = new DMatrixRMaj(27,1);
		CommonOps_DDRM.mult(alg.E,X,vectorT);

		TrifocalTensor foundT = new TrifocalTensor();
		foundT.convertFrom(vectorT);

		// the two tensors should be identical
		for( int i = 0; i < 3; i++ ) {
			assertTrue(MatrixFeatures_DDRM.isIdentical(tensor.getT(i),foundT.getT(i),1e-8));
		}
	}

	/**
	 * Give it a set of perfect inputs and see if it computes a valid trifocal tensor
	 */
	@Test
	public void perfectInput() {

		// create linear constraint matrix
		TrifocalLinearPoint7 constructA = new TrifocalLinearPoint7();
		// Make things easier by working in pixel coordinates
		constructA.N1 = new NormalizationPoint2D(0,1,0,1);
		constructA.N2 = new NormalizationPoint2D(0,1,0,1);
		constructA.N3 = new NormalizationPoint2D(0,1,0,1);

		constructA.createLinearSystem(observations);

		DMatrixRMaj A = constructA.A;

		// extract epipoles
		Point3D_F64 e2 = new Point3D_F64();
		Point3D_F64 e3 = new Point3D_F64();

		MultiViewOps.extractEpipoles(tensor,e2,e3);

		EnforceTrifocalGeometry alg = new EnforceTrifocalGeometry();

		alg.process(e2,e3,A);

		// Check if the found solution is valid by applying the trifocal constraint for 3 points
		TrifocalTensor found = new TrifocalTensor();
		alg.extractSolution(found);
		checkTrifocalWithConstraint(found,1e-6);

		// make sure the errors are zero too
		DMatrixRMaj errors = new DMatrixRMaj(observations.size(),1);
		alg.computeErrorVector(A,errors);

		for( int i = 0; i < errors.numRows; i++ )
			assertEquals(0,errors.get(i),1e-8);

	}
}
