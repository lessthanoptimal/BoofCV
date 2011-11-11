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
import org.ejml.alg.dense.mult.VectorVectorMult;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

/**
 * <p>
 * Using linear algebra it computes a planar homography matrix up to a scale factor:<br>
 * H<sub>s</sub> = &alpha;H<br>
 * where H<sub>s</sub> is the returned homography matrix.
 * </p>
 * <p>
 * This follows the procedure described in "An Invitation to 3-D Vision" 2004, but with out normalization TODO ???
 * since it was found to not be of much practical use.  If this matrix was to be decomposed further then
 * the normalization procedure would need to be followed.  A minimum number of four points are required.
 * </p>
 *
 * <p>
 * Primarily based on chapter 4 in, "Multiple View Geometry in Computer Vision"  2nd Ed. but uses normalization
 * from "An Invitation to 3-D Vision" 2004.
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyLinear4 extends EpipolarConstraintMatricesLinear {
	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images.
	 * </p>
	 *
	 * @param points A set of points that are generated from a planar object.  Minimum of 4 points required.
	 * @return true if the calculation was a success.
	 */
	public boolean process( List<AssociatedPair> points ) {
		if( points.size() < 4 )
			throw new IllegalArgumentException("Must be at least 4 points.");

		if( normalize ) {
			UtilEpipolar.computeNormalization(N1, N2, points);

			createHomoA(points,A);
		} else {
			// todo make this
			throw new RuntimeException("Un-normalized homography hasn't been created yet");
		}

		// compute the homograph matrix up to a scale factor
		if (computeMatrix(svd,A))
			return false;

		if( normalize )
			undoNormalizationH(M,N1,N2);

		adjustHomographSign(points.get(0));

		return true;
	}


	/**
	 * Undoes normalization for a homography matrix.
	 */
	protected void undoNormalizationH(DenseMatrix64F M, DenseMatrix64F N1, DenseMatrix64F N2) {
		SimpleMatrix a = SimpleMatrix.wrap(M);
		SimpleMatrix b = SimpleMatrix.wrap(N1);
		SimpleMatrix c = SimpleMatrix.wrap(N2);

		SimpleMatrix result = c.invert().mult(a).mult(b);

		M.set(result.getMatrix());
	}

	/**
	 * Since the sign of the homography is ambiguous a point is required to make sure the correct
	 * one was selected.
	 *
	 * @param p test point, used to determine the sign of the matrix.
	 */
	protected void adjustHomographSign( AssociatedPair p ) {
		// now figure out the sign
		DenseMatrix64F x1 = new DenseMatrix64F(3,1,true,p.currLoc.x,p.currLoc.y,1);
		DenseMatrix64F x2 = new DenseMatrix64F(3,1,true,p.keyLoc.x,p.keyLoc.y,1);

		double val = VectorVectorMult.innerProdA(x2, M, x1);
		if( val < 0 )
			CommonOps.scale(-1, M);
	}

	/**
	 * Compute the 'A' matrix for a homography.
	 *
	 * @param points
	 * @param A
	 */
	protected void createHomoA(List<AssociatedPair> points, DenseMatrix64F A ) {
		A.reshape(points.size()*2,9, false);
		A.zero();

		Point2D_F64 f_norm = new Point2D_F64();
		Point2D_F64 s_norm = new Point2D_F64();

		final int size = points.size();
		for( int i = 0; i < size; i++ ) {
			AssociatedPair p = points.get(i);

			// the first image
			Point2D_F64 f = p.keyLoc;
			// the second image
			Point2D_F64 s = p.currLoc;

			// normalize the points
			normalize(f,f_norm,N1);
			normalize(s,s_norm,N2);

			A.set(i*2   , 3 , -f_norm.x);
			A.set(i*2   , 4 , -f_norm.y);
			A.set(i*2   , 5 , -1);
			A.set(i*2   , 6 , s_norm.y*f_norm.x);
			A.set(i*2   , 7 , s_norm.y*f_norm.y);
			A.set(i*2   , 8 , s_norm.y);
			A.set(i*2+1 , 0 , f_norm.x);
			A.set(i*2+1 , 1 , f_norm.y);
			A.set(i*2+1 , 2 , 1);
			A.set(i*2+1 , 6 , -s_norm.x*f_norm.x);
			A.set(i*2+1 , 7 , -s_norm.x*f_norm.y);
			A.set(i*2+1 , 8 , -s_norm.x);
		}
	}
}
