/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

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

		DMatrixRMaj K = ImplPerspectiveOps_F64.calibrationMatrix(parameters, null);
		DMatrixRMaj K_adj = new DMatrixRMaj(3,3);
		CommonOps_DDRM.mult(adjustMatrix, K, K_adj);

		ImplPerspectiveOps_F64.matrixToParam(K_adj, parameters.width, parameters.height, adjustedParam);

		return adjustedParam;
	}

	public static DMatrixRMaj calibrationMatrix(double fx, double fy, double skew,
												   double xc, double yc) {
		return new DMatrixRMaj(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
	}

	public static DMatrixRMaj calibrationMatrix(CameraPinhole param , DMatrixRMaj K ) {

		if( K == null ) {
			K = new DMatrixRMaj(3,3);
		}
		CommonOps_DDRM.fill(K, 0);

		K.data[0] = (double)param.fx;
		K.data[1] = (double)param.skew;
		K.data[2] = (double)param.cx;
		K.data[4] = (double)param.fy;
		K.data[5] = (double)param.cy;
		K.data[8] = 1;

		return K;
	}

	public static <C extends CameraPinhole>C matrixToParam(DMatrixRMaj K , int width , int height , C param ) {

		if( param == null )
			param = (C)new CameraPinhole();

		param.fx = K.get(0,0);
		param.fy = K.get(1,1);
		param.skew = K.get(0,1);
		param.cx = K.get(0,2);
		param.cy = K.get(1,2);

		param.width = width;
		param.height = height;

		return param;
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

	public static Point2D_F64 renderPixel( DMatrixRMaj worldToCamera , Point3D_F64 X ) {
		DMatrixRMaj P = worldToCamera;

		double x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		double y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		double z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		Point2D_F64 pixel = new Point2D_F64();

		pixel.x = x/z;
		pixel.y = y/z;

		return pixel;
	}

	public static DMatrixRMaj createCameraMatrix( DMatrixRMaj R , Vector3D_F64 T , DMatrixRMaj K ,
													 DMatrixRMaj ret ) {
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
