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

package boofcv.alg.sfm;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Various helper functions for testing SFM algorithms
 *
 * @author Peter Abeles
 */
public class SfmTestHelper {

	/**
	 * Renders a 3D point in the left and right camera views given the stereo parameters. Lens distortion
	 * is taken in account.
	 *
	 * @param param Stereo parameters
	 * @param X Point location in 3D space
	 * @param left location in pixels in left camera
	 * @param right location in pixels in right camera
	 */
	public static void renderPointPixel( StereoParameters param , Point3D_F64 X ,
										 Point2D_F64 left , Point2D_F64 right ) {
		// compute the location of X in the right camera's reference frame
		Point3D_F64 rightX = new Point3D_F64();
		SePointOps_F64.transform(param.getRightToLeft().invert(null), X, rightX);

		// location of object in normalized image coordinates
		Point2D_F64 normLeft = new Point2D_F64(X.x/X.z,X.y/X.z);
		Point2D_F64 normRight = new Point2D_F64(rightX.x/rightX.z,rightX.y/rightX.z);

		// convert into pixel coordinates
		Point2D_F64 pixelLeft =  PerspectiveOps.convertNormToPixel(param.left, normLeft.x, normLeft.y, null);
		Point2D_F64 pixelRight =  PerspectiveOps.convertNormToPixel(param.right, normRight.x, normRight.y, null);

		// take in account lens distortion
		Point2Transform2_F32 distLeft = LensDistortionOps.transformPoint(param.left).distort_F32(true,true);
		Point2Transform2_F32 distRight = LensDistortionOps.transformPoint(param.right).distort_F32(true,true);

		Point2D_F32 lensLeft = new Point2D_F32();
		Point2D_F32 lensRight = new Point2D_F32();

		distLeft.compute((float)pixelLeft.x,(float)pixelLeft.y,lensLeft);
		distRight.compute((float)pixelRight.x,(float)pixelRight.y,lensRight);

		// output solution
		left.set(lensLeft.x,lensLeft.y);
		right.set(lensRight.x,lensRight.y);
	}
}
