/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.feature.disparity;

import boofcv.factory.transform.census.CensusVariants;
import boofcv.struct.Configuration;
import boofcv.struct.pyramid.ConfigDiscreteLevels;

/**
 * Configurations for different types of disparity error metrics
 *
 * @author Peter Abeles
 */
public interface ConfigDisparityError extends Configuration {

	/**
	 * Configuration for Census
	 */
	class Census implements ConfigDisparityError {
		public CensusVariants variant = CensusVariants.BLOCK_5_5;

		@Override
		public void checkValidity() {

		}
	}

	/**
	 * Normalized cross correlation error
	 */
	class NCC implements ConfigDisparityError {

		/**
		 * Used to avoid a divide by zero error when dividing by the standard deviation. Only used with NCC. Smaller
		 * values are more mathematically accurate but make it more sensitive to floating point error.
		 * This has been tuned to work with pixel values that have been scaled to -1 to 1.
		 */
		public double eps = 5.5e-6;

		/**
		 * If true then the input will be normalized so that it has zero mean and a max absolute value of one. Reduces
		 * numerical issues.
		 */
		public boolean normalizeInput=true;

		@Override
		public void checkValidity() {

		}
	}

	/**
	 * Configuration for Hierarchical Mutual Information.
	 */
	class HMI implements ConfigDisparityError {
		/**
		 * Number of possible pixel values. MI was designed around 8-bit images and performance will
		 * degrade for large values.
		 */
		public int totalGrayLevels=256;
		/**
		 * Specifies the smallest layer in pyramid. Used to compute Mutual Information Cost
		 */
		public ConfigDiscreteLevels pyramidLayers = new ConfigDiscreteLevels(-1,50,-1);
		/**
		 * Radius of Gaussian kernel when applying smoothing during Mutual Information computation.
		 */
		public int smoothingRadius = 3;

		/**
		 * Number of additional iterations to perform. This should improve the MI estimate, but the cost
		 * is significant.
		 */
		public int extraIterations = 0;

		@Override
		public void checkValidity() {

		}
	}
}
