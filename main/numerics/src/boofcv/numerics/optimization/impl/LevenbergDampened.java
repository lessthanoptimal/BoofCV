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

import boofcv.numerics.optimization.OptimizationException;
import boofcv.numerics.optimization.functions.CoupledJacobian;
import org.ejml.UtilEjml;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

/**
 * <p>
 * Implementation of Levenberg's algorithm which explicitly computes the J(x)'J(x) matrix and iteratively
 * adjusts the dampening parameter.  If a candidate sample point decreases the score then it is accepted
 * ad the dampening parameter adjusted according to [1].  Because it immediately accepts any decrease
 * it will tend to require more function and gradient calculations and less linear solutions. Explicitly
 * computing J(x)'J(x) improves the linear solver's speed at the cost of some numerical precision.
 * </p>
 *
 * <p>
 * The step 'x' is computed using the following formula:
 * (J(k)'*J(k) + &mu;*I)x = -g = -J'*f<br>
 * where J is the Jacobian, &mu; is the damping coefficient, I is an identify matrix, g is the gradient,
 * f is the functions output.
 * </p>
 *
 * <p>
 * Unlike some implementations, the option for a scaling matrix is not provided.  Scaling can be done inside
 * the function itself and would add even more complexity to the code. The dampening parameter is updated
 * using the equation below from [1]: <br>
 * damp = damp * max( 1/3 , 1 - (2*ratio-1)^3 )<br>
 * where ratio is the actual reduction over the predicted reduction.
 * </p>
 *
 * <p>
 * [1] K. Madsen and H. B. Nielsen and O. Tingleff, "Methods for Non-Linear Least Squares Problems (2nd ed.)"
 * Informatics and Mathematical Modelling, Technical University of Denmark
 * </p>
 *
 * @author Peter Abeles
 */
// After some minor modifications it was compared against Matlab code in [1] and produced identical results
// in each step.  Stopping conditions and initialization is a bit different.
public class LevenbergDampened {

	// number of parameters
	protected int N;
	// number of functions
	protected int M;

	// Least-squares Function being optimized
	private CoupledJacobian function;

	// tolerance for termination
	private double absoluteErrorTol;
	private double relativeErrorTol;

	// current set of parameters being considered
	private DenseMatrix64F x = new DenseMatrix64F(1,1);

	// Current x being considered
	private DenseMatrix64F xtest = new DenseMatrix64F(1,1);
	protected DenseMatrix64F xdelta = new DenseMatrix64F(1,1);

	// function residuals values at x
	private DenseMatrix64F funcVals = new DenseMatrix64F(1,1);
	// jacobian at x
	private DenseMatrix64F jacobianVals = new DenseMatrix64F(1,1);

	// B=J'*J
	protected DenseMatrix64F B = new DenseMatrix64F(1,1);
	// diagonal elements of B
	protected DenseMatrix64F Bdiag = new DenseMatrix64F(1,1);
	// y=-J'*r
	protected DenseMatrix64F g = new DenseMatrix64F(1,1);

	// solver used to compute (A + mu*diag(A))d = g
	protected LinearSolver<DenseMatrix64F> solver;

	// function value norm at x
	private double fnorm;
	private double fnormPrev;

	// size of the step taken
	private double step;

	// levenberg marquardt dampening parameter
	protected double dampParam;
	private double initialDampParam;

	// used to scale the dampening parameter
	private double nu;

	// is it searching for a new dampening parameter or setting up the next iteration
	private int mode;

	// has it converged or not
	private boolean hasConverged;

	// total number of iterations
	private int iterationCount;

	// message explaining failure
	private String message;

	/**
	 * Specifies termination condition and linear solver.  Selection of the linear solver an effect
	 * speed and robustness.
	 *
	 * @param initialDampParam Initial value of the dampening parameter.  Tune.. try 1e-3;
	 * @param absoluteErrorTol Absolute convergence test.
	 * @param relativeErrorTol Relative convergence test based on function magnitude.
	 */
	public LevenbergDampened(double initialDampParam,
							 double absoluteErrorTol,
							 double relativeErrorTol) {
		this.initialDampParam = initialDampParam;
		this.absoluteErrorTol = absoluteErrorTol;
		this.relativeErrorTol = relativeErrorTol;

	}

