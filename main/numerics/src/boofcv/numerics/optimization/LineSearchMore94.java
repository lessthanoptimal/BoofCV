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
 * Line search algorithm that provides a guaranteed sufficient decrease according to the Wolfe condition.
 * This is the same algorithm by Minpack-2 [2].   The logic in the code below is based off of [2] but the original
 * paper [1] was being looked at too.
 * </p>
 *
 *
 * <p>
 * Wolfe condition<br>
 * &phi;(&alpha;) &le; &phi;(0) + &mu;&alpha;&phi;'(0)<br>
 * | &phi;'(&alpha;)| &le; &eta; |&phi;'(0)|<br>
 * wher
 *
 * <p>
 * [1] Jorge J. More and David J. Thuente, "Line Search Algorithms with Guaranteed Sufficient Decrease"
 * ACM Transactions of Mathematical Software, Vol 20 , No. 3, September 1994, Pages 286-307<br>
 * [2] MINPACK-2 source code http://ftp.mcs.anl.gov/pub/MINPACK-2/csrch/
 * </p>
 * @author Peter Abeles
 */
public class LineSearchMore94 implements LineSearch {

	// Various magic numbers.  See [1]
	private static final double p5=0.5,p66=0.66,xtrapl=1.1,xtrapu=4.0;

	// computes the function value
	private FunctionStoS function;
	// computes the function derivative
	private FunctionStoS derivative;

	// thresholds to test for convergence
	private double ftol; // tolerance for sufficient decrease.  ftol > 0
	private double gtol; // tolerance for curvature condition. gtol >= 0
	private double xtol; // relative tolerance for an acceptable step. xtol >= 0
					     // Only a warning is provided if xtol has not been meet

	// value of the function and derivative at 0
	private double finit;
	private double ginit;

	private double gtest;
	private double width;
	private double width1;

	// flag indicating if the step size has been bracketed yet
	private boolean bracket;
	// which stage in the optimization process is it
	private int stage;
	
	// stx and sty define the closed interval that the solution must lie inside of.
	// the order of stx and sty (lower and upper bound) can change as the search progresses
	// stx = alpha_l in the paper, this is the current best step
	private double stx,fx,gx;
	// sty = alpha_u in the paper
	private double sty,fy,gy;
	private double stmin,stmax;
	// Current trial value for the step.  Corresponds to alpha_t in the paper
	private double stp, fp, gp;
	// lower and upper bound for the step stp
	private double stpmin,stpmax;

	// Indicates if there were any numerical issues that caused it to stop iterating
	private String message;

	// Indicates if this iit is the first iteration and indicates if what values need to be computed at stp
	private boolean firstIteration;
	// Indicates if it converged to a solution
	private boolean converged;

	/**
	 *
	 * @param ftol Tolerance for sufficient decrease. ftol > 0. Smaller value for loose tolerance.  Try 1e-4
	 * @param gtol Tolerance for curvature condition. gtol >= 0. Larger value for loose tolerance.  Try 1e-3
	 * @param xtol Relative tolerance for acceptable step. xtol >= 0. Larger value for loose tolerance.  Try 1e-4.
	 * @param stpmin
	 * @param stepmax
	 */
	public LineSearchMore94(double ftol, double gtol, double xtol, 
							double stpmin, double stepmax) {
		if( stepmax < stpmin )
			throw new IllegalArgumentException("stpmin must be < stpmax");
		if( stpmin <= 0 )
			throw new IllegalArgumentException("stpmin must be > 0");
		if( ftol < 0 )
			throw new IllegalArgumentException("ftol must be >= 0 ");
		if( gtol < 0 )
			throw new IllegalArgumentException("gtol must be >= 0 ");
		if( xtol < 0 )
			throw new IllegalArgumentException("xtol must be >= 0 ");

		this.ftol = ftol;
		this.gtol = gtol;
		this.xtol = xtol;
		this.stpmin = stpmin;
		this.stpmax = stepmax;
	}

	@Override
	public void setFunction(FunctionStoS function, FunctionStoS derivative) {
		this.function = function;
		this.derivative = derivative;
	}

	@Override
	public void init(double funcAtZero, double derivAtZero, double funcAtInit, double stepInit ) {

		if( stepInit < stpmin)
			throw new IllegalArgumentException("Initial step is less than the minimum allowed step.");
		if( stepInit > stpmax)
			throw new IllegalArgumentException("Initial step is more than the maximum allowed step.");
		if( derivAtZero >= 0 )
			throw new IllegalArgumentException("Initial derivative is >= 0");
		
		this.bracket = false;
		this.stage = 0;
		this.finit = funcAtZero;
		this.ginit = derivAtZero;
		this.gtest = ftol*ginit;
		this.width = stpmax - stpmin;
		this.width1 = width/p5;
		
		this.stp = stepInit;
		this.fp = funcAtInit;
		
		stx = 0;
		fx = finit;
		gx = ginit;
		sty = 0;
		fy = finit;
		gy = ginit;
		stmin = 0;
		stmax = stp + xtrapu*stp;

		message = null;
		firstIteration = true;
		converged = false;
	}

