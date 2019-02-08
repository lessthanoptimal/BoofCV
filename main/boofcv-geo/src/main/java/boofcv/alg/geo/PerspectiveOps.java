/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.impl.ImplPerspectiveOps_F32;
import boofcv.alg.geo.impl.ImplPerspectiveOps_F64;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.AssociatedTriple;
import georegression.geometry.UtilVector3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.GeoTuple3D_F64;
import georegression.struct.point.*;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrix3;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Functions related to perspective geometry and intrinsic camera calibration.
 *
 * @author Peter Abeles
 */
public class PerspectiveOps {

	/**
	 * Approximates a pinhole camera using the distoriton model
	 * @param p2n Distorted pixel to undistorted normalized image coordinates
	 * @return
	 */
	public static CameraPinhole approximatePinhole( Point2Transform2_F64 p2n ,
													int width , int height )
	{
		Point2D_F64 na = new Point2D_F64();
		Point2D_F64 nb = new Point2D_F64();

		// determine horizontal FOV using dot product of (na.x, na.y, 1 ) and (nb.x, nb.y, 1)
		p2n.compute(0,height/2,na);
		p2n.compute(width-1,height/2,nb);

		double abdot = na.x*nb.x + na.y*nb.y + 1;
		double normA = Math.sqrt(na.x*na.x + na.y*na.y + 1);
		double normB = Math.sqrt(nb.x*nb.x + nb.y*nb.y + 1);

		double hfov = Math.acos( abdot/(normA*normB));

		// vertical FOV
		p2n.compute(width/2,0,na);
		p2n.compute(width/2,height-1,nb);

		abdot = na.x*nb.x + na.y*nb.y + 1;
		normA = Math.sqrt(na.x*na.x + na.y*na.y + 1);
		normB = Math.sqrt(nb.x*nb.x + nb.y*nb.y + 1);

		double vfov = Math.acos( abdot/(normA*normB));

		return createIntrinsic(width,height, UtilAngle.degree(hfov), UtilAngle.degree(vfov));
	}

