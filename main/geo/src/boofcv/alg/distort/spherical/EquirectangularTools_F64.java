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
import georegression.struct.point.Point3D_F64;

/**
 * Contains common operations for handling coordinates in an equirectangular image.
 * On most globes, a positive latitude corresponds to the north pole, or up, and negative towards the south pole.
 * Images have 0 on the top and increase downwards.  To compensate for this the y-axis can be flipped.  This
 * is indicated by functions with FV (flip vertical) on the end of their name.
 *
 * <p>
 * longtitude is along the x-axis and goes from -pi to pi<br>
 * latitude is along the y-axis and goes from -pi/2 to pi/2
 * </p>
 *
 * @author Peter Abeles
 */
public class EquirectangularTools_F64 {
	// input image width and height
	int width;
	int height;

	// internal storage to avoid declaring new memory
	Point2D_F64 temp = new Point2D_F64();

	/**
	 * Specifies the image and which latitude/longtiude will comprise the center axises
	 * @param width Image width
	 * @param height Image height
	 */
	public void configure( int width , int height ) {
		this.width = width;
		this.height = height;
	}

	/**
	 * Converts equirectangular into normalized pointing vector
	 *
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param norm Normalized pointing vector
	 */
	public void equiToNorm(double x , double y , Point3D_F64 norm ) {
		equiToLonlat(x,y, temp);
		ConvertCoordinates3D_F64.latlonToUnitVector(temp.y,temp.x, norm);
	}

	public void normToEqui( double nx , double ny , double nz , Point2D_F64 rect ) {
		/**/double r = /**/Math.sqrt(nx*nx + ny*ny);

		/**/double lon = /**/Math.atan2(ny,nx);
		/**/double lat = UtilAngle.atanSafe(-nz,r);

		lonlatToEqui( (double) lon, (double) lat,rect);
	}

	public void equiToNormFV(double x , double y , Point3D_F64 norm ) {
		equiToLonlatFV(x,y, temp);
		ConvertCoordinates3D_F64.latlonToUnitVector(temp.y,temp.x, norm);
	}

	public void normToEquiFV( double nx , double ny , double nz , Point2D_F64 rect ) {
		/**/double r = /**/Math.sqrt(nx*nx + ny*ny);

		/**/double lon = /**/Math.atan2(ny,nx);
		/**/double lat = UtilAngle.atanSafe(-nz,r);

		lonlatToEquiFV( (double) lon, (double) lat,rect);
	}

	/**
	 * Converts the equirectangular coordinate into a latitude and longitude
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param lonlat  (output) x = longitude, y = latitude
	 */
	public void equiToLonlat(double x , double y , Point2D_F64 lonlat ) {
		lonlat.x = (x/width - 0.5)*GrlConstants.PI2; // longitude
		lonlat.y = (y/height - 0.5)*GrlConstants.PI; // latitude
	}

	/**
	 * <p>
	 * Converts the equirectangular coordinate into a latitude and longitude.
	 * Vertical equirectangular axis has been flipped
	 * </p>
	 * y' = height - y - 1
	 *
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param lonlat  (output) x = longitude, y = latitude
	 */
	public void equiToLonlatFV(double x , double y , Point2D_F64 lonlat ) {
		lonlat.x = (x/width - 0.5)*GrlConstants.PI2; // longitude
		lonlat.y = ((height-y-1.0)/height - 0.5)*GrlConstants.PI; // latitude
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates
	 * @param lon Longitude
	 * @param lat Latitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void lonlatToEqui(double lon , double lat , Point2D_F64 rect ) {
		rect.x = UtilAngle.wrapZeroToOne(lon / GrlConstants.PI2 + 0.5)*width;
		rect.y = UtilAngle.wrapZeroToOne(lat / GrlConstants.PI + 0.5)*height;
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates.
	 * Vertical equirectangular axis has been flipped
	 * @param lon Longitude
	 * @param lat Latitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void lonlatToEquiFV(double lon , double lat , Point2D_F64 rect ) {
		rect.x = UtilAngle.wrapZeroToOne(lon / GrlConstants.PI2 + 0.5)*width;
		rect.y = UtilAngle.wrapZeroToOne(lat / GrlConstants.PI + 0.5)*height;
		rect.y = height - rect.y - 1;
	}
}
