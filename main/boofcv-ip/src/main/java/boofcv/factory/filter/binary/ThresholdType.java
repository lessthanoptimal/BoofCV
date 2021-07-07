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

package boofcv.factory.filter.binary;

import boofcv.alg.filter.binary.*;

/**
 * Enum for all the types of thresholding provided in BoofCV
 *
 * @author Peter Abeles
 */
public enum ThresholdType {
	/**
	 * Fixed threshold
	 */
	FIXED(false, true),
	/**
	 * Globally adaptive set using entropy equation
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeEntropy(int[], int, int)
	 */
	GLOBAL_ENTROPY(true, true),
	/**
	 * Globally adaptive set using Otsu's equation
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeOtsu(int[], int, int)
	 */
	GLOBAL_OTSU(true, true),
	/**
	 * Global Adaptive using Li's equation.
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeLi(int[], int)
	 */
	GLOBAL_LI(true, true),
	/**
	 * Global Adaptive using Huang's equation.
	 *
	 * @see boofcv.alg.filter.binary.GThresholdImageOps#computeHuang(int[], int)
	 */
	GLOBAL_HUANG(true, true),
	/**
	 * Locally adaptive computed using Guassian weights
	 *
	 * @see ThresholdImageOps#localGaussian
	 */
	LOCAL_GAUSSIAN(true, false),
	/**
	 * Locally adaptive using an average
	 *
	 * @see ThresholdImageOps#localMean
	 */
	LOCAL_MEAN(true, false),
	/**
	 * Applies a local Otsu across the entire image
	 *
	 * @see ThresholdLocalOtsu
	 */
	LOCAL_OTSU(true, false),
	/**
	 * Breaks the image into blocks and computes the min and max inside each block. Then thresholds
	 * each pixel using interpolated min/max values.
	 *
	 * @see ThresholdBlockMinMax
	 */
	BLOCK_MIN_MAX(true, false),

	/**
	 * Breaks the image into blocks and computes the mean inside each block.
	 *
	 * @see ThresholdBlockMean
	 */
	BLOCK_MEAN(true, false),

	/**
	 * Breaks the image into blocks and computes the an Otsu threshold in each block
	 *
	 * @see ThresholdBlockOtsu
	 */
	BLOCK_OTSU(true, false),

	/**
	 * Locally adaptive computed using Niblack's method
	 *
	 * @see ThresholdNiblackFamily
	 */
	LOCAL_NIBLACK(true, false),

	/**
	 * Locally adaptive computed using Savola's method
	 *
	 * @see ThresholdNiblackFamily
	 */
	LOCAL_SAVOLA(true, false),

	/**
	 * Locally adaptive computed using Wolf's method
	 *
	 * @see ThresholdNiblackFamily
	 */
	LOCAL_WOLF(true, false),

	/**
	 * Locally adaptive computed using NICK method
	 *
	 * @see boofcv.alg.filter.binary.ThresholdNick
	 */
	LOCAL_NICK(true, false);

	final boolean adaptive;
	final boolean global;

	ThresholdType( boolean adaptive, boolean global ) {
		this.adaptive = adaptive;
		this.global = global;
	}

	public boolean isAdaptive() {
		return adaptive;
	}

	public boolean isGlobal() {
		return global;
	}
}
