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
 * Quadratic solver for an arbitrary 2D region
 *
 * @author Peter Abeles
 */
public class FitQuadratic2D {

	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(10,6);

	DenseMatrix64F A = new DenseMatrix64F(1,6);
	DenseMatrix64F b = new DenseMatrix64F(1,1);
	DenseMatrix64F x = new DenseMatrix64F(6,1);

	double foundX;
	double foundY;

	
	public void reset() {
		A.reshape(0,6);
		b.reshape(0,0);
	}
	
	public void add( double x , double y , double value ) {

		int row = A.numRows;

		// increase the size of A in larger steps for efficiency if needed
		if( A.data.length < 6*row+6 ) {
			int n = A.data.length*2;
			A.reshape(n,1,true);
			b.reshape(n/6,1,true);

		}

		A.reshape(row+1,6,true);
		b.reshape(row+1,1,true);

		A.set(row,0,x*x);
		A.set(row,1,x*y);
		A.set(row,2,y*y);
		A.set(row,3,x);
		A.set(row,4,y);
		A.set(row,5,1);
		
		b.set(row,value);
	}
	
	public boolean process() {
		if( !solver.setA(A))
			return false;
		
		solver.solve(b,x);

		double a = x.data[0];
		double b = x.data[1];
		double c = x.data[2];
		double d = x.data[3];
		double e = x.data[4];

		
		double bottom = 4*a*c - b*b;
		foundX = (b*e - 2*d*c)/bottom;
		foundY = (b*d - 2*a*e)/bottom;

		return true;
	}

	public double getFoundX() {
		return foundX;
	}

	public double getFoundY() {
		return foundY;
	}
}
