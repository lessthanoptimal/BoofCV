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
 * Contations common operations for handling coordinates in an equirectangular image.  An equirectangular image is
 * a spherical coordinate system which has been projected onto a 2D image.
 *
 * longtitude is along the x-axis and goes from -pi to pi
 * latitude is along the y-axis and goes from -pi/2 to pi/2
 *
 * @author Peter Abeles
 */
public class EquirectangularCoordinates_F32 {
	// input image width and height
	int width;
	int height;

	// location focus as a fractional coordinate
	float latCenterFrac;
	float lonCenterFrac;

	// internal storage to avoid declaring new memory
	Point2D_F32 temp = new Point2D_F32();

	public void configure( int width , int height , float latitudeCenter , float longitudeCenter  ) {
		this.width = width;
		this.height = height;

		latCenterFrac = (latitudeCenter + GrlConstants.F_PId2)/GrlConstants.F_PI;
		lonCenterFrac = (longitudeCenter + (float)Math.PI)/(GrlConstants.F_PI2);
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
	 * @param latlon  (output) x = latitude, y = longitude
	 */
	public void equiToLatlon( float x , float y , Point2D_F32 latlon ) {
		float lat = (x/(width-1) - latCenterFrac)*GrlConstants.F_PI2;
		latlon.x = UtilAngle.bound(lat);

		float lon = (y/(height-1) - lonCenterFrac)*GrlConstants.F_PI;
		latlon.y = UtilAngle.boundHalf(lon);
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates
	 * @param lat Latitude
	 * @param lon Longitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void latlonToRect(float lat , float lon , Point2D_F32 rect ) {
		rect.x = UtilAngle.wrapZeroToOne(lat / GrlConstants.F_PI + latCenterFrac)*(width-1);
		rect.y = UtilAngle.reflectZeroToOne(lon / GrlConstants.F_PId2 + lonCenterFrac)*(height-1);
	}

}
