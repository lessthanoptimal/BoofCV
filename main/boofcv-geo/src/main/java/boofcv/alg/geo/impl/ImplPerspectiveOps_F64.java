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

package boofcv.alg.geo.impl;

import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrix3x3;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link PerspectiveOps} functions for 64-bit floats
 *
 * @author Peter Abeles
 */
public class ImplPerspectiveOps_F64 {

	public static <C extends CameraPinhole> C adjustIntrinsic( C parameters,
															   DMatrixRMaj adjustMatrix,
															   @Nullable C adjustedParam ) {
		if (adjustedParam == null)
			adjustedParam = parameters.createLike();
		adjustedParam.setTo(parameters);

		DMatrixRMaj K = ImplPerspectiveOps_F64.pinholeToMatrix(parameters, (DMatrixRMaj)null);
		DMatrixRMaj K_adj = new DMatrixRMaj(3, 3);
		CommonOps_DDRM.mult(adjustMatrix, K, K_adj);

		ImplPerspectiveOps_F64.matrixToPinhole(K_adj, parameters.width, parameters.height, adjustedParam);

		return adjustedParam;
	}

	public static DMatrixRMaj pinholeToMatrix( double fx, double fy, double skew,
											   double cx, double cy, @Nullable DMatrixRMaj K ) {
		if (K == null) {
			K = new DMatrixRMaj(3, 3);
		} else {
			K.reshape(3, 3);
		}

		CommonOps_DDRM.fill(K, 0);

		K.data[0] = fx;
		K.data[1] = skew;
		K.data[2] = cx;
		K.data[4] = fy;
		K.data[5] = cy;
		K.data[8] = 1;

		return K;
	}

	public static DMatrixRMaj pinholeToMatrix( CameraPinhole param, @Nullable DMatrixRMaj K ) {
		return pinholeToMatrix((double)param.fx, (double)param.fy, (double)param.skew, (double)param.cx, (double)param.cy, K);
	}

	public static DMatrix3x3 pinholeToMatrix( CameraPinhole param, @Nullable DMatrix3x3 K ) {

		if (K == null) {
			K = new DMatrix3x3();
		} else {
			CommonOps_DDF3.fill(K, 0);
		}

		K.a11 = (double)param.fx;
		K.a12 = (double)param.skew;
		K.a13 = (double)param.cx;
		K.a22 = (double)param.fy;
		K.a23 = (double)param.cy;
		K.a33 = 1;

		return K;
	}

	public static CameraPinhole matrixToPinhole( DMatrixRMaj K, int width, int height, @Nullable CameraPinhole output ) {

		if (output == null)
			output = new CameraPinhole();

		output.fx = K.get(0, 0);
		output.fy = K.get(1, 1);
		output.skew = K.get(0, 1);
		output.cx = K.get(0, 2);
		output.cy = K.get(1, 2);

		output.width = width;
		output.height = height;

		return output;
	}

	public static Point2D_F64 convertNormToPixel( CameraModel param, double x, double y, @Nullable Point2D_F64 pixel ) {

		if (pixel == null)
			pixel = new Point2D_F64();

		Point2Transform2_F64 normToPixel = LensDistortionFactory.narrow(param).distort_F64(false, true);

		normToPixel.compute(x, y, pixel);

		return pixel;
	}

	public static Point2D_F64 convertNormToPixel( DMatrixRMaj K, Point2D_F64 norm, @Nullable Point2D_F64 pixel ) {
		if (pixel == null)
			pixel = new Point2D_F64();

		PinholeNtoP_F64 alg = new PinholeNtoP_F64();
		alg.setK(K.get(0, 0), K.get(1, 1), K.get(0, 1), K.get(0, 2), K.get(1, 2));

		alg.compute(norm.x, norm.y, pixel);

		return pixel;
	}

	public static Point2D_F64 convertPixelToNorm( CameraModel param, Point2D_F64 pixel, @Nullable Point2D_F64 norm ) {
		if (norm == null)
			norm = new Point2D_F64();

		Point2Transform2_F64 pixelToNorm = LensDistortionFactory.narrow(param).undistort_F64(true, false);

		pixelToNorm.compute(pixel.x, pixel.y, norm);

		return norm;
	}

