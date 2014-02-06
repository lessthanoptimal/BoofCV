/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.pose;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

import java.util.List;

/**
 * <p>
 * Given two views of N objects and the known rotation, estimate the translation. A linear system
 * is constructed from the equations below and solved for.  A minimum of two point observations is required
 * since rotation is already known.  This high level characteristics of this algorithm was
 * stated in [1], but the mathematics were not described or sketched.
 * </p>
 *
 * <p>
 * Derivation:
 * <pre>
 * &lambda;x = R*X + T
 * 0 = hat(x)*R*X + hat(x)*T
 * hat(x)*T = -hat(x)*R*X
 * </pre>
 * where hat(x) is the cross product matrix of the homogeneous (x,y,1) vector, R is a rotation
 * matrix, T is the known translation, and X is the known point in 3D.
 * </p>
 *
 * <p>
 * [1] Tardif, J.-P., Pavlidis, Y., and Daniilidis, K. "Monocular visual odometry in urban
 * environments using an omnidirectional camera," IROS 2008
 * </p>
 *
 * @author Peter Abeles
 */
public class PositionFromPairLinear2 {
	LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(300, 3);
	
	// storage for system of equations
	DenseMatrix64F A = new DenseMatrix64F(3,3);
	DenseMatrix64F x = new DenseMatrix64F(3,1);
	DenseMatrix64F b = new DenseMatrix64F(3,1);

	Point3D_F64 RX = new Point3D_F64();
	
	// found translation
	Vector3D_F64 T = new Vector3D_F64();
	
	/**
	 * Computes the translation given two or more feature observations and the known rotation
	 *
	 * @param R Rotation matrix. World to view.
	 * @param worldPts Location of features in world coordinates.
	 * @param observed Observations of point in current view.  Normalized coordinates.
	 * @return true if it succeeded.
	 */
	public boolean process( DenseMatrix64F R , List<Point3D_F64> worldPts , List<Point2D_F64> observed )
	{
		if( worldPts.size() != observed.size() )
			throw new IllegalArgumentException("Number of worldPts and observed must be the same");
		if( worldPts.size() < 2 )
			throw new IllegalArgumentException("A minimum of two points are required");
		
		int N = worldPts.size();
		
		A.reshape(3*N,3); b.reshape(A.numRows, 1);

		
		for( int i = 0; i < N; i++ ) {
			Point3D_F64 X = worldPts.get(i);
			Point2D_F64 o = observed.get(i);
			
			int indexA = i*3*3;
			int indexB = i*3;

			A.data[indexA+1] = -1;
			A.data[indexA+2] = o.y;
			A.data[indexA+3] = 1;
			A.data[indexA+5] = -o.x;
			A.data[indexA+6] = -o.y;
			A.data[indexA+7] = o.x;

			GeometryMath_F64.mult(R,X,RX);

			b.data[indexB++] =   1*RX.y - o.y*RX.z;
			b.data[indexB++] =  -1*RX.x + o.x*RX.z;
			b.data[indexB  ] = o.y*RX.x - o.x*RX.y;
		}
		
		if( !solver.setA(A) )
			return false;
		
		solver.solve(b,x);
		
		T.x = x.data[0];
		T.y = x.data[1];
		T.z = x.data[2];
		
		return true;
	}

	public Vector3D_F64 getT() {
		return T;
	}
}
