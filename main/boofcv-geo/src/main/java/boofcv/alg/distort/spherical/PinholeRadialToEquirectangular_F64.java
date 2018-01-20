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

package boofcv.alg.distort.spherical;

import boofcv.alg.distort.radtan.LensDistortionRadialTangential;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

/**
 * Renders a pinhole camera view from an equirectangular image.  When no additional rotation
 * is applied the camera will lie along the +z axis.  Before use you must invoke the following functions:
 * <ul>
 *     <li>{@link #setPinhole}</li>
 *     <li>{@link #setEquirectangularShape}</li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class PinholeRadialToEquirectangular_F64 extends EquirectangularDistortBase_F64 {

	// camera model without distortion
	private CameraPinholeRadial pinhole;

	/**
	 * Specifies the pinhole camera
	 * @param pinhole intrinsic parameters of pinhole camera
	 */
	public void setPinhole( CameraPinholeRadial pinhole ) {
		this.pinhole = pinhole;
		declareVectors( pinhole.width, pinhole.height );

		// computing the 3D ray through each pixel in the pinhole camera at it's canonical
		// location
		Point2Transform2_F64 pixelToNormalized =
				new LensDistortionRadialTangential(pinhole).undistort_F64(true,false);

		Point2D_F64 norm = new Point2D_F64();
		for (int pixelY = 0; pixelY < pinhole.height; pixelY++) {
			for (int pixelX = 0; pixelX < pinhole.width; pixelX++) {
				pixelToNormalized.compute(pixelX, pixelY, norm);
				Point3D_F64 v = vectors[pixelY*pinhole.width+pixelX];

				v.set(norm.x,norm.y,1);
			}
		}
	}

	public CameraPinholeRadial getPinhole() {
		return pinhole;
	}
}
