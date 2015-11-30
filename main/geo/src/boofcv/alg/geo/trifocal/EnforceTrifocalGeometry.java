/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.geo.TrifocalTensor;
import georegression.struct.point.Point3D_F64;
import org.ejml.alg.dense.decomposition.svd.SafeSvd;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;

/**
 * <p>
 * Applies geometric constraints to an estimated trifocal tensor.  See page 394 in [1].
 * </p>
 *
 * <p>References:</p>
 * <ul>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class EnforceTrifocalGeometry {

	// SVD which computes U and not V
	private SingularValueDecomposition<DenseMatrix64F> svdU;
	// SVD which computes V and not U
	private SingularValueDecomposition<DenseMatrix64F> svdV;

	// Storage for SVD
	private DenseMatrix64F U = new DenseMatrix64F(27,18);
	// Contains the linear mapping from TODO
	private DenseMatrix64F Up = new DenseMatrix64F(1,1);

	// Storage for solution as a function of the 18-nullity unknowns
	private DenseMatrix64F xp = new DenseMatrix64F(1,1);

	// Storage for the A*U, where A is the linear constraint matrix and U is the solution's subspace
	private DenseMatrix64F AU = new DenseMatrix64F(1,1);

	// The adjusted trifocal tensor in vector format
	private DenseMatrix64F vectorT = new DenseMatrix64F(27,1);


	// From the definition of the trifocal tensor: T_i = a_i*b_4^T + a_4*b_i^T
	// Columns of E are multiplied by the following unknowns:
	// [a(0,0) , a(0,1) , a(0,2) , a(1,0) ....  b(0,0) , b(0,1) , b(0,2) , b(1,0) ]
	// Where a and b are elements of 3x3 matrices A and B in the P2 = [A|e2] P3=[B|e3]
	protected DenseMatrix64F E = new DenseMatrix64F(27,18);

	public EnforceTrifocalGeometry() {
		svdU = DecompositionFactory.svd(10,10,true,false,true);
		svdV = DecompositionFactory.svd(10,10,false,true,false);
		svdV = new SafeSvd(svdV); // can't modify the input in this case
	}

	/**
	 * Computes a trifocal tensor which minimizes the algebraic error given the
	 * two epipoles and the linear constraint matrix.  The epipoles are from a previously
	 * computed trifocal tensor.
	 *
	 * @param e2 Epipole of first image in the second image
	 * @param e3 Epipole of first image in the third image
	 * @param A Linear constraint matrix for trifocal tensor created from image observations.
	 */
	public void process( Point3D_F64 e2 , Point3D_F64 e3 , DenseMatrix64F A ) {
		// construct the linear system that the solution which solves the unknown square
		// matrices in the camera matrices
		constructE(e2, e3);

		// Computes U, which is used to map the 18 unknowns onto the 27 trifocal unknowns
		svdU.decompose(E);
		svdU.getU(U, false);

		// Copy the parts of U which correspond to the non singular parts if the SVD
		// since there are only really 18-nullity unknowns due to linear dependencies
		SingularOps.descendingOrder(U,false,svdU.getSingularValues(),svdU.numberOfSingularValues(),null,false);
		int rank = SingularOps.rank(svdU, 1e-13);
		Up.reshape(U.numRows,rank);
		CommonOps.extract(U,0,U.numRows,0,Up.numCols,Up,0,0);

		// project the linear constraint matrix into this subspace
		AU.reshape(A.numRows,Up.numCols);
		CommonOps.mult(A,Up,AU);

		// Extract the solution of ||A*U*x|| = 0 from the null space
		svdV.decompose(AU);

		xp.reshape(rank,1);
		SingularOps.nullVector(svdV,true,xp);

		// Translate the solution from the subspace and into a valid trifocal tensor
		CommonOps.mult(Up,xp,vectorT);

		// the sign of vectorT is arbitrary, but make it positive for consistency
		if( vectorT.data[0] > 0 )
			CommonOps.changeSign(vectorT);
	}

	/**
	 * Returns the algebraic error vector. error = A*U*x.  length = number
	 * of observations
	 */
	public void computeErrorVector( DenseMatrix64F A , DenseMatrix64F errors ) {
		errors.reshape(A.numRows,1);
		CommonOps.mult(A,vectorT,errors);
	}

	/**
	 * Inserts the found trifocal tensor into the provided object.
	 */
	public void extractSolution( TrifocalTensor tensor ) {
		tensor.convertFrom(vectorT);
	}

	/**
	 * The matrix E is a linear system for computing the trifocal tensor.  The columns
	 * are the unknown square matrices from view 2 and 3.  The right most column in
	 * both projection matrices are the provided epipoles, whose values are inserted into E
	 */
	protected void constructE( Point3D_F64 e2 , Point3D_F64 e3 ) {
		E.zero();

		for( int i = 0; i < 3; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				for( int k = 0; k < 3; k++ ) {
					// which element in the trifocal tensor is being manipulated
					int row = 9*i + 3*j + k;
					// which unknowns are being multiplied by
					int col1 = j*3 + i;
					int col2 = k*3 + i + 9;

					E.data[row*18 + col1 ] = e3.getIndex(k);
					E.data[row*18 + col2 ] = -e2.getIndex(j);
				}
			}
		}
	}

}
