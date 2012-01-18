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
 * Line search which meets the strong Wolfe line condition.  The Wolfe condition stipulates that &alpha;<sub>k</sub> (the step size)
 * should give sufficient decrease in the objective function below. The two parameters 0 < c<sub>1</sub> < c<sub>2</sub> 1
 * determine how stringent the search is. For a full description of optimization parameters see [1].
 * </p>
 * <p>
 * Wolfe condition<br>
 * &phi;(&alpha;) &le; &phi;(0) + c<sub>1</sub>&alpha;&phi;'(0)<br>
 * | &phi;'(&alpha;)| &le; c<sub>2</sub> |&phi;'(0)|<br>
 * where &phi; is the objective function and &phi;' is its derivative.
 * </p>
 *
 * <p>
 * A typical application of using this line search is to find the minimum of an 'N' dimensional function
 * along a line with slope 'p'.  In which case &phi;(&alpha;) is defined below:<br>
 * &phi;(&alpha;<sub>k</sub>) = f(x<sub>k</sub> + &alpha;<sub>k</sub>*p<sub>k</sub>)
 * </p>
 * 
 * <p>
 * [1] R. Fletcher, "Practical Methods of Optimization" 2nd Ed. 1986
 * </p>
 * 
 * @author Peter Abeles
 */
public class LineSearchFletcher86 extends CommonLineSearch {

	// prevents alpha from getting too close to the bound's extreme values
	double t1,t2,t3;

	// search parameters that defined the Wolfe condition
	private double c1,c2;
	// maximum allowed step bounds
	private double alphaMax;

	// previous iteration step length and value
	protected double alphaPrev;
	protected double valuePrev;
	protected double derivPrev;

	// mode that the search is in
	int mode;
	
	double alphaLow;
	double alphaHi;
	double valueLow;

	String message;
	
	/**
	 *
	 * @param c1 Controls required reduction in value. Try 1e-4
	 * @param c2 Controls decrease in derivative magnitude. Try 0.9
	 * @param t1 Prevents alpha from growing too large during bracket phase.  Try 9
	 * @param t2 Prevents alpha from being too close to bounds during sectioning.  Recommend t2 < c2. Try 0.1
	 * @param t3 Prevents alpha from being too close to bounds during sectioning.  Try 0.5
	 * @param alphaMax Maximum allowed value of alpha.  Problem dependent.

	 */
	public LineSearchFletcher86(double c1, double c2,
								double t1, double t2, double t3,
								double alphaMax) {
		if( alphaMax <= 0 )
			throw new IllegalArgumentException("Maximum alpha must be greater than zero");
		if( c1 < 0 )
			throw new IllegalArgumentException("c1 must be more than zero");
		else if( c1 >= c2 )
			throw new IllegalArgumentException("c1 must be less than c2");
		else if( c2 >= 1 )
			throw new IllegalArgumentException("c2 must be less than one");

		this.c1 = c1;
		this.c2 = c2;
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.alphaMax = alphaMax;
	}

	@Override
	public void init(double valueZero, double derivZero, double initValue, double initAlpha) {
		initializeSearch(valueZero,derivZero,initValue,initAlpha);

		alphaPrev = 0;
		valuePrev = valueZero;
		derivPrev = derivZero;

		mode = 0;

		message = null;
	}

	/**
	 * @inheritdoc
	 */
	@Override
	public boolean iterate()
	{
		if( mode <= 1 ) {
			return bracket();
		} else {
			return section();
		}
	}

