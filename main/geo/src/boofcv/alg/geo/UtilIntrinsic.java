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

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.distort.SequencePointTransform_F32;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

/**
 * @author Peter Abeles
 */
public class UtilIntrinsic {

	/**
	 * <p>
	 * Applies an invertible homography transform to the calibration matrix.  This can be used
	 * to create a virtual camera where more of the distorted image is visible.  A new transform
	 * is returned and (optionally) new intrinsic parameters are computed.
	 * </p>
	 *
	 * <p>
	 * The returned transform:<br>
	 * &lambda;x = A*K*[R|T]X<br>
	 * where A is the homography.
	 * </p>
	 *
	 * @param distortPixel Transform that distorts the pixels in an image.
	 * @param parameters Original intrinsic camera parameters
	 * @param adjustMatrix Invertible homography
	 * @param adjustedParam The new intrinsic calibration matrix.
	 * @return The new transform.
	 */
	public static PointTransform_F32 adjustDistortion_F32( PointTransform_F32 distortPixel ,
														   IntrinsicParameters parameters ,
														   DenseMatrix64F adjustMatrix ,
														   IntrinsicParameters adjustedParam )
	{
		if( adjustedParam != null ) {
			DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(parameters);
			DenseMatrix64F K_adj = new DenseMatrix64F(3,3);
			CommonOps.mult(adjustMatrix, K, K_adj);

			UtilIntrinsic.matrixToParam(K_adj, parameters.width, parameters.height, adjustedParam);
		}

		PointTransformHomography_F32 adjust = new PointTransformHomography_F32(adjustMatrix);

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
	 * @return Calibration matrix 3x3
	 */
	public static DenseMatrix64F calibrationMatrix( IntrinsicParameters param ) {
		return new DenseMatrix64F(3,3,true,param.fx,param.skew,param.cx,0,param.fy,param.cy,0,0,1);
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
	public static IntrinsicParameters matrixToParam( DenseMatrix64F K , int width , int height , IntrinsicParameters param ) {

		if( param == null )
			param = new IntrinsicParameters();

		param.fx = K.get(0,0);
		param.fy = K.get(1,1);
		param.skew = K.get(0,1);
		param.cx = K.get(0,2);
		param.cy = K.get(1,2);

		param.width = width;
		param.height = height;

		return param;
	}
}
