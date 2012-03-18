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
import org.ejml.UtilEjml;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.alg.dense.linsol.LinearSolverSafe;
import org.ejml.alg.dense.mult.MatrixMultProduct;
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

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
public class LevenbergDampened extends LevenbergDenseBase {

	// solver used to compute (A + mu*diag(A))d = g
	protected LinearSolver<DenseMatrix64F> solver;

	/**
	 * Specifies termination condition and dampening parameter
	 *
	 * @param initialDampParam Initial value of the dampening parameter.  Tune.. try 1e-3;
	 * @param absoluteErrorTol Absolute convergence test.
	 * @param relativeErrorTol Relative convergence test based on function magnitude.
	 */
	public LevenbergDampened(double initialDampParam, double absoluteErrorTol, double relativeErrorTol) {
		super(initialDampParam, absoluteErrorTol, relativeErrorTol);
	}


	@Override
	protected void computeJacobian( DenseMatrix64F residuals , DenseMatrix64F gradient) {
		// calculate the Jacobian values at the current sample point
		function.computeJacobian(jacobianVals.data);

		// compute helper matrices
		// B = J'*J;   g = J'*r
		// Take advantage of symmetry when computing B and only compute the upper triangular
		// portion used by cholesky decomposition
		MatrixMultProduct.inner_reorder_upper(jacobianVals, B);
		CommonOps.multTransA(jacobianVals, residuals, gradient);

		// extract diagonal elements from B
		CommonOps.extractDiag(B, Bdiag);
	}

	@Override
	protected boolean computeStep(double lambda, DenseMatrix64F gradientNegative , DenseMatrix64F step) {
		// add dampening parameter
		for( int i = 0; i < N; i++ ) {
			int index = B.getIndex(i,i);
			B.data[index] = Bdiag.data[i] + lambda;
		}

		// compute the change in step.
		if( solver.setA(B) ) {
			if( solver.quality() > UtilEjml.EPS ) {

				// solve for change in x
				solver.solve(gradientNegative, step);

				return true;
			}
		}
		return false;
	}

	/**
	 * Specifies function being optimized.
	 *
	 * @param function Computes residuals and Jacobian.
	 */
	@Override
	public void setFunction( CoupledJacobian function ) {
		super.setFunction(function);

		solver = LinearSolverFactory.symmPosDef(N);
		if( solver.modifiesB() )
			this.solver = new LinearSolverSafe<DenseMatrix64F>(solver);
	}

	/**
	 * compute the change predicted by the model
	 *
	 * m_k(0) - m_k(p_k) = -g_k'*p - 0.5*p'*B*p
	 * (J'*J+mu*I)*p = -J'*r = -g
	 *
	 * @return predicted reduction
	 */
	@Override
	protected double predictedReduction( DenseMatrix64F param, DenseMatrix64F gradientNegative , double mu ) {
		double p_dot_p = VectorVectorMult.innerProd(param,param);
		double p_dot_g = VectorVectorMult.innerProd(param,gradientNegative);
		return 0.5*(mu*p_dot_p + p_dot_g);
	}

}
