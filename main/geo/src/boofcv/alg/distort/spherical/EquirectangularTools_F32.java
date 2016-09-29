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

import georegression.geometry.ConvertCoordinates3D_F32;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Vector3D_F32;

/**
 * Contains common operations for handling coordinates in an equirectangular image.  An equirectangular image is
 * a spherical coordinate system which has been projected onto a 2D image.
 *
 * <p>
 * longtitude is along the x-axis and goes from -pi to pi<br>
 * latitude is along the y-axis and goes from -pi/2 to pi/2
 * </p>
 *
 * @author Peter Abeles
 */
public class EquirectangularTools_F32 {
	// input image width and height
	int width;
	int height;

	// location focus as a fractional coordinate
	float latCenterFrac;
	float lonCenterFrac;

	// internal storage to avoid declaring new memory
	Point2D_F32 temp = new Point2D_F32();

	/**
	 * Specifies the image and which latitude/longtiude will comprise the center axises
	 * @param width Image width
	 * @param height Image height
	 * @param longitudeCenter center longitude line. -pi to pi
	 * @param latitudeCenter center latitude line. -pi/2 to pi/2
	 */
	public void configure( int width , int height , float longitudeCenter, float latitudeCenter  ) {
		this.width = width;
		this.height = height;

		lonCenterFrac = (longitudeCenter + (float)Math.PI)/(GrlConstants.F_PI2);
		latCenterFrac = (latitudeCenter + GrlConstants.F_PId2)/GrlConstants.F_PI;
	}

	/**
	 * Converts equirectangular into normalized pointing vector
	 *
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param norm Normalized pointing vector
	 */
	public void equiToNorm(float x , float y , Vector3D_F32 norm ) {
		equiToLatlon(x,y, temp);
		ConvertCoordinates3D_F32.latlonToUnitVector(temp.x,temp.y, norm);
	}

	/**
	 * Converts the equirectangular coordinate into a latitude and longitude
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param latlon  (output) x = longitude, y = latitude
	 */
	public void equiToLatlon( float x , float y , Point2D_F32 latlon ) {
		float lon = (x/width - lonCenterFrac)*GrlConstants.F_PI;
		float lat = (y/height - latCenterFrac)*GrlConstants.F_PI2;

		latlon.x = UtilAngle.boundHalf(lon);
		latlon.y = UtilAngle.bound(lat);
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates
	 * @param lon Longitude
	 * @param lat Latitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void latlonToRect(float lon , float lat , Point2D_F32 rect ) {
		rect.x = UtilAngle.wrapZeroToOne(lon / GrlConstants.F_PI + lonCenterFrac)*width;
		rect.y = UtilAngle.reflectZeroToOne(lat / GrlConstants.F_PI2 + latCenterFrac)*height;
	}

	public float getCenterLatitude() {
		return latCenterFrac*GrlConstants.F_PI - GrlConstants.F_PId2;
	}

	public float getCenterLongitude() {
		return lonCenterFrac*GrlConstants.F_PI2 - GrlConstants.F_PI;
	}

}
