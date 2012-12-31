/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.f;


import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.SingularOps;
import org.ejml.ops.SpecializedOps;

import java.util.List;

/**
 * <p>
 * Given a set of 8 or more points this class computes the essential or fundamental matrix.  The result is
 * often used as an initial guess for more accurate non-linear approaches.
 * </p>
 *
 * <p>
 * The computed fundamental matrix follow the following convention (with no noise) for the associated pair:
 * x2<sup>T</sup>*F*x1 = 0<br>
 * x1 = keyLoc and x2 = currLoc.
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
public class FundamentalLinear8 extends FundamentalLinear {

	/**
	 * Specifies which type of matrix is to be computed
	 *
	 * @param computeFundamental true it computes a fundamental matrix and false for essential
	 */
	public FundamentalLinear8( boolean computeFundamental ) {
		super(computeFundamental);
	}

	/**
	 * <p>
	 * Computes a fundamental or essential matrix from a set of associated point correspondences.
	 * </p>
	 *
	 * @param points List of corresponding image coordinates. In pixel for fundamental matrix or
	 *               normalized coordinates for essential matrix.
	 * @return true If successful or false if it failed
	 */
	public boolean process( List<AssociatedPair> points , DenseMatrix64F solution ) {
		if( points.size() < 8 )
			throw new IllegalArgumentException("Must be at least 8 points. Was only "+points.size());

		// use normalized coordinates for pixel and calibrated
		// TODO re-evaluate decision to normalize for calibrated case
		LowLevelMultiViewOps.computeNormalization(points, N1, N2);
		createA(points,A);

		if (process(A,solution))
			return false;

		undoNormalizationF(solution,N1,N2);

		if( computeFundamental )
			return projectOntoFundamentalSpace(solution);
		else
			return projectOntoEssential(solution);
	}

	/**
	 * Computes the SVD of A and extracts the essential/fundamental matrix from its null space
	 */
	protected boolean process(DenseMatrix64F A, DenseMatrix64F F ) {
		if( !svdNull.decompose(A) )
			return true;

		if( A.numRows > 8 )
			SingularOps.nullVector(svdNull,true,F);
		else {
			// handle a special case since the matrix only has 8 singular values and won't select
			// the correct column
			DenseMatrix64F V = svdNull.getV(null,false);
			SpecializedOps.subvector(V, 0, 8, V.numCols, false, 0, F);
		}

		return false;
	}
}
