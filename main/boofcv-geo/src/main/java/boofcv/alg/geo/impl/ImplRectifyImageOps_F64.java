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

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.distort.PointToPixelTransform_F64;
import boofcv.alg.distort.PointTransformHomography_F64;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.SequencePoint2Transform2_F64;
import georegression.struct.shapes.RectangleLength2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.simple.SimpleMatrix;

import static boofcv.alg.distort.LensDistortionOps.narrow;

/**
 * <p>
 * Implementation of functions inside of {@link boofcv.alg.geo.RectifyImageOps} for 64-bit floats
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplRectifyImageOps_F64 {

	public static void fullViewLeft(CameraPinholeRadial paramLeft,
									DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
									DMatrixRMaj rectifyK)
	{
		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeRadial(paramLeft);

		Point2Transform2_F64 tranLeft = transformPixelToRect(paramLeft, rectifyLeft);

		RectangleLength2D_F64 bound = DistortImageOps.boundBox_F64(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F64(tranLeft));

		double scaleX = paramLeft.width/bound.width;
		double scaleY = paramLeft.height/bound.height;

		double scale = Math.min(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	public static void fullViewLeft(int imageWidth,int imageHeight,
									DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight )
	{
		Point2Transform2_F64 tranLeft = new PointTransformHomography_F64(rectifyLeft);

		RectangleLength2D_F64 bound = DistortImageOps.boundBox_F64(imageWidth, imageHeight,
				new PointToPixelTransform_F64(tranLeft));

		double scaleX = imageWidth/bound.width;
		double scaleY = imageHeight/bound.height;

		double scale = Math.min(scaleX,scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	public static void allInsideLeft(CameraPinholeRadial paramLeft,
									 DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
									 DMatrixRMaj rectifyK)
	{
		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeRadial(paramLeft);

		Point2Transform2_F64 tranLeft = transformPixelToRect(paramLeft, rectifyLeft);

		RectangleLength2D_F64 bound = LensDistortionOps.boundBoxInside(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F64(tranLeft));

		LensDistortionOps.roundInside(bound);

		double scaleX = paramLeft.width/(double)bound.width;
		double scaleY = paramLeft.height/(double)bound.height;

		double scale = Math.max(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	public static void allInsideLeft( int imageWidth,int imageHeight,
									  DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight )
	{
		PointTransformHomography_F64 tranLeft = new PointTransformHomography_F64(rectifyLeft);

		RectangleLength2D_F64 bound = LensDistortionOps.boundBoxInside(imageWidth, imageHeight,
				new PointToPixelTransform_F64(tranLeft));

		double scaleX = imageWidth/(double)bound.width;
		double scaleY = imageHeight/(double)bound.height;

		double scale = Math.max(scaleX, scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	/**
	 * Internal function which applies the rectification adjustment to a calibrated stereo pair
	 */
	private static void adjustCalibrated(DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
										 DMatrixRMaj rectifyK,
										 RectangleLength2D_F64 bound, double scale) {
		// translation
		double deltaX = -bound.x0*scale;
		double deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,new double[]{scale,0,deltaX,0,scale,deltaY,0,0,1});
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);
		SimpleMatrix K = SimpleMatrix.wrap(rectifyK);

		// remove previous calibration matrix
		SimpleMatrix K_inv = K.invert();
		rL = K_inv.mult(rL);
		rR = K_inv.mult(rR);

		// compute new calibration matrix and apply it
		K = A.mult(K);

		rectifyK.set(K.getDDRM());
		rectifyLeft.set(K.mult(rL).getDDRM());
		rectifyRight.set(K.mult(rR).getDDRM());
	}

	/**
	 * Internal function which applies the rectification adjustment to an uncalibrated stereo pair
	 */
	private static void adjustUncalibrated(DMatrixRMaj rectifyLeft, DMatrixRMaj rectifyRight,
										   RectangleLength2D_F64 bound, double scale) {
		// translation
		double deltaX = -bound.x0*scale;
		double deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,new double[]{scale,0,deltaX,0,scale,deltaY,0,0,1});
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);

		rectifyLeft.set(A.mult(rL).getDDRM());
		rectifyRight.set(A.mult(rR).getDDRM());
	}

	public static Point2Transform2_F64 transformRectToPixel(CameraPinholeRadial param,
															DMatrixRMaj rectify)
	{
		Point2Transform2_F64 add_p_to_p = narrow(param).distort_F64(true, true);

		DMatrixRMaj rectifyInv = new DMatrixRMaj(3,3);
		CommonOps_DDRM.invert(rectify,rectifyInv);
		PointTransformHomography_F64 removeRect = new PointTransformHomography_F64(rectifyInv);

		return new SequencePoint2Transform2_F64(removeRect,add_p_to_p);
	}

	public static Point2Transform2_F64 transformPixelToRect(CameraPinholeRadial param,
															DMatrixRMaj rectify)
	{
		Point2Transform2_F64 remove_p_to_p = narrow(param).undistort_F64(true, true);

		PointTransformHomography_F64 rectifyDistort = new PointTransformHomography_F64(rectify);

		return new SequencePoint2Transform2_F64(remove_p_to_p,rectifyDistort);
	}

	public static Point2Transform2_F64 transformPixelToRectNorm(CameraPinholeRadial param,
																DMatrixRMaj rectify,
																DMatrixRMaj rectifyK) {
		if (rectifyK.get(0, 1) != 0)
			throw new IllegalArgumentException("Skew should be zero in rectified images");

		Point2Transform2_F64 remove_p_to_p = narrow(param).undistort_F64(true, true);

		PointTransformHomography_F64 rectifyDistort = new PointTransformHomography_F64(rectify);

		PinholePtoN_F64 pixelToNorm = new PinholePtoN_F64();
		pixelToNorm.set(rectifyK.get(0, 0), rectifyK.get(1, 1),
				rectifyK.get(0, 1),
				rectifyK.get(0, 2), rectifyK.get(1, 2));

		return new SequencePoint2Transform2_F64(remove_p_to_p, rectifyDistort, pixelToNorm);
	}

}