	/**
	 * Constructor which allows the solver to be specified.
	 */
	protected LevenbergDampened(LinearSolver<DenseMatrix64F> solver,
							 double initialDampParam,
							 double absoluteErrorTol,
							 double relativeErrorTol) {
		this(initialDampParam,absoluteErrorTol,relativeErrorTol);
		this.solver = solver;
		if( solver.modifiesA() || solver.modifiesB() )
			this.solver = new LinearSolverSafe<DenseMatrix64F>(solver);
	}

	/**
	 * Specify the initial value of the dampening parameter.
	 *
	 * @param initialDampParam initial value
	 */
	public void setInitialDampParam(double initialDampParam) {
		this.initialDampParam = initialDampParam;
	}

	/**
	 * Specifies function being optimized.
	 *
	 * @param function Computes residuals and Jacobian.
	 */
	public void setFunction( CoupledJacobian function ) {

		this.function = function;

		this.N = function.getN();
		this.M = function.getM();

		x.reshape(N, 1, false);
		xdelta.reshape(N, 1, false);
		xtest.reshape(N,1,false);
		funcVals.reshape(M,1,false);
		jacobianVals.reshape(M,N,false);

		B.reshape(N,N,false);
		Bdiag.reshape(N,1,false);
		g.reshape(N,1,false);

		if( solver == null ) {
			solver = LinearSolverFactory.symmPosDef(N);
			if( solver.modifiesA() || solver.modifiesB() )
				this.solver = new LinearSolverSafe<DenseMatrix64F>(solver);
		}
	}

	/**
	 * Sets the initial parameter being searched.
	 *
	 * @param initial
	 */
	public void initialize(double[] initial) {
		if( initial.length < N)
			throw new IllegalArgumentException("Expected N="+N+" parameters");
		System.arraycopy(initial,0,x.data,0,N);

		// calculate residuals
		function.setInput(initial);
		function.computeFunctions(funcVals.data);
		// error at this point
		fnorm = computeError();

		hasConverged = false;
		mode = 0;
		fnormPrev = 0;
		dampParam = initialDampParam;
		step = 0;
		nu = 2;
		iterationCount = 0;
	}

	public double[] getParameters() {
		return x.data;
	}

	/**
	 * Performs a single step in the optimization.
	 *
	 * @return true if the optimization has finished.
	 * @throws boofcv.numerics.optimization.OptimizationException
	 */
	public boolean iterate() throws OptimizationException {
		if( mode == 0 ) {
			return initSamplePoint();
		} else {
			if( !computeStep() )
				return true;
		}

		return false;
	}

	/**
	 * Evaluates the Jacobian, computes the gradient and Hessian approximation.  Checks for convergence.
	 *
	 * @return true if it has converged
	 */
	private boolean initSamplePoint() {
		// calculate the Jacobian values at the current sample point
		function.computeJacobian(jacobianVals.data);

		// compute helper matrices
		// B = J'*J;   g = J'*r
		CommonOps.multTransA(jacobianVals, jacobianVals, B); // todo take advantage of symmetry
		CommonOps.multTransA(jacobianVals, funcVals, g);
		CommonOps.scale(-1, g);

		// calculate the Jacobian norm, function norm already computed
		double gx = CommonOps.elementMaxAbs(g);

		// check for absolute convergence
		if( Math.abs(fnorm-fnormPrev) <= absoluteErrorTol && step*Math.abs(gx) <= absoluteErrorTol )
			return terminateSearch(true, null);

		// check for relative convergence
		if( Math.abs(fnorm-fnormPrev) <= relativeErrorTol*Math.abs(fnorm)
				&& step*Math.abs(gx) <= relativeErrorTol*Math.abs(fnorm) )
			return terminateSearch(true, null);

		// extract diagonal elements from B
		CommonOps.extractDiag(B, Bdiag);

		// some recommend this initialization method.
//		if( iterationCount == 0 ) {
//			dampParam = initialDampParam*CommonOps.elementMaxAbs(Bdiag);
//		}

		mode = 1;
		return false;
	}

