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

package boofcv.numerics.optimization.wrap;

import boofcv.numerics.optimization.functions.FunctionNtoN;
import boofcv.numerics.optimization.functions.FunctionNtoS;
import boofcv.numerics.optimization.functions.FunctionStoS;
import boofcv.numerics.optimization.functions.GradientLineFunction;
import boofcv.numerics.optimization.impl.NumericalDerivativeForward;
import boofcv.numerics.optimization.impl.NumericalGradientForward;

/**
 * Numerically computes the gradient and line derivative.  Results are cached independently for function output,
 * gradient, and line derivative.
 *
 * @author Peter Abeles
 */
// todo use already computed function value for gradient and derivative computation
public class CachedNumericalGradientLineFunction implements GradientLineFunction {

	// number of parameters
	protected int N;

	// description of line search
	protected double []start;
	protected double []direction;

	// has the output already been computed at the current position
	protected boolean cachedFunction;
	protected boolean cachedGradient;
	protected boolean cachedDerivative;

	// current input parameters
	protected double[] currentInput;
	// current gradient and function output
	protected double[] currentGradient;
	protected double currentOutput;
	protected double currentStep;
	protected double currentDerivative;

	// input functions
	protected FunctionNtoS function;
	protected FunctionNtoN gradient;
	protected FunctionStoS lineDerivative;

	public CachedNumericalGradientLineFunction(FunctionNtoS function ) {
		this.function = function;
		this.N = function.getN();
	
		this.gradient = new NumericalGradientForward(function);
		FunctionStoS lineFunction = new LineFunction();
		this.lineDerivative = new NumericalDerivativeForward(lineFunction);

		currentInput = new double[N];
		currentGradient = new double[N];
	}

	@Override
	public void setLine(double[] start, double[] direction) {
		this.start = start;
		this.direction = direction;
	}

	@Override
	public void setInput(double x) {
		for( int i = 0; i < N; i++ ) {
			currentInput[i] = start[i] + x*direction[i];
		}
		currentStep = x;
		cachedFunction = false;
		cachedGradient = false;
		cachedDerivative = false;
	}

	@Override
	public int getN() {
		return N;
	}

	@Override
	public void setInput(double[] x) {
		System.arraycopy(x,0,currentInput,0,N);
		currentStep = Double.NaN; // force a hard failure if functions are not called in the right order
		cachedFunction = false;
		cachedGradient = false;
		cachedDerivative = false;
	}

	@Override
	public double computeFunction() {
		if( cachedFunction )
			return currentOutput;
		currentOutput = function.process(currentInput);
		cachedFunction = true;
		return currentOutput;
	}

	@Override
	public void computeGradient(double[] gradient) {
		if( !cachedGradient ) {
			cachedGradient = true;
			this.gradient.process(currentInput,currentGradient);
		}
		System.arraycopy(currentGradient, 0, gradient, 0, N);
	}

	@Override
	public double computeDerivative() {
		if( !cachedDerivative ) {
			cachedDerivative = true;
			currentDerivative = lineDerivative.process(currentStep);
		}
		
		return currentDerivative;
	}
	
	private class LineFunction implements FunctionStoS
	{
		double point[];

		private LineFunction() {
			point = new double[N];
		}

		@Override
		public double process(double x) {
			for( int i = 0; i < N; i++ ) {
				point[i] = start[i] + x*direction[i];
			}
			
			return function.process(point);
		}
	}
}
