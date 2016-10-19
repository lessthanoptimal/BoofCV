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

package boofcv.struct.calib;

import java.io.Serializable;

/**
 * <p>
 * Intrinsic camera parameters for a pinhole camera.  Specifies the calibration
 * matrix K and distortion parameters.
 * </p>
 *
 * <pre>
 *     [ fx skew  cx ]
 * K = [  0   fy  cy ]
 *     [  0    0   1 ]
 * </pre>
 *
 * @author Peter Abeles
 */
public class CameraPinhole extends CameraModel implements Serializable {

	// serialization version
	public static final long serialVersionUID = 1L;

	/** focal length along x and y axis (units: pixels) */
	public double fx,fy;
	/** skew parameter, typically 0 (units: pixels)*/
	public double skew;
	/** image center (units: pixels) */
	public double cx,cy;

	/**
	 * Default constructor.  flipY is false and everything else is zero or null.
	 */
	public CameraPinhole() {
	}

	public CameraPinhole(CameraPinhole param ) {
		set(param);
	}

	public CameraPinhole(double fx, double fy,
						 double skew,
						 double cx, double cy,
						 int width, int height ) {
		fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public CameraPinhole fsetK(double fx, double fy,
							   double skew,
							   double cx, double cy,
							   int width, int height) {
		this.fx = fx;
		this.fy = fy;
		this.skew = skew;
		this.cx = cx;
		this.cy = cy;
		this.width = width;
		this.height = height;

		return this;
	}

	public void set( CameraPinhole param ) {
		this.fx = param.fx;
		this.fy = param.fy;
		this.skew = param.skew;
		this.cx = param.cx;
		this.cy = param.cy;
		this.width = param.width;
		this.height = param.height;
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

	public double getSkew() {
		return skew;
	}

	public void setSkew(double skew) {
		this.skew = skew;
	}

	public void print() {
		System.out.println("Shape "+width+" "+height);
		System.out.printf("center %7.2f %7.2f\n", cx, cy);
		System.out.println("fx = " + fx);
		System.out.println("fy = "+fy);
		System.out.println("skew = "+skew);
	}
}