	private boolean computeStep() {
		// Solves for xdelta
		// (B + mu*I)xdelta = -g
		// where mu is dampParam
		if( !solveForXDelta() )
			return false;

		// xtest = x + delta x
		CommonOps.add(x, xdelta, xtest);
		// take in account rounding error
		CommonOps.sub(xtest, x, xdelta);

		// compute the residuals at x
		function.setInput(xtest.data);
		function.computeFunctions(funcVals.data);

		// actual reduction
		double ftestnorm = computeError();
		double actualReduction = fnorm - ftestnorm;

		// Predicted reduction
		double predictedReduction = predictedReduction(xdelta,dampParam);

		// update the dampParam depending on the results
		if( predictedReduction > 0 && actualReduction >= 0 ) {
			// set the test point to be the new point
			DenseMatrix64F temp = x;
			x = xtest; xtest = temp;
			// updated residual norm
			fnormPrev = fnorm;
			fnorm = ftestnorm;
			// update step magnitude
			step = NormOps.normF(xdelta);

			// reduction ratio
			double ratio = actualReduction/predictedReduction;
			// reduce the amount of dampening.  Magic equation from [1].  My attempts to improve
			// upon it have failed.  It is truly magical.
			dampParam *= Math.max(0.3333333,1-Math.pow(2*ratio-1,3));

			nu = 2;

			// start the iteration over again
			mode = 0;
			iterationCount++;
		} else {
			//  did not improve, increase the amount of dampening
			dampParam *= nu;
			nu *= 2;
		}
		
		return true;
	}

	/**
	 * Sets up the linear system to find the change in x.  If the solver fails or is nearly singular
	 * then the dampParam is increased.
	 */
	protected boolean solveForXDelta() {
		double max = CommonOps.elementMax(Bdiag);

		// if the matrix is null do a simple gradient descent search
		if( max == 0 ) {
			for( int i = 0; i < N; i++ ) {
				xdelta.data[i] = g.data[i]/dampParam;
			}
			return true;
		}

		// Adjust the dampening parameter until the solution can be solved
		// This is not designed to take advantage of QR decomposition, which is why cholesky is recommended
		boolean failed = true;
		for( int iter = 0; iter < 1000 && failed; iter++ ) {
			// add dampening parameter
			for( int i = 0; i < N; i++ ) {
				int index = B.getIndex(i,i);
				B.data[index] = Bdiag.data[i] + dampParam;
			}
			
			// compute the change in step.
			if( solver.setA(B) ) {
				if( solver.quality() > UtilEjml.EPS ) {
					failed = false;
				}
			}
			if( failed ) {
				dampParam = Math.max(10*dampParam,max*UtilEjml.EPS);
			}
		}
		if( failed ) {
			message = "Failed to find dampParam which cold be solved";
			return false;
		}

		// solve for change in x
		solver.solve(g, xdelta);

		return true;
	}

	/**
	 * compute the change predicted by the model
	 *
	 * m_k(0) - m_k(p_k) = -g_k'*p - 0.5*p'*B*p
	 * (J'*J+mu*I)*p = -J'*r = -g
	 *
	 * @return predicted reduction
	 */
	protected double predictedReduction( DenseMatrix64F p, double mu ) {
		double p_dot_p = VectorVectorMult.innerProd(p,p);
		double p_dot_g = VectorVectorMult.innerProd(p,g);
		return 0.5*(mu*p_dot_p + p_dot_g);
	}

	/**
	 * Helper function that lets converged and the final message bet set in one line
	 */
	private boolean terminateSearch( boolean converged , String message ) {
		this.hasConverged = converged;

		return true;
	}

	/**
	 * Computes the residual's error:
	 *
	 * sum_i 0.5*fi(x)^2
	 */
	private double computeError() {
		return VectorVectorMult.innerProd(funcVals,funcVals)/2;
	}

	public boolean isConverged() {
		return hasConverged;
	}

	public int getIterationCount() {
		return iterationCount;
	}

	public String getMessage() {
		return message;
	}
}
