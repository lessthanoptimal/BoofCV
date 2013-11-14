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

package boofcv.abst.tracker;

/**
 * Configuration for {@link boofcv.alg.tracker.circulant.CirculantTracker}.
 *
 * @author Peter Abeles
 */
public class ConfigCirculantTracker {
	/**
	 * Spatial bandwidth.  Proportional to target size.
	 */
	public double output_sigma_factor = 1.0/16.0;
	/**
	 *  gaussian kernel bandwidth
	 */
	public float sigma = 0.2f;
	/**
	 * Regularization term.
	 */
	public float lambda = 1e-2f;
	/**
	 * Weighting factor mixing old track image and new one.  Effectively adjusts the rate at which it can adjust
	 * to changes in appearance.  Values closer to zero slow down the rate of change.  0f is no update.
	 * 0.075f is recommended.
	 */
	public float interp_factor = 0.075f;
	/**
	 * Maximum pixel value.  Used to normalize image.  8-bit images are 255
	 */
	public float maxPixelValue = 255.0f;

	public ConfigCirculantTracker(float interp_factor) {
		this.interp_factor = interp_factor;
	}

	public ConfigCirculantTracker() {
	}
}
