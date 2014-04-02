/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.calib;

import java.io.Serializable;

/**
 * <p>
 * Intrinsic camera parameters for a calibrated camera.  Specifies the calibration
 * matrix K and distortion parameters.
 * </p>
 *
 * <p>
 * <pre>
 *     [ fx skew  cx ]
 * K = [  0   fy  cy ]
 *     [  0    0   1 ]
 * </pre>
 * </p>
 *
 * <p>
 * Radial Distortion:<br>
 * x' = x + x[k<sub>1</sub> r<sup>2</sup> + ... + k<sub>n</sub> r<sup>2n</sup>]<br>
 * r<sup>2</sup> = u<sup>2</sup> + v<sup>2</sup><br>
 * where x' is the distorted pixel, x=(x,y) are pixel coordinates and (u,v)=K<sup>-1</sup>x are normalized image coordinates.
 * </p>
 *
 * <p>
 * If flipY axis is true then the y-coordinate was transformed by, y = height - y - 1, (flipped along
 * the vertical axis) during calibration.  This is done to ensure that the image coordinate system has
 * the following characteristics, +Z axis points out of the camera optical axis and is right handed.
 * </p>
 *
 * @author Peter Abeles
 */
// todo move distortion parameters into its own class?
public class IntrinsicParameters implements Serializable {

	// serialization version
	public static final long serialVersionUID = 1L;

	/** image shape (units: pixels) */
	public int width,height;

	/** When calibrated was the y-axis flipped: y = (height - y - 1) */
	public boolean flipY;

	/** focal length along x and y axis (units: pixels) */
	public double fx,fy;
	/** skew parameter, typically 0 (units: pixels)*/
	public double skew;
	/** image center (units: pixels) */
	public double cx,cy;

	/** radial distortion parameters */
	public double radial[];

	public IntrinsicParameters() {
	}

	public IntrinsicParameters(double fx, double fy,
							   double skew,
							   double cx, double cy,
							   int width, int height,
							   boolean flipY, double[] radial) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;
		this.width = width;
		this.height = height;
		this.flipY = flipY;
		this.radial = radial;
	}

	public IntrinsicParameters( IntrinsicParameters param ) {
		set(param);
	}

	public void set( IntrinsicParameters param ) {
		this.fx = param.fx;
		this.fy = param.fy;
		this.skew = param.skew;
		this.cx = param.cx;
		this.cy = param.cy;
		this.width = param.width;
		this.height = param.height;
		this.flipY = param.flipY;
		this.radial = param.radial;

		if( param.radial != null )
			radial = param.radial.clone();
	}

	public double getCx() {
		return cx;
	}

	public void setCx(double cx) {
		this.cx = cx;
	}

	public double getCy() {
		return cy;
	}

	public void setCy(double cy) {
		this.cy = cy;
	}

	public double getFx() {
		return fx;
	}

	public void setFx(double fx) {
		this.fx = fx;
	}

	public double getFy() {
		return fy;
	}

	public void setFy(double fy) {
		this.fy = fy;
	}

	public double[] getRadial() {
		return radial;
	}

	public void setRadial(double[] radial) {
		this.radial = radial;
	}

	public double getSkew() {
		return skew;
	}

	public void setSkew(double skew) {
		this.skew = skew;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean isFlipY() {
		return flipY;
	}

	public void setFlipY(boolean flipY) {
		this.flipY = flipY;
	}

	public void print() {
		System.out.println("Shape "+width+" "+height+" flipY = "+ flipY);
		System.out.printf("center %7.2f %7.2f\n", cx, cy);
		System.out.println("fx = " + fx);
		System.out.println("fy = "+fy);
		System.out.println("skew = "+skew);
		if( radial != null ) {
			for( int i = 0; i < radial.length; i++ ) {
				System.out.printf("radial[%d] = %6.2e\n",i,radial[i]);
			}
		} else {
			System.out.println("No radial");
		}
	}
}
