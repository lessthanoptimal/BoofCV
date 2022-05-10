/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.dense.row.factory.DecompositionFactory_DDRM;
import org.ejml.interfaces.decomposition.SingularValueDecomposition_F64;

import java.util.List;

/**
 * <p>Estimates homography between two views and independent radial distortion from each camera. Radial
 * distortion is modeled using the one parameter division model [1]. Implementation based
 * on the 6-point algorithm in [2] that solves 2 quadratic equations and 6 linear equations.</p>
 *
 * <p>f(x,y) = [x, y, 1 + &lambda;(x**2 + y**2)]<sup>T</sup></p>
 *
 * <ol>
 *     <li>Fitzgibbon, Andrew W. "Simultaneous linear estimation of multiple view geometry and lens distortion."
 *     CVPR 2001</li>
 *     <li>Radial distortion homography - Kukelova, Z., Heller, J., Bujnak, M., & Pajdla, T. (2015)</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class HomographyRadial6Pts {

	// cross product constraint matrix
	DMatrixRMaj skewA = new DMatrixRMaj(6, 8);
	double crossSingularRatio;

	// Storage for the two null spaces
	DMatrixRMaj null1 = new DMatrixRMaj(8, 1);
	DMatrixRMaj null2 = new DMatrixRMaj(8, 1);



	SingularValueDecomposition_F64<DMatrixRMaj> svd =
			DecompositionFactory_DDRM.svd(6, 8, false, true, true);

	/**
	 * <p>
	 * Computes the homography matrix given a set of observed points in two images. A set of {@link AssociatedPair}
	 * is passed in. The computed homography 'H' is found such that the attributes 'p1' and 'p2' in {@link AssociatedPair}
	 * refers to x1 and x2, respectively, in the equation  below:<br>
	 * x<sub>2</sub> = H*x<sub>1</sub>
	 * </p>
	 *
	 * @param points A set of observed image points that are generated from a planar object. Minimum of 4 pairs required.
	 * @param found Output: Where results are written to
	 * @return True if successful. False if it failed.
	 */
	public boolean process( List<AssociatedPair> points, Results found ) {
		if (points.size() < 6)
			throw new IllegalArgumentException("A minimum of 6 points is required");

		return true;
	}

	/**
	 * Construct a matrix that encapsulates the constraint of applying cross product as a skew symmetric matrix to
	 * input observations:
	 *
	 * alpha*x' = H*x
	 * skew(x')*H*x = 0
	 *
	 * @return true if no errors detected
	 */
	boolean linearCrossConstraint(List<AssociatedPair> points) {
		skewA.reshape(points.size(), 8);

		// NOTE: p2 = x' and p1 = x in paper
		for (int row = 0; row < points.size(); row++) {
			AssociatedPair p = points.get(row);
			int index = row*skewA.numCols;

			// x**2 + y**2
			double r = p.p1.normSq();

			// Matrix is stored in a row-major format. This is filling in a row
			skewA.data[index++] = -p.p2.y*p.p1.x;
			skewA.data[index++] = -p.p2.y*p.p1.y;
			skewA.data[index++] = -p.p2.y;
			skewA.data[index++] = p.p2.x*p.p1.x;
			skewA.data[index++] = p.p2.x*p.p1.y;
			skewA.data[index++] = p.p2.x;
			skewA.data[index++] = -p.p2.y*r;
			skewA.data[index] = p.p2.x*r;
		}

		// Find the null space
		if (!svd.decompose(skewA))
			return false;

		DMatrixRMaj V_t = svd.getV(null, true);
		DMatrixRMaj W = svd.getW(null);

		// Singular values are in an arbitrary order initially
		SingularOps_DDRM.descendingOrder(null, false, W, V_t, true);

		// If there is a well-defined null space then sv[4] >>> sv[5]
		// EPS in denominator to avoid divide by zero
		crossSingularRatio = W.unsafe_get(4,4) / (UtilEjml.EPS + W.unsafe_get(5,5));

		// Space the null space
		CommonOps_DDRM.extract(V_t, 5, 6, 0, 8, null1);
		CommonOps_DDRM.extract(V_t, 6, 7, 0, 8, null2);

		return true;
	}

	/**
	 * The solution vector v1 = gamma*n1 + n2, where (n1, n2) are the previously found null space.
	 * There is a known relaionship between elements in n1 and n2 which is then exploited to create a
	 * quadratic equation to solve for the unknown radial distortion.
	 *
	 * @return true if no errors detected
	 */
	boolean solveQuadraticRelationship() {
		// Note the conversion from 0 indexed to 1 indexed, so that variables match what's in the paper
		double n13 = null1.data[2];
		double n16 = null1.data[5];
		double n23 = null2.data[2];
		double n26 = null2.data[5];
		double n17 = null1.data[6];
		double n18 = null1.data[7];
		double n27 = null2.data[6];
		double n28 = null2.data[7];

		return true;
	}

	public static class Results {
		/** Homography between the two views */
		public final DMatrixRMaj H = new DMatrixRMaj(3,3);
		/** Radial distortion in view-1 and view-2 */
		public double l1, l2;
	}
}
