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

import boofcv.numerics.optimization.OptimizationException;
import boofcv.numerics.optimization.functions.CoupledJacobian;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.NormOps;

/**
 * <p>
 * Implementation of Levenberg-Marquardt which explicitly computes the J(x)'J(x) matrix and iteratively
 * adjusts the dampening parameter.  If a candidate sample point decreases the score then it is accepted
 * ad the dampening parameter adjusted according to [1].  Because it immediately accepts any decrease
 * it will tend to require more function and gradient calculations and less linear solutions. Explicitly
 * computing J(x)'J(x) improves the linear solver's speed at the cost of some numerical precision.
 * </p>
 *
 * <p>
 * The step 'x' is computed using the following formula:
 * (J(k)'*J(k) + &mu;I)x = -g = -J'*f<br>
 * where J is the Jacobian, &mu; is the damping coefficient, I is an identify matrix, g is the gradient,
 * f is the functions output.
 * </p>
 *
 * <p>
 * The linear solver it uses is specified in the constructor.  Cholesky based solver will be the fastest
 * but can fail if the J(x)'J(x) matrix is nearly singular.  In those situations a pseudo inverse type
 * solver should be used, which is immune to that type of problem but much more expensive.
 * </p>
 *
 * <p>
 * [1] K. Madsen and H. B. Nielsen and O. Tingleff, "Methods for Non-Linear Least Squares Problems (2nd ed.)"
 * Informatics and Mathematical Modelling, Technical University of Denmark
 * </p>
 *
 * @author Peter Abeles
 */
// TODO add scaling variables
public class LevenbergMarquardtDampened {

	// number of parameters
	private int N;
	// number of functions
	private int M;

	// Least-squares Function being optimized
	private CoupledJacobian function;

	// tolerance for termination
	private double absoluteErrorTol;
	private double relativeErrorTol;
	
	// current set of parameters being considered
	private DenseMatrix64F x;

	// Current x being considered
	private DenseMatrix64F xtest;
	private DenseMatrix64F xdelta;

	// function residuals values at x
	private DenseMatrix64F funcVals;
	// jacobian at x
	private DenseMatrix64F jacobianVals;

	// B=J'*J
	private DenseMatrix64F B;
	// y=-J'*r
	private DenseMatrix64F g;
	// B plus dampening
	private DenseMatrix64F Btest;

	// solver used to compute (A + mu*diag(A))d = g
	private LinearSolver<DenseMatrix64F> solver;

	// function value norm at x
	private double fnorm;
	private double fnormPrev;

	// size of the step taken
	private double step;

	// levenberg marquardt dampening parameter
	private double dampParam;
	// used to scale the dampening parameter
	private double v;

	// is it searching for a new dampening parameter or setting up the next iteration
	private int mode;

	// has it converged or not
	private boolean hasConverged;
	// warning message
	private String message;
	
	// total number of iterations
	private int iterationCount;

	/**
	 * Specifies termination condition and linear solver.  Selection of the linear solver an effect
	 * speed and robustness.
	 *
	 * @param solver Linear solver. Cholesky or pseudo-inverse are recommended.
	 * @param absoluteErrorTol Absolute convergence test.
	 * @param relativeErrorTol Relative convergence test based on function magnitude.
	 */
	public LevenbergMarquardtDampened(LinearSolver<DenseMatrix64F> solver,
									  double absoluteErrorTol,
									  double relativeErrorTol) {
		this.solver = solver;
		this.absoluteErrorTol = absoluteErrorTol;
		this.relativeErrorTol = relativeErrorTol;

		if( solver.modifiesA() || solver.modifiesB() )
			this.solver = new LinearSolverSafe<DenseMatrix64F>(solver);
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

		x = new DenseMatrix64F(N,1);
		xdelta = new DenseMatrix64F(N,1);
		xtest = new DenseMatrix64F(N,1);
		funcVals = new DenseMatrix64F(M,1);
		jacobianVals = new DenseMatrix64F(M,N);

		B = new DenseMatrix64F(N,N);
		g = new DenseMatrix64F(N,1);

		Btest = new DenseMatrix64F(N,N);
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
		fnorm = NormOps.normF(funcVals);

		hasConverged = false;
		mode = 0;
		fnormPrev = 0;
		dampParam = 1;
		step = 0;
		v = 2;
		iterationCount = 0;
	}

	public double[] getParameters() {
		return x.data;
	}

