/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.epipolar;


import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

/**
 * <p>
 * Given a set of points this class computes the  essential or fundamental matrix (depending on if the input puts are
 * normalized or not) that define epipolar constraints of the form:<br>
 * <br>
 * x<sup>T</sup><sub>2</sub>*F*x<sub>1</sub> = 0<br>
 * where F is a matrix,  x is a pixel location in the right (2) or left (1) images or in the case
 * of a plane:<br>
 * hat{x}<sub>2</sub>H x<sub>1</sub>
 * where hat(x) is a skew symmetric cross product matrix. The solution to these problems are
 * computed using a linear least-squares approach.  The result is often used as an initial guess
 * for more accurate non-linear approaches.
 * </p>
 *
 * <p>
 * When dealing with points in pixel coordinates then normalization is required to reduce numerical errors.
 * </p>
 *
 * @author Peter Abeles
 */
public class FundamentalLinear8 extends EpipolarConstraintMatricesLinear {

	/**
	 * <p>
	 * Computes a fundamental matrix from a set of associated points. This formulation requires a minimum
	 * of eight points.  If input points are poorly condition for linear operations (in pixel coordinates)
	 * then make sure normalization is turned on.
	 * </p>
	 *
	 * <p>
	 * Follows the procedures outlined in "An Invitation to 3-D Vision" 2004 with some minor modifications.
	 * </p>
	 *
	 * @param points List of correlated points in image coordinates from perspectives. Either in pixel or normalized image coordinates.
	 * @return true if it thinks it succeeded and false if it knows it failed.
	 */
	public boolean process( List<AssociatedPair> points ) {
		if( points.size() < 8 )
			throw new IllegalArgumentException("Must be at least 8 points. Was only "+points.size());

		if( normalize ) {
			UtilEpipolar.computeNormalization(N1, N2, points);

			createA(points,A);
		} else {
			createA_nonorm(points,A);
		}

		if (computeMatrix(svd,A))
			return false;

		if( normalize )
			undoNormalizationF(M,N1,N2);

		// just happens that since F is in a row major format there is no need to copy any memory
		return enforceSmallZeroSingularValue();
	}

	/**
	 * Undo the normalization done to the input matrices for a Fundamental matrix.
	 * <br>
	 * M = N<sub>2</sub><sup>T</sup>*M*N<sub>1</sub>
	 *
	 * @param M Either the homography or fundamental matrix computed from normalized points.
	 * @param N1 normalization matrix.
	 * @param N2 normalization matrix.
	 */
	protected void undoNormalizationF(DenseMatrix64F M, DenseMatrix64F N1, DenseMatrix64F N2) {
		SimpleMatrix a = SimpleMatrix.wrap(M);
		SimpleMatrix b = SimpleMatrix.wrap(N1);
		SimpleMatrix c = SimpleMatrix.wrap(N2);

		SimpleMatrix result = c.transpose().mult(a).mult(b);

		M.set(result.getMatrix());
	}

	/**
	 * Reorganizes the epipolar constraint equation (x<sup>T</sup><sub>2</sub>*F*x<sub>1</sub> = 0) such that it
	 * is formulated as a standard linear system of the form Ax=0.  Where A contains the pixel locations and x is
	 * the reformatted fundamental matrix.
	 *
	 * @param points Set of associated points in left and right images.
	 * @param A Matrix where the reformatted points are written to.
	 */
	protected void createA(List<AssociatedPair> points, DenseMatrix64F A ) {
		A.reshape(points.size(),9, false);
		A.zero();

		Point2D_F64 f_norm = new Point2D_F64();
		Point2D_F64 s_norm = new Point2D_F64();

		final int size = points.size();
		for( int i = 0; i < size; i++ ) {
			AssociatedPair p = points.get(i);

			Point2D_F64 f = p.keyLoc;
			Point2D_F64 s = p.currLoc;

			// normalize the points
			normalize(f,f_norm,N1);
			normalize(s,s_norm,N2);

			// perform the Kronecker product with the two points being in
			// homogeneous coordinates (z=1)
			A.set(i,0,s_norm.x*f_norm.x);
			A.set(i,1,s_norm.x*f_norm.y);
			A.set(i,2,s_norm.x);
			A.set(i,3,s_norm.y*f_norm.x);
			A.set(i,4,s_norm.y*f_norm.y);
			A.set(i,5,s_norm.y);
			A.set(i,6,f_norm.x);
			A.set(i,7,f_norm.y);
			A.set(i,8,1);
		}
	}

	/**
	 * Same as {@link #createA} but does not normalize and should be slightly faster.
	 */
	protected void createA_nonorm(List<AssociatedPair> points, DenseMatrix64F A ) {
		A.reshape(points.size(),9, false);
		A.zero();

		final int size = points.size();
		for( int i = 0; i < size; i++ ) {
			AssociatedPair p = points.get(i);

			Point2D_F64 f = p.keyLoc;
			Point2D_F64 s = p.currLoc;

			// perform the Kronecker product with the two points being in
			// homogeneous coordinates (z=1)
			A.set(i,0,s.x*f.x);
			A.set(i,1,s.x*f.y);
			A.set(i,2,s.x);
			A.set(i,3,s.y*f.x);
			A.set(i,4,s.y*f.y);
			A.set(i,5,s.y);
			A.set(i,6,f.x);
			A.set(i,7,f.y);
			A.set(i,8,1);
		}
	}

}