	/**
	 * Searches for an upper bound.
	 * @return
	 */
	protected boolean bracket() {
		// the value of alpha was passed in
		if( mode != 0 ) {
			valueT = function.process(alphaT);
			derivT = Double.NaN;
		} else {
			mode = 1;
		}

		// check for upper bounds
		if( valueT > valueZero + c1* alphaT *derivZero ) {
			setModeToSection(alphaPrev,valuePrev,alphaT);
			return false;
		}
		if( valueT >= valuePrev ) {
			setModeToSection(alphaPrev,valuePrev,alphaT);
			return false;
		}

		derivT = derivative.process(alphaT);
		if( Math.abs(derivT) <= -c2*derivZero ) {
			return true;
		}

		if( derivT >= 0 ) {
			setModeToSection(alphaT,valueT,alphaPrev);
			return false;
		}

		// use interpolation to pick the next sample point
		double temp = alphaT;
		alphaT = interpolate(2* alphaT -alphaPrev, Math.min(alphaMax, alphaT +t1*(alphaT -alphaPrev)));
		alphaPrev = temp;
		derivPrev = derivT;
		valuePrev = valueT;

		// see if it is taking significant steps
		if( checkSmallStep() ) {
			message = "WARNING: Small steps";
			return true;
		}

		return false;
	}

	private void setModeToSection( double alphaLow , double valueLow , double alphaHigh )
	{
		this.alphaLow = alphaLow;
		this.valueLow = valueLow;
		this.alphaHi = alphaHigh;
		mode = 2;
	}
	
	/**
	 * Using the found bracket for alpha it searches for a better estimate.
	 */
	protected boolean section()
	{
		// compute the value at the new sample point
		double temp = alphaT;
		alphaT = interpolate(alphaLow+t2*(alphaHi-alphaLow),alphaHi-t3*(alphaHi-alphaLow));
		// save the previous step
		if( !Double.isNaN(derivT)) {
			// needs to keep a step with a derivative
			alphaPrev = temp;
			valuePrev = valueT;
			derivPrev = derivT;
		}

		// see if there is a significant change in alpha
		if( checkSmallStep() ) {
			message = "WARNING: Small steps";
			return true;
		}

		valueT = function.process(alphaT);
		derivT = Double.NaN;

		// check for convergence
		if( valueT > valueZero + c1* alphaT *derivZero  || valueT >= valueLow ) {
			alphaHi = alphaT;
		} else {
			derivT = derivative.process(alphaT);

			// check for termination
			if( Math.abs(derivT) <= -c2*derivZero )
				return true;

			if( derivT *(alphaHi-alphaLow) >= 0 )
				alphaHi = alphaLow;
			// check on numerical prevision
			if( Math.abs((alphaLow - alphaT)* derivT) <= tolStep ) {
				return true;
			}
			alphaLow = alphaT;
			valueLow = valueT;
		}

		return false;
	}

	@Override
	public double getStep() {
		return alphaT;
	}

	@Override
	public String getWarning() {
		return message;
	}

	/**
	 * Checks to see if alpha is changing by a significant amount.  If it change is too small
	 * it can get stuck in a loop\
	 */
	protected boolean checkSmallStep() {
		double max = Math.max(alphaT,alphaPrev);
		return( Math.abs(alphaT -alphaPrev)/max < tolStep );
	}

	/**
	 * Use either quadratic of cubic interpolation to guess the minimum.
	 */
	protected double interpolate( double boundA , double boundB )
	{
		double alphaNew;

		// interpolate minimum for rapid convergence
		if( Double.isNaN(derivT) ) {
			alphaNew = SearchInterpolate.quadratic(valuePrev, derivPrev, valueT, alphaT);
		} else {
			alphaNew = SearchInterpolate.cubic2(valuePrev,derivPrev,alphaPrev, valueT, derivT, alphaT);
			if( Double.isNaN(alphaNew))
				alphaNew = SearchInterpolate.quadratic(valuePrev, derivPrev, valueT, alphaT);
		}

		// order the bound
		double l,u;
		if( boundA < boundB ) {
			l=boundA;u=boundB;
		} else {
			l=boundB;u=boundA;
		}

		// enforce min/max allowed values
		if( alphaNew < l )
			alphaNew = l;
		else if( alphaNew > u )
			alphaNew = u;

		return alphaNew;
	}
}
