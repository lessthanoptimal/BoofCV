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
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.SingularValueDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.SingularOps;
import org.ejml.ops.SpecializedOps;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

/**
 * <p>
 * Given a set of 8 or more points this class computes the essential or fundamental matrix.  The result is
 * often used as an initial guess for more accurate non-linear approaches.
 * </p>
 *
 * <p>
 * References:
 * <ul>
 * <li> Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004 </li>
 * <li> R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003 </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class FundamentalLinear8 {

	// contains the set of equations that are solved
	protected DenseMatrix64F A = new DenseMatrix64F(1,9);
	protected SingularValueDecomposition<DenseMatrix64F> svd
			= DecompositionFactory.svd(0, 0, true, true, false);

	// either the fundamental or essential matrix
	protected DenseMatrix64F F = new DenseMatrix64F(3,3);

	// SVD decomposition of F = U*S*V^T
	protected DenseMatrix64F svdU;
	protected DenseMatrix64F svdS;
	protected DenseMatrix64F svdV;

	protected DenseMatrix64F temp0 = new DenseMatrix64F(3,3);

	// matrix used to normalize results
	protected DenseMatrix64F N1 = new DenseMatrix64F(3,3);
	protected DenseMatrix64F N2 = new DenseMatrix64F(3,3);

	// should it compute a fundamental (true) or essential (false) matrix?
	boolean computeFundamental;

	/**
	 * When computing the essential matrix normalization is optional because pixel coordinates
	 *
	 * @param computeFundamental true it computes a fundamental matrix and false for essential
	 */
	public FundamentalLinear8( boolean computeFundamental ) {
		this.computeFundamental = computeFundamental;
	}

	/**
	 * Returns the computed Fundamental or Essential matrix.
	 *
	 * @return Fundamental or Essential matrix.
	 */
	public DenseMatrix64F getF() {
		return F;
	}

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

		// use normalized coordinates for pixel and calibrated
		// un-normalized passed unit tests in 8 point, but failed in 7 point, hinting
		// that there could be situations where normalization might be needed in 8 point
		UtilEpipolar.computeNormalization(N1, N2, points);
		createA(points,A);

		if (process(A))
			return false;

		undoNormalizationF(F,N1,N2);

		if( computeFundamental )
			return projectOntoFundamentalSpace(F);
		else
			return projectOntoEssential(F);
	}

	/**
	 * Computes the SVD of A and extracts the essential/fundamental matrix from its null space
	 */
	protected boolean process(DenseMatrix64F A) {
		if( !svd.decompose(A) )
			return true;

		if( A.numRows > 8 )
			SingularOps.nullSpace(svd,F);
		else {
			// handle a special case since the matrix only has 8 singular values and won't select
			// the correct column
			DenseMatrix64F V = svd.getV(false);
			SpecializedOps.subvector(V, 0, 8, V.numCols, false, 0, F);
		}

		return false;
	}

	/**
	 * Projects the found estimate of E onto essential space.
	 *
	 * @return true if svd returned true.
	 */
	protected boolean projectOntoEssential( DenseMatrix64F E ) {
		if( !svd.decompose(E) ) {
			return false;
		}
		svdV = svd.getV(false);
		svdU = svd.getU(false);
		svdS = svd.getW(null);

		SingularOps.descendingOrder(svdU, false, svdS, svdV, false);

		// project it into essential space
		// the scale factor is arbitrary, but the first two singular values need
		// to be the same.  so just set them to one
		svdS.set(0, 0, 1);
		svdS.set(1, 1, 1);
		svdS.set(2, 2, 0);

		// recompute F
		CommonOps.mult(svdU, svdS, temp0);
		CommonOps.multTransB(temp0,svdV, E);

		return true;
	}

	/**
	 * Projects the found estimate of F onto Fundamental space.
	 *
	 * @return true if svd returned true.
	 */
	protected boolean projectOntoFundamentalSpace( DenseMatrix64F F ) {
		if( !svd.decompose(F) ) {
			return false;
		}
		svdV = svd.getV(false);
		svdU = svd.getU(false);
		svdS = svd.getW(null);

		SingularOps.descendingOrder(svdU, false, svdS, svdV, false);

		// the smallest singular value needs to be set to zero, unlike
		svdS.set(2, 2, 0);

		// recompute F
		CommonOps.mult(svdU, svdS, temp0);
		CommonOps.multTransB(temp0,svdV, F);

		return true;
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
			UtilEpipolar.pixelToNormalized(f, f_norm, N1);
			UtilEpipolar.pixelToNormalized(s, s_norm, N2);

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
	 * Returns the U from the SVD of F.
	 *
	 * @return U matrix.
	 */
	public DenseMatrix64F getSvdU() {
		return svdU;
	}

	/**
	 * Returns the S from the SVD of F.
	 *
	 * @return S matrix.
	 */
	public DenseMatrix64F getSvdS() {
		return svdS;
	}

	/**
	 * Returns the V from the SVD of F.
	 *
	 * @return V matrix.
	 */
	public DenseMatrix64F getSvdV() {
		return svdV;
	}
}
