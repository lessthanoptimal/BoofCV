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

/**
 * Runs a standard battery of tests against line search algorithms
 * 
 * @author Peter Abeles
 */
public abstract class LineSearchEvaluator {
	
	protected abstract LineSearch createSearch();
	
	private void performTest( FunctionStoS func , FunctionStoS deriv ,
							  double alpha0 )
	{
		CallCounterStoS f = new CallCounterStoS(func);
		CallCounterStoS d = new CallCounterStoS(deriv);

		LineSearch alg = createSearch();
		alg.setFunction(f,d);

		double valueZero = func.process(0);
		double derivZero = deriv.process(0);
		double valueInit = func.process(alpha0);

		alg.init(valueZero,derivZero,valueInit,alpha0);
		
		for( int i = 0; i < 50 && !alg.iterate() ; i++ ){}
		double found = alg.getStep();
		double foundDeriv = deriv.process(found);
	
		System.out.printf("alpha %f found = %f deriv %5.2e  count f = %d d = %d\n", alpha0, found,foundDeriv, f.count, d.count);
	}

	public void fletcher1() {
		FunctionStoS f = new FletcherFunction1();
		FunctionStoS g = new FletcherDerivative1();

		performTest(f,g,0.1);
		performTest(f,g,1);
	}

	public void more1() {
		FunctionStoS f = new MoreFunction1(2);
		FunctionStoS g = new MoreDerivative1(2);

		performTest(f,g,1e-3);
		performTest(f,g,1e-1);
		performTest(f,g,10);
		performTest(f,g,1e3);
	}

	public void more2() {
		FunctionStoS f = new MoreFunction2(0.004);
		FunctionStoS g = new MoreDerivative2(0.004);

		performTest(f,g,1e-3);
		performTest(f,g,1e-1);
		performTest(f,g,10);
		performTest(f,g,1e3);
	}

	private static class FletcherFunction1 implements FunctionStoS
	{
		@Override
		public double process(double alpha) {
			return 100*Math.pow(alpha,4) + Math.pow(1-alpha,2);
		}
	}

	private static class FletcherDerivative1 implements FunctionStoS
	{
		@Override
		public double process(double alpha) {
			return 400*Math.pow(alpha,3) -2*(1-alpha);
		}
	}
	
	private static class MoreFunction1 implements FunctionStoS
	{
		double beta;

		private MoreFunction1(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			return -input/(input*input + beta);
		}
	}

	private static class MoreDerivative1 implements FunctionStoS
	{
		double beta;

		private MoreDerivative1(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			double input2 = input*input;
			double inner = input2+beta;

			return -1/inner + 2*input2*Math.pow(inner,-2);
		}
	}

	private static class MoreFunction2 implements FunctionStoS
	{
		double beta;

		private MoreFunction2(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			return Math.pow(input+beta,5) - 2*Math.pow(input+beta,4);
		}
	}

	private static class MoreDerivative2 implements FunctionStoS
	{
		double beta;

		private MoreDerivative2(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			return 5*Math.pow(input+beta,4) - 8*Math.pow(input+beta,3);
		}
	}
}
