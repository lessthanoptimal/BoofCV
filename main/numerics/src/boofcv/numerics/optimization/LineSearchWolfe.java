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
 * <p>
 * Implementation of the strong Wolfe line search.  The Wolfe condition stipulates that &alpha;<sub>k</sub> (the step size)
 * should give sufficient decrease in the objective function below. The two parameters 0 < c<sub>1</sub> < c<sub>2</sub> 1
 * determine how stringent the search is.
 * </p>
 * 
 * <p>
 * Conditions for &alpha;<sub>k</sub><br>
 * f(x<sub>k</sub> + &alpha;<sub>k</sub>*p<sub>k</sub>) &le; f(x<sub>k</sub>) + c<sub>1</sub>&alpha;<sub>k</sub>g<sub>k</sub><sup>T</sup>p<sub>k</sub><br>
 * | g<sub>k</sub>(x<sub>k</sub> + &alpha;<sub>k</sub>*p<sub>k</sub>)<sup>T</sup>p<sub>k</sub>| &le;c<sub>2</sub> |g<sub>k</sub><sup>T</sup>p<sub>k</sub>|<br>
 * where f is the objective function and g is the gradient.
 * </p>
 * 
 * <p>
 * This search is a popular choice and should be used with BFGS.  Quadratic or cubic interpolation is used to choose the step length
 * for each iteration.
 * </p>
 * 
 * @author Peter Abeles
 */
public class LineSearchWolfe {
	
	// first and second order derivatives
	FunctionStoS func;
	FunctionStoS derivative;

	// maximum number of allowed iterations
	int maxIterations;
	
	// search parameters
	double c1,c2;
	// step bounds
	double stepMax,step1;

	// function value at alpha = 0
	double valueZero;
	// function derivative at alpha = 0
	double derivZero;
	
	// current step length
	double alpha;
	// current function value at alpha
	double value;

	// previous iteration step length and value
	double alphaPrev;
	double valuePrev;
	
	public double search( final double valueZero , final double derivZero ,
						  final double initValue , final double initAlpha )
	{
		this.valueZero = valueZero;
		this.derivZero = derivZero;
		alpha = initAlpha;
		value = initValue;

		alphaPrev = 0;
		valuePrev = valueZero;

		for( int iter = 0; iter < maxIterations; iter++ ) {
			if( iter > 0 ) {
				valuePrev = value;
				value = func.process(alpha);

				// check for upper bounds
				if( Math.abs(value) > valueZero + c1*alpha*derivZero )
					return zoom(alphaPrev,alpha,valuePrev);
				if( value >= alphaPrev )
					return zoom(alphaPrev,alpha,valuePrev);
			}

			double deriv = derivative.process(alpha);
			if( Math.abs(deriv) <= -c2*derivZero )
				return alpha;
			
			if( deriv >= 0 )
				return zoom(alpha,alphaPrev,value);

			// use interpolation to pick the next sample point
			double temp = alpha;
			alpha = interpolate(alpha,stepMax);
			alphaPrev = temp;

			// todo check for insignificant changes
		}

		return alpha;
	}

	protected double zoom( double alphaLow , double alphaHi ,
						   double valueLow )
	{

		for( int iter = 0; iter < maxIterations; iter++ ) {

			// compute the value at the new sample point
			double temp = alpha;
			alpha = interpolate(alphaLow,alphaHi);
			alphaPrev = temp;

			valuePrev = value;
			value = func.process(alpha);
			
			// check for convergence
			if( value > valueZero + c1*alpha*derivZero  || value >= valueLow ) {
				alphaHi = alpha;
			} else {
				double deriv = derivative.process(alpha);
				if( Math.abs(deriv) <= -c2*derivZero )
					return alpha;
				if( deriv*(alphaHi-alphaLow) >= 0 )
					alphaHi = alphaLow;
				alphaLow = alpha;
				valueLow = value;
			}

			// todo check to see if any progress is being made
		}

		// low and high should be very similar in this case
		return  alphaLow;
	}

	/**
	 * Use either quadratic of cubic interpolation to guess the minimum.
	 */
	private double interpolate( double min , double max )
	{
		double alphaNew;

		// interpolate minimum for rapid convergence
		if( valueZero == valuePrev ) {
			alphaNew = SearchInterpolate.quadratic(valueZero, derivZero, value, alpha);
		} else {
			alphaNew = SearchInterpolate.cubic(valueZero,derivZero,value,alpha,valuePrev,alphaPrev);
		}

		// enforce min/max allowed values
		if( alphaNew < min )
			alphaNew = min;
		else if( alphaNew > max )
			alphaNew = max;
		
		return alphaNew;
	}
}