	/**
	 * Performs a single step in the optimization.
	 *
	 * @return true if the optimization has finished.
	 * @throws OptimizationException
	 */
	public boolean iterate() throws OptimizationException {
		if( mode == 0 ) {
			return initSamplePoint();
		} else {
			computeStep();
		}

		return false;
	}

	/**
	 * Evaluates the Jacobian, computes the gradient and Hessian approximation.  Checks for convergence.
	 *
	 * @return true if it has converged
	 */
	private boolean initSamplePoint() {
		System.out.println("-------- init sample");
		// calculate the Jacobian values at the current sample point
		function.computeJacobian(jacobianVals.data);

		// compute helper matrices
		CommonOps.multTransA(jacobianVals, jacobianVals, B); // todo take advantage of symmetry
		CommonOps.multTransA(jacobianVals,funcVals, g);
		CommonOps.scale(-1, g);

//		System.out.println("--------- Jacobian");
//		jacobianVals.print();
//		System.out.println("--------- B");
//		B.print();

		// calculate the function and jacobean norm
		fnorm = NormOps.normF(funcVals);
		double gx = CommonOps.elementMaxAbs(g);

		System.out.println(" gx = "+gx);
		
		// check for absolute convergence
		if( Math.abs(fnorm-fnormPrev) <= absoluteErrorTol && step*Math.abs(gx) <= absoluteErrorTol )
			return terminateSearch(true, null);

		// check for relative convergence
		if( Math.abs(fnorm-fnormPrev) <= relativeErrorTol*Math.abs(fnorm)
				&& step*Math.abs(gx) <= relativeErrorTol*Math.abs(fnorm) )
			return terminateSearch(true, null);

		// todo replace with just saving the diagonal elements
		Btest.set(B);
		mode = 1;
		return false;
	}

	private void computeStep() {
		// add dampening parameter
		for( int i = 0; i < N; i++ ) {
			int index = Btest.getIndex(i,i);
			Btest.data[index] = (1+dampParam)*B.data[index];
		}

		// compute the change in step.
		if( !solver.setA(Btest) ) {
			// the matrix is singular, which can only be caused by a gradient with zero values
			throw new OptimizationException("Singular matrix encountered.  Use psuedo inverse solver instead");
		} else {
			// solve for change in x
			solver.solve(g,xdelta);

			// xtest = x + delta x
			CommonOps.add(x, xdelta, xtest);

			// compute the residuals at x
			function.setInput(xtest.data);
			function.computeFunctions(funcVals.data);

			// actual reduction
			double ftestnorm = NormOps.normF(funcVals);
			double actualReduction = fnorm - ftestnorm;

			// Predicted reduction
			double predictedReduction = predictedReduction(xdelta,dampParam);

			// reduction ratio
			double ratio = actualReduction/predictedReduction;

			// update the dampParam depending on the results
			if( ratio > 0 && actualReduction > 0 ) {
				// set the test point to be the new point
				DenseMatrix64F temp = x;
				x = xtest; xtest = temp;
				// updated residual norm
				fnormPrev = fnorm;
				fnorm = ftestnorm;
				// update step magnitude
				step = NormOps.normF(xdelta);

				// reduce the amount of dampening
				dampParam *= Math.max(0.333,1-Math.pow(2*ratio-1,3));
				v = 2;

				System.out.println("Damp adjust: "+dampParam+"  ratio "+ratio);

				// start the iteration over again
				mode = 0;
				iterationCount++;
			} else {
				// increase the dampening and try again
				dampParam *= v;
				v *= 2;
				
				System.out.println("Damp Up: "+dampParam);
			}
		}
	}

	/**
	 * compute the change predicted by the model
	 *
	 * m_k(0) - m_k(p_k) = -g_k'*p - 0.5*p'*B*p
	 * (J'*J+mu*diag(J'*J))*p = -J'*r = -g
	 * 
	 * @return predicted reduction
	 */
	private double predictedReduction( DenseMatrix64F p, double mu ) {

		double p_dot_g = VectorVectorMult.innerProd(p,g);
		double p_JJ_p = 0;
		for( int i = 0; i < N; i++ )
			p_JJ_p += p.data[i]*B.get(i,i)*p.data[i];

		// The variable g is really the negative of g
		return 0.5*(p_dot_g-mu*p_JJ_p);
	}

	/**
	 * Helper function that lets converged and the final message bet set in one line
	 */
	private boolean terminateSearch( boolean converged , String message ) {
		this.hasConverged = converged;
		this.message = message;

		return true;
	}

	public boolean isConverged() {
		return hasConverged;
	}

	public String getWarning() {
		return message;
	}

	public int getIterationCount() {
		return iterationCount;
	}
}