	public static Point2D_F64 convertPixelToNorm( DMatrixRMaj K, Point2D_F64 pixel, @Nullable Point2D_F64 norm ) {
		if (norm == null)
			norm = new Point2D_F64();

		var alg = new PinholePtoN_F64();
		alg.setK(K.get(0, 0), K.get(1, 1), K.get(0, 1), K.get(0, 2), K.get(1, 2));

		alg.compute(pixel.x, pixel.y, norm);

		return norm;
	}

	public static Point2D_F64 convertPixelToNorm( CameraPinhole intrinsic, double pixelX, double pixelY, @Nullable Point2D_F64 norm ) {
		if (norm == null)
			norm = new Point2D_F64();

		double a11 = (double)(1.0/intrinsic.fx);
		double a12 = (double)(-intrinsic.skew/(intrinsic.fx*intrinsic.fy));
		double a13 = (double)((intrinsic.skew*intrinsic.cy - intrinsic.cx*intrinsic.fy)/(intrinsic.fx*intrinsic.fy));
		double a22 = (double)(1.0/intrinsic.fy);
		double a23 = (double)(-intrinsic.cy/intrinsic.fy);

		norm.x = a11*pixelX + a12*pixelY + a13;
		norm.y = a22*pixelY + a23;

		return norm;
	}

	public static @Nullable Point2D_F64 renderPixel( Se3_F64 worldToCamera,
													 @Nullable DMatrixRMaj K, Point3D_F64 X,
													 @Nullable Point2D_F64 pixel ) {
		DMatrixRMaj R = worldToCamera.R;
		Vector3D_F64 T = worldToCamera.T;

		// [R T]*X
		double x = R.data[0]*X.x + R.data[1]*X.y + R.data[2]*X.z + T.x;
		double y = R.data[3]*X.x + R.data[4]*X.y + R.data[5]*X.z + T.y;
		double z = R.data[6]*X.x + R.data[7]*X.y + R.data[8]*X.z + T.z;

		// see if it's behind the camera
		if (z <= 0)
			return null;

		if (pixel == null)
			pixel = new Point2D_F64();

		pixel.setTo(x/z, y/z);

		if (K == null)
			return pixel;

		// convert into pixel coordinates
		return GeometryMath_F64.mult(K, pixel, pixel);
	}

	public static @Nullable Point2D_F64 renderPixel( Se3_F64 worldToCamera,
													 double fx, double skew, double cx, double fy, double cy,
													 Point3D_F64 X, @Nullable Point2D_F64 pixel ) {
		var X_cam = new Point3D_F64();

		SePointOps_F64.transform(worldToCamera, X, X_cam);

		// see if it's behind the camera
		if (X_cam.z <= 0)
			return null;

		if (pixel == null)
			pixel = new Point2D_F64();

		double xx = X_cam.x/X_cam.z;
		double yy = X_cam.y/X_cam.z;

		pixel.x = fx*xx + skew*yy + cx;
		pixel.y = fy*yy + cy;

		return pixel;
	}

	public static Point3D_F64 renderPointing( Se3_F64 worldToCamera,
											  double fx, double skew, double cx, double fy, double cy,
											  Point3D_F64 X, @Nullable Point3D_F64 pixel ) {
		var X_cam = new Point3D_F64();

		SePointOps_F64.transform(worldToCamera, X, X_cam);

		if (pixel == null)
			pixel = new Point3D_F64();

		// Make sure the norm of the point is 1 to avoid numerical issues
		double n = X_cam.norm();
		double xx = X_cam.x/n;
		double yy = X_cam.y/n;
		double zz = X_cam.z/n;

		pixel.x = fx*xx + skew*yy + cx*zz;
		pixel.y = fy*yy + cy*zz;
		pixel.z = zz;

		return pixel;
	}

