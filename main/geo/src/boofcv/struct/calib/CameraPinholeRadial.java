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
 * <p>Adds radial and tangential distortion to the intrinsic parameters of a {@link CameraPinhole pinhole camera}.</p?
 *
 * <p>
 * Radial and Tangential Distortion:<br>
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
public class CameraPinholeRadial extends CameraPinhole implements Serializable {

	/** radial distortion parameters */
	public double radial[];
	/** tangential distortion parameters */
	public double t1, t2;

	/**
	 * Default constructor.  flipY is false and everything else is zero or null.
	 */
	public CameraPinholeRadial() {
	}

	public CameraPinholeRadial(CameraPinholeRadial param ) {
		set(param);
	}

	public CameraPinholeRadial(double fx, double fy,
							   double skew,
							   double cx, double cy,
							   int width, int height ) {
		fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public CameraPinholeRadial fsetK(double fx, double fy,
									 double skew,
									 double cx, double cy,
									 int width, int height) {
		return (CameraPinholeRadial)super.fsetK(fx, fy, skew, cx, cy, width, height);
	}

	public CameraPinholeRadial fsetRadial(double ...radial ) {
		this.radial = radial.clone();
		return this;
	}

	public CameraPinholeRadial fsetTangental(double t1 , double t2) {
		this.t1 = t1;
		this.t2 = t2;
		return this;
	}

	public void set( CameraPinholeRadial param ) {
		super.set(param);

		if( param.radial != null )
			radial = param.radial.clone();

		this.t1 = param.t1;
		this.t2 = param.t2;
	}

	/**
	 * If true then distortion parameters are specified.
	 */
	public boolean isDistorted() {
		if( radial != null && radial.length > 0 ) {
			for (int i = 0; i < radial.length; i++) {
				if( radial[i] != 0 )
					return true;
			}
		}
		return t1 != 0 || t2 != 0;
	}

	public double[] getRadial() {
		return radial;
	}

	public void setRadial(double... radial) {
		this.radial = radial;
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
		super.print();
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
