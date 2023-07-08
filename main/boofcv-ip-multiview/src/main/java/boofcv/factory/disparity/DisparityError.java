/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.disparity;

/**
 * Different disparity error functions that are available. Not all algorithm will support all errors
 *
 * @author Peter Abeles
 */
public enum DisparityError {
	/**
	 * Sum of Absolute Difference (SAD). It's often recommended that an image derivative like Laplacian is applied
	 * first to add improved performance ot variable lighting between the two images.
	 */
	SAD,
	/**
	 * Census. Can handle affine changes in lighting between the two images. There are many different possible
	 * sampling patterns.
	 *
	 * @see boofcv.alg.transform.census.CensusTransform
	 */
	CENSUS,
	/**
	 * Normalized Cross Correlation (NCC). The NCC radius specifies the size of the local region used to compute
	 * normalization statistics.
	 */
	NCC;

	public boolean isCorrelation() {
		return switch (this) {
			case SAD, CENSUS -> false;
			default -> true;
		};
	}

	/**
	 * If the error is distance squared or false if distance
	 */
	public boolean isSquared() {
		return switch (this) {
			case SAD, CENSUS -> false;
			default -> true;
		};
	}
}
