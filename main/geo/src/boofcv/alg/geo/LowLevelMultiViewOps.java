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

package boofcv.alg.geo;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Lists of operations used by various multi-view algorithms, but not of use to the typical user.
 *
 * @author Peter Abeles
 */
public class LowLevelMultiViewOps {
	/**
	 * <p>
	 * Computes a normalization matrix used to reduce numerical errors inside of linear estimation algorithms.
	 * Pixels coordinates are poorly scaled for linear algebra operations resulting in excessive numerical error.
	 * This function computes a transform which will minimize numerical error by properly scaling the pixels.
	 * </p>
	 *
	 * <pre>
	 * N = [ 1/&sigma;_x     0      -&mu;_x/&sigma;_x ]
	 *     [    0   1/&sigma;_y 0   -&mu;_y/&sigma;_y ]
	 *     [    0      0          1    ]
	 * </pre>
	 *
	 * <p>
	 * Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004
	 * </p>
	 *
	 * @param points Input: List of observed points. Not modified.
	 * @param N Output: 3x3 normalization matrix for first set of points. Modified.
	 */
	public static void computeNormalization(List<Point2D_F64> points, DenseMatrix64F N )
	{
		double meanX1 = 0;
		double meanY1 = 0;

		for( Point2D_F64 p : points ) {
			meanX1 += p.x;
			meanY1 += p.y;
		}

		meanX1 /= points.size();
		meanY1 /= points.size();

		double stdX1 = 0;
		double stdY1 = 0;

		for( Point2D_F64 p : points ) {
			double dx = p.x - meanX1;
			double dy = p.y - meanY1;
			stdX1 += dx*dx;
			stdY1 += dy*dy;
		}

		stdX1 = Math.sqrt(stdX1/points.size());
		stdY1 = Math.sqrt(stdY1/points.size());
		N.zero();

		N.set(0, 0, 1.0 / stdX1);
		N.set(1, 1, 1.0 / stdY1);
		N.set(0, 2, -meanX1 / stdX1);
		N.set(1, 2, -meanY1 / stdY1);
		N.set(2, 2, 1.0);
	}

	/**
	 * <p>
	 * Computes two normalization matrices for each set of point correspondences in the list of
	 * {@link boofcv.struct.geo.AssociatedPair}.  Same as {@link #computeNormalization(java.util.List, org.ejml.data.DenseMatrix64F)},
	 * but for two views.
	 * </p>
	 *
	 * @param points Input: List of observed points that are to be normalized. Not modified.
	 * @param N1 Output: 3x3 normalization matrix for first set of points. Modified.
	 * @param N2 Output: 3x3 normalization matrix for second set of points. Modified.
	 */
	public static void computeNormalization(List<AssociatedPair> points, DenseMatrix64F N1, DenseMatrix64F N2)
	{
		double meanX1 = 0; double meanY1 = 0;
		double meanX2 = 0; double meanY2 = 0;

		for( AssociatedPair p : points ) {
			meanX1 += p.p1.x;  meanY1 += p.p1.y;
			meanX2 += p.p2.x; meanY2 += p.p2.y;
		}

		meanX1 /= points.size(); meanY1 /= points.size();
		meanX2 /= points.size(); meanY2 /= points.size();

		double stdX1 = 0; double stdY1 = 0;
		double stdX2 = 0; double stdY2 = 0;

		for( AssociatedPair p : points ) {
			double dx = p.p1.x - meanX1;
			double dy = p.p1.y - meanY1;
			stdX1 += dx*dx;
			stdY1 += dy*dy;

			dx = p.p2.x - meanX2;
			dy = p.p2.y - meanY2;
			stdX2 += dx*dx;
			stdY2 += dy*dy;
		}

		stdX1 = Math.sqrt(stdX1/points.size()); stdY1 = Math.sqrt(stdY1/points.size());
		stdX2 = Math.sqrt(stdX2/points.size()); stdY2 = Math.sqrt(stdY2/points.size());

		N1.zero(); N2.zero();

		N1.set(0,0,1.0/stdX1);
		N1.set(1,1,1.0/stdY1);
		N1.set(0,2,-meanX1/stdX1);
		N1.set(1,2,-meanY1/stdY1);
		N1.set(2,2,1.0);

		N2.set(0,0,1.0/stdX2);
		N2.set(1,1,1.0/stdY2);
		N2.set(0,2,-meanX2/stdX2);
		N2.set(1,2,-meanY2/stdY2);
		N2.set(2,2,1.0);
	}

