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

import boofcv.alg.distort.NormalizedToPixel_F64;
import boofcv.alg.distort.PixelToNormalized_F64;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.SequencePointTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class PerspectiveOps {

	/**
	 * <p>
	 * Creates a new {@link PointTransform_F32} which is the same as applying a homography transform
	 * and another arbitrary transform.  A typical application is removing lens distortion from
	 * camera.s  The order that each transform is applied depends on if the arbitrary transform is forward or
	 * reverse transform.  A new set of camera parameters is computed to account for the adjustment.
	 * </p>
	 *
	 * <p>
	 * When removing camera distortion, the undistorted image is likely to have a different shape not
	 * entirely encloded by the original image.  This can be compensated for by transforming the undistorted
	 * image using a homography transform.  Typically this will translate and scale the undistorted image.
	 * The end result is a new virtual camera which has the adjusted intrinsic camera parameters.
	 * </p>
	 *
	 * <p>
	 * The returned transform:<br>
	 * &lambda;x = A*K*[R|T]X<br>
	 * where A is the homography.
	 * </p>
	 *
	 * @param distortPixel Transform that distorts the pixels in an image.
	 * @param forwardTran If true then the distortion expects undistorted pixels as input, false means it
	 *                    expects distorted pixels as input.
	 * @param parameters Original intrinsic camera parameters
	 * @param adjustMatrix Invertible homography
	 * @param adjustedParam The new intrinsic calibration matrix.
	 * @return The new transform.
	 */
	public static PointTransform_F32 adjustIntrinsic_F32(PointTransform_F32 distortPixel,
														 boolean forwardTran,
														 IntrinsicParameters parameters,
														 DenseMatrix64F adjustMatrix,
														 IntrinsicParameters adjustedParam)
	{
		if( adjustedParam != null ) {
			DenseMatrix64F K = PerspectiveOps.calibrationMatrix(parameters, null);
			DenseMatrix64F K_adj = new DenseMatrix64F(3,3);
			DenseMatrix64F A;
			// in the reverse case
			if( !forwardTran ) {
				A = new DenseMatrix64F(3,3);
				CommonOps.invert(adjustMatrix,A);
			} else {
				A = adjustMatrix;
			}
			CommonOps.mult(A, K, K_adj);

			PerspectiveOps.matrixToParam(K_adj, parameters.width, parameters.height,
					parameters.flipY, adjustedParam);
		}

		PointTransformHomography_F32 adjust = new PointTransformHomography_F32(adjustMatrix);

		if( forwardTran )
			return new SequencePointTransform_F32(distortPixel,adjust);
		else
			return new SequencePointTransform_F32(adjust,distortPixel);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param fx Focal length x-axis in pixels
	 * @param fy Focal length y-axis in pixels
	 * @param skew skew in pixels
	 * @param xc camera center x-axis in pixels
	 * @param yc center center y-axis in pixels
	 * @return Calibration matrix 3x3
	 */
	public static DenseMatrix64F calibrationMatrix(double fx, double fy, double skew,
												   double xc, double yc) {
		return new DenseMatrix64F(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param param Intrinsic parameters structure that is to be converted into a matrix
	 * @param K Storage for calibration matrix, must be 3x3.  If null then a new matrix is declared
	 * @return Calibration matrix 3x3
	 */
	public static DenseMatrix64F calibrationMatrix( IntrinsicParameters param , DenseMatrix64F K ) {

		if( K == null ) {
			K = new DenseMatrix64F(3,3);
		}
		CommonOps.fill(K, 0);

		K.data[0] = param.fx;
		K.data[1] = param.skew;
		K.data[2] = param.cx;
		K.data[4] = param.fy;
		K.data[5] = param.cy;
		K.data[8] = 1;

		return K;
	}

	/**
	 * Converts a calibration matrix into intrinsic parameters
	 *
	 * @param K Camera calibration matrix.
	 * @param width Image width in pixels
	 * @param height Image height in pixels
	 * @param flipY When calibrated was the y-axis adjusted with: y = (height - y - 1)
	 * @param param Where the intrinsic parameter are written to.  If null then a new instance is declared.
	 * @return IntrinsicParameters structure.
	 */
	public static IntrinsicParameters matrixToParam( DenseMatrix64F K , int width , int height , boolean flipY,
													 IntrinsicParameters param ) {

		if( param == null )
			param = new IntrinsicParameters();

		param.fx = K.get(0,0);
		param.fy = K.get(1,1);
		param.skew = K.get(0,1);
		param.cx = K.get(0,2);
		param.cy = K.get(1,2);

		param.width = width;
		param.height = height;
		param.flipY = flipY;

		return param;
	}

	/**
	 * <p>
	 * Convenient function for converting from normalized image coordinates to the original image pixel coordinate.
	 * If speed is a concern then {@link NormalizedToPixel_F64} should be used instead.
	 * </p>
	 * <p>
	 * NOTE: Lens distortion handled!
	 * </p>
	 *
	 * @param param Intrinsic camera parameters
	 * @param x X-coordinate of normalized.
	 * @param y Y-coordinate of normalized.
	 * @param pixel Optional storage for output.  If null a new instance will be declared.
	 * @return pixel image coordinate
	 */
	public static Point2D_F64 convertNormToPixel( IntrinsicParameters param , double x , double y , Point2D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F64();

		NormalizedToPixel_F64 alg = new NormalizedToPixel_F64();
		alg.set(param.fx,param.fy,param.skew,param.cx,param.cy);

		alg.compute(x,y,pixel);

		return pixel;
	}

	/**
	 * <p>
	 * Convenient function for converting from original image pixel coordinate to normalized< image coordinates.
	 * If speed is a concern then {@link PixelToNormalized_F64} should be used instead.
	 * </p>
	 * <p>
	 * NOTE: Lens distortion is not removed!
	 * </p>
	 *
	 * @param param Intrinsic camera parameters
	 * @param x X-coordinate of image pixel.
	 * @param y Y-coordinate of image pixel.
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F64 convertPixelToNorm( IntrinsicParameters param , double x , double y , Point2D_F64 norm ) {
		if( norm == null )
			norm = new Point2D_F64();

		PixelToNormalized_F64 alg = new PixelToNormalized_F64();
		alg.set(param.fx,param.fy,param.skew,param.cx,param.cy);

		alg.compute(x,y,norm);

		return norm;
	}

	/**
	 * Renders a point in world coordinates into the image plane in pixels or normalized image
	 * coordinates.
	 *
	 * @param worldToCamera Transform from world to camera frame
	 * @param K Optional.  Intrinsic camera calibration matrix.  If null then normalized image coordinates are returned.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane.
	 */
	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , DenseMatrix64F K , Point3D_F64 X ) {
		Point3D_F64 X_cam = new Point3D_F64();

		SePointOps_F64.transform(worldToCamera, X, X_cam);

		Point2D_F64 norm = new Point2D_F64(X_cam.x/X_cam.z,X_cam.y/X_cam.z);

		if( K == null )
			return norm;

		Point2D_F64 pixel = new Point2D_F64();

		GeometryMath_F64.mult(K, norm, pixel);

		return pixel;
	}

	/**
	 * Computes the image coordinate of a point given its 3D location and the camera matrix.
	 *
	 * @param worldToCamera 3x4 camera matrix for transforming a 3D point from world to image coordinates.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane.
	 */
	public static Point2D_F64 renderPixel( DenseMatrix64F worldToCamera , Point3D_F64 X ) {
		DenseMatrix64F P = worldToCamera;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		Point2D_F64 pixel = new Point2D_F64();

		pixel.x = x/z;
		pixel.y = y/z;

		return pixel;
	}

	/**
	 * Takes a list of {@link AssociatedPair} as input and breaks it up into two lists for each view.
	 *
	 * @param pairs Input: List of associated pairs.
	 * @param view1 Output: List of observations from 'key' view
	 * @param view2 Output: List of observations from 'curr' view
	 */
	public static void splitAssociated( List<AssociatedPair> pairs ,
										List<Point2D_F64> view1 , List<Point2D_F64> view2 ) {
		for( AssociatedPair p : pairs ) {
			view1.add(p.keyLoc);
			view2.add(p.currLoc);
		}
	}

	/**
	 * Takes a list of {@link AssociatedTriple} as input and breaks it up into three lists for each view.
	 *
	 * @param pairs Input: List of associated triples.
	 * @param view1 Output: List of observations from view 1
	 * @param view2 Output: List of observations from view 2
	 * @param view3 Output: List of observations from view 3
	 */
	public static void splitAssociated( List<AssociatedTriple> pairs ,
										List<Point2D_F64> view1 , List<Point2D_F64> view2 , List<Point2D_F64> view3 ) {
		for( AssociatedTriple p : pairs ) {
			view1.add(p.p1);
			view2.add(p.p2);
			view3.add(p.p3);
		}
	}

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
	 * {@link AssociatedPair}.  Same as {@link #computeNormalization(java.util.List, org.ejml.data.DenseMatrix64F)},
	 * but for two views.
	 * </p>
	 *
	 * @param points Input: List of observed points that are to be normalized. Not modified.
	 * @param N1 Output: 3x3 normalization matrix for first set of points. Modified.
	 * @param N2 Output: 3x3 normalization matrix for second set of points. Modified.
	 */
	public static void computeNormalization(List<AssociatedPair> points, DenseMatrix64F N1, DenseMatrix64F N2)
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
	 * {@link AssociatedTriple}.  Same as {@link #computeNormalization(java.util.List, org.ejml.data.DenseMatrix64F)},
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
	// TODO Rename this to avoid confusion
	public static void pixelToNormalized(DenseMatrix64F N, Point2D_F64 orig, Point2D_F64 normed) {
		normed.x = orig.x * N.data[0] + N.data[2];
		normed.y = orig.y * N.data[4] + N.data[5];
	}

	/**
	 * Create a 3x4 camera matrix. For calibrated camera P = [R|T].  For uncalibrated camera it is P = K*[R|T].
	 *
	 * @param R Rotation matrix. 3x3
	 * @param T Translation vector.
	 * @param K Optional camera calibration matrix 3x3.
	 * @param ret Storage for camera calibration matrix. If null a new instance will be created.
	 * @return Camera calibration matrix.
	 */
	public static DenseMatrix64F createCameraMatrix( DenseMatrix64F R , Vector3D_F64 T , DenseMatrix64F K ,
													 DenseMatrix64F ret ) {
		if( ret == null )
			ret = new DenseMatrix64F(3,4);

		CommonOps.insert(R,ret,0,0);

		ret.data[3] = T.x;
		ret.data[7] = T.y;
		ret.data[11] = T.z;

		if( K == null )
			return ret;

		DenseMatrix64F temp = new DenseMatrix64F(3,4);
		CommonOps.mult(K,ret,temp);

		ret.set(temp);

		return ret;
	}
}
