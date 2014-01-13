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

package boofcv.factory.weights;

/**
 * Types of distributions which are available for use as weights
 *
 * @author Peter Abeles
 */
public enum WeightType {
	/**
	 * Uniform distribution
	 */
	UNIFORM,
	/**
	 * Gaussian distribution where the input distance is assumed to be the distance squared.
	 */
	GAUSSIAN_SQ
}
