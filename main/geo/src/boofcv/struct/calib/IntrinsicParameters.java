/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
 * Radial and Tangental Distortion:<br>
 * x<sub>d</sub> = x<sub>n</sub> + x<sub>n</sub>[k<sub>1</sub> r<sup>2</sup> + ... + k<sub>n</sub> r<sup>2n</sup>] + dx<br>
 * dx<sub>u</sub> = [ 2t<sub>1</sub> u v + t<sub>2</sub>(r<sup>2</sup> + 2u<sup>2</sup>)] <br>
 * dx<sub>v</sub> = [ t<sub>1</sub>(r<sup>2</sup> + 2v<sup>2</sup>) + 2 t<sub>2</sub> u v]<br>
 * <br>
 * r<sup>2</sup> = u<sup>2</sup> + v<sup>2</sup><br>
 * where x<sub>d</sub> is the distorted coordinates, x<sub>n</sub>=(u,v) is undistorted normalized image coordinates.
 * Pixel coordinates are found x = K*[u;v]
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

	/** focal length along x and y axis (units: pixels) */
	public double fx,fy;
	/** skew parameter, typically 0 (units: pixels)*/
	public double skew;
	/** image center (units: pixels) */
	public double cx,cy;

	/** radial distortion parameters */
	public double radial[];
	/** tangential distortion parameters */
	public double t1, t2;

	/**
	 * Default constructor.  flipY is false and everything else is zero or null.
	 */
	public IntrinsicParameters() {
	}

	public IntrinsicParameters( IntrinsicParameters param ) {
		set(param);
	}

	public IntrinsicParameters( double fx, double fy,
								double skew,
								double cx, double cy,
								int width, int height ) {
		fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public IntrinsicParameters fsetK(double fx, double fy,
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

	public IntrinsicParameters fsetRadial( double ...radial ) {
		this.radial = radial.clone();
		return this;
	}

	public IntrinsicParameters fsetTangental( double t1 , double t2) {
		this.t1 = t1;
		this.t2 = t2;
		return this;
	}

	public void set( IntrinsicParameters param ) {
		this.fx = param.fx;
		this.fy = param.fy;
		this.skew = param.skew;
		this.cx = param.cx;
		this.cy = param.cy;
		this.width = param.width;
		this.height = param.height;
		this.radial = param.radial;

		if( param.radial != null )
			radial = param.radial.clone();

		this.t1 = param.t1;
		this.t2 = param.t2;
	}

	/**
	 * If true then distortion parameters are specified.
	 */
	public boolean isDistorted() {
		if( radial == null || radial.length == 1 )
			return t1 != 0 || t2 != 0;
		return true;
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

	public double getT1() {
		return t1;
	}

	public void setT1(double t1) {
		this.t1 = t1;
	}

	public double getT2() {
		return t2;
	}

	public void setT2(double t2) {
		this.t2 = t2;
	}

	public void print() {
		System.out.println("Shape "+width+" "+height);
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
		if( t1 != 0 && t2 != 0)
			System.out.printf("tangential = ( %6.2e , %6.2e)\n", t1, t2);
		else {
			System.out.println("No tangential");
		}
	}
}
