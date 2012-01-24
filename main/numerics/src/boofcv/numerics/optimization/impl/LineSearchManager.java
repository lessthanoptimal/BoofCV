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

import boofcv.numerics.optimization.FunctionStoS;
import boofcv.numerics.optimization.LineSearch;

/**
 * <p>
 * Manages data structures and simplifies line searches for nonlinear optimization.  A line search works by
 * searching a 1-dimensional subspace of an n-dimensional function for a local minimum.  A specialized
 * line search algorithm is passed in and is iterated until it converges to a solution.  The line derivative
 * can either be numerically computed and coupled to the linear search, or computed directly from
 * the gradient.
 * </p>
 *
 * <p>
 * The gradient is not returned since only the derivative along the line search is computed.
 * </p>
 *
 * @author Peter Abeles
 */
public class LineSearchManager {

	// initial derivative along the line
	private double derivAtZero;

	// Line search functions
	private LineStepFunction lineFunction;
	private FunctionStoS lineDerivative;

	// line search algorithm
	private LineSearch search;

	// function value at the step at the end of the search
	private double fstp;
	// minimum possible function value
	private double funcMinValue;
	// gtol in wolfe condition
	private double gtol;

	/**
	 * Specifies line search parameters.  'lineDerivative' can either be coupled or decoupled from
	 * 'lineFunction'.  Typically if a numerical derivative is used then it is coupled.  Coupled
	 * means that when line parameters are set in 'lineFunction' they are automatically set in
	 * 'lineDerivative'.  If 'lineDerivative' is of type LineStepDerivative it is assumed to not
	 * be coupled.
	 *
	 * @param search Line search algorithm
	 * @param lineFunction Line search function
	 * @param lineDerivative Line search function derivative
	 * @param funcMinValue Minimum possible function value
	 * @param gtol slope coefficient for wolfe condition. 0 < gtol <= 1
	 */
	public LineSearchManager( LineSearch search ,
							  LineStepFunction lineFunction ,
							  FunctionStoS lineDerivative ,
							  double funcMinValue , double gtol )
	{
		this.search = search;
		this.lineFunction = lineFunction;
		this.lineDerivative = lineDerivative;
		this.funcMinValue = funcMinValue;
		this.gtol = gtol;

		search.setFunction(lineFunction,lineDerivative);
	}

	/**
	 * Setup the line search from a new point and direction.
	 *
	 * @param funcAtStart Function's value at startPoint.
	 * @param startPoint Sample point.
	 * @param startDeriv Gradient at startPoint.
	 * @param direction Direction of the search.
	 * @param initialStep Size of the initial step.  Typically 1.
	 */
	public void initialize( double funcAtStart , double[] startPoint , double[] startDeriv,
							double[] direction , double initialStep , int N ) {
		// derivative of the line search is the dot product of the gradient and search direction
		derivAtZero = 0;
		double norm = 0;
		for( int i = 0; i < N; i++ ) {
			derivAtZero += startDeriv[i]*direction[i];
			norm += startDeriv[i]*startDeriv[i];
		}
		
		System.out.println("gradient norm "+Math.sqrt(norm));

		// setup line functions
		setLine(startPoint, direction);

		// use wolfe condition to set the maximum step size
		double maxStep = (funcMinValue-funcAtStart)/(gtol*derivAtZero);
		if( initialStep > maxStep )
			initialStep = maxStep;
		double funcAtInit = lineFunction.process(initialStep);
		search.init(funcAtStart,derivAtZero,funcAtInit,initialStep,0,maxStep);
	}

	/**
	 * Next iteration in the line search
	 *
	 * @return True of the line search has stopped.
	 */
	public boolean iterate() {
		if( search.iterate() ) {

			// line search function caches the previous
			fstp = lineFunction.process(search.getStep());

			return true;
		}
		return false;
	}

	/**
	 * Returns the line derivative at the start point
	 */
	public double getLineDerivativeAtZero() {
		return derivAtZero;
	}

	/**
	 * Specify the line that is being searched
	 *
	 * @param startPoint start of of the line
	 * @param direction The direction of the line
	 */
	protected void setLine( double[] startPoint , double []direction ) {
		lineFunction.setLine(startPoint,direction);

		if( lineDerivative instanceof LineStepDerivative ) {
			((LineStepDerivative)lineDerivative).setLine(startPoint,direction);
		}
	}

	/**
	 * True if the line search converged.
	 */
	public boolean isSuccess() {
		return search.isConverged();
	}

	/**
	 * Step size at search termination
	 *
	 * @return final step
	 */
	public double getStep() {
		return search.getStep();
	}

	/**
	 * Warning messages generated by the search
	 *
	 * @return warning message
	 */
	public String getWarning() {
		return search.getWarning();
	}

	/**
	 * Function value at 'step'
	 */
	public double getFStep() {
		return fstp;
	}
}
