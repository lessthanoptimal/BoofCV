/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.List;


/**
 * @author Peter Abeles
 */
public class UtilEpipolar {
	/**
	 * <p>
	 * Computes a normalization matrix.  Pixels coordinates are poorly scaled for linear algebra operations resulting in
	 * excessive numerical error.  This function computes a transform which will minimize numerical error
	 * by properly scaling the pixels.
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
	 * @param N1 normalization matrix for first set of points. Modified.
	 * @param N2 normalization matrix for second set of points. Modified.
	 * @param points List of observed points that are to be normalized. Not modified.
	 */
	public static void computeNormalization( DenseMatrix64F N1, DenseMatrix64F N2,
											 List<AssociatedPair> points)
	{
		double meanX1 = 0;
		double meanY1 = 0;
		double meanX2 = 0;
		double meanY2 = 0;

		for( AssociatedPair p : points ) {
			meanX1 += p.keyLoc.x;
			meanY1 += p.keyLoc.y;
			meanX2 += p.currLoc.x;
			meanY2 += p.currLoc.y;
		}

		meanX1 /= points.size();
		meanY1 /= points.size();
		meanX2 /= points.size();
		meanY2 /= points.size();

		double stdX1 = 0;
		double stdY1 = 0;
		double stdX2 = 0;
		double stdY2 = 0;

		for( AssociatedPair p : points ) {
			double dx = p.keyLoc.x - meanX1;
			double dy = p.keyLoc.y - meanY1;
			stdX1 += dx*dx;
			stdY1 += dy*dy;

			dx = p.currLoc.x - meanX2;
			dy = p.currLoc.y - meanY2;
			stdX2 += dx*dx;
			stdY2 += dy*dy;
		}

		stdX1 = Math.sqrt(stdX1/points.size());
		stdY1 = Math.sqrt(stdY1/points.size());
		stdX2 = Math.sqrt(stdX2/points.size());
		stdY2 = Math.sqrt(stdY2/points.size());

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
	 * Given the normalization matrix computed from {@link #computeNormalization}
	 * normalize the point.
	 *
	 * @param orig Unnormalized coordinate in pixels.
	 * @param normed Normalized coordinate.
	 * @param N Normalization matrix.
	 */
	public static void pixelToNormalized(Point2D_F64 orig, Point2D_F64 normed, DenseMatrix64F N) {
		normed.x = orig.x * N.data[0] + N.data[2];
		normed.y = orig.y * N.data[4] + N.data[5];
	}

	/**
	 * <p>
	 * Computes an essential matrix from a rotation and translation:<br>
	 * E = hat(T)*R
	 * </p>
	 *
	 * @param R Rotation matrix.
	 * @param T Translation vector.
	 * @return Essential matrix
	 */
	public static DenseMatrix64F computeEssential( DenseMatrix64F R , Vector3D_F64 T )
	{
		DenseMatrix64F E = new DenseMatrix64F(3,3);

		DenseMatrix64F T_hat = GeometryMath_F64.crossMatrix(T, null);
		CommonOps.mult(T_hat, R, E);

		return E;
	}

	/**
	 * <p>
	 * Computes a homography matrix from a rotation, translation, plane normal and plane distance:<br>
	 * H = R+(1/d)*T*N<sup>T</sup>
	 * </p>
	 *
	 * @param R Rotation matrix.
	 * @param T Translation vector.
	 * @param d Distance of closest point on plane to camera
	 * @param N Normal of plane
	 * @return Calibrated homography matrix
	 */
	public static DenseMatrix64F computeHomography( DenseMatrix64F R , Vector3D_F64 T ,
													double d , Vector3D_F64 N )
	{
		DenseMatrix64F H = new DenseMatrix64F(3,3);

		GeometryMath_F64.outerProd(T,N,H);
		CommonOps.divide(d,H);
		CommonOps.addEquals(H, R);

		return H;
	}

	/**
	 * <p>
	 * Computes a homography matrix from a rotation, translation, plane normal, plane distance, and
	 * calibration matrix:<br>
	 * H = K*(R+(1/d)*T*N<sup>T</sup>)*K<sup>-1</sup>
	 * </p>
	 *
	 * @param R Rotation matrix.
	 * @param T Translation vector.
	 * @param d Distance of closest point on plane to camera
	 * @param N Normal of plane
	 * @param K Intrinsic calibration matrix
	 * @return Uncalibrated homography matrix
	 */
	public static DenseMatrix64F computeHomography( DenseMatrix64F R , Vector3D_F64 T ,
													double d , Vector3D_F64 N ,
													DenseMatrix64F K )
	{
		DenseMatrix64F temp = new DenseMatrix64F(3,3);
		DenseMatrix64F K_inv = new DenseMatrix64F(3,3);

		DenseMatrix64F H = computeHomography(R,T,d,N);

		// apply calibration matrix to R
		CommonOps.mult(K,H,temp);

		CommonOps.invert(K,K_inv);
		CommonOps.mult(temp,K_inv,H);

		return H;
	}

}
