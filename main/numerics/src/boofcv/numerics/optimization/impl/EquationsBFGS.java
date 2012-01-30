/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization.impl;

import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

/**
 * <p>
 * Equations for updating the approximate Hessian matrix using BFGS equations.
 * </p>
 *
 * <p>
 * Forward:
 * <pre>
 *  B(k+1) = B(k) + [B(k)*s*s'*B(k)]/[s'*B*s]
 *                + y*y'/[y'*s]
 * </pre>
 * </p>
 * <p>
 * Inverse:
 * <pre>
 *  H(k+1) = (I-p*s*y')*H(k)*(I-p*y*s') + p*s*s'
 * </pre>
 * </p>
 * 
 * <p>
 * <ul>
 * <li>B = symmetric positive definite forward n by n matrix.</li>
 * <li>H = symmetric positive definite inverse n by n matrix.</li>
 * <li>s = x(k+1)-x(k) vector change in state.</li>
 * <li>y = x'(k+1)-x'(k) vector change in gradient.</li>
 * <li>p = 1/(y'*s) > 0
 * </ul>
 * </p>
 *
 *
 * @author Peter Abeles
 */
public class EquationsBFGS {

	/**
	 * Naive but easy to visually verify implementation of the inverse BFGS update.  Primarily
	 * for testing purposes.
	 *
	 * @param H inverse matrix being updated
	 * @param s change in state
	 * @param y change in gradient
	 */
	public static void naiveInverseUpdate(DenseMatrix64F H,
										  DenseMatrix64F s,
										  DenseMatrix64F y)
	{
		SimpleMatrix _y = new SimpleMatrix(y);
		SimpleMatrix _s = new SimpleMatrix(s);
		SimpleMatrix B = new SimpleMatrix(H);
		SimpleMatrix I = SimpleMatrix.identity(_y.getNumElements());

		double p = 1.0/_y.dot(_s);

		SimpleMatrix A1 = I.minus(_s.mult(_y.transpose()).scale(p));
		SimpleMatrix A2 = I.minus(_y.mult(_s.transpose()).scale(p));
		SimpleMatrix SS = _s.mult(_s.transpose()).scale(p);
		SimpleMatrix M = A1.mult(B).mult(A2).plus(SS);

		H.set(M.getMatrix());
	}

	/**
	 * Inverse update equation that orders the multiplications to minimize the number of operations.
	 *
	 * @param H symmetric inverse matrix being updated
	 * @param s change in state
	 * @param y change in gradient
	 * @param tempV0 Storage vector of length N
	 * @param tempV1 Storage vector of length N
	 */
	public static void inverseUpdate( DenseMatrix64F H , DenseMatrix64F s , DenseMatrix64F y ,
									  DenseMatrix64F tempV0, DenseMatrix64F tempV1)
	{
		double alpha = VectorVectorMult.innerProdA(y,H,y);
		double p = 1.0/VectorVectorMult.innerProd(s,y);

		// make sure storage variables have the correct dimension
		int N = H.numCols;
		tempV0.numRows = N; tempV0.numCols=1;
		tempV1.numRows = 1; tempV1.numCols=N;

		CommonOps.mult(H,y,tempV0);
		CommonOps.multTransA(y, H, tempV1);

		VectorVectorMult.rank1Update(-p, H , tempV0, s);
		VectorVectorMult.rank1Update(-p, H , s, tempV1);
		VectorVectorMult.rank1Update(p*alpha*p+p, H , s, s);
	}

	/**
	 *
	 *
	 * <p>
	 * [1] D. Byatt and I. D. Coope and C. J. Price, "Effect of limited precision on the BFGS quasi-Newton algorithm"
	 * Proc. of 11th Computational Techniques and Applications Conference CTAC-2003
	 * </p>
	 *
	 * @param C
	 * @param d
	 * @param y
	 * @param tempV0
	 */
	public static void conjugateUpdateD( DenseMatrix64F C , DenseMatrix64F d , DenseMatrix64F y ,
										 double step, DenseMatrix64F tempV0 )
	{
		DenseMatrix64F z = tempV0;

		CommonOps.multTransA(C, y, z);
		
		double dTd = VectorVectorMult.innerProd(d,d);
		double dTz = VectorVectorMult.innerProd(d,z);
		
		double middleScale = -dTd/dTz;
		double rightScale = dTd/Math.sqrt(-dTd*dTz/step);
		
		int N = d.getNumElements();
		for( int i = 0; i < N; i++ ) {
			d.data[i] += middleScale*z.data[i] + rightScale*d.data[i];
		}
	}

	/**
	 *
	 *
	 * <p>
	 * [1] D. Byatt and I. D. Coope and C. J. Price, "Effect of limited precision on the BFGS quasi-Newton algorithm"
	 * Proc. of 11th Computational Techniques and Applications Conference CTAC-2003
	 * </p>
	 *
	 * @param C
	 * @param d
	 * @param y
	 * @param tempV0
	 */
	public static void conjugateUpdateC( DenseMatrix64F C , DenseMatrix64F d , DenseMatrix64F y ,
										 double step, DenseMatrix64F tempV0 , DenseMatrix64F tempV1)
	{
		DenseMatrix64F z = tempV0;
		DenseMatrix64F d_bar = tempV1;

		CommonOps.multTransA(C,y,z);

		double dTd = VectorVectorMult.innerProd(d,d);
		double dTz = VectorVectorMult.innerProd(d,z);

		double middleScale = -dTd/dTz;
		double rightScale = dTd/Math.sqrt(-dTd*dTz/step);

		int N = d.getNumElements();
		for( int i = 0; i < N; i++ ) {
			d.data[i] += middleScale*z.data[i] + rightScale*d.data[i];
		}
	}
}