	/**
	 * Creates a set of intrinsic parameters, without distortion, for a camera with the specified characteristics
	 *
	 * @param width Image width
	 * @param height Image height
	 * @param hfov Horizontal FOV in degrees
	 * @param vfov Vertical FOV in degrees
	 * @return guess camera parameters
	 */
	public static CameraPinhole createIntrinsic(int width, int height, double hfov, double vfov) {
		CameraPinhole intrinsic = new CameraPinhole();
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
	public static CameraPinholeBrown createIntrinsic(int width, int height, double hfov) {
		CameraPinholeBrown intrinsic = new CameraPinholeBrown();
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
	public static void scaleIntrinsic(CameraPinhole param , double scale ) {
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
	 * <p>Recomputes the {@link CameraPinholeBrown} given an adjustment matrix.</p>
	 * K<sub>A</sub> = A*K<br>
	 * Where K<sub>A</sub> is the returned adjusted intrinsic matrix, A is the adjustment matrix and K is the
	 * original intrinsic calibration matrix.
	 *
	 * <p>
	 * NOTE: Distortion parameters are ignored in the provided {@link CameraPinholeBrown} class.
	 * </p>
	 *
	 * @param parameters (Input) Original intrinsic parameters. Not modified.
	 * @param adjustMatrix (Input) Adjustment matrix. Not modified.
	 * @param adjustedParam (Output) Optional storage for adjusted intrinsic parameters. Can be null.
	 * @return Adjusted intrinsic parameters.
	 */
	public static <C extends CameraPinhole>C adjustIntrinsic(C parameters,
															 DMatrixRMaj adjustMatrix,
															 C adjustedParam)
	{
		return ImplPerspectiveOps_F64.adjustIntrinsic(parameters, adjustMatrix, adjustedParam);
	}

	/**
	 *
	 * <p>Recomputes the {@link CameraPinholeBrown} given an adjustment matrix.</p>
	 * K<sub>A</sub> = A*K<br>
	 * Where K<sub>A</sub> is the returned adjusted intrinsic matrix, A is the adjustment matrix and K is the
	 * original intrinsic calibration matrix.
	 *
	 * <p>
	 * NOTE: Distortion parameters are ignored in the provided {@link CameraPinholeBrown} class.
	 * </p>
	 *
	 * @param parameters (Input) Original intrinsic parameters. Not modified.
	 * @param adjustMatrix (Input) Adjustment matrix. Not modified.
	 * @param adjustedParam (Output) Optional storage for adjusted intrinsic parameters. Can be null.
	 * @return Adjusted intrinsic parameters.
	 */
	public static <C extends CameraPinhole>C adjustIntrinsic(C parameters,
															 FMatrixRMaj adjustMatrix,
															 C adjustedParam)
	{
		return ImplPerspectiveOps_F32.adjustIntrinsic(parameters, adjustMatrix, adjustedParam);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param fx Focal length x-axis in pixels
	 * @param fy Focal length y-axis in pixels
	 * @param skew skew in pixels
	 * @param cx camera center x-axis in pixels
	 * @param cy center center y-axis in pixels
	 * @return Calibration matrix 3x3
	 */
	public static DMatrixRMaj pinholeToMatrix(double fx, double fy, double skew,
											  double cx, double cy) {
		return ImplPerspectiveOps_F64.pinholeToMatrix(fx, fy, skew, cx, cy,null);
	}

	public static void pinholeToMatrix(double fx, double fy, double skew,
									   double cx, double cy , DMatrix3x3 K ) {
		K.a11 = fx; K.a12 = skew; K.a13 = cx;
		K.a22 = fy; K.a23 = cy;
		K.a33 = 1;
		K.a21 = K.a31 = K.a32 = 0;
	}

	/**
	 * Analytic matrix inversion to 3x3 camera calibration matrix. Input and output
	 * can be the same matrix. Zeros are not set.
	 *
	 * @param K (Input) Calibration matrix
	 * @param Kinv (Output) inverse.
	 */
	public static void invertPinhole( DMatrix3x3 K , DMatrix3x3 Kinv) {
		double fx = K.a11;
		double skew = K.a12;
		double cx = K.a13;
		double fy = K.a22;
		double cy = K.a23;
		Kinv.a11 = 1.0/fx;
		Kinv.a12 = -skew/(fx*fy);
		Kinv.a13 = (skew*cy - cx*fy)/(fx*fy);
		Kinv.a22 = 1.0/fy;
		Kinv.a23 = -cy/fy;
		Kinv.a33 = 1;
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
	public static FMatrixRMaj pinholeToMatrix(float fx, float fy, float skew,
											  float xc, float yc) {
		return ImplPerspectiveOps_F32.pinholeToMatrix(fx, fy, skew, xc, yc, null);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param param Intrinsic parameters structure that is to be converted into a matrix
	 * @param K Storage for calibration matrix, must be 3x3.  If null then a new matrix is declared
	 * @return Calibration matrix 3x3
	 */
	public static DMatrixRMaj pinholeToMatrix(CameraPinhole param , DMatrixRMaj K )
	{
		return ImplPerspectiveOps_F64.pinholeToMatrix(param, K);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param param Intrinsic parameters structure that is to be converted into a matrix
	 * @param K Storage for calibration matrix, must be 3x3.  If null then a new matrix is declared
	 * @return Calibration matrix 3x3
	 */
	public static FMatrixRMaj pinholeToMatrix(CameraPinhole param , FMatrixRMaj K )
	{
		return ImplPerspectiveOps_F32.pinholeToMatrix(param, K);
	}

	/**
	 * Given the intrinsic parameters create a calibration matrix
	 *
	 * @param param Intrinsic parameters structure that is to be converted into a matrix
	 * @param K Storage for calibration matrix, must be 3x3.  If null then a new matrix is declared
	 * @return Calibration matrix 3x3
	 */
	public static DMatrix3x3 pinholeToMatrix(CameraPinhole param , DMatrix3x3 K )
	{
		return ImplPerspectiveOps_F64.pinholeToMatrix(param, K);
	}

	/**
	 * Converts a calibration matrix into intrinsic parameters
	 *
	 * @param K Camera calibration matrix.
	 * @param width Image width in pixels
	 * @param height Image height in pixels
	 * @param output (Output) Where the intrinsic parameter are written to.  If null then a new instance is declared.
	 * @return camera parameters
	 */
	public static <C extends CameraPinhole>C matrixToPinhole(DMatrixRMaj K , int width , int height , C output )
	{
		return (C)ImplPerspectiveOps_F64.matrixToPinhole(K, width, height, output);
	}

	/**
	 * Converts a calibration matrix into intrinsic parameters
	 *
	 * @param K Camera calibration matrix.
	 * @param width Image width in pixels
	 * @param height Image height in pixels
	 * @param output (Output) Where the intrinsic parameter are written to.  If null then a new instance is declared.
	 * @return camera parameters
	 */
	public static <C extends CameraPinhole>C matrixToPinhole(FMatrixRMaj K , int width , int height , C output )
	{
		return (C)ImplPerspectiveOps_F32.matrixToPinhole(K, width, height, output);
	}

	/**
	 * Given the transform from pixels to normalized image coordinates, create an approximate pinhole model
	 * for this camera. Assumes (cx,cy) is the image center and that there is no skew.
	 *
	 * @param pixelToNorm Pixel coordinates into normalized image coordinates
	 * @param width Input image's width
	 * @param height Input image's height
	 * @return Approximate pinhole camera
	 */
	public static CameraPinhole estimatePinhole(Point2Transform2_F64 pixelToNorm , int width , int height ) {

		Point2D_F64 normA = new Point2D_F64();
		Point2D_F64 normB = new Point2D_F64();
		Vector3D_F64 vectorA = new Vector3D_F64();
		Vector3D_F64 vectorB = new Vector3D_F64();

		pixelToNorm.compute(0,height/2,normA);
		pixelToNorm.compute(width,height/2,normB);
		vectorA.set(normA.x,normA.y,1);
		vectorB.set(normB.x,normB.y,1);
		double hfov = UtilVector3D_F64.acute(vectorA,vectorB);

		pixelToNorm.compute(width/2,0,normA);
		pixelToNorm.compute(width/2,height,normB);
		vectorA.set(normA.x,normA.y,1);
		vectorB.set(normB.x,normB.y,1);
		double vfov = UtilVector3D_F64.acute(vectorA,vectorB);

		CameraPinhole intrinsic = new CameraPinhole();
		intrinsic.width = width;
		intrinsic.height = height;
		intrinsic.skew = 0;
		intrinsic.cx = width/2;
		intrinsic.cy = height/2;
		intrinsic.fx = intrinsic.cx/Math.tan(hfov/2);
		intrinsic.fy = intrinsic.cy/Math.tan(vfov/2);

		return intrinsic;
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
	public static Point2D_F64 convertNormToPixel(CameraModel param , double x , double y , Point2D_F64 pixel ) {
		return ImplPerspectiveOps_F64.convertNormToPixel(param, x, y, pixel);
	}

	public static Point2D_F64 convertNormToPixel( CameraPinhole param , double x , double y , Point2D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F64();
		pixel.x = param.fx * x + param.skew * y + param.cx;
		pixel.y = param.fy * y + param.cy;

		return pixel;
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
	public static Point2D_F32 convertNormToPixel(CameraModel param , float x , float y , Point2D_F32 pixel ) {
		return ImplPerspectiveOps_F32.convertNormToPixel(param, x, y, pixel);
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
	public static Point2D_F64 convertNormToPixel(CameraModel param , Point2D_F64 norm , Point2D_F64 pixel ) {
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
	public static Point2D_F64 convertNormToPixel( DMatrixRMaj K, Point2D_F64 norm , Point2D_F64 pixel )
	{
		return ImplPerspectiveOps_F64.convertNormToPixel(K, norm, pixel);
	}

	/**
	 * <p>
	 * Convenient function for converting from distorted image pixel coordinate to undistorted normalized
	 * image coordinates. If speed is a concern then {@link PinholePtoN_F64} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param param Intrinsic camera parameters
	 * @param pixel Pixel coordinate
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F64 convertPixelToNorm(CameraModel param , Point2D_F64 pixel , Point2D_F64 norm ) {
		return ImplPerspectiveOps_F64.convertPixelToNorm(param, pixel, norm);
	}

	/**
	 * <p>
	 * Convenient function for converting from distorted image pixel coordinate to undistorted normalized
	 * image coordinates. If speed is a concern then {@link PinholePtoN_F32} should be used instead.
	 * </p>
	 *
	 * NOTE: norm and pixel can be the same instance.
	 *
	 * @param param Intrinsic camera parameters
	 * @param pixel Pixel coordinate
	 * @param norm Optional storage for output.  If null a new instance will be declared.
	 * @return normalized image coordinate
	 */
	public static Point2D_F32 convertPixelToNorm(CameraModel param , Point2D_F32 pixel , Point2D_F32 norm ) {
		return ImplPerspectiveOps_F32.convertPixelToNorm(param, pixel, norm);
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
	public static Point2D_F64 convertPixelToNorm( DMatrixRMaj K , Point2D_F64 pixel , Point2D_F64 norm ) {
		return ImplPerspectiveOps_F64.convertPixelToNorm(K, pixel, norm);
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
	public static Point2D_F32 convertPixelToNorm( FMatrixRMaj K , Point2D_F32 pixel , Point2D_F32 norm ) {
		return ImplPerspectiveOps_F32.convertPixelToNorm(K, pixel, norm);
	}

	public static Point2D_F64 convertPixelToNorm( CameraPinhole intrinsic , double x , double y, Point2D_F64 norm ) {
		return ImplPerspectiveOps_F64.convertPixelToNorm(intrinsic, x,y, norm);
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
	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , DMatrixRMaj K , Point3D_F64 X ) {
		return ImplPerspectiveOps_F64.renderPixel(worldToCamera,K,X);
//		if( K == null )
//			return renderPixel(worldToCamera,X);
//		return ImplPerspectiveOps_F64.renderPixel(worldToCamera,
//				K.data[0], K.data[1], K.data[2], K.data[4], K.data[5], X);
	}

	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , CameraPinhole K , Point3D_F64 X ) {
		return ImplPerspectiveOps_F64.renderPixel(worldToCamera,
				K.fy, K.skew, K.cx, K.fy, K.cy, X);
	}

	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , Point3D_F64 X ) {
		return ImplPerspectiveOps_F64.renderPixel(worldToCamera,
				1, 0, 0, 1, 0, X);
	}

	/**
	 * Renders a point in camera coordinates into the image plane in pixels.
	 *
	 * @param intrinsic Intrinsic camera parameters.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane or null if it's behind the camera
	 */
	public static Point2D_F64 renderPixel(CameraPinhole intrinsic , Point3D_F64 X ) {
		Point2D_F64 norm = new Point2D_F64(X.x/X.z,X.y/X.z);
		return convertNormToPixel(intrinsic, norm, norm);
	}

	/**
	 * Computes the image coordinate of a point given its 3D location and the camera matrix.
	 *
	 * @param worldToCamera 3x4 camera matrix for transforming a 3D point from world to image coordinates.
	 * @param X 3D Point in world reference frame..
	 * @return 2D Render point on image plane.
	 */
	public static Point2D_F64 renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X ) {
		return renderPixel(worldToCamera,X,(Point2D_F64)null);
	}

	public static Point2D_F64 renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X , @Nullable Point2D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F64();
		ImplPerspectiveOps_F64.renderPixel(worldToCamera, X, pixel);
		return pixel;
	}

	public static Point3D_F64 renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X , @Nullable Point3D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point3D_F64();
		ImplPerspectiveOps_F64.renderPixel(worldToCamera, X, pixel);
		return pixel;
	}

	/**
	 * Render a pixel in homogeneous coordinates from a 3x4 camera matrix and a 3D homogeneous point.
	 * @param cameraMatrix (Input) 3x4 camera matrix
	 * @param X (Input) 3D point in homogeneous coordinates
	 * @param x (Output) Rendered 2D point in homogeneous coordinates
	 * @return Rendered 2D point in homogeneous coordinates
	 */
	public static Point3D_F64 renderPixel( DMatrixRMaj cameraMatrix , Point4D_F64 X , @Nullable Point3D_F64 x) {
		if( x == null )
			x = new Point3D_F64();
		ImplPerspectiveOps_F64.renderPixel(cameraMatrix, X, x);
		return x;
	}

	/**
	 * Render a pixel in homogeneous coordinates from a 3x4 camera matrix and a 2D point.
	 * @param cameraMatrix (Input) 3x4 camera matrix
	 * @param X (Input) 3D point in homogeneous coordinates
	 * @param x (Output) Rendered 2D point coordinates
	 * @return Rendered 2D point coordinates
	 */
	public static Point2D_F64 renderPixel( DMatrixRMaj cameraMatrix , Point4D_F64 X , @Nullable Point2D_F64 x) {
		if( x == null )
			x = new Point2D_F64();
		ImplPerspectiveOps_F64.renderPixel(cameraMatrix, X, x);
		return x;
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
	public static DMatrixRMaj createCameraMatrix( DMatrixRMaj R , Vector3D_F64 T ,
												  @Nullable DMatrixRMaj K ,
												  @Nullable DMatrixRMaj ret ) {
		return ImplPerspectiveOps_F64.createCameraMatrix(R, T, K, ret);
	}

	/**
	 * Splits the projection matrix into a 3x3 matrix and 3x1 vector.
	 *
	 * @param P (Input) 3x4 projection matirx
	 * @param M (Output) M = P(:,0:2)
	 * @param T (Output) T = P(:,3)
	 */
	public static void projectionSplit( DMatrixRMaj P , DMatrixRMaj M , Vector3D_F64 T ) {
		CommonOps_DDRM.extract(P,0,3,0,3,M,0,0);
		T.x = P.get(0,3);
		T.y = P.get(1,3);
		T.z = P.get(2,3);
	}

	/**
	 * Splits the projection matrix into a 3x3 matrix and 3x1 vector.
	 *
	 * @param P (Input) 3x4 projection matirx
	 * @param M (Output) M = P(:,0:2)
	 * @param T (Output) T = P(:,3)
	 */
	public static void projectionSplit( DMatrixRMaj P , DMatrix3x3 M , DMatrix3 T ) {
		M.a11 = P.data[0]; M.a12 = P.data[1]; M.a13 = P.data[2 ]; T.a1 = P.data[3 ];
		M.a21 = P.data[4]; M.a22 = P.data[5]; M.a23 = P.data[6 ]; T.a2 = P.data[7 ];
		M.a31 = P.data[8]; M.a32 = P.data[9]; M.a33 = P.data[10]; T.a3 = P.data[11];
	}

	/**
	 * P = [M|T]
	 *
	 * @param M (Input) 3x3 matrix
	 * @param T (Input) 3x1 vector
	 * @param P (Output) [M,T]
	 */
	public static void projectionCombine( DMatrixRMaj M , Vector3D_F64 T , DMatrixRMaj P ) {
		CommonOps_DDRM.insert(M,P,0,0);
		P.data[3] = T.x;
		P.data[7] = T.y;
		P.data[11] = T.z;
	}

	/**
	 * Creates a transform from world coordinates into pixel coordinates.  can handle lens distortion
	 */
	public static WorldToCameraToPixel createWorldToPixel(CameraPinholeBrown intrinsic , Se3_F64 worldToCamera )
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

	public static double computeHFov(CameraPinhole intrinsic) {
		return 2*Math.atan((intrinsic.width/2)/intrinsic.fx);
	}

	public static double computeVFov(CameraPinhole intrinsic) {
		return 2*Math.atan((intrinsic.height/2)/intrinsic.fy);
	}

	/**
	 * Computes the cross-ratio between 4 points. This is an invariant under projective geometry.
	 * @param a0 Point
	 * @param a1 Point
	 * @param a2 Point
	 * @param a3 Point
	 * @return cross ratio
	 */
	public static double crossRatios( Point3D_F64 a0 , Point3D_F64 a1 , Point3D_F64 a2 , Point3D_F64 a3) {
		double d01 = a0.distance(a1);
		double d23 = a2.distance(a3);
		double d02 = a0.distance(a2);
		double d13 = a1.distance(a3);

		return (d01*d23)/(d02*d13);
	}

	/**
	 * Computes the cross-ratio between 4 points. This is an invariant under projective geometry.
	 * @param a0 Point
	 * @param a1 Point
	 * @param a2 Point
	 * @param a3 Point
	 * @return cross ratio
	 */
	public static double crossRatios( Point2D_F64 a0 , Point2D_F64 a1 , Point2D_F64 a2 , Point2D_F64 a3) {
		double d01 = a0.distance(a1);
		double d23 = a2.distance(a3);
		double d02 = a0.distance(a2);
		double d13 = a1.distance(a3);

		return (d01*d23)/(d02*d13);
	}

	/**
	 * Converts the SE3 into  a 3x4 matrix. [R|T]
	 * @param m (Input) transform
	 * @param A (Output) equivalent 3x4 matrix represenation
	 */
	public static DMatrixRMaj convertToMatrix( Se3_F64 m , DMatrixRMaj A ) {
		if( A == null )
			A = new DMatrixRMaj(3,4);
		else
			A.reshape(3,4);
		CommonOps_DDRM.insert(m.R,A,0,0);
		A.data[3] = m.T.x;
		A.data[7] = m.T.y;
		A.data[11] = m.T.z;
		return A;
	}

	/**
	 * Extracts a column from the camera matrix and puts it into the geometric 3-tuple.
	 */
	public static void extractColumn(DMatrixRMaj P, int col, GeoTuple3D_F64 a) {
		a.x = P.unsafe_get(0,col);
		a.y = P.unsafe_get(1,col);
		a.z = P.unsafe_get(2,col);
	}

	/**
	 * Inserts 3-tuple into the camera matrix's columns
	 */
	public static void insertColumn(DMatrixRMaj P, int col, GeoTuple3D_F64 a) {
		P.unsafe_set(0,col,a.x);
		P.unsafe_set(1,col,a.y);
		P.unsafe_set(2,col,a.z);
	}

	/**
	 * Computes: D = A<sup>T</sup>*B*C
	 *
	 * @param A (Input) 3x3 matrix
	 * @param B (Input) 3x3 matrix
	 * @param C (Input) 3x3 matrix
	 * @param output (Output) 3x3 matrix. Can be same instance A or B.
	 */
	public static void multTranA( DMatrixRMaj A , DMatrixRMaj B , DMatrixRMaj C , DMatrixRMaj output )
	{
		double t11 = A.data[0]*B.data[0] + A.data[3]*B.data[3] + A.data[6]*B.data[6];
		double t12 = A.data[0]*B.data[1] + A.data[3]*B.data[4] + A.data[6]*B.data[7];
		double t13 = A.data[0]*B.data[2] + A.data[3]*B.data[5] + A.data[6]*B.data[8];

		double t21 = A.data[1]*B.data[0] + A.data[4]*B.data[3] + A.data[7]*B.data[6];
		double t22 = A.data[1]*B.data[1] + A.data[4]*B.data[4] + A.data[7]*B.data[7];
		double t23 = A.data[1]*B.data[2] + A.data[4]*B.data[5] + A.data[7]*B.data[8];

		double t31 = A.data[2]*B.data[0] + A.data[5]*B.data[3] + A.data[8]*B.data[6];
		double t32 = A.data[2]*B.data[1] + A.data[5]*B.data[4] + A.data[8]*B.data[7];
		double t33 = A.data[2]*B.data[2] + A.data[5]*B.data[5] + A.data[8]*B.data[8];

		output.data[0] = t11*C.data[0] + t12*C.data[3] + t13*C.data[6];
		output.data[1] = t11*C.data[1] + t12*C.data[4] + t13*C.data[7];
		output.data[2] = t11*C.data[2] + t12*C.data[5] + t13*C.data[8];

		output.data[3] = t21*C.data[0] + t22*C.data[3] + t23*C.data[6];
		output.data[4] = t21*C.data[1] + t22*C.data[4] + t23*C.data[7];
		output.data[5] = t21*C.data[2] + t22*C.data[5] + t23*C.data[8];

		output.data[6] = t31*C.data[0] + t32*C.data[3] + t33*C.data[6];
		output.data[7] = t31*C.data[1] + t32*C.data[4] + t33*C.data[7];
		output.data[8] = t31*C.data[2] + t32*C.data[5] + t33*C.data[8];
	}

	/**
	 * Computes: D = A*B*C<sup>T</sup>
	 *
	 * @param A (Input) 3x3 matrix
	 * @param B (Input) 3x3 matrix
	 * @param C (Input) 3x3 matrix
	 * @param output (Output) 3x3 matrix. Can be same instance A or B.
	 */
	public static void multTranC( DMatrixRMaj A , DMatrixRMaj B , DMatrixRMaj C , DMatrixRMaj output )
	{
		double t11 = A.data[0]*B.data[0] + A.data[1]*B.data[3] + A.data[2]*B.data[6];
		double t12 = A.data[0]*B.data[1] + A.data[1]*B.data[4] + A.data[2]*B.data[7];
		double t13 = A.data[0]*B.data[2] + A.data[1]*B.data[5] + A.data[2]*B.data[8];

		double t21 = A.data[3]*B.data[0] + A.data[4]*B.data[3] + A.data[5]*B.data[6];
		double t22 = A.data[3]*B.data[1] + A.data[4]*B.data[4] + A.data[5]*B.data[7];
		double t23 = A.data[3]*B.data[2] + A.data[4]*B.data[5] + A.data[5]*B.data[8];

		double t31 = A.data[6]*B.data[0] + A.data[7]*B.data[3] + A.data[8]*B.data[6];
		double t32 = A.data[6]*B.data[1] + A.data[7]*B.data[4] + A.data[8]*B.data[7];
		double t33 = A.data[6]*B.data[2] + A.data[7]*B.data[5] + A.data[8]*B.data[8];

		output.data[0] = t11*C.data[0] + t12*C.data[1] + t13*C.data[2];
		output.data[1] = t11*C.data[3] + t12*C.data[4] + t13*C.data[5];
		output.data[2] = t11*C.data[6] + t12*C.data[7] + t13*C.data[8];

		output.data[3] = t21*C.data[0] + t22*C.data[1] + t23*C.data[2];
		output.data[4] = t21*C.data[3] + t22*C.data[4] + t23*C.data[5];
		output.data[5] = t21*C.data[6] + t22*C.data[7] + t23*C.data[8];

		output.data[6] = t31*C.data[0] + t32*C.data[1] + t33*C.data[2];
		output.data[7] = t31*C.data[3] + t32*C.data[4] + t33*C.data[5];
		output.data[8] = t31*C.data[6] + t32*C.data[7] + t33*C.data[8];
	}

	/**
	 * Multiplies A*P, where A = [sx 0 tx; 0 sy ty; 0 0 1]
	 */
	public static void inplaceAdjustCameraMatrix( double sx , double sy , double tx , double ty , DMatrixRMaj P ) {
		// multiply each column one at a time. Because of the zeros everything is decoupled
		for (int col = 0; col < 4; col++) {
			int row0 = col;
			int row1 = 4 + row0;
			int row2 = 4 + row1;
			P.data[row0] = P.data[row0]*sx + tx*P.data[row2];
			P.data[row1] = P.data[row1]*sy + ty*P.data[row2];
		}
	}
}
