/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.pinhole.PinholeNtoP_F32;
import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.GeometryMath_F64;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.List;

/**
 * Functions related to perspective geometry and intrinsic camera calibration.
 *
 * @author Peter Abeles
 */
public class PerspectiveOps {

	/**
	 * Creates a set of intrinsic parameters, without distortion, for a camera with the specified characteristics
	 *
	 * @param width Image width
	 * @param height Image height
	 * @param hfov Horizontal FOV in degrees
	 * @param vfov Vertical FOV in degrees
	 * @return guess camera parameters
	 */
	public static CameraPinholeRadial createIntrinsic(int width, int height, double hfov, double vfov) {
		CameraPinholeRadial intrinsic = new CameraPinholeRadial();
		intrinsic.width = width;
		intrinsic.height = height;
		intrinsic.cx = width / 2;
		intrinsic.cy = height / 2;
		intrinsic.fx = intrinsic.cx / Math.tan(UtilAngle.degreeToRadian(hfov/2.0));
		intrinsic.fy = intrinsic.cy / Math.tan(UtilAngle.degreeToRadian(vfov/2.0));

		return intrinsic;
	}

	/**
	 * Creates a set of intrinsic parameters, without distortion, for a camera with the specified characteristics.
	 * The focal length is assumed to be the same for x and y.
	 *
	 * @param width Image width
	 * @param height Image height
	 * @param hfov Horizontal FOV in degrees
	 * @return guess camera parameters
	 */
	public static CameraPinholeRadial createIntrinsic(int width, int height, double hfov) {
		CameraPinholeRadial intrinsic = new CameraPinholeRadial();
		intrinsic.width = width;
		intrinsic.height = height;
		intrinsic.cx = width / 2;
		intrinsic.cy = height / 2;
		intrinsic.fx = intrinsic.cx / Math.tan(UtilAngle.degreeToRadian(hfov/2.0));
		intrinsic.fy = intrinsic.fx;

		return intrinsic;
	}

	/**
	 * Multiplies each element of the intrinsic parameters by the provided scale factor.  Useful
	 * if the image has been rescaled.
	 *
	 * @param param Intrinsic parameters
	 * @param scale Scale factor that input image is being scaled by.
	 */
	public static void scaleIntrinsic(CameraPinholeRadial param , double scale ) {
		param.width = (int)(param.width*scale);
		param.height = (int)(param.height*scale);
		param.cx *= scale;
		param.cy *= scale;
		param.fx *= scale;
		param.fy *= scale;
		param.skew *= scale;
	}

