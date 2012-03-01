/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
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

package boofcv.numerics.solver;

import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;

/**
 * <p>
 * Fit observations to a 2D quadratic around a 3x3 region.  Observations are specified in a local
 * coordinate system with indexes from -1 to 1.  Typically the maximum value is at the local center,
 * (0,0) coordinate.
 * </p>
 *
 * <p>
 * Even if the center pixel is the max value, it is still possible for peak to be found outside the
 * 3x3 region.  In that situation what happened is that it fit the points inside 3x3 region to one side
 * of the curve.  In many applications the estimate should then be ignored.
 * If the minimum number of points was being considered then it would be impossible to go outside since
 * it must pass through each sample point.
 * </p>
 *
 * @author Peter Abeles
 */
public class FitQuadratic3by3 {
	
	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(9,6);
	DenseMatrix64F X = new DenseMatrix64F(6,1);
	DenseMatrix64F Y = new DenseMatrix64F(9,1);
	
	double deltaX;
	double deltaY;
	
	public FitQuadratic3by3() {
		DenseMatrix64F M = new DenseMatrix64F(9,6);

		int index = 0;
		for( int i = -1; i <= 1; i++ ) {
			for( int j = -1; j <= 1; j++ , index++ ) {
				M.set(index,0,j*j);
				M.set(index,1,i*j);
				M.set(index,2,i*i);
				M.set(index,3,j);
				M.set(index,4,i);
				M.set(index,5,1);
			}
		}
		
		if( !solver.setA(M) )
			throw new RuntimeException("Solver is broken");
	}

	/**
	 * Sets the value by index.  A row-major matrix is used.
	 * index = (y+1)*3+x+1
	 *
	 * @param index Array index from 0 to 8
	 * @param value value at index
	 */
	public void setValue( int index , double value ) {
		Y.set(index,value);
	}

	/**
	 * Sets the observed value by coordinate point.
	 *
	 * @param x x-coordinate.-1 to 1
	 * @param y y-coordinate -1 to 1
	 * @param value Observed value at that coordinate
	 */
	public void setValue( int x , int y , double value ) {
		setValue((y+1)*3+x+1,value);
	}

	/**
	 * Computes the maximum.
	 */
	public void process() {
		solver.solve(Y,X);

		double a = X.data[0];
		double b = X.data[1];
		double c = X.data[2];
		double d = X.data[3];
		double e = X.data[4];

		double bottom = 4*a*c - b*b;
		deltaX = (b*e - 2*d*c)/bottom;
		deltaY = (b*d - 2*a*e)/bottom;
	}

	public double getDeltaX() {
		return deltaX;
	}

	public double getDeltaY() {
		return deltaY;
	}
}
