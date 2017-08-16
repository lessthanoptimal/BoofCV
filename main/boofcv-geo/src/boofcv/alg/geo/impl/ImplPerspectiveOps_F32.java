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
import boofcv.alg.distort.pinhole.PinholeNtoP_F32;
import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraModel;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.Point2Transform2_F32;
import georegression.geometry.GeometryMath_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Vector3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.transform.se.SePointOps_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

/**
 * Implementation of {@link PerspectiveOps} functions for 32-bit floats
 *
 * @author Peter Abeles
 */
public class ImplPerspectiveOps_F32 {

	public static <C extends CameraPinhole>C adjustIntrinsic(C parameters,
															 FMatrixRMaj adjustMatrix,
															 C adjustedParam)
	{
		if( adjustedParam == null )
			adjustedParam = parameters.createLike();
		adjustedParam.set(parameters);

		FMatrixRMaj K = ImplPerspectiveOps_F32.calibrationMatrix(parameters, null);
		FMatrixRMaj K_adj = new FMatrixRMaj(3,3);
		CommonOps_FDRM.mult(adjustMatrix, K, K_adj);

		ImplPerspectiveOps_F32.matrixToParam(K_adj, parameters.width, parameters.height, adjustedParam);

		return adjustedParam;
	}

	public static FMatrixRMaj calibrationMatrix(float fx, float fy, float skew,
												   float xc, float yc) {
		return new FMatrixRMaj(3,3,true,fx,skew,xc,0,fy,yc,0,0,1);
	}

	public static FMatrixRMaj calibrationMatrix(CameraPinhole param , FMatrixRMaj K ) {

		if( K == null ) {
			K = new FMatrixRMaj(3,3);
		}
		CommonOps_FDRM.fill(K, 0);

		K.data[0] = (float)param.fx;
		K.data[1] = (float)param.skew;
		K.data[2] = (float)param.cx;
		K.data[4] = (float)param.fy;
		K.data[5] = (float)param.cy;
		K.data[8] = 1;

		return K;
	}

	public static <C extends CameraPinhole>C matrixToParam(FMatrixRMaj K , int width , int height , C param ) {

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

	public static Point2D_F32 convertNormToPixel(CameraModel param , float x , float y , Point2D_F32 pixel ) {

		if( pixel == null )
			pixel = new Point2D_F32();

		Point2Transform2_F32 normToPixel = LensDistortionOps.narrow(param).distort_F32(false,true);

		normToPixel.compute(x,y,pixel);

		return pixel;
	}

	public static Point2D_F32 convertNormToPixel( FMatrixRMaj K, Point2D_F32 norm , Point2D_F32 pixel ) {
		if( pixel == null )
			pixel = new Point2D_F32();

		PinholeNtoP_F32 alg = new PinholeNtoP_F32();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(norm.x,norm.y,pixel);

		return pixel;
	}

	public static Point2D_F32 convertPixelToNorm(CameraModel param , Point2D_F32 pixel , Point2D_F32 norm ) {
		if( norm == null )
			norm = new Point2D_F32();

		Point2Transform2_F32 pixelToNorm = LensDistortionOps.narrow(param).distort_F32(true, false);

		pixelToNorm.compute(pixel.x,pixel.y,norm);

		return norm;
	}

	public static Point2D_F32 convertPixelToNorm( FMatrixRMaj K , Point2D_F32 pixel , Point2D_F32 norm ) {
		if( norm == null )
			norm = new Point2D_F32();

		PinholePtoN_F32 alg = new PinholePtoN_F32();
		alg.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		alg.compute(pixel.x,pixel.y,norm);

		return norm;
	}


	public static Point2D_F32 renderPixel( Se3_F32 worldToCamera , FMatrixRMaj K , Point3D_F32 X ) {
		Point3D_F32 X_cam = new Point3D_F32();

		SePointOps_F32.transform(worldToCamera, X, X_cam);

		// see if it's behind the camera
		if( X_cam.z <= 0 )
			return null;

		Point2D_F32 norm = new Point2D_F32(X_cam.x/X_cam.z,X_cam.y/X_cam.z);

		if( K == null )
			return norm;

		// convert into pixel coordinates
		return GeometryMath_F32.mult(K, norm, norm);
	}

	public static Point2D_F32 renderPixel( FMatrixRMaj worldToCamera , Point3D_F32 X ) {
		FMatrixRMaj P = worldToCamera;

		float x = P.data[0]*X.x + P.data[1]*X.y + P.data[2]*X.z + P.data[3];
		float y = P.data[4]*X.x + P.data[5]*X.y + P.data[6]*X.z + P.data[7];
		float z = P.data[8]*X.x + P.data[9]*X.y + P.data[10]*X.z + P.data[11];

		Point2D_F32 pixel = new Point2D_F32();

		pixel.x = x/z;
		pixel.y = y/z;

		return pixel;
	}

	public static FMatrixRMaj createCameraMatrix( FMatrixRMaj R , Vector3D_F32 T , FMatrixRMaj K ,
													 FMatrixRMaj ret ) {
		if( ret == null )
			ret = new FMatrixRMaj(3,4);

		CommonOps_FDRM.insert(R,ret,0,0);

		ret.data[3] = T.x;
		ret.data[7] = T.y;
		ret.data[11] = T.z;

		if( K == null )
			return ret;

		FMatrixRMaj temp = new FMatrixRMaj(3,4);
		CommonOps_FDRM.mult(K,ret,temp);

		ret.set(temp);

		return ret;
	}
}