	/**
	 *
	 * <p>Recomputes the {@link CameraPinholeRadial} given an adjustment matrix.</p>
	 * K<sub>A</sub> = A*K<br>
	 * Where K<sub>A</sub> is the returned adjusted intrinsic matrix, A is the adjustment matrix and K is the
	 * original intrinsic calibration matrix.
	 *
	 * <p>
	 * NOTE: Distortion parameters are ignored in the provided {@link CameraPinholeRadial} class.
	 * </p>
	 *
	 * @param parameters (Input) Original intrinsic parameters. Not modified.
	 * @param adjustMatrix (Input) Adjustment matrix. Not modified.
	 * @param adjustedParam (Output) Optional storage for adjusted intrinsic parameters. Can be null.
	 * @return Adjusted intrinsic parameters.
	 */
	public static CameraPinholeRadial adjustIntrinsic(CameraPinholeRadial parameters,
													  DenseMatrix64F adjustMatrix,
													  CameraPinholeRadial adjustedParam)
	{
		if( adjustedParam == null )
			adjustedParam = new CameraPinholeRadial();

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(parameters, null);
		DenseMatrix64F K_adj = new DenseMatrix64F(3,3);
		CommonOps.mult(adjustMatrix, K, K_adj);

		PerspectiveOps.matrixToParam(K_adj, parameters.width, parameters.height, adjustedParam);

		return adjustedParam;
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
	public static DenseMatrix64F calibrationMatrix(CameraPinholeRadial param , DenseMatrix64F K ) {

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
	 * @param param Where the intrinsic parameter are written to.  If null then a new instance is declared.
	 * @return IntrinsicParameters structure.
	 */
	public static CameraPinholeRadial matrixToParam(DenseMatrix64F K , int width , int height ,
													CameraPinholeRadial param ) {

		if( param == null )
			param = new CameraPinholeRadial();

		param.fx = K.get(0,0);
		param.fy = K.get(1,1);
		param.skew = K.get(0,1);
		param.cx = K.get(0,2);
		param.cy = K.get(1,2);

		param.width = width;
		param.height = height;

		return param;
	}

	/**
	 * <p>
	 * Convenient function for converting from normalized image coordinates to the original image pixel coordinate.
	 * If speed is a concern then {@link PinholeNtoP_F64} should be used instead.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters
	 * @param x X-coordinate of normalized.
	 * @param y Y-coordinate of normalized.
	 * @param pixel Optional storage for output.  If null a new instance will be declared.
	 * @return pixel image coordinate
	 */
	public static Point2D_F64 convertNormToPixel(CameraPinholeRadial param , double x , double y , Point2D_F64 pixel ) {

		if( pixel == null )
			pixel = new Point2D_F64();

		Point2Transform2_F64 normToPixel = LensDistortionOps.transformPoint(param).distort_F64(false,true);

		normToPixel.compute(x,y,pixel);

		return pixel;
	}

	/**
	 * <p>
	 * Convenient function for converting from normalized image coordinates to the original image pixel coordinate.
	 * If speed is a concern then {@link PinholeNtoP_F32} should be used instead.
	 * </p>
	 *
	 * @param param Intrinsic camera parameters
	 * @param x X-coordinate of normalized.
	 * @param y Y-coordinate of normalized.
	 * @param pixel Optional storage for output.  If null a new instance will be declared.
	 * @return pixel image coordinate
	 */
	public static Point2D_F32 convertNormToPixel(CameraPinholeRadial param , float x , float y , Point2D_F32 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F32();

		Point2Transform2_F32 normToPixel = LensDistortionOps.transformPoint(param).distort_F32(false, true);

		normToPixel.compute(x,y,pixel);

		return pixel;
	}

	/**
	 * <p>
	 * Convenient function for converting from normalized image coordinates to the original image pixel coordinate.
	 * If speed is a concern then {@link PinholeNtoP_F64} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param param Intrinsic camera parameters
	 * @param norm Normalized image coordinate.
	 * @param pixel Optional storage for output.  If null a new instance will be declared.
	 * @return pixel image coordinate
	 */
	public static Point2D_F64 convertNormToPixel(CameraPinholeRadial param , Point2D_F64 norm , Point2D_F64 pixel ) {
		return convertNormToPixel(param,norm.x,norm.y,pixel);
	}

	/**
	 * <p>
	 * Convenient function for converting from normalized image coordinates to the original image pixel coordinate.
	 * If speed is a concern then {@link PinholeNtoP_F64} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param K Intrinsic camera calibration matrix
	 * @param norm Normalized image coordinate.
	 * @param pixel Optional storage for output.  If null a new instance will be declared.
	 * @return pixel image coordinate
	 */
	public static Point2D_F64 convertNormToPixel( DenseMatrix64F K, Point2D_F64 norm , Point2D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F64();

		PinholeNtoP_F64 alg = new PinholeNtoP_F64();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(norm.x,norm.y,pixel);

		return pixel;
	}

	/**
	 * <p>
	 * Convenient function for converting from original image pixel coordinate to normalized< image coordinates.
	 * If speed is a concern then {@link PinholePtoN_F64} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param param Intrinsic camera parameters
	 * @param pixel Pixel coordinate
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F64 convertPixelToNorm(CameraPinholeRadial param , Point2D_F64 pixel , Point2D_F64 norm ) {
		if( norm == null )
			norm = new Point2D_F64();

		Point2Transform2_F64 pixelToNorm = LensDistortionOps.transformPoint(param).distort_F64(true, false);

		pixelToNorm.compute(pixel.x,pixel.y,norm);

		return norm;
	}

	/**
	 * <p>
	 * Convenient function for converting from original image pixel coordinate to normalized< image coordinates.
	 * If speed is a concern then {@link PinholePtoN_F32} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param param Intrinsic camera parameters
	 * @param pixel Pixel coordinate
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F32 convertPixelToNorm(CameraPinholeRadial param , Point2D_F32 pixel , Point2D_F32 norm ) {
		if( norm == null )
			norm = new Point2D_F32();

		Point2Transform2_F32 pixelToNorm = LensDistortionOps.transformPoint(param).distort_F32(true, false);

		pixelToNorm.compute(pixel.x,pixel.y,norm);

		return norm;
	}

	/**
	 * <p>
	 * Convenient function for converting from original image pixel coordinate to normalized< image coordinates.
	 * If speed is a concern then {@link PinholePtoN_F64} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param K Intrinsic camera calibration matrix
	 * @param pixel Pixel coordinate.
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F64 convertPixelToNorm( DenseMatrix64F K , Point2D_F64 pixel , Point2D_F64 norm ) {
		if( norm == null )
			norm = new Point2D_F64();

		PinholePtoN_F64 alg = new PinholePtoN_F64();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(pixel.x,pixel.y,norm);

		return norm;
	}

	/**
	 * Renders a point in world coordinates into the image plane in pixels or normalized image
	 * coordinates.
	 *
	 * @param worldToCamera Transform from world to camera frame
	 * @param K Optional.  Intrinsic camera calibration matrix.  If null then normalized image coordinates are returned.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane or null if it's behind the camera
	 */
	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , DenseMatrix64F K , Point3D_F64 X ) {
		Point3D_F64 X_cam = new Point3D_F64();

		SePointOps_F64.transform(worldToCamera, X, X_cam);

		// see if it's behind the camera
		if( X_cam.z <= 0 )
			return null;

		Point2D_F64 norm = new Point2D_F64(X_cam.x/X_cam.z,X_cam.y/X_cam.z);

		if( K == null )
			return norm;

		// convert into pixel coordinates
		return GeometryMath_F64.mult(K, norm, norm);
	}

	/**
	 * Renders a point in camera coordinates into the image plane in pixels.
	 *
	 * @param intrinsic Intrinsic camera parameters.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane or null if it's behind the camera
	 */
	public static Point2D_F64 renderPixel(CameraPinholeRadial intrinsic , Point3D_F64 X ) {
		Point2D_F64 norm = new Point2D_F64(X.x/X.z,X.y/X.z);
		return PerspectiveOps.convertNormToPixel(intrinsic, norm, norm);
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
	 * @param view1 Output: List of observations from view 1
	 * @param view2 Output: List of observations from view 2
	 */
	public static void splitAssociated( List<AssociatedPair> pairs ,
										List<Point2D_F64> view1 , List<Point2D_F64> view2 ) {
		for( AssociatedPair p : pairs ) {
			view1.add(p.p1);
			view2.add(p.p2);
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

	/**
	 * Creates a transform from world coordinates into pixel coordinates.  can handle lens distortion
	 */
	public static WorldToCameraToPixel createWorldToPixel(CameraPinholeRadial intrinsic , Se3_F64 worldToCamera )
	{
		WorldToCameraToPixel alg = new WorldToCameraToPixel();
		alg.configure(intrinsic,worldToCamera);
		return alg;
	}

	/**
	 * Creates a transform from world coordinates into pixel coordinates.  can handle lens distortion
	 */
	public static WorldToCameraToPixel createWorldToPixel(LensDistortionNarrowFOV distortion , Se3_F64 worldToCamera )
	{
		WorldToCameraToPixel alg = new WorldToCameraToPixel();
		alg.configure(distortion,worldToCamera);
		return alg;
	}
}
