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

import boofcv.numerics.optimization.FunctionNtoS;
import boofcv.numerics.optimization.FunctionStoS;

/**
 * <p>
 * Computes the output of an {@link boofcv.numerics.optimization.FunctionNtoS} along a ray.  The ray is defined given an initial
 * point and a direction.  This is for use with {@link boofcv.numerics.optimization.LineSearch} algorithms.
 * </p>
 * <p>
 * g(x) = f( y<sub>0</sub> + x*p )<br>
 * where 'x'&isin;&real;<sup>1</sup> is the input, 'y'<sub>0</sub>  &isin; &real;<sup>N</sup> is the initial point,
 * and 'p'&isin;&real;<sup>N</sup> is the direction
 * </p>
 * 
 * @author Peter Abeles
 */
public class LineStepFunction implements FunctionStoS {

	// function that is being searched along a line 
	private FunctionNtoS wrap;
	// direction the ray traverses in
	private double direction[];
	// initial position of the ray
	private double initial[];
	// storage for position on the ray
	private double temp[];

	// previous step
	private double previousStep;
	// function value at the previous step
	private double functionStep;

	/**
	 * Specify the function being searched.
	 *
	 * @param wrap Function that is being searched along a line
	 */
	public LineStepFunction(FunctionNtoS wrap) {
		this.wrap = wrap;
		this.temp = new double[ wrap.getN() ];
	}

	/**
	 * Specify the line which is being searched
	 *
	 * @param initial Line's initial position
	 * @param direction Line's direction
	 */
	public void setLine(double initial[], double direction[]) {
		this.direction = direction;
		this.initial = initial;
	}

	/**
	 * Computes the function value at step.  The value at the previous call is saved and returned
	 * when the same step is requested twice in a row.
	 *
	 * @param step Size of the step.
	 * @return Value at the specified step
	 */
	@Override
	public double process(double step) {
		// if the same request is made twice in a row, return the same solution
		if( step == previousStep )
			return functionStep;

		for( int i = 0; i < temp.length; i++ ) {
			temp[i] = initial[i] + step*direction[i];
		}

		functionStep = wrap.process(temp);
		previousStep = step;
		return functionStep;
	}
}
