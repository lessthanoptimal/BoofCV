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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;
import org.ejml.UtilEjml;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * <p>
 * Give a set of lines in parametric notation, find the point of intersection between the lines. At least
 * two of the lines must not be parallel and the system is solved for using linear algebra.  The magnitude
 * of the line's slope determines its weight.
 * </p>
 *
 * <p>
 * The dot product of a line's slope and its normal is by definition equal to zero.  Therefor, any point
 * on the line will satisfy this equation: (x_p - x)*n_x + (y_p - y)*n_y = 0, where (x_p,y_p) and (x,p)
 * are points on the line, and (n_x,n_y) is a normal to the line. When (x,y) is the interest section of
 * 2 or more lines it can be solved for by stacking that equation for each line.
 * </p>
 *
 * @author Peter Abeles
 */
public class IntersectLinesLinear {

	// A*x = Y
	// A = [ dx , dy ]
	DenseMatrix64F A = new DenseMatrix64F(1,2);
	// Y = [ x_p*dx + y_p*dy ]
	DenseMatrix64F Y = new DenseMatrix64F(1,1);
	// this is the point of intersection
	DenseMatrix64F X = new DenseMatrix64F(2,1);

	// Used to solve the least squares problem
	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(30,2);

	public void reset() {
		A.reshape(0,2,false);
		Y.reshape(0, 1, false);
	}

	/**
	 * Computes the point of intersection.  Returns true if successful.
	 * @return True if successful and false otherwise.
	 */
	public boolean process( List<LineParametric2D_F64> lines ) {
		createA(lines);

		if( !solver.setA(A))
			return false;

		double quality = solver.quality();
		if( quality < UtilEjml.EPS )
			return false;

		solver.solve(Y,X);

		return true;
	}

	/**
	 * ASet up the linear system
	 */
	private void createA( List<LineParametric2D_F64> lines ) {
		final int N = lines.size();

		A.reshape(N,2,false);
		Y.reshape(N,1,false);

		for( int i = 0; i < N; i++ ) {
			LineParametric2D_F64 l = lines.get(i);
			Point2D_F64 p = l.getPoint();
			Vector2D_F64 v = l.slope;

			// the normal is equal to negative of the slope's inverse
			A.set(i,0,v.y);
			A.set(i,1,-v.x);

			Y.set(i,p.x*v.y - p.y*v.x);
		}

	}

	/**
	 * The point of intersection
	 * @return Point of intersection
	 */
	public Point2D_F64 getPoint() {
		return new Point2D_F64(X.get(0),X.get(1));
	}
}
