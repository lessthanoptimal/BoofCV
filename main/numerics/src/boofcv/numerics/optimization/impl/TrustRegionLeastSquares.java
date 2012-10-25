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

package boofcv.numerics.optimization.impl;

import boofcv.numerics.optimization.functions.CoupledJacobian;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;
import org.ejml.ops.SpecializedOps;

/**
 * <p>
 * Trust region based optimization algorithms switch between the optimal Gauss Newton step and
 * gradient descent.  A circular region is defined around the current estimate for which the
 * Taylor series expansion is trusted.  If the Gauss Newton step falls within this "trust region"
 * then it is used, otherwise the optimal quadratic solution is found subject to the trust
 * region's constraint.
 * </p>
 *
 * <p>
 * There are several different ways for which to estimate the solution to the trust region problem.
 * The Levenberg-Marquardt algorithm is actually a type of trust region algorithm, even though
 * it was originally proposed before the trust region approach had been.
 * </p>
 *
 * @author Peter Abeles
 */
public class TrustRegionLeastSquares {

	// algorithm for computing the step
	private TrustRegionStep stepAlg;

	// error function at x
	private double fx;
	// the previous error
	private double fx_prev;

	// Jacobian
	private DenseMatrix64F J = new DenseMatrix64F(1,1);
	// Sample point
	private DenseMatrix64F x = new DenseMatrix64F(1,1);
	// Candidate sample point
	private DenseMatrix64F candidate = new DenseMatrix64F(1,1);
	// Step from x to sample point
	private DenseMatrix64F xdelta = new DenseMatrix64F(1,1);
	// Residual error function
	private DenseMatrix64F residuals = new DenseMatrix64F(1,1);
	// Residual error for the candidate point
	private DenseMatrix64F candidateResiduals = new DenseMatrix64F(1,1);
	// Gradient of residuals
	private DenseMatrix64F gradient = new DenseMatrix64F(1,1);

	// size of the current trust region
	private double regionRadius;

	// maximum size of the trust region
	private double maxRadius;

	// tolerance for termination. magnitude of gradient. absolute
	private double gtol;
	// tolerance for termination, change in function value.  relative
	private double ftol;

	// function being optimized
	private CoupledJacobian function;

	private int mode = 0;
	private boolean updated;

	public TrustRegionLeastSquares(double maxRadius, TrustRegionStep stepAlg) {
		this.maxRadius = maxRadius;
		this.stepAlg = stepAlg;
	}

	public void setFunction( CoupledJacobian function ) {
		this.function = function;

		int m = function.getM();
		int n = function.getN();

		x.reshape(n,1);
		candidate.reshape(n,1);
		xdelta.reshape(n,1);
		J.reshape(m,n);
		residuals.reshape(m,1);
		candidateResiduals.reshape(m,1);
		gradient.reshape(n, 1);

		stepAlg.init(n,m);
	}

	/**
	 * Specify convergence tolerances
	 *
	 * @param gtol absolute convergence tolerance based on gradient norm. 0 <= gtol
	 */
	public void setConvergence( double ftol , double gtol ) {
		if( ftol < 0 || ftol >= 1 )
			throw new IllegalArgumentException("0 <= ftol < 1");
		
		if( gtol < 0 )
			throw new IllegalArgumentException("gtol < 0 ");

		this.gtol = gtol;
		this.ftol = ftol;
	}

	public void initialize(double[] initial) {
		System.arraycopy(initial,0,x.data,0,x.numRows);

		function.setInput(x.data);

		function.computeFunctions(residuals.data);
		fx = cost(residuals);
		fx_prev = 0;
		regionRadius = maxRadius;
		mode = 0;
	}

	/**
	 * Performs a single iteration.
	 *
	 * @return true if it has converged and false if it has not.
	 */
	public boolean iterate() {

		updated = false;
		if( mode == 0 ) {
			// compute the Jacobian and gradient
			function.computeJacobian(J.data);
			CommonOps.multTransA(J, residuals, gradient);

			// check for convergence
			double gnorm = CommonOps.elementMaxAbs(gradient);

			if( gnorm <= gtol || Math.abs(fx-fx_prev) <= ftol*Math.max(fx,fx_prev) ) {
				mode = 2;
				return true;
			}

			//initialize step finding
			stepAlg.setInputs(x,residuals,J,gradient,fx);
			mode = 1;
			fx_prev = fx;

		} else if( mode == 1 ) {
			if( findStep() ) {
				mode = 0;
			}
		} else {
			throw new RuntimeException("Has already converged");
		}

		return false;
	}

	/**
	 * Solves for the step within allowed region and accepts the step if it
	 * is a large enough improvement.
	 *
	 * @return true if it found a good step
	 */
	private boolean findStep() {
		// solve for the step
		stepAlg.computeStep(regionRadius,xdelta);

		// evaluate the candidate point
		CommonOps.add(x,xdelta,candidate);
		function.setInput(candidate.data);
		function.computeFunctions(candidateResiduals.data);
		double fxp = cost(candidateResiduals);

		// ratio of predicted reduction versus actual reduction
		double actual = fx-fxp;
		double predicted = stepAlg.predictedReduction();

		// uncomment to sanity check predicted reduction
//		checkPredicted(predicted,xdelta);

		boolean acceptCandidate;

		if( actual == 0 || predicted == 0 ) {
			acceptCandidate = true;
		} else {
			double reductionRatio = actual/predicted;
//			System.out.println("      ratio "+reductionRatio);
			
			if( reductionRatio < 0.25 ) {
				// if the model is a poor predictor reduce the size of the trust region
				regionRadius = 0.5*regionRadius;
			} else {
				// only increase the size of the trust region if it is taking a step of maximum size
				// otherwise just assume it's doing good enough job
				if( reductionRatio > 0.75 ) {
					double r = NormOps.normF(xdelta);
					regionRadius = Math.max(regionRadius,3*r);
				}
			}

			acceptCandidate = reductionRatio > 0;
		}

		if( acceptCandidate ) {
			// make the candidate the current step
			DenseMatrix64F temp = x;
			x = candidate;
			candidate = temp;

			temp=candidateResiduals;
			candidateResiduals = residuals;
			residuals=temp;

			fx = fxp;
			updated = true;
		}

		return acceptCandidate;
	}

//	private void checkPredicted( double found , DenseMatrix64F step ) {
//		SimpleMatrix J = SimpleMatrix.wrap(this.J);
//		SimpleMatrix h = SimpleMatrix.wrap(step);
//
//		SimpleMatrix r = SimpleMatrix.wrap(residuals);
//
//		double z = SpecializedOps.elementSumSq(r.plus(J.mult(h)).getMatrix());
//		double expected = fx-0.5*z;
//
//		if( Math.abs(found-expected)/Math.max(found,expected) > 1e-3 ) {
//			System.out.println("found "+found+"  expected "+expected);
//			throw new RuntimeException("Predicted reduction is unexpected");
//		}
//	}

	/**
	 * Cost is equal to (1/2)*f(x)<sup>T</sup>*f(x)
	 */
	private double cost( DenseMatrix64F residuals ) {
		return 0.5*SpecializedOps.elementSumSq(residuals);
	}

	public double[] getParameters() {
		return x.data;
	}

	public double getError() {
		return fx;
	}

	public boolean isConverged() {
		return mode == 2;
	}

	public boolean isUpdated() {
		return updated;
	}
}