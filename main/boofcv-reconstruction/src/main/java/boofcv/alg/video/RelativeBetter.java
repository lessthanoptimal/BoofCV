/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.video;

import lombok.Getter;
import lombok.Setter;

/**
 * Used to compare how much better a metric A is than metric B. If A is not better then 0.0 is returned.
 *
 * @author Peter Abeles
 */
public interface RelativeBetter {
	/**
	 * Computes how good valueA is relative to valueB. If equal or N is better than 0.0 is returned. Otherwise
	 * a positive value is returned.
	 *
	 * @param valueA Goodness value for A
	 * @param valueB Goodness value for B
	 * @return goodness ratio of A over B
	 */
	double computeBetterValue( double valueA , double valueB );

	/**
	 * Compares error metrics (0.0 = best, larger is worse) with a hard minimum in the value of B to dampen
	 * noise for small values and avoid divide by zero errors.
	 */
	class ErrorHardRatio implements RelativeBetter {
		@Getter @Setter double minimumB;

		public ErrorHardRatio( double minimumB ) {
			this.minimumB = minimumB;
		}

		@Override public double computeBetterValue( double valueA, double valueB ) {
			// if B is too small then there is insufficient information
			if (valueB<minimumB)
				return 0.0;

			if (valueA==0.0)
				return Double.MAX_VALUE;
			return Math.max(0.0 , valueB/valueA - 1.0);
		}
	}

	/**
	 * Same as {@link ErrorHardRatio} but it assumes the input has been squared
	 */
	class ErrorHardRatioSq implements RelativeBetter {
		@Getter @Setter double minimumB;

		public ErrorHardRatioSq( double minimumB ) {
			this.minimumB = minimumB;
		}

		@Override public double computeBetterValue( double valueA, double valueB ) {
			// if B is too small then there is insufficient information
			if (valueB<minimumB*minimumB)
				return 0.0;

			if (valueA==0.0)
				return Double.MAX_VALUE;
			return Math.max(0.0 , Math.sqrt(valueB)/Math.sqrt(valueA) - 1.0);
		}
	}


	/**
	 * Computes a ratio where the values are being maximized. 0 to positive infinity. The denominator
	 * has epsilon added to it to dampen out small values and avoid divide by zero
	 */
	class MaximizeSoftRatio implements RelativeBetter {
		@Getter @Setter double epsilon;

		public MaximizeSoftRatio( double epsilon ) {
			this.epsilon = epsilon;
		}

		@Override public double computeBetterValue( double valueA, double valueB ) {
			return Math.max(0.0 , valueA/(epsilon + valueB) - 1.0);
		}
	}
}
