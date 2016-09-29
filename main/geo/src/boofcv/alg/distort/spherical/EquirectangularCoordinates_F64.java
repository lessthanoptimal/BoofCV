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

import georegression.geometry.ConvertCoordinates3D_F64;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;

/**
 * Contations common operations for handling coordinates in an equirectangular image.  An equirectangular image is
 * a spherical coordinate system which has been projected onto a 2D image.
 *
 * longtitude is along the x-axis and goes from -pi to pi
 * latitude is along the y-axis and goes from -pi/2 to pi/2
 *
 * @author Peter Abeles
 */
public class EquirectangularCoordinates_F64 {
	// input image width and height
	int width;
	int height;

	// location focus as a fractional coordinate
	double latCenterFrac;
	double lonCenterFrac;

	// internal storage to avoid declaring new memory
	Point2D_F64 temp = new Point2D_F64();

	public void configure( int width , int height , double latitudeCenter , double longitudeCenter  ) {
		this.width = width;
		this.height = height;

		latCenterFrac = (latitudeCenter + GrlConstants.PId2)/GrlConstants.PI;
		lonCenterFrac = (longitudeCenter + Math.PI)/(GrlConstants.PI2);
	}

	/**
	 * Converts equirectangular into normalized pointing vector
	 *
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param norm Normalized pointing vector
	 */
	public void equiToNorm(double x , double y , Vector3D_F64 norm ) {
		equiToLatlon(x,y, temp);
		ConvertCoordinates3D_F64.latlonToUnitVector(temp.x,temp.y, norm);
	}

	/**
	 * Converts the equirectangular coordinate into a latitude and longitude
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param latlon  (output) x = latitude, y = longitude
	 */
	public void equiToLatlon( double x , double y , Point2D_F64 latlon ) {
		double lat = (x/(width-1) - latCenterFrac)*GrlConstants.PI2;
		latlon.x = UtilAngle.bound(lat);

		double lon = (y/(height-1) - lonCenterFrac)*GrlConstants.PI;
		latlon.y = UtilAngle.boundHalf(lon);
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates
	 * @param lat Latitude
	 * @param lon Longitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void latlonToRect(double lat , double lon , Point2D_F64 rect ) {
		rect.x = UtilAngle.wrapZeroToOne(lat / GrlConstants.PI + latCenterFrac)*(width-1);
		rect.y = UtilAngle.reflectZeroToOne(lon / GrlConstants.PId2 + lonCenterFrac)*(height-1);
	}

}
