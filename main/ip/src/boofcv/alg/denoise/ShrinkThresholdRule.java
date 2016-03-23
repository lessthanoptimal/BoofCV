/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.denoise;

import boofcv.struct.image.ImageGray;


/**
 * <p>
 * Generalized interface for thresholding wavelet coefficients in shrinkage based wavelet denoising applications.
 * </p>
 *
 * @author Peter Abeles
 */
public interface ShrinkThresholdRule<T extends ImageGray> {

	/**
	 * Applies shrinkage to entire image.  If the rule should only be applied to part
	 * of the image then a sub-image should be passed in. If the threshold is an infinite number
	 * then all the coefficients are considered below the threshold.
	 *
	 * @param image Image which is to be thresholded. Is modified.
	 * @param threshold Threshold used to modify the image.
	 */
	public void process( T image , Number threshold );
}
