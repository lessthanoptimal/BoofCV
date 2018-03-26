/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.h;


import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

/**
 * <p>
 * Using linear algebra it computes a planar homography matrix using for or more points. Typically used
 * as an initial estimate for a non-linear optimization.
 * </p>
 *
 * <p>
 * The algorithm works by solving the equation below:<br>
 * hat(x<sub>2</sub>)*H*x<sub>1</sub> = 0<br>
 * where hat(x) is the skew symmetric cross product matrix. To solve this equation is is reformatted into
 * A*H<sup>s</sup>=0 using the Kronecker product and the null space solved for.
 * </p>
 *
 * <p>
 * Primarily based on chapter 4 in, "Multiple View Geometry in Computer Vision"  2nd Ed. but uses normalization
 * from "An Invitation to 3-D Vision" 2004.
 * </p>
 *
 * @author Peter Abeles
 */
public class HomographyDirectLinearTransform {

	// contains the set of equations that are solved
	protected DMatrixRMaj A = new DMatrixRMaj(1,9);
	protected SolveNullSpace<DMatrixRMaj> solverNullspace = new SolveNullSpaceSvd_DDRM();

	// Used to normalize input points
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();
	protected NormalizationPoint2D N2 = new NormalizationPoint2D();

	// pick a reasonable scale and sign
	private AdjustHomographyMatrix adjust = new AdjustHomographyMatrix();

	// normalize image coordinates to avoid numerical errors?
	boolean normalize;

	/**
	 * Configure homography calculation
	 *
	 * @param normalizeInput Should image coordinate be normalized?  Needed when coordinates are in units of pixels.
	 */
	public HomographyDirectLinearTransform(boolean normalizeInput) {
		this.normalize = normalizeInput;
	}

	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images.  A set of {@link AssociatedPair}
	 * is passed in.  The computed homography 'H' is found such that the attributes 'p1' and 'p2' in {@link AssociatedPair}
	 * refers to x1 and x2, respectively, in the equation  below:<br>
	 * x<sub>2</sub> = H*x<sub>1</sub>
	 * </p>
	 *
	 * @param points A set of observed image points that are generated from a planar object.  Minimum of 4 pairs required.
	 * @param foundH Output: Storage for the found solution. 3x3 matrix.
	 * @return true if the calculation was a success.
	 */
	public boolean process( List<AssociatedPair> points , DMatrixRMaj foundH ) {
		if( points.size() < 4 )
			throw new IllegalArgumentException("Must be at least 4 points.");

		if( normalize ) {
			LowLevelMultiViewOps.computeNormalization(points, N1, N2);

			createANormalized(points, A);
		} else {
			createA(points,A);
		}

		// compute the homograph matrix up to a scale factor
		if (computeH(A,foundH))
			return false;

		if( normalize )
			undoNormalizationH(foundH,N1,N2);

		// pick a good scale and sign for H
		adjust.adjust(foundH,points.get(0));

		return true;
	}


	/**
	 * Computes the SVD of A and extracts the homography matrix from its null space
	 */
	protected boolean computeH(DMatrixRMaj A, DMatrixRMaj H) {

		if( !solverNullspace.process(A.copy(),1,H) )
			return true;

		H.numRows = 3;
		H.numCols = 3;

		return false;
	}

	/**
	 * Undoes normalization for a homography matrix.
	 */
	public static void undoNormalizationH(DMatrixRMaj M, NormalizationPoint2D N1, NormalizationPoint2D N2) {
		SimpleMatrix a = SimpleMatrix.wrap(M);
		SimpleMatrix b = SimpleMatrix.wrap(N1.matrix());
		SimpleMatrix c_inv = SimpleMatrix.wrap(N2.matrixInv());

		SimpleMatrix result = c_inv.mult(a).mult(b);

		M.set(result.getDDRM());
	}

	/**
	 * Compute the 'A' matrix used to solve for H from normalized points.
	 */
	protected void createANormalized(List<AssociatedPair> points, DMatrixRMaj A) {
		A.reshape(points.size()*2,9, false);
		A.zero();

		Point2D_F64 f_norm = new Point2D_F64();
		Point2D_F64 s_norm = new Point2D_F64();

		final int size = points.size();
		for( int i = 0; i < size; i++ ) {
			AssociatedPair p = points.get(i);

			// the first image
			Point2D_F64 f = p.p1;
			// the second image
			Point2D_F64 s = p.p2;

			// normalize the points
			N1.apply(f, f_norm);
			N2.apply(s, s_norm);

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

/**
	 * Compute the 'A' matrix used to solve for H from un-normalized points.
	 */
	protected void createA(List<AssociatedPair> points, DMatrixRMaj A) {
		A.reshape(points.size()*2,9, false);
		A.zero();

		final int size = points.size();
		for( int i = 0; i < size; i++ ) {
			AssociatedPair p = points.get(i);

			// the first image
			Point2D_F64 f = p.p1;
			// the second image
			Point2D_F64 s = p.p2;

			A.set(i*2   , 3 , -f.x);
			A.set(i*2   , 4 , -f.y);
			A.set(i*2   , 5 , -1);
			A.set(i*2   , 6 , s.y*f.x);
			A.set(i*2   , 7 , s.y*f.y);
			A.set(i*2   , 8 , s.y);
			A.set(i*2+1 , 0 , f.x);
			A.set(i*2+1 , 1 , f.y);
			A.set(i*2+1 , 2 , 1);
			A.set(i*2+1 , 6 , -s.x*f.x);
			A.set(i*2+1 , 7 , -s.x*f.y);
			A.set(i*2+1 , 8 , -s.x);
		}
	}
}
