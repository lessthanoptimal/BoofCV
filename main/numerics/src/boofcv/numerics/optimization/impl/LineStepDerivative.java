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

import boofcv.numerics.optimization.FunctionNtoN;
import boofcv.numerics.optimization.FunctionStoS;

/**
 * <p>
 * Computes the derivative along a ray from a the Jacobian computed using
 * {@link boofcv.numerics.optimization.FunctionNtoN}.  The ray is defined given an initial
 * point and a direction.  This is for use with {@link boofcv.numerics.optimization.LineSearch}
 * algorithms.
 * </p>
 * <p>
 * g'(x) = g( y<sub>0</sub> + x*p )<sup>T</sup>p<br>
 * where 'x'&isin;&real;<sup>1</sup> is the input, 'y'<sub>0</sub>  &isin; &real;<sup>N</sup> is the initial point,
 * and 'p'&isin;&real;<sup>N</sup> is the direction
 * </p>
 *
 * @author Peter Abeles
 */
public class LineStepDerivative implements FunctionStoS {

	// derivative that is being searched along a line
	private FunctionNtoN wrap;
	// direction the ray traverses in
	private double direction[];
	// initial position of the ray
	private double initial[];

	// storage for position on the ray
	private double temp[];

	// storage for the derivative output
	private double output[];

	/**
	 * Specify the function being searched.
	 *
	 * @param wrap Function that is being searched along a line
	 */
	public LineStepDerivative(FunctionNtoN wrap) {
		this.wrap = wrap;
		this.temp = new double[ wrap.getN() ];
		this.output = new double[ wrap.getN() ];
	}

	/**
	 * Specify the line which is being searched
	 *
	 * @param initial Line's initial position
	 * @param direction Line's direction
	 */
	public void setLine( double initial[] , double direction[] ) {
		this.direction = direction;
		this.initial = initial;
	}

	@Override
	public double process(double scale) {
		for( int i = 0; i < temp.length; i++ ) {
			temp[i] = initial[i] + scale*direction[i];
		}
		
		wrap.process(temp,output);

		// The output is the dot product of the gradient and direction
		double ret = 0;
		for( int i = 0; i < direction.length; i++ ) {
			ret += output[i]*direction[i];
		}

		return ret;
	}
}
