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
 * Same as {@link PolynomialNeville_F32} but it assumes that the points are
 * sampled at integer values only.
 *
 * @author Peter Abeles
 */
public class PolynomialNevilleFixed_F32 {

	// number of sample points
	private int size;
	private float y[];

	float c[];
	float d[];

	public PolynomialNevilleFixed_F32(int maxDegree) {
		c = new float[maxDegree];
		d = new float[maxDegree];
	}

	public PolynomialNevilleFixed_F32(int maxDegree, float y[], int size) {
		this(maxDegree);
		setInput(y, size);
	}

	public void setInput(float y[], int size) {
		this.size = size;
		this.y = y;
	}

	/**
	 * @param sample
	 * @return
	 */
	public float process(float sample, int i0, int i1) {
		if (i1 < i0 || (i1 - i0 + 1) > c.length || i1 >= size) {
			throw new IllegalArgumentException("Bad arguments");
		}

		int M = i1 - i0 + 1;

		// compute the closest index
		int closestIndex = sample % 1f <= 0.5f ? (int) sample : ((int) sample) + 1;
		if (closestIndex > i1) closestIndex = i1;
		else if (closestIndex < i0) closestIndex = i0;
		closestIndex -= i0;

		//set c and b arrays to their initial values
		for (int i = 0; i < M; i++) {
			float valY = y[i + i0];
			c[i] = valY;
			d[i] = valY;
		}

		float estimate = y[i0 + closestIndex--];

		for (int m = 1; m < M; m++) {
			for (int i = 0; i < M - m; i++) {
				float ho = i0 + i - sample;
				float hp = i0 + i + m - sample;
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