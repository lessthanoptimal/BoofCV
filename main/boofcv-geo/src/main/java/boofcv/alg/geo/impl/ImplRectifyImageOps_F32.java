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

package boofcv.alg.geo.impl;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.LensDistortionOps_F32;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.SequencePoint2Transform2_F32;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;
import org.ejml.simple.SimpleMatrix;

import javax.annotation.Nullable;

import static boofcv.factory.distort.LensDistortionFactory.narrow;

/**
 * <p>
 * Implementation of functions inside of {@link boofcv.alg.geo.RectifyImageOps} for 32-bit floats
 * </p>
 *
 * @author Peter Abeles
 */
public class ImplRectifyImageOps_F32 {

	public static void fullViewLeft(CameraPinholeBrown paramLeft,
									@Nullable FMatrixRMaj rectifiedR, FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
									FMatrixRMaj rectifyK, ImageDimension rectifiedSize)
	{
		computeRectifiedSize(paramLeft, rectifiedR, rectifiedSize);

		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeBrown(paramLeft);

		Point2Transform2_F32 tranLeft = transformPixelToRect(paramLeft, rectifyLeft);

		Point2D_F32 work = new Point2D_F32();
		RectangleLength2D_F32 bound = DistortImageOps.boundBox_F32(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F32(tranLeft),work);

		float scaleX = rectifiedSize.width/bound.width;
		float scaleY = rectifiedSize.height/bound.height;

		float scale = (float)Math.min(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	public static void fullViewLeft(int imageWidth,int imageHeight,
									FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight )
	{
		Point2Transform2_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		Point2D_F32 work = new Point2D_F32();
		RectangleLength2D_F32 bound = DistortImageOps.boundBox_F32(imageWidth, imageHeight,
				new PointToPixelTransform_F32(tranLeft),work);

		float scaleX = imageWidth/bound.width;
		float scaleY = imageHeight/bound.height;

		float scale = (float)Math.min(scaleX,scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	public static void allInsideLeft(CameraPinholeBrown paramLeft,
									 @Nullable FMatrixRMaj rectifiedR,
									 FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
									 FMatrixRMaj rectifyK, ImageDimension rectifiedSize)
	{
		computeRectifiedSize(paramLeft, rectifiedR, rectifiedSize);

		// need to take in account the order in which image distort will remove rectification later on
		paramLeft = new CameraPinholeBrown(paramLeft);

		Point2Transform2_F32 tranLeft = transformPixelToRect(paramLeft, rectifyLeft);

		Point2D_F32 work = new Point2D_F32();
		RectangleLength2D_F32 bound = LensDistortionOps_F32.boundBoxInside(paramLeft.width, paramLeft.height,
				new PointToPixelTransform_F32(tranLeft),work);

		LensDistortionOps_F32.roundInside(bound);

		float scaleX = rectifiedSize.width/(float)bound.width;
		float scaleY = rectifiedSize.height/(float)bound.height;

		float scale = (float)Math.max(scaleX, scaleY);

		adjustCalibrated(rectifyLeft, rectifyRight, rectifyK, bound, scale);
	}

	private static void computeRectifiedSize(CameraPinholeBrown paramLeft, @Nullable FMatrixRMaj rectifiedR, ImageDimension rectifiedSize) {
		if (rectifiedR != null) {
			// The image axis and baseline might not be aligned. In that case you will want to rotate the rectified
			// image to maximize the number of pixels rendered inside of it
			float theta = (float)Math.atan2(rectifiedR.get(1, 0), rectifiedR.get(0, 0));
			float c = (float)Math.cos(theta);
			float s = (float)Math.sin(theta);
			int w = paramLeft.width;
			int h = paramLeft.height;
			rectifiedSize.width = (int) (float)Math.round(Math.abs(c * w + s * h));
			rectifiedSize.height = (int) (float)Math.round(Math.abs(-s * w + c * h));
		} else {
			rectifiedSize.width = paramLeft.width;
			rectifiedSize.height = paramLeft.height;
		}
	}

	public static void allInsideLeft( int imageWidth,int imageHeight,
									  FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight )
	{
		PointTransformHomography_F32 tranLeft = new PointTransformHomography_F32(rectifyLeft);

		Point2D_F32 work = new Point2D_F32();
		RectangleLength2D_F32 bound = LensDistortionOps_F32.boundBoxInside(imageWidth, imageHeight,
				new PointToPixelTransform_F32(tranLeft), work);

		float scaleX = imageWidth/(float)bound.width;
		float scaleY = imageHeight/(float)bound.height;

		float scale = (float)Math.max(scaleX, scaleY);

		adjustUncalibrated(rectifyLeft, rectifyRight, bound, scale);
	}

	/**
	 * Internal function which applies the rectification adjustment to a calibrated stereo pair
	 */
	private static void adjustCalibrated(FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
										 FMatrixRMaj rectifyK,
										 RectangleLength2D_F32 bound, float scale) {
		// translation
		float deltaX = -bound.x0*scale;
		float deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,new float[]{scale,0,deltaX,0,scale,deltaY,0,0,1});
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);
		SimpleMatrix K = SimpleMatrix.wrap(rectifyK);

		// remove previous calibration matrix
		SimpleMatrix K_inv = K.invert();
		rL = K_inv.mult(rL);
		rR = K_inv.mult(rR);

		// compute new calibration matrix and apply it
		K = A.mult(K);

		rectifyK.set(K.getFDRM());
		rectifyLeft.set(K.mult(rL).getFDRM());
		rectifyRight.set(K.mult(rR).getFDRM());
	}

	/**
	 * Internal function which applies the rectification adjustment to an uncalibrated stereo pair
	 */
	private static void adjustUncalibrated(FMatrixRMaj rectifyLeft, FMatrixRMaj rectifyRight,
										   RectangleLength2D_F32 bound, float scale) {
		// translation
		float deltaX = -bound.x0*scale;
		float deltaY = -bound.y0*scale;

		// adjustment matrix
		SimpleMatrix A = new SimpleMatrix(3,3,true,new float[]{scale,0,deltaX,0,scale,deltaY,0,0,1});
		SimpleMatrix rL = SimpleMatrix.wrap(rectifyLeft);
		SimpleMatrix rR = SimpleMatrix.wrap(rectifyRight);

		rectifyLeft.set(A.mult(rL).getFDRM());
		rectifyRight.set(A.mult(rR).getFDRM());
	}

	public static Point2Transform2_F32 transformRectToPixel(CameraPinholeBrown param,
															FMatrixRMaj rectify)
	{
		Point2Transform2_F32 add_p_to_p = narrow(param).distort_F32(true, true);

		FMatrixRMaj rectifyInv = new FMatrixRMaj(3,3);
		CommonOps_FDRM.invert(rectify,rectifyInv);
		PointTransformHomography_F32 removeRect = new PointTransformHomography_F32(rectifyInv);

		return new SequencePoint2Transform2_F32(removeRect,add_p_to_p);
	}

	public static Point2Transform2_F32 transformPixelToRect(CameraPinholeBrown param,
															FMatrixRMaj rectify)
	{
		Point2Transform2_F32 remove_p_to_p = narrow(param).undistort_F32(true, true);

		PointTransformHomography_F32 rectifyDistort = new PointTransformHomography_F32(rectify);

		return new SequencePoint2Transform2_F32(remove_p_to_p,rectifyDistort);
	}

	public static Point2Transform2_F32 transformPixelToRectNorm(CameraPinholeBrown param,
																FMatrixRMaj rectify,
																FMatrixRMaj rectifyK) {
		if (rectifyK.get(0, 1) != 0)
			throw new IllegalArgumentException("Skew should be zero in rectified images");

		Point2Transform2_F32 remove_p_to_p = narrow(param).undistort_F32(true, true);

		PointTransformHomography_F32 rectifyDistort = new PointTransformHomography_F32(rectify);

		PinholePtoN_F32 pixelToNorm = new PinholePtoN_F32();
		pixelToNorm.set(rectifyK.get(0, 0), rectifyK.get(1, 1),
				rectifyK.get(0, 1),
				rectifyK.get(0, 2), rectifyK.get(1, 2));

		return new SequencePoint2Transform2_F32(remove_p_to_p, rectifyDistort, pixelToNorm);
	}

}
