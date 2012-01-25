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
import boofcv.numerics.optimization.functions.GradientLineFunction;

/**
 * From separate classes for the function and gradient computations implements
 * {@link GradientLineFunction}.  Results are cached until the next time a line set
 * of parameters or line position is specified.
 * 
 * @author Peter Abeles
 */
public class CachedGradientLineFunction implements GradientLineFunction {

	// number of parameters
	protected int N;

	// description of line search
	protected double []start;
	protected double []direction;

	// has the output already been computed at the current position
	protected boolean cachedFunction;
	protected boolean cachedGradient;

	// current input parameters
	protected double[] currentInput;
	// current gradient and function output
	protected double[] currentGradient;
	protected double currentOutput;

	// input functions
	protected FunctionNtoS function;
	protected FunctionNtoN gradient;

	public CachedGradientLineFunction(FunctionNtoS function, FunctionNtoN gradient) {
		this.function = function;
		this.gradient = gradient;
		this.N = function.getN();
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
		cachedFunction = false;
		cachedGradient = false;
	}

	@Override
	public int getN() {
		return N;
	}

	@Override
	public void setInput(double[] x) {
		System.arraycopy(x,0,currentInput,0,N);
		cachedFunction = false;
		cachedGradient = false;
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
		if( !cachedGradient ) {
			cachedGradient = true;
			this.gradient.process(currentInput,currentGradient);
		}
		
		double dot = 0;
		for( int i = 0; i < N; i++ ) {
			dot += currentGradient[i]*direction[i];
		}
		
		return dot;
	}
}
