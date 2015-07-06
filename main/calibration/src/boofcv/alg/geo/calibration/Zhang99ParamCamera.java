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

package boofcv.alg.geo.calibration;

/**
 * Intrinsic camera parameters for optimization in Zhang99
 *
 * @author Peter Abeles
 */
public class Zhang99ParamCamera {
	// camera calibration matrix
	public double a,b,c,x0,y0;
	// radial distortion
	public double radial[];
	// tangential distortion
	public double t1,t2;

	// does it assume c = 0?
	public boolean assumeZeroSkew;
	// should it estimate the tangetial terms?
	public boolean includeTangential;

	public Zhang99ParamCamera(boolean assumeZeroSkew, int numRadial, boolean includeTangential) {
		this.assumeZeroSkew = assumeZeroSkew;
		radial = new double[numRadial];
		this.includeTangential = includeTangential;
	}

	public Zhang99ParamCamera() {
	}

	/**
	 * Sets to zero parameters which are assumed zero and not estimated
	 */
	public void zeroNotUsed() {
		if( assumeZeroSkew)
			c = 0;
		if( !includeTangential ) {
			t1 = t2 = 0;
		}
	}

	/**
	 * Returns the total number of parameters being estimated
	 */
	public int numParameters() {
		int total = 4 + radial.length;
		if( !assumeZeroSkew )
			total += 1;
		if( includeTangential ) {
			total += 2;
		}

		return total;
	}

	/**
	 * Sets the camera parameters from the passed in array
	 *
	 * @return number of parameters read.
	 */
	public int setFromParam( double param[] ) {
		int index = 0;

		a = param[index++];
		b = param[index++];
		if (!assumeZeroSkew)
			c = param[index++];
		x0 = param[index++];
		y0 = param[index++];

		for (int i = 0; i < radial.length; i++) {
			radial[i] = param[index++];
		}

		if (includeTangential) {
			t1 = param[index++];
			t2 = param[index++];
		}

		return index;
	}

	/**
	 * Writes the parameters into the provided array
	 *
	 * @return number of parameters
	 */
	public int convertToParam( double param[] ) {
		int index = 0;

		param[index++] = a;
		param[index++] = b;
		if (!assumeZeroSkew)
			param[index++] = c;
		param[index++] = x0;
		param[index++] = y0;

		for (int i = 0; i < radial.length; i++) {
			param[index++] = radial[i];
		}

		if (includeTangential) {
			param[index++] = t1;
			param[index++] = t2;
		}

		return index;
	}
}
