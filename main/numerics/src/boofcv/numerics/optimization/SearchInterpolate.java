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
 * Contains interpolation functions for use in line searches.  These interpolation
 * algorithms are designed to meet the condition below, without being too small,
 * </p>
 *
 * <p>
 * Sufficient decrease equation:<br>
 * f(&alpha;) &le; f(0) + c<sub>1</sub>&alpha;g(0)<br>
 * where f is the function, g is its derivative, and &alpha; is the step length.
 * </p>
 *
 * <p>
 * See Chapter 3 in "Numerical Optimization 2nd Ed." by Jorge Nocedal and Stephen J. Wright, 2006
 * </p>
 *
 * @author Peter Abeles
 */
public class SearchInterpolate {

	/**
	 * <p>
	 * Quadratic interpolation using two function values and one derivative.
	 * </p>
	 * <p>
	 * [1] MINPACK-2 source code http://ftp.mcs.anl.gov/pub/MINPACK-2/dcstep.f
	 * </p>
	 *
	 * @param f0 Value of f(x0)
	 * @param g0 Derivative f'(x0)
	 * @param x0 First sample point
	 * @param f1 Value of f(x1)
	 * @param x1 Second sample point
	 * @return Interpolated point
	 */
	public static double quadratic( double f0 , double g0 , double x0 , double f1 , double x1 ) {

		return x0 + ((g0/((f0-f1)/(x1-x0)+g0))/2.0)*(x1-x0);
	}

	/**
	 * <p>
	 * Quadratic interpolation using two derivatives.
	 * </p>
	 * <p>
	 * [1] MINPACK-2 source code http://ftp.mcs.anl.gov/pub/MINPACK-2/dcstep.f
	 * </p>
	 *
	 * @param g0 Derivative f'(x0)
	 * @param x0 First sample point
	 * @param g1 Derivative f'(x1)
	 * @param x1 Second sample point
	 * @return Interpolated point
	 */
	public static double quadratic2( double g0 , double x0 , double g1  , double x1 ) {

		return x0 + (g0/(g0-g1))*(x1-x0);
	}

	/**
	 * Interpolates the next step using a cubic model.  Interpolation works by solving for 'a' and 'b' in
	 * the equation below. Designed to minimize the number of times the derivative
	 * needs to be computed.  Care has been taken reduce overflow/underflow by normalizing.
	 *
	 * &phi;(&alpha;) =  a*&alpha;<sup>3</sup>  + b*&alpha;<sup>2</sup> + &alpha;<sup>3</sup>  + &alpha;*&phi;'(0) + &phi(0)
	 *
	 * @param f0 Function value at f(0)
	 * @param g0 Derivative value at g(0)
	 * @param f1 Function value at f(a1)
	 * @param alpha1 value of a1
	 * @param f2 Function value at f(a2)
	 * @param alpha2 value of a2
	 *
	 * @return Interpolated step length
	 */
	public static double cubic( double f0 , double g0 ,
								double f1 , double alpha1 ,
								double f2 , double alpha2 ) {
		// Several different formulation were considered for solving this equation.
		// See ExamineCubicInterpolateStability in benchmark directory
		// Turns out that the straight forward implementation is just about the best.

		double denominator = alpha1*alpha1*alpha2*alpha2*(alpha2-alpha1);
		double a11 = alpha1*alpha1/denominator;
		double a12 = -alpha2*alpha2/denominator;
		double a21 = -alpha1*a11;
		double a22 = -alpha2*a12;

		double y1 = f2 - f0 - g0*alpha2;
		double y2 = f1 - f0 - g0*alpha1;

		double a = a11*y1 + a12*y2;
		double b = a21*y1 + a22*y2;
		
	 	return (-b+Math.sqrt(b*b-3*a*g0))/(3.0*a);
	}


	/**
	 * <p>
	 * Cubic interpolation using the function and derivative computed at two different points.  This particular
	 * implementation taken from [1] and appears to be designed to maximize stability.
	 * </p>
	 * <p>
	 * [1] MINPACK-2 source code http://ftp.mcs.anl.gov/pub/MINPACK-2/dcstep.f
	 * </p>
	 * 
	 * @param f0 Value of f(x0)
	 * @param g0 Derivative f'(x0)
	 * @param x0 First sample point
	 * @param f1 Value of f(x1)
	 * @param g1 Derivative g'(x1)
	 * @param x1 Second sample point
	 * @return Interpolated point
	 */
	public static double cubic2( double f0 , double g0 , double x0 ,
								 double f1 , double g1 , double x1 ) {

		double theta = 3.0*(f0-f1)/(x1-x0) + g0 + g1;
		double s = Math.max(Math.abs(theta),Math.abs(g0));
		s= Math.max(s,Math.abs(g1));
		double gamma = s*Math.sqrt((theta/s)*(theta/s) - (g0/s)*(g1/s));
		if( x0 > x1 )
			gamma = -gamma;
		double p = (gamma-g0) + theta;
		double q = ((gamma-g0)+gamma) + g1;
		
		return x0 + (p/q)*(x1-x0);
	}

	/**
	 * <p>
	 * Use cubic interpolation only if the cubic tends to infinity in the direction of the step or if the minim of the
	 * cubic is beyond x1.  Otherwise the the step will be max if x0 > x1 else it will be min.
	 * </p>
	 * <p>
	 * [1] MINPACK-2 source code http://ftp.mcs.anl.gov/pub/MINPACK-2/dcstep.f
	 * </p>
	 *
	 * @param f0 Value of f(x0)
	 * @param g0 Derivative f'(x0)
	 * @param x0 First sample point
	 * @param f1 Value of f(x1)
	 * @param g1 Derivative g'(x1)
	 * @param x1 Second sample point
	 * @return Interpolated point
	 */
	public static double cubicSafe( double f0 , double g0 , double x0 ,
									double f1 , double g1 , double x1 ,
									double min , double max ) {

		double theta = 3.0*(f0-f1)/(x1-x0) + g0 + g1;
		double s = Math.max(Math.abs(theta),Math.abs(g0));
		s= Math.max(s,Math.abs(g1));
		double gamma = s*Math.sqrt((theta/s)*(theta/s) - (g0/s)*(g1/s));
		if( x1 < x0 )
			gamma = -gamma;
		double p = (gamma-g0) + theta;
		double q = (gamma+(g1-g0))+gamma;

		double r = p/q;

		// gamma == 0 only rises if the cubic does not tend to infinity in the direction of the step
		
		if( r < 0 && gamma != 0 ) {
			return x0 + r*(x1-x0);
		} else if( x0 > x1 ) {
			return max;
		} else {
			return min;
		}

	}
}
