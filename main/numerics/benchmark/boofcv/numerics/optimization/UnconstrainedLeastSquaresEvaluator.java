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

import boofcv.numerics.optimization.funcs.*;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import boofcv.numerics.optimization.impl.NumericalJacobianForward;

/**
 * @author Peter Abeles
 */
public abstract class UnconstrainedLeastSquaresEvaluator {

	boolean verbose = true;
	boolean printSummary;
	int maxIteration = 500;

	protected UnconstrainedLeastSquaresEvaluator(boolean verbose, boolean printSummary) {
		this.verbose = verbose;
		this.printSummary = printSummary;
	}

	/**
	 * Creates a line search algorithm
	 *
	 * @return Line search algorithm
	 */
	protected abstract UnconstrainedLeastSquares createSearch( double minimumValue );

	/**
	 * Run the line search algorithm on the two inputs and compute statistics
	 *
	 * @param func Function being searched
	 * @param deriv Derivative being searched
	 * @param initial Initial point
	 * @return statics
	 */
	private NonlinearResults performTest( FunctionNtoM func , FunctionNtoMxN deriv ,
										  double initial[] , double optimal[] , double minimValue)
	{
		if( deriv == null ) {
			deriv = new NumericalJacobianForward(func);
		}

		CallCounterNtoM f = new CallCounterNtoM(func);
		CallCounterNtoMxN d = new CallCounterNtoMxN(deriv);

		UnconstrainedLeastSquares alg = createSearch(minimValue);
		alg.setFunction(f,d);

		alg.initialize(initial,1e-10,1e-20);
		double initialError = alg.getFunctionValue();
		int iter;
		for( iter = 0; iter < maxIteration && !alg.iterate() ; iter++ ) {
			if( verbose && alg.isUpdated() ) {
				double error = alg.getFunctionValue();
				System.out.println("  error = "+error);
			}
		}
		if( verbose )
			System.out.println("*** total iterations = "+iter);
		double found[] = alg.getParameters();
		double finalError = alg.getFunctionValue();

		if( printSummary ) {
			// compute distance from optimal solution if one is provided
			double dist = Double.NaN;
			if( optimal != null ) {
				dist = 0;
				for( int i = 0; i < func.getN(); i++ ) {
					dist += Math.pow(found[i]-optimal[i],2);
				}
				dist = Math.sqrt(dist);
			}
			
			System.out.printf("value{ init %7.1e final = %7.2e} optimal %7.1e count f = %2d d = %2d\n",
					initialError, finalError, dist , f.count, d.count);
		}

		NonlinearResults ret = new NonlinearResults();
		ret.numFunction = f.count;
		ret.numGradient = d.count;
		ret.f = finalError;
		ret.x = found;

		return ret;
	}

	private NonlinearResults performTest( EvalFuncLeastSquares func ) {
		double[] initial = func.getInitial();

		return performTest(func.getFunction(),func.getJacobian(),initial,func.getOptimal(),0);
	}


	public NonlinearResults helicalValley() {
		return performTest(new EvalFuncHelicalValley());
	}

	public NonlinearResults rosenbrock() {
		return performTest(new EvalFuncRosenbrock());
	}

	public NonlinearResults rosenbrockMod( double lambda ) {
		return performTest(new EvalFuncRosenbrockMod(lambda));
	}

	public NonlinearResults variably() {
		return performTest(new EvalFuncVariablyDimensioned(10));
	}

	public NonlinearResults trigonometric() {
		return performTest(new EvalFuncTrigonometric(10));
	}
	public NonlinearResults badlyScaledBrown() {
		return performTest(new EvalFuncBadlyScaledBrown());
	}

	public NonlinearResults powell() {
		return performTest(new EvalFuncPowell());
	}
}
