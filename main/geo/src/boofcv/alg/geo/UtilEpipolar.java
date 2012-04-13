/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.alg.dense.decomposition.DecompositionFactory;
import org.ejml.alg.dense.decomposition.QRDecomposition;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

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

	/**
	 * <p>
	 * Extracts the epipoles from an essential or fundamental matrix.  The epipoles are extracted
	 * from the left and right null space of the provided matrix.  Note that the found epipoles are
	 * in homogeneous coordinates.  If the epipole is at infinity then z=0
	 * </p>
	 *
	 * <p>
	 * e<sub>2</sub><sup>T</sup>*F = 0 <br>
	 * F*e<sub>1</sub> = 0
	 * </p>
	 *
	 * @param F Fundamental or Essential 3x3 matrix.  Not modified.        g
	 * @param e1 Found right epipole in homogeneous coordinates, Modified.
	 * @param e2 Found left epipole in homogeneous coordinates, Modified.
	 */
	public static void extractEpipoles( DenseMatrix64F F , Point3D_F64 e1 , Point3D_F64 e2 ) {
		SimpleMatrix f = SimpleMatrix.wrap(F);
		SimpleSVD svd = f.svd();
		
		SimpleMatrix U = svd.getU();
		SimpleMatrix V = svd.getV();

		e2.set(U.get(0,2),U.get(1,2),U.get(2,2));
		e1.set(V.get(0,2),V.get(1,2),V.get(2,2));
	}


	/**
	 * <p>
	 * Given a fundamental matrix a pair of projection matrices [R|T] can be extracted.  There are multiple
	 * solutions which can be found, the canonical projection matrix is defined as: <br>
	 * <pre>
	 * P=[I|0] and P'= [M|-M*t] = [[e']*F + e'*v^t | lambda*e']
	 * </pre>
	 * where e' is the epipole F<sup>T</sup>e' = 0, [e'] is the cross product matrix for the enclosed vector,
	 * v is an arbitrary 3-vector and lambda is a non-zero scalar.
	 * </p>
	 *
	 * <p>
	 * Page 256 in R. Hartley, and A. Zisserman, "Multiple View Geometry in Computer Vision", 2nd Ed, Cambridge 2003
	 * </p>
	 *
	 * @see #extractEpipoles
	 *
	 * @param F A fundamental matrix
	 * @param v Arbitrary 3-vector.  Just pick some value, say (1,1,1).
	 * @param lambda A non zero scalar.  Try one.
	 * @param e2 Left epipole.
	 * @return The canonical camera matrix P'
	 */
	public static DenseMatrix64F canonicalCamera( DenseMatrix64F F , Point3D_F64 e2, Vector3D_F64 v , double lambda ) {

		DenseMatrix64F crossMatrix = new DenseMatrix64F(3,3);
		GeometryMath_F64.crossMatrix(e2, crossMatrix);

		DenseMatrix64F outer = new DenseMatrix64F(3,3);
		GeometryMath_F64.outerProd(e2,v,outer);

		DenseMatrix64F KR = new DenseMatrix64F(3,3);
		CommonOps.mult(crossMatrix,F,KR);
		CommonOps.add(KR, outer, KR);

		DenseMatrix64F P = new DenseMatrix64F(3,4);
		CommonOps.insert(KR,P,0,0);

		P.set(0,3,lambda*e2.x);
		P.set(1,3,lambda*e2.y);
		P.set(2,3,lambda*e2.z);

		return P;
	}

	/**
	 * <p>
	 * Decomposes a camera matrix P=A*[R|T], where A is an upper triangular camera calibration
	 * matrix, R is a rotation matrix, and T is a translation vector.  NOTE: There are multiple valid solutions
	 * to this problem and only one solution is returned. NOTE: The camera center will be on the plane at infinity.
	 * </p>
	 *
	 * @param P Camera matrix, 3 by 4. Input
	 * @param K Camera calibration matrix, 3 by 3.  Output.
	 * @param pose The rotation and translation. Output.
	 */
	public static void decomposeCameraMatrix(DenseMatrix64F P, DenseMatrix64F K, Se3_F64 pose) {
		DenseMatrix64F KR = new DenseMatrix64F(3,3);
		CommonOps.extract(P, 0, 3, 0, 3, KR, 0, 0);

		QRDecomposition<DenseMatrix64F> qr = DecompositionFactory.qr(3, 3);

		if( !CommonOps.invert(KR) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		if( !qr.decompose(KR) )
			throw new RuntimeException("QR decomposition failed!  Bad input?");

		DenseMatrix64F U = qr.getQ(null,false);
		DenseMatrix64F B = qr.getR(null, false);

		if( !CommonOps.invert(U,pose.getR()) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		Point3D_F64 KT = new Point3D_F64(P.get(0,3),P.get(1,3),P.get(2,3));
		GeometryMath_F64.mult(B, KT, pose.getT());

		if( !CommonOps.invert(B,K) )
			throw new RuntimeException("Inverse failed!  Bad input?");

		CommonOps.scale(1.0/K.get(2,2),K);
	}

}
