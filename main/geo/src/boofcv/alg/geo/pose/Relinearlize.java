/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.interfaces.linsol.LinearSolver;
import org.ejml.ops.CommonOps;

/**
 * <p>
 * Used in case 4 of EPnP.  See [1] for details.  This technique appears to not be all that accurate
 * in practice, but better than nothing.  Or maybe there is a bug in the implementation below
 * since even with perfect data it appears to generate large errors.  One possible source
 * of implementation error is that the entire null space is not being used.  More of
 * the null space (particularly in the planar case) could be used if more relinearization
 * was run multiple times.
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

	// how much of the null space is sued
	int numNull;

	// contains the null space
	DenseMatrix64F V;
	// contains one possible solution
	DenseMatrix64F x0 = new DenseMatrix64F(1,1);
	// lookup table for indices
	int table[] = new int[10*10];

	SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(3, 3, false, true, false);

	// used inside of solveConstraintMatrix
	DenseMatrix64F AA = new DenseMatrix64F(1,1);
	DenseMatrix64F yy = new DenseMatrix64F(1,1);
	DenseMatrix64F xx = new DenseMatrix64F(1,1);

	// used to compute one possible solution
	LinearSolver<DenseMatrix64F> pseudo = LinearSolverFactory.pseudoInverse(true);

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

		if( numControl == 4 ) {
			x0.reshape(10,1,false);
			AA.reshape(10,9,false);
			yy.reshape(10,1,false);
			xx.reshape(9,1,false);
			numNull = 3;
		} else {
			x0.reshape(6,1,false);
			AA.reshape(4,2,false);
			yy.reshape(4,1,false);
			xx.reshape(2,1,false);
			numNull = 1;
		}

		int index = 0;
		for( int i = 0; i < numControl; i++ ) {
			for( int j = i; j < numControl; j++ ) {
				table[i*numControl+j] = table[j*numControl+i] = index++;
			}
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
		V = svd.getV(null,true);

		// compute one possible solution
		pseudo.setA(L_full);
		pseudo.solve(y,x0);

		// add additional constraints to reduce the number of possible solutions
		DenseMatrix64F alphas = solveConstraintMatrix();

		// compute the final solution
		for( int i = 0; i < x0.numRows; i++ ) {
			for( int j = 0; j < numNull; j++ ) {
				x0.data[i] += alphas.data[j]*valueNull(j,i);
			}
		}

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
		for( int i = 0; i < numControl; i++ ) {
			for( int j = i+1; j < numControl; j++ ) {
				for( int k = j; k < numControl; k++ , rowAA++ ) {
					// x_{ii}*x_{jk} = x_{ik}*x_{ji}
					extractXaXb(getIndex(i, i), getIndex(j, k), XiiXjk);
					extractXaXb(getIndex(i, k), getIndex(j, i), XikXji);

					for( int l = 1; l <= AA.numCols; l++ ) {
						AA.set(rowAA,l-1,XikXji[l]-XiiXjk[l]);
					}
					yy.set(rowAA,XiiXjk[0]-XikXji[0]);
				}
			}
		}
//		AA.print();
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

		double x0b = x0.get(indexB);
		double v0b = valueNull(0, indexB);

		if( numControl == 4 ) {
			double v1a = valueNull(1, indexA);
			double v2a = valueNull(2, indexA);
			
			double v1b = valueNull(1, indexB);
			double v2b = valueNull(2, indexB);
			multiplyQuadratic4(x0a,v0a,v1a,v2a,x0b,v0b,v1b,v2b,quadratic);
		} else {
			multiplyQuadratic2(x0a,v0a,x0b,v0b,quadratic);
		}
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

	private void multiplyQuadratic2( double x0 , double x1 ,
									 double y0 , double y1 ,
									 double quadratic[] )
	{
		quadratic[0] = x0*y0;
		quadratic[1] = x0*y1 + y0*x1;
		quadratic[2] = x1*y1;
	}
}
