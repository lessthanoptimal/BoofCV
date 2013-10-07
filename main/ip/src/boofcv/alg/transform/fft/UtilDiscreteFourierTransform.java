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

package boofcv.alg.transform.fft;

/**
 * @author Peter Abeles
 */
public class UtilDiscreteFourierTransform {
	/**
	 * true if the number provided is a power of two
	 * @param x number
	 * @return true if it is a power of two
	 */
	public static boolean isPowerOf2(int x) {
		if (x <= 0)
			return false;
		else
			return (x & (x - 1)) == 0;
	}

	/**
	 * Returns the closest power-of-two number greater than or equal to x.
	 *
	 * @param x
	 * @return the closest power-of-two number greater than or equal to x
	 */
	public static int nextPow2(int x) {
		if (x < 1)
			throw new IllegalArgumentException("x must be greater or equal 1");
		if ((x & (x - 1)) == 0) {
			return x; // x is already a power-of-two number
		}
		x |= (x >>> 1);
		x |= (x >>> 2);
		x |= (x >>> 4);
		x |= (x >>> 8);
		x |= (x >>> 16);
		x |= (x >>> 32);
		return x + 1;
	}
}
