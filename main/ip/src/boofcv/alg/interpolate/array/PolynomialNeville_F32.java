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
 * <p>
 * Neville's algorithm for polynomial interpolation and extrapolation.  Neville's algorithm improves upon
 * Lagrange's formula by avoiding repetitive calculations.
 * </p>
 * <p>
 * See Numerical Recipes Third Edition page 118.
 * </p>
 * @author Peter Abeles
 */
public class PolynomialNeville_F32 extends Interpolate1D_F32 {
	private float c[];
	private float d[];

	public PolynomialNeville_F32(int maxDegree) {
		super(maxDegree);
		c = new float[M];
		d = new float[M];
	}

	public PolynomialNeville_F32(int maxDegree, float x[], float y[], int size) {
		super(maxDegree, x, y, size);
		c = new float[M];
		d = new float[M];
	}

	@Override
	protected float compute(float sample) {
		int i0 = index0;

		// find the index with the smallest difference and set c and b arrays
		// to their initial values
		int closestIndex = 0;
		float smallestDiff = Math.abs(sample - x[i0]);

		for (int i = i0; i < i0 + M; i++) {
			float diff = Math.abs(sample - x[i]);
			if (diff < smallestDiff) {
				closestIndex = i - i0;
				smallestDiff = diff;
			}
			c[i - i0] = y[i];
			d[i - i0] = y[i];
		}

		float estimate = y[i0 + closestIndex--];

		for (int m = 1; m < M; m++) {
			for (int i = 0; i < M - m; i++) {
				float ho = x[i0 + i] - sample;
				float hp = x[i0 + i + m] - sample;
				float w = c[i + 1] - d[i];

				float den = ho - hp;
				if (den == 0.0) {
					throw new RuntimeException("Two x's are identical");
				}
				den = w / den;
				d[i] = hp * den;
				c[i] = ho * den;
			}

			if (2 * (closestIndex + 1) < M - m) {
				estimate += c[closestIndex + 1];
			} else {
				estimate += d[closestIndex];
				closestIndex--;
			}
		}
		return estimate;
	}
}