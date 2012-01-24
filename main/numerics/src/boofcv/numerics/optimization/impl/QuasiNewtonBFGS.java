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

import boofcv.numerics.optimization.FunctionNtoN;
import boofcv.numerics.optimization.FunctionNtoS;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.ejml.simple.SimpleMatrix;

/**
 * <p>
 * Quasi-Newton nonlinear optimization using BFGS update on the approximate inverse Hessian with
 * a line search.  The function and its gradient is required.  If no gradient is available then a numerical
 * gradient will be used.  The line search must meet the Wolfe or strong Wolfe condition.  This
 * technique is automatically scale invariant and no scale matrix is required.  In most situations
 * super-linear convergence can be expected. Based on the description provided in [1].
 * </p>
 *
 * <p>
 * If no initial estimate for the inverse Hessian matrix is provided a scaled identify matrix will be
 * used.  The scale of the identify matrix is generated with a heuristic that using the gradient at
 * two sample points.  The procedure with justification is described on page 143 in [1].
 * </p>
 *
 * <p>
 * The inverse Hessian update requires only a rank-2 making it efficient.  Stability requires that the
 * line search maintain the Wolfe or strong Wolfe condition or else the inverse Hessian matrix can stop
 * being symmetric positive definite.
 * </p>
 *
 * <p>
 * [1] Jorge Nocedal, Stephen J. Wright, "Numerical Optimization" 2nd Ed, 2006 Springer
 * </p>
 * @author Peter Abeles
 */
// TODO take advantage of B being symmetric - might be able to do more.  See dlmmv() function dvmlm.f
// TODO add optional sanity checks to make sure it is symmetric positive definite
public class QuasiNewtonBFGS
{
	// number of inputs
	private int N;

	// convergence conditions
	double relativeErrorTol; // relative error tolerance
	double absoluteErrorTol; // absolute error tolerance

	// function being minimized and its gradient
	private FunctionNtoS function;
	private FunctionNtoN gradient;

	// searches for a parameter that meets the Wolfe condition
	private LineSearchManager lineSearch;

	// is the initial H_inverse manually provided?
	private boolean manualB;
	
	// inverse of the Hessian approximation
	private DenseMatrix64F B;
	// search direction
	private DenseMatrix64F searchVector;
	// gradient
	private DenseMatrix64F g;
	// difference between current and previous x
	private DenseMatrix64F s;
	// difference between current and previous gradient
	private DenseMatrix64F y;
	
	// current set of parameters being considered
	private DenseMatrix64F x;
	// function value at x(k)
	private double fx;

	// storage
	private DenseMatrix64F tempNx1;

	// mode that the algorithm is in
	private int mode;
	// error message
	private String message;
	// if it converged to a solution or not
	private boolean hasConverged;

	// is it the first iteration
	private boolean firstIteration;

	/**
	 * Configures the search.
	 *
	 * @param function Function being optimized
	 * @param gradient Computes the function's gradient
	 * @param lineSearch Line search that selects a solution that meets the Wolfe condition.
	 * @param relativeErrorTol Relative error termination condition. >= 0
	 * @param absoluteErrorTol Absolute error termination condition. >= 0
	 */
	public QuasiNewtonBFGS( FunctionNtoS function , FunctionNtoN gradient,
							LineSearchManager lineSearch ,
							double relativeErrorTol ,
							double absoluteErrorTol )
	{
		if( relativeErrorTol < 0 )
			throw new IllegalArgumentException("relativeErrorTol < 0");
		if( absoluteErrorTol < 0 )
			throw new IllegalArgumentException("absoluteErrorTol < 0");

		this.lineSearch = lineSearch;
		this.function = function;
		this.gradient = gradient;
		this.relativeErrorTol = relativeErrorTol;
		this.absoluteErrorTol = absoluteErrorTol;

		N = function.getN();
		
		B = new DenseMatrix64F(N,N);
		searchVector = new DenseMatrix64F(N,1);
		g = new DenseMatrix64F(N,1);
		s = new DenseMatrix64F(N,1);
		y = new DenseMatrix64F(N,1);
		x = new DenseMatrix64F(N,1);

		tempNx1 = new DenseMatrix64F(N,1);
		manualB = false;
	}

	/**
	 * Manually specify the initial inverse hessian approximation.
	 * @param Hinverse Initial hessian approximation
	 */
	public void setInitialHInv( DenseMatrix64F Hinverse) {
		B.set(Hinverse); 
		manualB = true;
	}

	public void initialize(double[] initial) {
		this.mode = 0;
		this.hasConverged = false;
		this.message = null;        
		this.manualB = false;
		this.firstIteration = true;

		// set the change in x to be zero
		s.zero();
		// default to an initial inverse Hessian approximation as
		// the identity matrix.  This can be overridden or improved by an heuristic below
		CommonOps.setIdentity(B);

		// save the initial value of x
		System.arraycopy(initial, 0, x.data, 0, N);

		fx = function.process(initial);
	}


