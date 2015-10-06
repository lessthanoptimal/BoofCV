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

package boofcv.factory.filter.binary;

import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.image.ImageUInt8;

/**
 * Enum for all the types of thresholding provided in BoofCV
 *
 * @author Peter Abeles
 */
public enum ThresholdType {
	/**
	 * Fixed threshold
	 */
	FIXED,
	/**
	 * Globally adaptive set using entropy equation
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeEntropy(int[], int, int)
	 */
	GLOBAL_ENTROPY,
	/**
	 * Globally adaptive set using Otsu's equation
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu(int[], int, int)
	 */
	GLOBAL_OTSU,
	/**
	 * Locally adaptive computed using Guassian weights
	 *
	 * @see ThresholdImageOps#adaptiveGaussian(ImageUInt8, ImageUInt8, int, int, boolean, ImageUInt8, ImageUInt8)
	 */
	LOCAL_GAUSSIAN,
	/**
	 * Locally adaptive computed using Guassian weights
	 *
	 * @see ThresholdImageOps#adaptiveSquare(ImageUInt8, ImageUInt8, int, int, boolean, ImageUInt8, ImageUInt8)
	 */
	LOCAL_SQUARE,
	/**
	 * Locally adaptive computed using Savola's method
	 *
	 * @see boofcv.alg.filter.binary.impl.ThresholdSauvola
	 */
	LOCAL_SAVOLA
}
