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

import boofcv.numerics.optimization.functions.GradientLineFunction;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

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
// TODO add optional sanity checks to make sure it is symmetric positive definite
// TODO make H's magnitude configurable
public class QuasiNewtonBFGS
{
	// number of inputs
	private int N;

	// convergence conditions
	double relativeErrorTol; // relative error tolerance
	double absoluteErrorTol; // absolute error tolerance

	// function being minimized and its gradient
	private GradientLineFunction function;

	// searches for a parameter that meets the Wolfe condition
	private LineSearchManager lineSearch;

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
	private DenseMatrix64F temp0_Nx1;
	private DenseMatrix64F temp1_Nx1;

	// mode that the algorithm is in
	private int mode;
	// error message
	private String message;
	// if it converged to a solution or not
	private boolean hasConverged;

	// How many full processing cycles have there been
	private int iterations;

	/**
	 * Configures the search.
	 *
	 * @param function Function being optimized
	 * @param lineSearch Line search that selects a solution that meets the Wolfe condition.
	 * @param relativeErrorTol Relative error termination condition. >= 0
	 * @param absoluteErrorTol Absolute error termination condition. >= 0
	 */
	public QuasiNewtonBFGS( GradientLineFunction function ,
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
		this.relativeErrorTol = relativeErrorTol;
		this.absoluteErrorTol = absoluteErrorTol;

		N = function.getN();
		
		B = new DenseMatrix64F(N,N);
		searchVector = new DenseMatrix64F(N,1);
		g = new DenseMatrix64F(N,1);
		s = new DenseMatrix64F(N,1);
		y = new DenseMatrix64F(N,1);
		x = new DenseMatrix64F(N,1);

		temp0_Nx1 = new DenseMatrix64F(N,1);
		temp1_Nx1 = new DenseMatrix64F(N,1);
	}

	/**
	 * Manually specify the initial inverse hessian approximation.
	 * @param Hinverse Initial hessian approximation
	 */
	public void setInitialHInv( DenseMatrix64F Hinverse) {
		B.set(Hinverse);
	}

	public void initialize(double[] initial) {
		this.mode = 0;
		this.hasConverged = false;
		this.message = null;
		this.iterations = 0;

		// set the change in x to be zero
		s.zero();
		// default to an initial inverse Hessian approximation as
		// the identity matrix.  This can be overridden or improved by an heuristic below
		CommonOps.setIdentity(B);

		// save the initial value of x
		System.arraycopy(initial, 0, x.data, 0, N);

		function.setInput(initial);
		fx = function.computeFunction();
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
//		System.out.println("QN iterations "+iterations);
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
//		System.out.println("funv = "+fx);
		// Compute the function's gradient
		function.computeGradient(temp0_Nx1.data);

		// compute the change in gradient
		for( int i = 0; i < N; i++ ) {
			y.data[i] = temp0_Nx1.data[i] - g.data[i];
			g.data[i] = temp0_Nx1.data[i];
		}

		// Update the inverse Hessian matrix
		if( iterations != 0 ) {
			EquationsBFGS.inverseUpdate(B, s, y, temp0_Nx1, temp1_Nx1);
		}

		// compute the search direction
		CommonOps.mult(-1,B,g, searchVector);

		// use the line search to find the next x
		lineSearch.initialize(fx, x.data,g.data, searchVector.data,1,N);

		mode = 1;
		iterations++;
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