	/**
	 * <p>
	 * Computes three normalization matrices for each set of point correspondences in the list of
	 * {@link boofcv.struct.geo.AssociatedTriple}.  Same as {@link #computeNormalization(java.util.List, org.ejml.data.DenseMatrix64F)},
	 * but for three views.
	 * </p>
	 *
	 * @param points Input: List of observed points that are to be normalized. Not modified.
	 * @param N1 Output: 3x3 normalization matrix for first set of points. Modified.
	 * @param N2 Output: 3x3 normalization matrix for second set of points. Modified.
	 * @param N3 Output: 3x3 normalization matrix for third set of points. Modified.
	 */
	public static void computeNormalization( List<AssociatedTriple> points,
											 DenseMatrix64F N1, DenseMatrix64F N2, DenseMatrix64F N3 )
	{
		double meanX1 = 0; double meanY1 = 0;
		double meanX2 = 0; double meanY2 = 0;
		double meanX3 = 0; double meanY3 = 0;

		for( AssociatedTriple p : points ) {
			meanX1 += p.p1.x; meanY1 += p.p1.y;
			meanX2 += p.p2.x; meanY2 += p.p2.y;
			meanX3 += p.p3.x; meanY3 += p.p3.y;
		}

		meanX1 /= points.size(); meanY1 /= points.size();
		meanX2 /= points.size(); meanY2 /= points.size();
		meanX3 /= points.size(); meanY3 /= points.size();

		double stdX1 = 0; double stdY1 = 0;
		double stdX2 = 0; double stdY2 = 0;
		double stdX3 = 0; double stdY3 = 0;

		for( AssociatedTriple p : points ) {
			double dx = p.p1.x - meanX1; double dy = p.p1.y - meanY1;
			stdX1 += dx*dx; stdY1 += dy*dy;

			dx = p.p2.x - meanX2; dy = p.p2.y - meanY2;
			stdX2 += dx*dx; stdY2 += dy*dy;

			dx = p.p3.x - meanX3; dy = p.p3.y - meanY3;
			stdX3 += dx*dx; stdY3 += dy*dy;
		}

		stdX1 = Math.sqrt(stdX1/points.size()); stdY1 = Math.sqrt(stdY1/points.size());
		stdX2 = Math.sqrt(stdX2/points.size()); stdY2 = Math.sqrt(stdY2/points.size());
		stdX3 = Math.sqrt(stdX3/points.size()); stdY3 = Math.sqrt(stdY3/points.size());

		N1.zero(); N2.zero(); N3.zero();

		N1.set(0,0,1.0/stdX1);
		N1.set(1,1,1.0/stdY1);
		N1.set(0,2,-meanX1/stdX1);
		N1.set(1,2,-meanY1/stdY1);
		N1.set(2,2,1.0);

		N2.set(0,0,1.0/stdX2);
		N2.set(1,1,1.0/stdY2);
		N2.set(0,2,-meanX2/stdX2);
		N2.set(1,2,-meanY2/stdY2);
		N2.set(2,2,1.0);

		N3.set(0,0,1.0/stdX3);
		N3.set(1,1,1.0/stdY3);
		N3.set(0,2,-meanX3/stdX3);
		N3.set(1,2,-meanY3/stdY3);
		N3.set(2,2,1.0);
	}

	/**
	 * Given the normalization matrix computed from {@link #computeNormalization(java.util.List, org.ejml.data.DenseMatrix64F)}
	 * normalize the point.
	 *
	 * @param N Normalization matrix.
	 * @param orig Unnormalized coordinate in pixels.
	 * @param normed Normalized coordinate.
	 */
	public static void applyPixelNormalization(DenseMatrix64F N, Point2D_F64 orig, Point2D_F64 normed) {
		normed.x = orig.x * N.data[0] + N.data[2];
		normed.y = orig.y * N.data[4] + N.data[5];
	}
}
