/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.MassageSingularValues;
import boofcv.alg.geo.NormalizationPoint2D;
import boofcv.misc.BoofLambdas;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;

import java.util.List;

/**
 * <p>
 * Base class for linear algebra based algorithms for computing the Fundamental/Essential matrices.
 * </p>
 *
 * <p>
 * The computed fundamental matrix follow the following convention (with no noise) for the associated pair:
 * x2<sup>T</sup>*F*x1 = 0<br>
 * x1 = keyLoc and x2 = currLoc.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class FundamentalLinear {

	// contains the set of equations that are solved
	protected DMatrixRMaj A = new DMatrixRMaj(1, 9);
	// svd used to extract the null space
	protected SolveNullSpace<DMatrixRMaj> solverNull = new SolveNullSpaceSvd_DDRM();

	// Used to put a matrix onto essential or fundamental space
	protected MassageSingularValues massger = new MassageSingularValues();
	protected BoofLambdas.ProcessObject<DMatrixRMaj> opEssential;
	protected BoofLambdas.ProcessObject<DMatrixRMaj> opFundamental;

	// matrix used to normalize results
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();
	protected NormalizationPoint2D N2 = new NormalizationPoint2D();

	/** should it compute a fundamental (true) or essential (false) matrix? */
	@Getter boolean computeFundamental;

	/**
	 * Specifies which type of matrix is to be computed
	 *
	 * @param computeFundamental true it computes a fundamental matrix and false for essential
	 */
	protected FundamentalLinear( boolean computeFundamental ) {
		this.computeFundamental = computeFundamental;

		opEssential = ( W ) -> {
			// project it into essential space
			// the scale factor is arbitrary, but the first two singular values need
			// to be the same. so just set them to one
			W.unsafe_set(0, 0, 1);
			W.unsafe_set(1, 1, 1);
			W.unsafe_set(2, 2, 0);
		};

		opFundamental = ( W ) -> {
			// the smallest singular value needs to be set to zero, unlike
			W.set(2, 2, 0);
		};
	}

	/**
	 * Projects the found estimate of E onto essential space.
	 *
	 * @return true if svd returned true.
	 */
	protected boolean projectOntoEssential( DMatrixRMaj E ) {
		return massger.process(E, opEssential);
	}

	/**
	 * Projects the found estimate of F onto Fundamental space.
	 *
	 * @return true if svd returned true.
	 */
	protected boolean projectOntoFundamentalSpace( DMatrixRMaj F ) {
		return massger.process(F, opFundamental);
	}

	/**
	 * Reorganizes the epipolar constraint equation (x<sup>T</sup><sub>2</sub>*F*x<sub>1</sub> = 0) such that it
	 * is formulated as a standard linear system of the form Ax=0. Where A contains the pixel locations and x is
	 * the reformatted fundamental matrix.
	 *
	 * @param points Set of associated points in left and right images.
	 * @param A Matrix where the reformatted points are written to.
	 */
	protected void createA( List<AssociatedPair> points, DMatrixRMaj A ) {
		A.reshape(points.size(), 9, false);
		A.zero();

		Point2D_F64 f_norm = new Point2D_F64();
		Point2D_F64 s_norm = new Point2D_F64();

		final int size = points.size();
		for (int i = 0; i < size; i++) {
			AssociatedPair p = points.get(i);

			Point2D_F64 f = p.p1;
			Point2D_F64 s = p.p2;

			// normalize the points
			N1.apply(f, f_norm);
			N2.apply(s, s_norm);

			// perform the Kronecker product with the two points being in
			// homogeneous coordinates (z=1)
			A.unsafe_set(i, 0, s_norm.x*f_norm.x);
			A.unsafe_set(i, 1, s_norm.x*f_norm.y);
			A.unsafe_set(i, 2, s_norm.x);
			A.unsafe_set(i, 3, s_norm.y*f_norm.x);
			A.unsafe_set(i, 4, s_norm.y*f_norm.y);
			A.unsafe_set(i, 5, s_norm.y);
			A.unsafe_set(i, 6, f_norm.x);
			A.unsafe_set(i, 7, f_norm.y);
			A.unsafe_set(i, 8, 1);
		}
	}
}
