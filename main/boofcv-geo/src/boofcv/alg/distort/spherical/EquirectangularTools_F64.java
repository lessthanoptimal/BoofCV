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

import boofcv.struct.geo.GeoLL_F64;
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
 * Coordinate System:
 * <ul>
 * <li>longitude is along the x-axis and goes from -pi to pi</li>
 * <li>latitude is along the y-axis and goes from -pi/2 to pi/2</li>
 * <li>image center (width/2, (height-1)/2.0f) or (lat=0, lon=0) corresponds to a unit sphere of (1,0,0)</li>
 * <li>unit sphere of (0,0,1) is pixel (width/2,0) and (0,0,-1) is (width/2,height-1)</li>
 * <li>unit sphere of (0,1,0) is pixel (3*width/4,(height-1)/2) and (0,0,-1) is (width/4,(height-1)/2)</li>
 * </ul>
 * Coordinate System with y-flipped:
 * <ul>
 * <li>longitude is along the x-axis and goes from -pi to pi</li>
 * <li>latitude is along the y-axis and goes from pi/2 to -pi/2</li>
 * <li>image center (width/2, height/2) or (lat=0, lon=0) corresponds to a unit sphere of (1,0,0)</li>
 * <li>unit sphere of (0,0,1) is pixel (width/2,height-1) and (0,0,-1) is (width/2,0)</li>
 * <li>unit sphere of (0,1,0) is pixel (3*width/4,(height-1)/2) and (0,0,-1) is (width/4,(height-1)/2)</li>
 * </ul>
 *
 * <p>Latitude and Longitude shown on a sphere in equirectangular format.  Note that y is NOT flipped and angles
 * are shown in degrees not radians git push.</p>
 * <center>
 * <img src="doc-files/equirectangular_lat_lon.png"/>
 * </center>
 *
 * @author Peter Abeles
 */
public class EquirectangularTools_F64 {
	// input image width and height
	int width;
	int height;

	// internal storage to avoid declaring new memory
	GeoLL_F64 temp = new GeoLL_F64();

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
		equiToLatLon(x,y, temp);
		ConvertCoordinates3D_F64.latlonToUnitVector(temp.lat,temp.lon, norm);
	}

	public void normToEqui( double nx , double ny , double nz , Point2D_F64 rect ) {
		/**/double r = /**/Math.sqrt(nx*nx + ny*ny);

		/**/double lon = /**/Math.atan2(ny,nx);
		/**/double lat = UtilAngle.atanSafe(-nz,r);

		latlonToEqui((double) lat, (double) lon, rect);
	}

	public void equiToNormFV(double x , double y , Point3D_F64 norm ) {
		equiToLatLonFV(x,y, temp);
		ConvertCoordinates3D_F64.latlonToUnitVector(temp.lat,temp.lon, norm);
	}

	public void normToEquiFV( double nx , double ny , double nz , Point2D_F64 rect ) {
		/**/double r = /**/Math.sqrt(nx*nx + ny*ny);

		/**/double lon = /**/Math.atan2(ny,nx);
		/**/double lat = UtilAngle.atanSafe(-nz,r);

		latlonToEquiFV((double) lat, (double) lon, rect);
	}

	/**
	 * Converts the equirectangular coordinate into a latitude and longitude
	 * @param x pixel coordinate in equirectangular image
	 * @param y pixel coordinate in equirectangular image
	 * @param geo  (output)
	 */
	public void equiToLatLon(double x , double y , GeoLL_F64 geo ) {
		geo.lon = (x/width - 0.5)*GrlConstants.PI2;
		geo.lat = (y/(height-1) - 0.5)*GrlConstants.PI;
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
	 * @param geo  (output)
	 */
	public void equiToLatLonFV(double x , double y , GeoLL_F64 geo ) {
		geo.lon = (x/width - 0.5)*GrlConstants.PI2;
		geo.lat = ((height-y-1.0)/(height-1) - 0.5)*GrlConstants.PI;
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates
	 * @param lat Latitude
	 * @param lon Longitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void latlonToEqui(double lat, double lon, Point2D_F64 rect) {
		rect.x = UtilAngle.wrapZeroToOne(lon / GrlConstants.PI2 + 0.5)*width;
		rect.y = UtilAngle.reflectZeroToOne(lat / GrlConstants.PI + 0.5)*(height-1);
	}

	/**
	 * Convert from latitude-longitude coordinates into equirectangular coordinates.
	 * Vertical equirectangular axis has been flipped
	 * @param lat Latitude
	 * @param lon Longitude
	 * @param rect (Output) equirectangular coordinate
	 */
	public void latlonToEquiFV(double lat, double lon, Point2D_F64 rect) {
		rect.x = UtilAngle.wrapZeroToOne(lon / GrlConstants.PI2 + 0.5)*width;
		rect.y = UtilAngle.reflectZeroToOne(lat / GrlConstants.PI + 0.5)*(height-1);
		rect.y = height - rect.y - 1;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}
}
