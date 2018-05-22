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

package boofcv.alg.geo;

import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrix1Row;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Lists of operations used by various multi-view algorithms, but not of use to the typical user.
 *
 * @author Peter Abeles
 */
public class LowLevelMultiViewOps {
	/**
	 * <p>Computes a transform which will normalize the points such that they have zero mean and a standard
	 * deviation of one
	 * </p>
	 *
	 * <p>
	 * Y. Ma, S. Soatto, J. Kosecka, and S. S. Sastry, "An Invitation to 3-D Vision" Springer-Verlad, 2004
	 * </p>
	 *
	 * @param points Input: List of observed points. Not modified.
	 * @param normalize Output: 3x3 normalization matrix for first set of points. Modified.
	 */
	public static void computeNormalization(List<Point2D_F64> points, NormalizationPoint2D normalize )
	{
		double meanX = 0;
		double meanY = 0;

		for( Point2D_F64 p : points ) {
			meanX += p.x;
			meanY += p.y;
		}

		meanX /= points.size();
		meanY /= points.size();

		double stdX = 0;
		double stdY = 0;

		for( Point2D_F64 p : points ) {
			double dx = p.x - meanX;
			double dy = p.y - meanY;
			stdX += dx*dx;
			stdY += dy*dy;
		}

		normalize.meanX = meanX;
		normalize.meanY = meanY;

		normalize.stdX = Math.sqrt(stdX/points.size());
		normalize.stdY = Math.sqrt(stdY/points.size());
	}

	/**
	 * <p>
	 * Computes two normalization matrices for each set of point correspondences in the list of
	 * {@link boofcv.struct.geo.AssociatedPair}.  Same as {@link #computeNormalization(java.util.List, NormalizationPoint2D)},
	 * but for two views.
	 * </p>
	 *
	 * @param points Input: List of observed points that are to be normalized. Not modified.
	 * @param N1 Output: 3x3 normalization matrix for first set of points. Modified.
	 * @param N2 Output: 3x3 normalization matrix for second set of points. Modified.
	 */
	public static void computeNormalization(List<AssociatedPair> points, NormalizationPoint2D N1, NormalizationPoint2D N2)
	{
		double meanX1 = 0; double meanY1 = 0;
		double meanX2 = 0; double meanY2 = 0;

		for( AssociatedPair p : points ) {
			meanX1 += p.p1.x; meanY1 += p.p1.y;
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

		N1.meanX = meanX1; N1.meanY = meanY1;
		N2.meanX = meanX2; N2.meanY = meanY2;

		N1.stdX = Math.sqrt(stdX1/points.size()); N1.stdY = Math.sqrt(stdY1/points.size());
		N2.stdX = Math.sqrt(stdX2/points.size()); N2.stdY = Math.sqrt(stdY2/points.size());
	}

	/**
	 * <p>
	 * Computes three normalization matrices for each set of point correspondences in the list of
	 * {@link boofcv.struct.geo.AssociatedTriple}.  Same as {@link #computeNormalization(java.util.List, NormalizationPoint2D)},
	 * but for three views.
	 * </p>
	 *
	 * @param points Input: List of observed points that are to be normalized. Not modified.
	 * @param N1 Output: 3x3 normalization matrix for first set of points. Modified.
	 * @param N2 Output: 3x3 normalization matrix for second set of points. Modified.
	 * @param N3 Output: 3x3 normalization matrix for third set of points. Modified.
	 */
	public static void computeNormalization( List<AssociatedTriple> points,
											 NormalizationPoint2D N1, NormalizationPoint2D N2, NormalizationPoint2D N3 )
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

		N1.meanX = meanX1; N1.meanY = meanY1;
		N2.meanX = meanX2; N2.meanY = meanY2;
		N3.meanX = meanX3; N3.meanY = meanY3;

		N1.stdX = Math.sqrt(stdX1/points.size()); N1.stdY = Math.sqrt(stdY1/points.size());
		N2.stdX = Math.sqrt(stdX2/points.size()); N2.stdY = Math.sqrt(stdY2/points.size());
		N3.stdX = Math.sqrt(stdX3/points.size()); N3.stdY = Math.sqrt(stdY3/points.size());
	}

	public static void applyNormalization(List<AssociatedPair> points,
										  NormalizationPoint2D N1, NormalizationPoint2D N2,
										  DMatrix1Row X1 , DMatrixRMaj X2 )
	{
		final int size = points.size();

		X1.reshape(size,2);
		X2.reshape(size,2);

		for (int i = 0,index = 0; i < size; i++,index+=2) {
			AssociatedPair pair = points.get(i);

			X1.data[index]   = (pair.p1.x - N1.meanX)/N1.stdX;
			X1.data[index+1] = (pair.p1.y - N1.meanY)/N1.stdY;

			X2.data[index]   = (pair.p2.x - N2.meanX)/N2.stdX;
			X2.data[index+1] = (pair.p2.y - N2.meanY)/N2.stdY;
		}
	}
}
