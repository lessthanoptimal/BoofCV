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

import boofcv.numerics.optimization.LineSearch;
import boofcv.numerics.optimization.functions.CoupledDerivative;
import org.ejml.UtilEjml;

/**
 * <p>
 * Line search which meets the strong Wolfe line condition.  The Wolfe condition stipulates that &alpha;<sub>k</sub> (the step size)
 * should give sufficient decrease in the objective function below. The two parameters 0 < ftol &le; gtol < 1
 * determine how stringent the search is. For a full description of optimization parameters see [1].
 * </p>
 * <p>
 * Wolfe condition<br>
 * &phi;(&alpha;) &le; &phi;(0) + ftol*&alpha;*&phi;'(0)<br>
 * | &phi;'(&alpha;)| &le; gtol*|&phi;'(0)|<br>
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
public class LineSearchFletcher86 implements LineSearch {

	// step tolerance change
	protected double tolStep = UtilEjml.EPS;

	// function being minimized
	protected CoupledDerivative function;

	// function value at alpha = 0
	protected double valueZero;
	// function derivative at alpha = 0
	protected double derivZero;

	// current step length, function value, and derivative
	protected double stp;
	protected double fp;
	protected double gp;

	// prevents alpha from getting too close to the bound's extreme values
	double t1,t2,t3;

	// double value of the function and derivative at zero
	double fzero,gzero;

	// largest allowed step
	double stmax;
	// minimum acceptable value of f
	double fmin;

	// search parameters that defined the Wolfe condition
	private double ftol, gtol;
	// previous iteration step length and value
	protected double stprev;

	protected double fprev;
	protected double gprev;
	// mode that the search is in
	int mode;

	// maximum allowed step
	private double stpmax;

	// bounds on stp
	double pLow;
	double pHi;
	double fLow; // the value at pLow

	String message;
	boolean converged;
	
	/**
	 *
	 * @param ftol Controls required reduction in value. Try 1e-4
	 * @param gtol Controls decrease in derivative magnitude. Try 0.9
	 * @param fmin Minimum acceptable value of f(x). zero for least squares.
	 * @param t1 Prevents alpha from growing too large during bracket phase.  Try 9
	 * @param t2 Prevents alpha from being too close to bounds during sectioning.  Recommend t2 < c2. Try 0.1
	 * @param t3 Prevents alpha from being too close to bounds during sectioning.  Try 0.5

	 */
	public LineSearchFletcher86(double ftol, double gtol, double fmin,
								double t1, double t2, double t3 ) {
		if( ftol < 0 )
			throw new IllegalArgumentException("c1 must be more than zero");
		else if( ftol > gtol)
			throw new IllegalArgumentException("c1 must be less or equal to than c2");
		else if( gtol >= 1 )
			throw new IllegalArgumentException("c2 must be less than one");

		this.ftol = ftol;
		this.gtol = gtol;
		this.t1 = t1;
		this.t2 = t2;
		this.t3 = t3;
		this.fmin = fmin;
	}

	/**
	 * @inheritdoc
	 */
	@Override
	public void setFunction(CoupledDerivative function ) {
		this.function = function;
	}

	@Override
	public void init(double funcAtZero, double derivAtZero, double funcAtInit, double initAlpha,
					 double stepMin, double stepMax ) {
		if( stepMax <= 0 )
			throw new IllegalArgumentException("stepMax must be greater than zero");

		initializeSearch(funcAtZero, derivAtZero, funcAtInit,initAlpha);

		fzero = funcAtZero;
		gzero = derivAtZero;
		
		stprev = 0;
		fprev = funcAtZero;
		gprev = derivAtZero;

		mode = 0;

		message = null;
		converged = false;

		this.stmax = (fmin-fzero)/(ftol*gzero);
		this.stpmax = stepMax;
	}

	protected void initializeSearch( final double valueZero , final double derivZero ,
									 final double initValue , final double initAlpha ) {
		if( derivZero >= 0 )
			throw new IllegalArgumentException("Derivative at zero must be decreasing");
		if( initAlpha <= 0 )
			throw  new IllegalArgumentException("initAlpha must be more than zero");

		this.valueZero = valueZero;
		this.derivZero = derivZero;
		stp = initAlpha;
		fp = initValue;
		gp = Double.NaN;
	}

	/**
	 * @inheritdoc
	 */
	@Override
	public boolean iterate()
	{
		boolean ret;
		if( mode <= 1 ) {
			ret = converged = bracket();
		} else {
			ret = converged = section();
		}

		return ret;
	}

	/**
	 * Searches for an upper bound.
	 */
	protected boolean bracket() {
//		System.out.println("------------- bracket");
		// the value of alpha was passed in
		function.setInput(stp);
		if( mode != 0 ) {
			fp = function.computeFunction();
			gp = Double.NaN;
		} else {
			mode = 1;
		}

		// check for upper bounds
		if( fp > valueZero + ftol * stp *derivZero ) {
			setModeToSection(stprev, fprev, stp);
			return false;
		}
		if( fp >= fprev) {
			setModeToSection(stprev, fprev, stp);
			return false;
		}

		gp = function.computeDerivative();
		if( Math.abs(gp) <= -gtol *derivZero ) {
			return true;
		}

		// if the derivative is positive it is on the other side of the dip and has
		// been bracketed
		if( gp >= 0 ) {
			setModeToSection(stp, fp, stprev);
			return false;
		}
		
		if( stmax <= 2*stp - stprev ) {
			stprev = stp;
			gprev = gp;
			fprev = fp;
			stp = stmax;
		} else {
			// use interpolation to pick the next sample point
			double temp = stp;
			stp = interpolate(2*stp - stprev, Math.min(stpmax, stp +t1*(stp - stprev)));
			stprev = temp;
			gprev = gp;
			fprev = fp;
		}

		// see if it is taking significant steps
		if( checkSmallStep() ) {
			message = "WARNING: Small steps";
			return true;
		}

		return false;
	}

	private void setModeToSection( double alphaLow , double valueLow , double alphaHigh )
	{
		this.pLow = alphaLow;
		this.fLow = valueLow;
		this.pHi = alphaHigh;
		mode = 2;
	}
	
	/**
	 * Using the found bracket for alpha it searches for a better estimate.
	 */
	protected boolean section()
	{
//		System.out.println("------------- section");
		// compute the value at the new sample point
		double temp = stp;
		stp = interpolate(pLow +t2*(pHi - pLow), pHi -t3*(pHi - pLow));
		// save the previous step
		if( !Double.isNaN(gp)) {
			// needs to keep a step with a derivative
			stprev = temp;
			fprev = fp;
			gprev = gp;
		}

		// see if there is a significant change in alpha
		if( checkSmallStep() ) {
			message = "WARNING: Small steps";
			return true;
		}

		function.setInput(stp);
		fp = function.computeFunction();
		gp = Double.NaN;

		// check for convergence
		if( fp > valueZero + ftol * stp *derivZero  || fp >= fLow) {
			pHi = stp;
		} else {
			gp = function.computeDerivative();

			// check for termination
			if( Math.abs(gp) <= -gtol *derivZero )
				return true;

			if( gp *(pHi - pLow) >= 0 )
				pHi = pLow;
			// check on numerical prevision
			if( Math.abs((pLow - stp)* gp) <= tolStep ) {
				return true;
			}
			pLow = stp;
			fLow = fp;
		}

		return false;
	}

	@Override
	public double getStep() {
		return stp;
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
		double max = Math.max(stp, stprev);
		return( Math.abs(stp - stprev)/max < tolStep );
	}

	/**
	 * Use either quadratic of cubic interpolation to guess the minimum.
	 */
	protected double interpolate( double boundA , double boundB )
	{
		double alphaNew;

		// interpolate minimum for rapid convergence
		if( Double.isNaN(gp) ) {
			alphaNew = SearchInterpolate.quadratic(fprev, gprev, stprev, fp, stp);
		} else {
			alphaNew = SearchInterpolate.cubic2(fprev, gprev, stprev, fp, gp, stp);
			if( Double.isNaN(alphaNew))
				alphaNew = SearchInterpolate.quadratic(fprev, gprev, stprev, fp, stp);
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

	@Override
	public boolean isConverged() {
		return converged;
	}

	@Override
	public double getFunction() {
		return fp;
	}
}