	public static @Nullable Point2D_F64 renderPixel( Se3_F64 worldToCamera,
													 double fx, double skew, double cx, double fy, double cy,
													 Point4D_F64 X, @Nullable Point2D_F64 pixel ) {

		DMatrixRMaj R = worldToCamera.R;
		Vector3D_F64 T = worldToCamera.T;

		// [R T]*X
		double x = R.data[0]*X.x + R.data[1]*X.y + R.data[2]*X.z + T.x*X.w;
		double y = R.data[3]*X.x + R.data[4]*X.y + R.data[5]*X.z + T.y*X.w;
		double z = R.data[6]*X.x + R.data[7]*X.y + R.data[8]*X.z + T.z*X.w;

		// see if it's behind the camera
		if (z <= 0)
			return null;

		if (pixel == null)
			pixel = new Point2D_F64();

		double xx = x/z;
		double yy = y/z;

		pixel.x = fx*xx + skew*yy + cx;
		pixel.y = fy*yy + cy;

		return pixel;
	}

	public static void renderPixel( DMatrixRMaj worldToCamera, Point3D_F64 X, Point3D_F64 pixelH ) {
		DMatrixRMaj P = worldToCamera;

		pixelH.x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		pixelH.y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		pixelH.z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];
	}

	public static void renderPixel( DMatrixRMaj worldToCamera, Point3D_F64 X, Point2D_F64 pixel ) {
		DMatrixRMaj P = worldToCamera;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		pixel.x = x/z;
		pixel.y = y/z;
	}

	public static void renderPixel( DMatrixRMaj cameraMatrix, Point4D_F64 X, Point3D_F64 pixelH ) {
		DMatrixRMaj P = cameraMatrix;

		// @formatter:off
		pixelH.x = P.data[0]*X.x + P.data[1]*X.y + P.data[2 ]*X.z + P.data[3 ]*X.w;
		pixelH.y = P.data[4]*X.x + P.data[5]*X.y + P.data[6 ]*X.z + P.data[7 ]*X.w;
		pixelH.z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11]*X.w;
		// @formatter:on
	}

	public static void renderPixel( DMatrixRMaj cameraMatrix, Point4D_F64 X, Point2D_F64 pixel ) {
		DMatrixRMaj P = cameraMatrix;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3]*X.w;
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7]*X.w;
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11]*X.w;

		pixel.x = x/z;
		pixel.y = y/z;
	}

	public static double distance3DvsH( Point3D_F64 a, Point4D_F64 b, double tol ) {
		// convert the homogenous point into a 3D point.
		double x = b.x;
		double y = b.y;
		double z = b.z;

		double r = Math.sqrt(x*x + y*y + z*z);

		// See if the homogenous point is at infinity, within tolerance
		if (r*tol > Math.abs(b.w)) {
			return Double.POSITIVE_INFINITY;
		}

		// Finish the conversion to 3D
		x /= b.w;
		y /= b.w;
		z /= b.w;

		return a.distance(x, y, z);
	}

	public static double distance( Point4D_F64 a, Point4D_F64 b ) {

		double na = a.norm();
		double nb = b.norm();

		if (na == 0.0 || nb == 0.0)
			return a.distance(b);

		// take in account sign ambiguity
		return Math.sqrt(Math.min(distance(a, b, na, nb), distance(a, b, -na, nb)));
	}

	public static double distance( Point4D_F64 a, Point4D_F64 b, double na, double nb ) {
		double xa = a.x/na;
		double ya = a.y/na;
		double za = a.z/na;
		double wa = a.w/na;

		double xb = b.x/nb;
		double yb = b.y/nb;
		double zb = b.z/nb;
		double wb = b.w/nb;

		double dx = xa - xb;
		double dy = ya - yb;
		double dz = za - zb;
		double dw = wa - wb;

		return dx*dx + dy*dy + dz*dz + dw*dw;
	}

	public static DMatrixRMaj createCameraMatrix( DMatrixRMaj R, Vector3D_F64 T,
												  @Nullable DMatrixRMaj K,
												  @Nullable DMatrixRMaj ret ) {
		if (ret == null)
			ret = new DMatrixRMaj(3, 4);

		CommonOps_DDRM.insert(R, ret, 0, 0);

		ret.data[3] = T.x;
		ret.data[7] = T.y;
		ret.data[11] = T.z;

		if (K == null)
			return ret;

		DMatrixRMaj temp = new DMatrixRMaj(3, 4);
		CommonOps_DDRM.mult(K, ret, temp);

		ret.setTo(temp);

		return ret;
	}
}
