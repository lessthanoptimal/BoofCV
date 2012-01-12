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

package boofcv.numerics.optimization;

import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * @author Peter Abeles
 */
public class QuasiNewtonBFGS implements UnconstrainedMinimization
{
	FunctionNtoS function;
	FunctionNtoN jacobian;

	// inverse of the Hessian approximation
	DenseMatrix64F B;
	// search direction
	DenseMatrix64F p;
	// gradient
	DenseMatrix64F g;
	DenseMatrix64F s;
	DenseMatrix64F y;

	DenseMatrix64F tempNx1;
	
	public void setFunction(FunctionNtoS function,
							FunctionNtoN jacobian )
	{
		this.function = function;
		this.jacobian = jacobian;
		
		int N = function.getNumberOfInputs();
		B = new DenseMatrix64F(N,N);
		p = new DenseMatrix64F(N,1);

		tempNx1 = new DenseMatrix64F(N,1);

		CommonOps.setIdentity(B);
	}
	
	public void setInitialH( DenseMatrix64F Hinit) {
		B.set(Hinit);
	}

	@Override
	public boolean optimize(double[] initial, double[] result) {

		while( true ) {
			// compute the search direction
			CommonOps.mult(B,g,p);

			// use the line search to find the next x

			// TODO compute s and y

			// update the B matrix
			updateBFGS();
			
			// check for termination
			break;
		}


		return false;
	}

	/**
	 *  Perform the BFGS 
	 *  
	 *  B(k+1) = B(k) + [B(k)*s(k)*s(k)'*B(k)]/[s(k)'*B(k)*s(k)] 
	 *                + y(k)*y(k)'/[y(k)'*s(k)]
	 */
	private void updateBFGS() {
		// s(k)'*B(k)*s(k)
		double middleBottom = VectorVectorMult.innerProdA(s,B,s);
		// B(k)*s(k)
		CommonOps.mult(B,s, tempNx1);

		// y(k)'*s(k)
		double rightBottom = VectorVectorMult.innerProd(y, s);

		// perform the update
		specialOuter(B,tempNx1,middleBottom,y,rightBottom);
	}
	
	private double normF2( DenseMatrix64F v ) {
		final int N = v.getNumElements();
		double total = 0;
		for( int i = 0; i < N; i++ ) {
			double d = v.data[i];
			total += d*d;
		}
		return total;
	}

	/**
	 * A = A - (v0*v0')/divisor0 + (v1*v1')/divisor1
	 *
	 * Highly specialized double outer product to speed up the update function and require less
	 * extra memory
	 */
	protected static void specialOuter( DenseMatrix64F A ,
										DenseMatrix64F v0 , double divisor0 ,
										DenseMatrix64F v1 , double divisor1 )
	{
		final int N = A.numCols;

		int indexA = 0;
		for( int y = 0; y < N; y++ ) {
			double a0 = v0.data[y];
			double a1 = v1.data[y];

			for( int x = 0; x < N; x++ ) {
				A.data[indexA++] += a1*v1.data[x]/divisor1 - a0*v0.data[x]/divisor0;
			}
		}
	}
}
