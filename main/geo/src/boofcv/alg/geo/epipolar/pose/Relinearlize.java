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

package boofcv.alg.geo.epipolar.pose;

import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * <p>
 * Used in case 4 of EPnP.  See [1] for details.
 * </p>
 *
 * <p>
 * [1] Aviad Kipnis and Adi Shamir, "Cryptanalysis of HPE Public Key Cryptosystem" 1999
 * </p>
 *
 * @author Peter Abeles
 */
public class Relinearlize {

	// number of control points.  4 for general 3 for planar
	int numControl;

	// contains the null space
	DenseMatrix64F V;
	// contains one possible solution
	DenseMatrix64F x0 = new DenseMatrix64F(1,1);
	// lookup table for indices
	int table[] = new int[4*4];

	SingularValueDecomposition<DenseMatrix64F> svd =
			DecompositionFactory.svd(3, 3, false, true, false);

	// used inside of solveConstraintMatrix
	DenseMatrix64F AA = new DenseMatrix64F(10,9);
	DenseMatrix64F yy = new DenseMatrix64F(10,1);
	DenseMatrix64F xx = new DenseMatrix64F(9,1);

	// used to compute one possible soluton
	LinearSolver<DenseMatrix64F> pseudo = LinearSolverFactory.pseudoInverse();

	// stores constraints
	double XiiXjk[] = new double[10];
	double XikXji[] = new double[10];

	/**
	 * Specified the number of control points.
	 *
	 * @param numControl 3 = planar, 4 = general
	 */
	public void setNumberControl( int numControl ) {
		this.numControl = numControl;
		int index = 0;
		for( int i = 0; i < numControl; i++ ) {
			for( int j = i; j < numControl; j++ ) {
				table[i*numControl+j] = table[j*numControl+i] = index++;
			}
		}

		if( numControl == 4 ) {
			x0.reshape(10,1,false);
			AA.reshape(10,9,false);
			yy.reshape(10,1,false);
			xx.reshape(9,1,false);
		} else {
			x0.reshape(6,1,false);
			AA.reshape(10,9,false);
			yy.reshape(10,1,false);
			xx.reshape(9,1,false);
		}
	}

	/**
	 * Estimates betas using relinearization.
	 *
	 * @param L_full Linear constraint matrix
	 * @param y distances between world control points
	 * @param betas Estimated betas.  Output.
	 */
	public void process( DenseMatrix64F L_full , DenseMatrix64F y , double betas[] ) {

		svd.decompose(L_full);

		// extract null space
		V = svd.getV(true);

		// compute one possible solution
		pseudo.setA(L_full);
		pseudo.solve(y,x0);

		// add additional constraints to reduce the number of possible solutions
		DenseMatrix64F alphas = solveConstraintMatrix();

		// compute the final solution
		for( int i = 0; i < x0.numCols; i++ ) {
			for( int j = 0; j < 3; j++ ) {
				x0.data[i] += alphas.data[j]*valueNull(j,i);
			}
		}
		
		y.print();
		CommonOps.mult(L_full,x0,y);
		y.print();
		

		if( numControl == 4 ) {
			betas[0] = Math.sqrt(Math.abs(x0.data[0]));
			betas[1] = Math.sqrt(Math.abs(x0.data[4]))*Math.signum(x0.data[1]);
			betas[2] = Math.sqrt(Math.abs(x0.data[7]))*Math.signum(x0.data[2]);
			betas[3] = Math.sqrt(Math.abs(x0.data[9]))*Math.signum(x0.data[3]);
		} else {
			betas[0] = Math.sqrt(Math.abs(x0.data[0]));
			betas[1] = Math.sqrt(Math.abs(x0.data[3]))*Math.signum(x0.data[1]);
			betas[2] = Math.sqrt(Math.abs(x0.data[5]))*Math.signum(x0.data[2]);
		}
	}

	/**
	 * Apply additional constraints to reduce the number of possible solutions
	 *
	 * x(k) = x_{ij} = bi*bj = x0(k) + a1*V0(k) + a2*V1(k) + a3*V2(k)
	 *
	 * constraint:
	 * x_{ii}*x_{jk} = x_{ik}*x_{ji}
	 *
	 */
	protected DenseMatrix64F solveConstraintMatrix() {

		int rowAA = 0;
		for( int i = 0; i < 4; i++ ) {
			for( int j = i+1; j < 4; j++ ) {
				for( int k = j; k < 4; k++ , rowAA++ ) {
					// x_{ii}*x_{jk} = x_{ik}*x_{ji}
					extractXaXb(getIndex(i, i), getIndex(j, k), XiiXjk);
					extractXaXb(getIndex(i, k), getIndex(j, i), XikXji);

					for( int l = 1; l < 10; l++ ) {
						AA.set(rowAA,l-1,XikXji[l]-XiiXjk[l]);
					}
					yy.set(rowAA,XiiXjk[0]-XikXji[0]);
				}
			}
		}

		CommonOps.solve(AA, yy, xx);
		
		return xx;
	}
	
	public double valueNull( int which , int index ) {
		return V.get(V.numCols-numControl+which,index);
	}
	
	private int getIndex( int i , int j ) {
		return table[i*numControl+j];
	}
	
	private void extractXaXb(int indexA, int indexB, double quadratic[]) {

		double x0a = x0.get(indexA);
		double v0a = valueNull(0, indexA);
		double v1a = valueNull(1, indexA);

		double x0b = x0.get(indexB);
		double v0b = valueNull(0, indexB);
		double v1b = valueNull(1, indexB);

		if( numControl == 4 ) {
			double v2a = valueNull(2, indexA);
			double v2b = valueNull(2, indexB);
			multiplyQuadratic4(x0a,v0a,v1a,v2a,x0b,v0b,v1b,v2b,quadratic);
		} else
			multiplyQuadratic3(x0a,v0a,v1a,x0b,v0b,v1b,quadratic);
	}

	
	private void multiplyQuadratic4( double x0 , double x1 , double x2 , double x3 , 
									 double y0 , double y1 , double y2 , double y3 ,
									 double quadratic[] )
	{
		quadratic[0] = x0*y0;
		quadratic[1] = x0*y1 + y0*x1;
		quadratic[2] = x0*y2 + y0*x2;
		quadratic[3] = x0*y3 + y0*x3;
		quadratic[4] = x1*y1;
		quadratic[5] = x1*y2 + y1*x2;
		quadratic[6] = x1*y3 + y1*x3;
		quadratic[7] = x2*y2;
		quadratic[8] = x2*y3 + y2*x3;
		quadratic[9] = x3*y3;
	}

	private void multiplyQuadratic3( double x0 , double x1 , double x2 ,
									 double y0 , double y1 , double y2  ,
									 double quadratic[] )
	{
		quadratic[0] = x0*y0;
		quadratic[1] = x0*y1 + y0*x1;
		quadratic[2] = x0*y2 + y0*x2;
		quadratic[3] = x1*y1;
		quadratic[4] = x1*y2 + y1*x2;
		quadratic[5] = x2*y2;
	}
}