	@Override
	public boolean iterate() throws OptimizationException {

		// update the function and derivative values at stp
		gp = derivative.process(stp);
		if( !firstIteration ) {
			fp = function.process(stp);
		} else {
			firstIteration = false;
		}

		// check to see if it should enter the second stage
		double ftest = finit + stp*gtest;
		if( stage == 0 && fp <= ftest && gp >= 0 )
			stage = 1;
		
		// check warning conditions
		if( bracket && (stp <= stpmin || stp >= stpmax))
			message = "Rounding error preventing progress.";
		if( bracket && stpmax - stpmin <= xtol* stpmax)
			message = "XTOL test satisfied";
		if( stp == stpmax && fp <= ftest && gp <= gtest )
			message = "stp == stpmax";
		if( stp == stpmin && (fp > ftest || gp <= gtest ))
			message = "stp = stpmin";

		// test for convergence
		if( fp <= ftest && Math.abs(gp) <= gtol*(-ginit)) {
			converged = true;
			return true;
		}

		// Warning messages indicates that no progress can be made and that it should stop iterating
		if( message != null )
			return true;

		// todo comment what fx is in stage 0
		if( stage == 0 && fp <= fx && fp > ftest ) {
			// transform values using the axially function phi(x) that measures the difference between the function
			// being optimized and its upper bound as specified by the first Wolfe equation
			// phi(x) = f(x) - f(0) - mu*f'(0)*x
			fp -= stp*gtest;
			fx -= stx*gtest;
			fy -= sty*gtest;
			gp -= gtest;
			gx -= gtest;
			gy -= gtest;

			// compute the new step
			dcstep();

			// transform these values back into the original function
			fx += stx*gtest;
			fy += sty*gtest;
			gx += gtest;
			gy += gtest;
		} else {
			dcstep();
		}
		
		// decide if a bisection step is needed
		if( bracket ) {
			if( Math.abs(sty-stx) >= p66*width1 )
				width1 = width;
			width = Math.abs(sty-stx);
		}
		
		// set the minimum and maximum steps allowed for stp
		if( bracket ) {
			// if a bracket has been found then stx and sty define the lower and upper bounds, but
			// their order is variable
			stmin = Math.min(stx,sty);
			stmax = Math.max(stx,sty);
		} else {
			stmin = stp + xtrapl*(stp-stx);
			stmax = stp + xtrapu*(stp-stx);
		}

		// force the stp to be within the bound stpmax and stpmin
		stp = Math.max(stp, stpmin);
		stp = Math.min(stp, stpmax);

		// see if further progress can be made. If not set stp to be equal to
		// the best point obtained so far
		if( bracket && (stp <= stmin || stp >= stmax ) ||  (bracket && stmax-stmin <= xtol*stmax))
			stp=stx;
		
		return false;
	}

	/**
	 * Computes the new step and updates fx,fy,gx,dy.
	 */
	private void dcstep() {
		double sgnd = gp*Math.signum(gx);

		double stpf;

		if( fp > fx ) {
			stpf = handleCase1();
		} else if( sgnd < 0 ) {
			stpf = handleCase2();
		} else if( Math.abs(gp) < Math.abs(gx) ) {
			stpf = handleCase3();
		} else {
			stpf = handleCase4();
		}
		
		if( fp > fx ) {
			sty = stp;
			fy = fp;
			gy = gp;
		} else {
			if( sgnd < 0 ) {
				sty = stx;
				fy = fx;
				gy = gx;
			}
			stx = stp;
			fx = fp;
			gx = gp;
		}
		
		stp = stpf;
	}

	/**
	 * 
	 * @return
	 */
	private double handleCase1() {

		double stpc = SearchInterpolate.cubic2(fx, gx, stx, fp, gp, stp);
		double stpq = SearchInterpolate.quadratic(fx,gx,stx,fp,stp);

		bracket = true;
		if( Math.abs(stpc-stx) < Math.abs(stpq-stx)) {
			return stpc;
		} else {
			return stpc + (stpq-stpc)/2.0;
		}
	}

	private double handleCase2() {
		double stpc = SearchInterpolate.cubic2(fp, gp, stp, fx, gx, stx);
		double stps = SearchInterpolate.quadratic2(gp,stp,gx,stx);

		bracket = true;
		if( Math.abs(stpc -stp) > Math.abs(stps-stp)) {
			return stpc;
		} else {
			return stps;
		}
	}

	private double handleCase3() {
		double stpf;
		double stpc = SearchInterpolate.cubicSafe(fp, gp, stp, fx, gx, stx, stpmin, stpmax);
		double stpq = SearchInterpolate.quadratic(fp,gp,stp,fx,stx);

		if( bracket ) {
			if( Math.abs(stpc-stp) < Math.abs(stpq-stp)){
				stpf = stpc;
			} else {
				stpf = stpq;
			}
			if( stp > stx ) {
				stpf = Math.min(stp+p66*(sty-stp),stpf);
			} else {
				stpf = Math.max(stp+p66*(sty-stp),stpf);
			}
		} else {
			if( Math.abs(stpc-stp) > Math.abs(stpq-stp)) {
				stpf = stpc;
			} else {
				stpf = stpq;
			}
			stpf = Math.min(stpmax,stpf);
			stpf = Math.max(stpmin,stpf);
		}
		return stpf;
	}

	private double handleCase4() {
		double stpf;
		if( bracket ) {
			double stpc = SearchInterpolate.cubic2(fp, gp, stp, fy, gy, sty);
			stpf = stpc;
		} else if( stp < stx ) {
			stpf = stpmax;
		} else {
			stpf = stpmin;
		}
		return stpf;
	}

	@Override
	public boolean isConverged() {
		return converged;
	}

	@Override
	public double getStep() {
		return stp;
	}

	@Override
	public String getWarning() {
		return message;
	}
}