	public double[] getParameters() {
		return x.data;
	}


	/**
	 * Perform one iteration in the optimization.
	 *
	 * @return true if the optimization has stopped.
	 */
	public boolean iterate() {
		if( mode == 0 ) {
//			System.out.println("----------------- Compute search direction");
			computeSearchDirection();
			return false;
		} else {
//			System.out.println("----------------- Perform line search");
			return performLineSearch();
		}
	}

	/**
	 * Computes the next search direction using BFGS
	 */
	private void computeSearchDirection() {
		// Compute the function's value and gradient
//		fx = function.process(x.data); // todo hasn't this been computed already?
		gradient.process(x.data, tempNx1.data); // todo gradient computed twice when not numerical

		// compute the change in the gradient
		for( int i = 0; i < N; i++ ) {
			y.data[i] = tempNx1.data[i] - g.data[i];
			g.data[i] = tempNx1.data[i];
		}

		// Update the inverse Hessian matrix
		if( !firstIteration ) {
			updateBFGS();
		}

		// compute the search direction
		CommonOps.mult(-1,B,g, searchVector);

		// Optionally, use gradient information to adjust the initial B automatically
		if( firstIteration && !manualB ) {
			automaticScaleB();
		}

		// use the line search to find the next x
		lineSearch.initialize(fx, x.data,g.data, searchVector.data,1,N);

		mode = 1;
		firstIteration = false;
	}

	/**
	 * Performs a 1-D line search along the chosen direction until the Wolfe conditions
	 * have been meet.
	 *
	 * @return true if the search has terminated.
	 */
	private boolean performLineSearch() {
		if( lineSearch.iterate() ) {
			// see if the line search failed
			if( !lineSearch.isSuccess() ) {
				return terminateSearch(false,lineSearch.getWarning());
			}

			// update variables
			double step = lineSearch.getStep();

			// compute the new x and the change in the x
			for( int i = 0; i < N; i++ )
				x.data[i] += s.data[i] = step * searchVector.data[i];

			// convergence tests
			double g0 = lineSearch.getLineDerivativeAtZero();
			double fstp = lineSearch.getFStep();

			// see if the actual different and predicted differences are smaller than the
			// error tolerance
			if( Math.abs(fstp-fx) <= absoluteErrorTol && step*Math.abs(g0) <= absoluteErrorTol )
				return terminateSearch(true,null);

			// check for relative convergence
			if( Math.abs(fstp-fx) <= relativeErrorTol*Math.abs(fx)
					&& step*Math.abs(g0) <= relativeErrorTol*Math.abs(fx) )
				return terminateSearch(true,null);

			// current function value is now the previous
			fx = fstp;

			// start the loop again
			mode = 0;
		}
		return false;
	}

	/**
	 * Use the initial step computed from the provisional identity B matrix to compute
	 * a B matrix which has a better scale.
	 */
	private void automaticScaleB() {
		// compute the new point
		for( int i = 0; i < N; i++ ) {
			s.data[i] = x.data[i] + searchVector.data[i];
		}
		
		// compute the change in gradient
		gradient.process(s.data, y.data);
		CommonOps.sub(y,g,y);
		
		// change the scale of the initial identity matrix
		double a = VectorVectorMult.innerProd(y, searchVector)/ NormOps.fastNormF(y);
		
		for( int i = 0; i < N; i++ ) {
			B.set(i,i,a);
		}

		// recompute the search direction
		CommonOps.mult(-1,B,g, searchVector);
	}

	/**
	 *  Perform the BFGS update
	 *  
	 *  B(k+1) = B(k) + [B(k)*s(k)*s(k)'*B(k)]/[s(k)'*B(k)*s(k)] 
	 *                + y(k)*y(k)'/[y(k)'*s(k)]
	 */
	private void updateBFGS() {
		SimpleMatrix y = new SimpleMatrix(this.y);
		SimpleMatrix s = new SimpleMatrix(this.s);
		SimpleMatrix B = new SimpleMatrix(this.B);
		SimpleMatrix I = SimpleMatrix.identity(N);
		
		double p = 1.0/y.dot(s);

		SimpleMatrix A1 = I.minus(s.mult(y.transpose()).scale(p));
		SimpleMatrix A2 = I.minus(y.mult(s.transpose()).scale(p));
		SimpleMatrix SS = s.mult(s.transpose()).scale(p);
		SimpleMatrix M = A1.mult(B).mult(A2).plus(SS);

		this.B.set(M.getMatrix());
		
		System.out.println("B det = "+M.determinant());
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

	/**
	 * Helper function that lets converged and the final message bet set in one line
	 */
	private boolean terminateSearch( boolean converged , String message ) {
		this.hasConverged = converged;
		this.message = message;
		
		return true;
	}

	/**
	 * True if the line search converged to a solution
	 */
	public boolean isConverged() {
		return hasConverged;
	}

	/**
	 * Returns the warning message, or null if there is none
	 */
	public String getWarning() {
		return message;
	}
}
