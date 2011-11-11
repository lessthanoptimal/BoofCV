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

import georegression.struct.point.Point2D_F64;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;


/**
 * <p>
 * Base class for linear algorithm which solve the epipolar constraints to compute the essential/fundamental or
 * homography matrix.  The solution will be optimal in a abstract linear sense.  To produce a solution which is
 * optimal in more relevant metrics, such as Euclidean, non-linear optimization is required.
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004 </li>
 * <li> R. Hartley, and A. Zisserman, "Multi View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 * </p>
 *
 * @author Peter Abeles
 */
public class EpipolarConstraintMatricesLinear {
	// contains the set of equations that are solved
	protected DenseMatrix64F A = new DenseMatrix64F(1,9);
	protected SingularValueDecomposition<DenseMatrix64F> svd = DecompositionFactory.svd(0,0,true,true,false);

	// either the fundamental or homography matrix
	protected DenseMatrix64F M = new DenseMatrix64F(3,3);

	protected DenseMatrix64F temp0 = new DenseMatrix64F(3,3);

	// matrix used to normalize results
	protected DenseMatrix64F N1 = new DenseMatrix64F(3,3);
	protected DenseMatrix64F N2 = new DenseMatrix64F(3,3);

	// if true it will normalize observations. Normalization will reduce numerical issues
	// and is needed when dealing with pixel coordinates due to the wide range of values from small to large.
	protected boolean normalize = true;

	/**
	 * Returns the fundamental or homography matrix depending on which operation was called.
	 *
	 * @return F or H matrix. This matrix is saved internally and reused.
	 */
	public DenseMatrix64F getEpipolarMatrix() {
		return M;
	}

	/**
	 * Sets points normalization on and off.  Turn normalization on if dealing with pixels
	 * and off if with normalized image coordinates.  Note that having normalization on when
	 * it is not needed will not adversely affect the solution, except make it more computationally
	 * expensive to compute.
	 *
	 * @param normalize The new normalization value
	 */
	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	/**
	 * Computes the epipolar constraint matrix by performing a SVD on A.  The column of V corresponding
	 * to the smallest singular value of A is the solution we are looking for.
	 */
	protected boolean computeMatrix( SingularValueDecomposition<DenseMatrix64F> svd , DenseMatrix64F A ) {
		if( !svd.decompose(A) )
			return true;

		SingularOps.nullSpace(svd,M);

		return false;
	}

	/**
	 * Makes sure the smallest SVD in F is equal to zero.  This is done by finding the smallest SVD
	 * and setting it to zero then recomputing F.
	 *
	 * @return true if svd returned true.
	 */
	protected boolean enforceSmallZeroSingularValue() {
		int indexSmallest;
		if( !svd.decompose(M) ) {
			return false;
		}

		indexSmallest = findSmallestSingularValue(svd);

		DenseMatrix64F V = svd.getV(false);
		DenseMatrix64F U = svd.getU(false);
		DenseMatrix64F D = svd.getW(null);

		// force the smallest SVD to be zero
		D.set(indexSmallest,indexSmallest,0);

		// recompute F
		CommonOps.mult(U,D,temp0);
		CommonOps.multTransB(temp0,V, M);

		return true;
	}

	private static int findSmallestSingularValue(SingularValueDecomposition svd ) {
		double smallestVal = Double.MAX_VALUE;
		int indexSmallest = -1;

		double s[] = svd.getSingularValues();
		int N = svd.numberOfSingularValues();
		for( int i = 0; i < N; i++ ) {
			if( s[i] < smallestVal ) {
				smallestVal = s[i];
				indexSmallest = i;
			}
		}
		return indexSmallest;
	}

	/**
	 * Given the normalization matrix it will normalize the point
	 */
	protected static void normalize( Point2D_F64 orig , Point2D_F64 normed , DenseMatrix64F N ) {
		normed.x = orig.x * N.data[0] + N.data[2];
		normed.y = orig.y * N.data[4] + N.data[5];
	}

}
