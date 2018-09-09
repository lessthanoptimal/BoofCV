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

package boofcv.alg.geo.impl;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.pinhole.PinholeNtoP_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.PerspectiveOps;
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
import org.ejml.data.FMatrix3x3;
import org.ejml.dense.fixed.CommonOps_DDF3;
import org.ejml.dense.fixed.CommonOps_FDF3;
import org.ejml.dense.row.CommonOps_DDRM;

import javax.annotation.Nullable;

/**
 * Implementation of {@link PerspectiveOps} functions for 64-bit floats
 *
 * @author Peter Abeles
 */
public class ImplPerspectiveOps_F64 {

	public static <C extends CameraPinhole>C adjustIntrinsic(C parameters,
															 DMatrixRMaj adjustMatrix,
															 C adjustedParam)
	{
		if( adjustedParam == null )
			adjustedParam = parameters.createLike();
		adjustedParam.set(parameters);

		DMatrixRMaj K = ImplPerspectiveOps_F64.pinholeToMatrix(parameters, (DMatrixRMaj)null);
		DMatrixRMaj K_adj = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(adjustMatrix, K, K_adj);

		ImplPerspectiveOps_F64.matrixToPinhole(K_adj, parameters.width, parameters.height, adjustedParam);

		return adjustedParam;
	}

	public static DMatrixRMaj pinholeToMatrix(double fx, double fy, double skew,
											  double xc, double yc) {
		return new DMatrixRMaj(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
	}

	public static DMatrixRMaj pinholeToMatrix(CameraPinhole param , DMatrixRMaj K ) {

		if( K == null ) {
			K = new DMatrixRMaj(3,3);
		}
		CommonOps_DDRM.fill(K, 0);

		K.data[0] = param.fx;
		K.data[1] = param.skew;
		K.data[2] = param.cx;
		K.data[4] = param.fy;
		K.data[5] = param.cy;
		K.data[8] = 1;

		return K;
	}

	public static DMatrix3x3 pinholeToMatrix(CameraPinhole param , DMatrix3x3 K ) {

		if( K == null ) {
			K = new DMatrix3x3();
		} else {
			CommonOps_DDF3.fill(K,0);
		}

		K.a11 = param.fx;
		K.a12 = param.skew;
		K.a13 = param.cx;
		K.a22 = param.fy;
		K.a23 = param.cy;
		K.a33 = 1;

		return K;
	}

	public static FMatrix3x3 pinholeToMatrix(CameraPinhole param , FMatrix3x3 K ) {

		if( K == null ) {
			K = new FMatrix3x3();
		} else {
			CommonOps_FDF3.fill(K,0);
		}

		K.a11 = (float)param.fx;
		K.a12 = (float)param.skew;
		K.a13 = (float)param.cx;
		K.a22 = (float)param.fy;
		K.a23 = (float)param.cy;
		K.a33 = 1;

		return K;
	}

	public static CameraPinhole matrixToPinhole(DMatrixRMaj K , int width , int height , CameraPinhole output ) {

		if( output == null )
			output = new CameraPinhole();

		output.fx = K.get(0,0);
		output.fy = K.get(1,1);
		output.skew = K.get(0,1);
		output.cx = K.get(0,2);
		output.cy = K.get(1,2);

		output.width = width;
		output.height = height;

		return output;
	}

	public static Point2D_F64 convertNormToPixel(CameraModel param , double x , double y , Point2D_F64 pixel ) {

		if( pixel == null )
			pixel = new Point2D_F64();

		Point2Transform2_F64 normToPixel = LensDistortionOps.narrow(param).distort_F64(false,true);

		normToPixel.compute(x,y,pixel);

		return pixel;
	}

	public static Point2D_F64 convertNormToPixel( DMatrixRMaj K, Point2D_F64 norm , Point2D_F64 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F64();

		PinholeNtoP_F64 alg = new PinholeNtoP_F64();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(norm.x,norm.y,pixel);

		return pixel;
	}

	public static Point2D_F64 convertPixelToNorm(CameraModel param , Point2D_F64 pixel , Point2D_F64 norm ) {
		if( norm == null )
			norm = new Point2D_F64();

		Point2Transform2_F64 pixelToNorm = LensDistortionOps.narrow(param).distort_F64(true, false);

		pixelToNorm.compute(pixel.x,pixel.y,norm);

		return norm;
	}

	public static Point2D_F64 convertPixelToNorm( DMatrixRMaj K , Point2D_F64 pixel , Point2D_F64 norm ) {
		if( norm == null )
			norm = new Point2D_F64();

		PinholePtoN_F64 alg = new PinholePtoN_F64();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(pixel.x,pixel.y,norm);

		return norm;
	}


	public static Point2D_F64 renderPixel( Se3_F64 worldToCamera , DMatrixRMaj K , Point3D_F64 X ) {
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

	public static void renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X , Point3D_F64 pixelH ) {
		DMatrixRMaj P = worldToCamera;

		pixelH.x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		pixelH.y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		pixelH.z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];
	}

	public static void renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X , Point2D_F64 pixel ) {
		DMatrixRMaj P = worldToCamera;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		pixel.x = x/z;
		pixel.y = y/z;
	}

	public static void renderPixel(DMatrixRMaj cameraMatrix , Point4D_F64 X , @Nullable Point3D_F64 pixelH) {
		DMatrixRMaj P = cameraMatrix;

		pixelH.x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3]*X.w;
		pixelH.y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7]*X.w;
		pixelH.z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11]*X.w;
	}

	public static void renderPixel(DMatrixRMaj cameraMatrix , Point4D_F64 X , @Nullable Point2D_F64 pixel) {
		DMatrixRMaj P = cameraMatrix;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3]*X.w;
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7]*X.w;
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11]*X.w;

		pixel.x = x/z;
		pixel.y = y/z;
	}


	public static DMatrixRMaj createCameraMatrix( DMatrixRMaj R , Vector3D_F64 T ,
												  @Nullable DMatrixRMaj K ,
												  @Nullable DMatrixRMaj ret ) {
		if( ret == null )
			ret = new DMatrixRMaj(3,4);

		CommonOps_DDRM.insert(R,ret,0,0);

		ret.data[3] = T.x;
		ret.data[7] = T.y;
		ret.data[11] = T.z;

		if( K == null )
			return ret;

		DMatrixRMaj temp = new DMatrixRMaj(3,4);
		CommonOps_DDRM.mult(K,ret,temp);

		ret.set(temp);

		return ret;
	}
}
