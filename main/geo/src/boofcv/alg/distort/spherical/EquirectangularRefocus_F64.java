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

import boofcv.struct.distort.PointTransform_F64;
import georegression.struct.point.Point2D_F64;

/**
 * Applies a point transform that can be used to re-render an equirectangular image with a new center.  This lets you
 * close which parts of the image should be highly distorted and which should not.
 *
 * @author Peter Abeles
 */
public class EquirectangularRefocus_F64 implements PointTransform_F64 {

	EquirectangularTools_F64 inputOps = new EquirectangularTools_F64();
	EquirectangularTools_F64 outputOps = new EquirectangularTools_F64();

	Point2D_F64 latlon = new Point2D_F64();

	/**
	 * Specifies the image and which latitude/longtiude will comprise the center axises
	 * @param width Image width
	 * @param height Image height
	 * @param longitudeCenter center longitude line. -pi to pi
	 * @param latitudeCenter center latitude line. -pi/2 to pi/2
	 */
	public void configure( int width , int height , double longitudeCenter , double latitudeCenter ) {
		inputOps.configure(width, height, 0,0);
		outputOps.configure(width, height, longitudeCenter, latitudeCenter);
	}

	@Override
	public void compute(double x, double y, Point2D_F64 out) {

		inputOps.equiToLatlon(x,y,latlon);
		outputOps.latlonToRect(latlon.x,latlon.y,out);
	}
}
