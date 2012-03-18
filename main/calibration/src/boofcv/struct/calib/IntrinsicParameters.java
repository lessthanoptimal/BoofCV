/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
 * @author Peter Abeles
 */
public class IntrinsicParameters implements Serializable {
	// focal length along x and y axis
	public double fx,fy;
	// skew parameter, typically 0
	public double skew;
	// image center
	public double cx,cy;

	// radial distortion parameters
	public double radial[];

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

	public void print() {
		System.out.println("center x = "+cx);
		System.out.println("center y = "+cy);
		System.out.println("fx = "+fx);
		System.out.println("fy = "+fy);
		System.out.println("skew = "+skew);
		for( int i = 0; i < radial.length; i++ ) {
			System.out.printf("radial[%d] = %6.2e\n",i,radial[i]);
		}
	}
}
