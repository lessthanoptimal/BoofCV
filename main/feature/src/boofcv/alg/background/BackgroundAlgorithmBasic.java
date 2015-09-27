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

package boofcv.alg.background;

/**
 * <p>
 * Performs background subtraction on an image using the very simple per-pixel "basic" model, as described in [1].
 * A simple image is used to represent the background.  Each pixel is updated individually using the formula below:<br>
 * <br>
 * {@code B(i+1) = (1-&alpha;)*B(i) + &alpha;I(i)}<br>
 * Where B is the background image, I is the current observed image, and &alpha; is the learning rate 0 to 1.
 * Where 0 is static and 1 is instant.
 * </p>
 * <p>
 * If a specific pixel has not been observed before it will be set to the value of the equivalent pixel in the
 * input image.
 * </p>
 *
 * <p>
 * [1] Benezeth, Y., Jodoin, P. M., Emile, B., Laurent, H., & Rosenberger, C. (2010).
 * Comparative study of background subtraction algorithms. Journal of Electronic Imaging, 19(3), 033003-033003.
 * </p>
 *
 * @author Peter Abeles
 */
public interface BackgroundAlgorithmBasic {

	/**
	 * Returns the learning rate.
	 * @return 0 (slow) to 1 (fast)
	 */
	float getLearnRate();

	/**
	 * Specifies the learning rate
	 * @param learnRate 0 (slow) to 1 (fast)
	 */
	void setLearnRate(float learnRate);

	/**
	 * Returns the threshold for background/moving object.
	 * @return background threshold
	 */
	float getThreshold();

	/**
	 * Sets the threshold for background/moving object
	 * @param threshold background threshold
	 */
	void setThreshold(float threshold);
}
