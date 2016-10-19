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

package boofcv.alg.feature.associate;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

/**
 * Checks to see if two observations from a left to right stereo camera are consistent.  Observations are converted
 * to rectified coordinates.  They are then checked to see if their y-axis are the same to within tolerance and that
 * the left x-coordinate is larger than the right x-coordinate, to within tolerance.
 *
 * @author Peter Abeles
 */
public class StereoConsistencyCheck {

	// convert from original image pixels into rectified image pixels
	protected Point2Transform2_F64 leftImageToRect;
	protected Point2Transform2_F64 rightImageToRect;

	// storage for rectified pixels
	Point2D_F64 rectLeft = new Point2D_F64();
	Point2D_F64 rectRight = new Point2D_F64();

	// tolerance
	double toleranceY;
	double toleranceX;

	public StereoConsistencyCheck(double toleranceX, double toleranceY) {
		this.toleranceX = toleranceX;
		this.toleranceY = toleranceY;
	}

	public void setCalibration(StereoParameters param) {
		CameraPinholeRadial left = param.getLeft();
		CameraPinholeRadial right = param.getRight();

		// compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(left, null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(right, null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();

		leftImageToRect = RectifyImageOps.transformPixelToRect_F64(param.left, rect1);
		rightImageToRect = RectifyImageOps.transformPixelToRect_F64(param.right, rect2);
	}

	/**
	 * Checks to see if the observations from the left and right camera are consistent.  Observations
	 * are assumed to be in the original image pixel coordinates.
	 *
	 * @param left Left camera observation in original pixels
	 * @param right Right camera observation in original pixels
	 * @return true for consistent
	 */
	public boolean checkPixel( Point2D_F64 left , Point2D_F64 right ) {
		leftImageToRect.compute(left.x,left.y,rectLeft);
		rightImageToRect.compute(right.x, right.y, rectRight);

		return checkRectified(rectLeft,rectRight);
	}

	/**
	 * Checks to see if the observations from the left and right camera are consistent.  Observations
	 * are assumed to be in the rectified image pixel coordinates.
	 *
	 * @param left Left camera observation in rectified pixels
	 * @param right Right camera observation in rectified pixels
	 * @return true for consistent
	 */
	public boolean checkRectified( Point2D_F64 left , Point2D_F64 right ) {
		// rectifications should make them appear along the same y-coordinate/epipolar line
		if( Math.abs(left.y - right.y) > toleranceY )
			return false;

		// features in the right camera should appear left of features in the image image
		return right.x <= left.x + toleranceX;
	}
}
