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

package boofcv.alg.geo.h;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PairLineNorm;
import georegression.geometry.GeometryMath_F64;
import org.ejml.alg.dense.decomposition.svd.SafeSvd;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.DecompositionFactory;
import org.ejml.interfaces.decomposition.SingularValueDecomposition;
import org.ejml.ops.CommonOps;

import java.util.Arrays;

/**
 * The scale and sign of a homography matrix is ambiguous.  This contains functions which pick a reasonable scale
 * and the correct sign.  The second smallest singular value is set to one and the sign is chosen such that
 * the basic properties work.
 *
 * @author Peter Abeles
 */
public class AdjustHomographyMatrix {

	protected SingularValueDecomposition<DenseMatrix64F> svd = new SafeSvd(DecompositionFactory.svd(0, 0, true, true, false));

	DenseMatrix64F H_t = new DenseMatrix64F(3,3);

	public boolean adjust( DenseMatrix64F H , AssociatedPair p ) {
		if( !findScaleH(H) )
			return false;

		adjustHomographSign(p,H);

		return true;
	}

	public boolean adjust( DenseMatrix64F H , PairLineNorm p ) {
		if( !findScaleH(H) )
			return false;

		adjustHomographSign(p,H);

		return true;
	}

	/**
	 * The scale of H is found by computing the second smallest singular value.
	 */
	protected boolean findScaleH( DenseMatrix64F H ) {
		if( !svd.decompose(H) )
			return false;

		Arrays.sort(svd.getSingularValues(), 0, 3);

		double scale = svd.getSingularValues()[1];
		CommonOps.divide(H,scale);

		return true;
	}

	/**
	 * Since the sign of the homography is ambiguous a point is required to make sure the correct
	 * one was selected.
	 *
	 * @param p test point, used to determine the sign of the matrix.
	 */
	protected void adjustHomographSign( AssociatedPair p , DenseMatrix64F H ) {
		double val = GeometryMath_F64.innerProd(p.p2, H, p.p1);

		if( val < 0 )
			CommonOps.scale(-1, H);
	}

	/**
	 * Since the sign of the homography is ambiguous a point is required to make sure the correct
	 * one was selected.
	 *
	 * @param p test point, used to determine the sign of the matrix.
	 */
	protected void adjustHomographSign( PairLineNorm p , DenseMatrix64F H ) {

		CommonOps.transpose(H,H_t);

		double val = GeometryMath_F64.innerProd(p.l1, H_t, p.l2);

		if( val < 0 )
			CommonOps.scale(-1, H);
	}
}
