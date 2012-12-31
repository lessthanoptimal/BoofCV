/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.array;


/**
 * Langrange's formula is a straight forward way to perform polynomial interpolation.  It is
 * not the most computationally efficient approach and does not provide any estimate of its accuracy.
 * The order of the polynomial refers to the number of points used in the interpolation minus one.
 *
 * @author Peter Abeles
 */
public class LagrangeFormula {

	/**
	 * UsingLlangrange's formula it interpulates the value of a function at the specified sample
	 * point given discrete samples.  Which samples are used and the order of the approximation are
	 * given by i0 and i1.
	 *
	 * @param sample Where the estimate is done.
	 * @param x	  Where the function was sampled.
	 * @param y	  The function's value at the sample points
	 * @param i0	 The first point considered.
	 * @param i1	 The last point considered.
	 * @return The estimated y value at the sample point.
	 */
	public static double process_F64(double sample, double x[], double y[], int i0, int i1) {
		double result = 0;

		for (int i = i0; i <= i1; i++) {
			double numerator = 1.0;

			for (int j = i0; j <= i1; j++) {
				if (i != j)
					numerator *= sample - x[j];
			}

			double denominator = 1.0;

			double a = x[i];

			for (int j = i0; j <= i1; j++) {
				if (i != j)
					denominator *= a - x[j];
			}

			result += (numerator / denominator) * y[i];
		}

		return result;
	}

	/**
	 * UsingLlangrange's formula it interpulates the value of a function at the specified sample
	 * point given discrete samples.  Which samples are used and the order of the approximation are
	 * given by i0 and i1.  The order is = i1-i0+1.
	 *
	 * @param sample Where the estimate is done.
	 * @param x	  Where the function was sampled.
	 * @param y	  The function's value at the sample points
	 * @param i0	 The first point considered.
	 * @param i1	 The last point considered.
	 * @return The estimated y value at the sample point.
	 */
	public static float process_F32(float sample, float x[], float y[], int i0, int i1) {
		float result = 0;

		for (int i = i0; i <= i1; i++) {
			float numerator = 1.0f;

			for (int j = i0; j <= i1; j++) {
				if (i != j)
					numerator *= sample - x[j];
			}

			float denominator = 1.0f;

			float a = x[i];

			for (int j = i0; j <= i1; j++) {
				if (i != j)
					denominator *= a - x[j];
			}

			result += (numerator / denominator) * y[i];
		}

		return result;
	}
}
