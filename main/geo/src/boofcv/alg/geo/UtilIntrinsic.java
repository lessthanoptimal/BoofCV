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
			DenseMatrix64F K = UtilIntrinsic.calibrationMatrix(parameters,null);
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

			UtilIntrinsic.matrixToParam(K_adj, parameters.width, parameters.height,
					parameters.flipY,adjustedParam);
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
}
