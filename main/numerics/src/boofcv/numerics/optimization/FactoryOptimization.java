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

package boofcv.numerics.optimization;

import boofcv.numerics.optimization.impl.LevenbergDampened;
import boofcv.numerics.optimization.impl.LevenbergMarquardtDampened;
import boofcv.numerics.optimization.wrap.WrapLevenbergDampened;
import boofcv.numerics.optimization.wrap.WrapQuasiNewtonBFGS;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;

/**
 * Creates optimization algorithms using easy to use interfaces.  These implementations/interfaces
 * are designed to be easy to use and effective for most tasks.  If more control is needed then
 * create an implementation directly.
 *
 * @author Peter Abeles
 */
public class FactoryOptimization {

	/**
	 * <p>
	 * Creates a solver for the unconstrained minimization problem.  Here a function has N parameters
	 * and a single output.  The goal is the minimize the output given the function and its derivative.
	 * </p>
	 *
	 * @param relativeErrorTol Relative tolerance used to terminate the optimization. 0 <= x < 1
	 * @param absoluteErrorTol Absolute tolerance used to terminate the optimization. 0 <= x
	 * @param minFunctionValue The smallest possible value out of the function.  Sometimes used to bound
	 *                         the problem.
	 * @return UnconstrainedMinimization
	 */
	public static UnconstrainedMinimization unconstrained( double relativeErrorTol,
														   double absoluteErrorTol,
														   double minFunctionValue )
	{
		return new WrapQuasiNewtonBFGS(relativeErrorTol,absoluteErrorTol,minFunctionValue);
	}

	/**
	 * <p>
	 * Unconstrained least squares Levenberg-Marquardt (LM) optimizer for dense problems.  There are many
	 * different variants of LM and this function provides an easy to use interface for selecting and
	 * configuring them.  Scaling of function parameters and output might be needed to ensure good results.
	 * </p>
	 *
	 * @param relativeErrorTol tolerance used to terminate the optimization. 0 <= tol < 1
	 * @param absoluteErrorTol Absolute tolerance used to terminate the optimization. 0 <= tol
	 * @param dampInit Initial value of dampening parameter.  Tune.  Start at around 1e-3.
	 * @param robust If true a slower, more robust algorithm that can handle more degenerate cases will be used.
	 * @return UnconstrainedLeastSquares
	 */
	public static UnconstrainedLeastSquares leastSquaresLM( double relativeErrorTol,
															double absoluteErrorTol,
															double dampInit ,
															boolean robust )
	{
		LinearSolver<DenseMatrix64F> solver;

		if( robust ) {
			solver = LinearSolverFactory.solverPseudoInverse();
		} else {
			solver = LinearSolverFactory.symmPosDef(10);
		}

		LevenbergMarquardtDampened alg = new LevenbergMarquardtDampened(solver,
				dampInit,absoluteErrorTol,relativeErrorTol);
		return new WrapLevenbergDampened(alg);
	}

	/**
	 * <p>
	 * Unconstrained least squares Levenberg optimizer for dense problems.  Some times works better than
	 * </p>
	 *
	 * @param relativeErrorTol tolerance used to terminate the optimization. 0 <= tol < 1
	 * @param absoluteErrorTol Absolute tolerance used to terminate the optimization. 0 <= tol
	 * @param dampInit Initial value of dampening parameter.  Tune.  Start at around 1e-3.
	 * @return UnconstrainedLeastSquares
	 */
	public static UnconstrainedLeastSquares leastSquareLevenberg( double relativeErrorTol,
																  double absoluteErrorTol,
																  double dampInit )
	{

		LevenbergDampened alg = new LevenbergDampened(dampInit,absoluteErrorTol,relativeErrorTol);
		return new WrapLevenbergDampened(alg);
	}
}
