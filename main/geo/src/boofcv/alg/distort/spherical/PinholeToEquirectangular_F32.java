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

package boofcv.alg.distort.spherical;

import boofcv.alg.distort.pinhole.PinholePtoN_F32;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;

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
public class PinholeToEquirectangular_F32 extends EquirectangularDistortBase_F32 {

	// camera model without distortion
	CameraPinhole pinhole;

	/**
	 * Specifies the pinhole camera
	 * @param pinhole intrinsic parameters of pinhole camera
	 */
	public void setPinhole( CameraPinhole pinhole ) {
		this.pinhole = pinhole;
		declareVectors( pinhole.width, pinhole.height );

		// computing the 3D ray through each pixel in the pinhole camera at it's canonical
		// location
		PinholePtoN_F32 pixelToNormalized = new PinholePtoN_F32();
		pixelToNormalized.set(pinhole.fx,pinhole.fy,pinhole.skew,pinhole.cx,pinhole.cy);

		Point2D_F32 norm = new Point2D_F32();
		for (int pixelY = 0; pixelY < pinhole.height; pixelY++) {
			for (int pixelX = 0; pixelX < pinhole.width; pixelX++) {
				pixelToNormalized.compute(pixelX, pixelY, norm);
				Point3D_F32 v = vectors[pixelY*pinhole.width+pixelX];

				v.set(norm.x,norm.y,1);
			}
		}
	}
}
