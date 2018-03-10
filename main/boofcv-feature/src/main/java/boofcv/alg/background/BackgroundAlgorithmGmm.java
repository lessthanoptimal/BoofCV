/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background;

/**
 * <p>
 * Background model in which each pixel is modeled as a Gaussian mixture model. The number of Gaussians in the
 * mixture is determined dynamically. If a pixel value is encountered which doesn't match any mixture then a
 * new Gaussian is added. Gaussians are removed when their weight becomes negative. See [1] for details.
 * </p>
 *
 * <p>Tuning Parameters:</p>
 * <ul>
 * <li><b>learningPeriod:</b> Specifies how fast a Gaussian changes. Larger values is slower learning. Try 100</li>
 * <li><b>decayCoef:</b> Adjusts how quickly a Gaussian's weight is reduced. Try 0.001</li>
 * <li><b>maxGaussian:</b> Maximum number of Gaussian models. Try 10</li>
 * <li><b>initial variance</b> The initial variance assigned to pixels when they are first observed.  By default this is
 * Float.MIN_VALUE.
 * </ul>
 *
 * <p>
 * [1] Zivkovic, Zoran. "Improved adaptive Gaussian mixture model for background subtraction."
 * In Pattern Recognition, 2004. ICPR 2004. Proceedings of the 17th International Conference on,
 * vol. 2, pp. 28-31. IEEE, 2004.
 * </p>
 *
 * @author Peter Abeles
 */
public interface BackgroundAlgorithmGmm {

	/**
	 * Returns the initial variance assigned to a pixel
	 * @return initial variance
	 */
	float getInitialVariance();

	/**
	 * Sets the initial variance assigned to a pixel
	 * @param initialVariance initial variance
	 */
	void setInitialVariance(float initialVariance);

	/**
	 * Returns the learning period.
	 */
	float getLearningPeriod();

	/**
	 * Specifies the learning rate
	 * @param period Must be more than 0.
	 */
	void setLearningPeriod(float period);

	/**
	 * Minimum value of a Gaussian's weight to be considered part of the background
	 */
	void setSignificantWeight( float value );
}
