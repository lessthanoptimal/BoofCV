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

package boofcv.numerics.optimization;

import boofcv.numerics.optimization.funcs.EvalFuncHelicalValley;
import boofcv.numerics.optimization.funcs.EvalFuncLeastSquares;
import boofcv.numerics.optimization.funcs.EvalFuncRosenbrock;
import boofcv.numerics.optimization.impl.NumericalGradientForward;

/**
 * @author Peter Abeles
 */
public abstract class UnconstrainedMinimizationEvaluator {

	boolean verbose = true;
	int maxIteration = 200;

	/**
	 * Creates a line search algorithm
	 *
	 * @return Line search algorithm
	 */
	protected abstract UnconstrainedMinimization createSearch( double minimumValue );

	/**
	 * Run the line search algorithm on the two inputs and compute statistics
	 *
	 * @param func Function being searched
	 * @param deriv Derivative being searched
	 * @param initial Initial point
	 * @return statics
	 */
	private NonlinearResults performTest( FunctionNtoS func , FunctionNtoN deriv ,
										  double initial[] , double optimal[] , double minimValue)
	{
		if( deriv == null ) {
			deriv = new NumericalGradientForward(func);
		}

		CallCounterNtoS f = new CallCounterNtoS(func);
		CallCounterNtoN d = new CallCounterNtoN(deriv);

		UnconstrainedMinimization alg = createSearch(minimValue);
		alg.setFunction(f,d);

		double initialValue = func.process(initial);

		alg.initialize(initial);
		int iter = 0;
		for( iter = 0; iter < maxIteration && !alg.iterate() ; iter++ ){
			printError(optimal, alg);
		}
		printError(optimal, alg);
		System.out.println("*** total iterations = "+iter);
		double found[] = alg.getParameters();

		double finalValue = func.process(found);

		if( verbose ) {
			System.out.printf("value{ init %4.1e final = %6.2e} count f = %2d d = %2d\n",
					initialValue, finalValue, f.count, d.count);
		}

		NonlinearResults ret = new NonlinearResults();
		ret.numFunction = f.count;
		ret.numGradient = d.count;
		ret.f = finalValue;

		return ret;
	}

	private void printError(double[] optimal, UnconstrainedMinimization alg) {
		if( optimal != null ) {
			double x[] = alg.getParameters();
			double n = 0;
			for( int j = 0; j < x.length; j++ ) {
				double dx = x[j]-optimal[j];
				n += dx*dx;
			}
			System.out.println("||x(k)-x(*)|| = "+Math.sqrt(n));
		}
	}

	private NonlinearResults performTest( EvalFuncLeastSquares func ) {
		FunctionNtoS nl = new LsToNonLinear(func.getFunction());
		double[] initial = func.getInitial();
		
		System.out.println("optimal = "+nl.process(func.getOptimal()));

		return performTest(nl,null,initial,func.getOptimal(),0);
	}

	public NonlinearResults helicalValley() {
		return performTest(new EvalFuncHelicalValley());
	}

	public NonlinearResults rosenbrock() {
		return performTest(new EvalFuncRosenbrock());
	}
}
