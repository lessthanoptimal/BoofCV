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

import boofcv.numerics.optimization.functions.FunctionStoS;
import boofcv.numerics.optimization.wrap.WrapCoupledDerivative;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a standard battery of tests against line search algorithms
 * 
 * @author Peter Abeles
 */
public abstract class LineSearchEvaluator {

	// should it output results to standard out
	boolean verbose;

	protected LineSearchEvaluator(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * Creates a line search algorithm
	 *
	 * @return Line search algorithm
	 */
	protected abstract LineSearch createSearch();

	/**
	 * Run the line search algorithm on the two inputs and compute statistics
	 * 
	 * @param func Function being searched
	 * @param deriv Derivative being searched
	 * @param initStep Initial step
	 * @return statics
	 */
	private Results performTest( FunctionStoS func , FunctionStoS deriv , 
								 double initStep , double minStep , double maxStep )
	{
		CallCounterStoS f = new CallCounterStoS(func);
		CallCounterStoS d = new CallCounterStoS(deriv);

		LineSearch alg = createSearch();
		alg.setFunction(new WrapCoupledDerivative(f,d));

		double valueZero = func.process(0);
		double derivZero = deriv.process(0);
		double valueInit = func.process(initStep);

		alg.init(valueZero,derivZero,valueInit,initStep,minStep,maxStep);
		
		for( int i = 0; i < 50 && !alg.iterate() ; i++ ){}
		double found = alg.getStep();
		double foundDeriv = deriv.process(found);

		if( verbose ) {
		System.out.printf("step{ init %4.1e final = %6.3f} deriv %9.2e  count f = %2d d = %2d\n", 
				initStep, found,foundDeriv, f.count, d.count);
		}

		Results ret = new Results();
		ret.numIterations = d.count;
		ret.deriv = foundDeriv;
		ret.f = func.process(found);
		ret.deriv = foundDeriv;
		ret.x = found;
		
		return ret;
	}

	/**
	 * Processes and compile results for the function at the specified initial steps
	 */
	private List<Results> process( FunctionStoS f , FunctionStoS g , double ...initSteps )
	{
		List<Results> results = new ArrayList<Results>();
		
		for( double step : initSteps ) {
			results.add(performTest(f,g,step,0,Double.POSITIVE_INFINITY));
		}

		return results;
	}

	private List<Results> processMore( FunctionStoS f , FunctionStoS g , double ...initSteps )
	{
		List<Results> results = new ArrayList<Results>();

		for( double step : initSteps ) {
			double maxStep = 4.0*Math.max(1,step);
			results.add(performTest(f,g,step,0,maxStep));
		}

		return results;
	}

	public List<Results> fletcher1() {
		FunctionStoS f = new FletcherFunction1();
		FunctionStoS g = new FletcherDerivative1();

		return process(f,g,0.1,1);
	}

	public List<Results> more1() {
		FunctionStoS f = new MoreFunction1(2);
		FunctionStoS g = new MoreDerivative1(2);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
	}

	public List<Results> more2() {
		FunctionStoS f = new MoreFunction2(0.004);
		FunctionStoS g = new MoreDerivative2(0.004);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
	}

	public List<Results> more3() {
		FunctionStoS f = new MoreFunction3(39,0.01);
		FunctionStoS g = new MoreDerivative3(39,0.01);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
	}

	public List<Results> more4() {
		FunctionStoS f = new MoreFunction4(0.001,0.001);
		FunctionStoS g = new MoreDerivative4(0.001,0.001);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
	}

	public List<Results> more5() {
		FunctionStoS f = new MoreFunction4(0.01,0.001);
		FunctionStoS g = new MoreDerivative4(0.01,0.001);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
	}

	public List<Results> more6() {
		FunctionStoS f = new MoreFunction4(0.001,0.01);
		FunctionStoS g = new MoreDerivative4(0.001,0.01);

		return processMore(f, g, 1e-3, 1e-1, 10, 1e3);
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
		public double process(double x) {
			return  (x*x-beta)/Math.pow(x*x+beta,2);
		}
	}

	private static class MoreFunction2 implements FunctionStoS
	{
		double beta;

		private MoreFunction2(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double x) {
			double t = x + beta;
			return Math.pow(t,5) - 2*Math.pow(t,4);
		}
	}

	private static class MoreDerivative2 implements FunctionStoS
	{
		double beta;

		private MoreDerivative2(double beta) {
			this.beta = beta;
		}

		@Override
		public double process(double x) {
			double t = x + beta;
			return 5*Math.pow(t,4) - 8*Math.pow(t,3);
		}
	}

	private static class MoreFunction3 implements FunctionStoS
	{
		double l,beta;

		private MoreFunction3(double l , double beta) {
			this.l = l;
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			double right = (2.0*(1-beta)/(l*Math.PI))*Math.sin((l*Math.PI)*input/2.0);
			return func(input) + right;
		}
		
		private double func( double input ) {
			if( input <= 1 - beta ) {
				return 1-input;
			} else if( input >= 1 + beta ) {
				return input - 1;
			} else {
				return (1.0/(2.0*beta))*Math.pow(input-1,2.0) + beta/2.0;
			}
		}
	}

	private static class MoreDerivative3 implements FunctionStoS
	{
		double l,beta;

		private MoreDerivative3(double l , double beta) {
			this.l = l;
			this.beta = beta;
		}

		@Override
		public double process(double input) {
			double right = (1 - beta) * Math.cos((l*Math.PI)*input/2.0);
			return func(input) + right;
		}

		private double func( double input ) {
			if( input <= 1 - beta ) {
				return -1;
			} else if( input >= 1 + beta ) {
				return 1;
			} else {
				return (1.0/beta)*(input-1);
			}
		}
	}

	private static class MoreFunction4 implements FunctionStoS
	{
		double b1,b2;

		private MoreFunction4(double b1 , double b2) {
			this.b1 = b1;
			this.b2 = b2;
		}

		@Override
		public double process(double x) {
			double t1 = gamma(b1);
			double t2 = gamma(b2);
			double f = t1*Math.sqrt(Math.pow(1-x,2)+b2*b2) + t2*Math.sqrt(x*x+b1*b1);
			return f;
		}
		
		private double gamma( double b ) {
			return Math.sqrt(1+b*b) - b;
		}
	}
	private static class MoreDerivative4 implements FunctionStoS
	{
		double b1,b2;

		private MoreDerivative4(double b1 , double b2) {
			this.b1 = b1;
			this.b2 = b2;
		}

		@Override
		public double process(double x) {
			double t1 = gamma(b1);
			double t2 = gamma(b2);

			return -t1*((1-x)/Math.sqrt(Math.pow(1-x,2)+b2*b2)) +
					+ t2*(x/Math.sqrt(x*x+b1*b1));
		}

		private double gamma( double b ) {
			return Math.sqrt(1+b*b) - b;
		}
	}
	
	public static class Results
	{
		// number of iterations it took
		public int numIterations;
		// value of the function at the output
		public double f;
		// value of the derivative at the output
		public double deriv;
		// final step
		public double x;
	}
}
