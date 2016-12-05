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

package boofcv.factory.background;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link ConfigBackgroundBasic}.
 *
 * @author Peter Abeles
 */
public class ConfigBackgroundBasic implements Configuration {

	/**
	 * Specifies how fast it will adapt to changes in the background. From 0 to 1, inclusive.  0 = static  1.0 = instant.
	 */
	public float learnRate = 0.05f;

	/**
	 * Threshold for classifying a pixel as background or not.  If euclidean distance less than or equal to this value
	 * it is background.
	 */
	public float threshold;

	/**
	 * Specifies which interpolation it will use.  {@link InterpolationType#BILINEAR} or
	 * {@link InterpolationType#NEAREST_NEIGHBOR} recommended.
	 *
	 * <p>ONLY USED FOR MOVING BACKGROUNDS!</p>
	 */
	public InterpolationType interpolation = InterpolationType.BILINEAR;

	public ConfigBackgroundBasic(float threshold) {
		this.threshold = threshold;
	}

	public ConfigBackgroundBasic(float threshold, float learnRate) {
		this.threshold = threshold;
		this.learnRate = learnRate;
	}

	@Override
	public void checkValidity() {
		if( learnRate < 0 || learnRate > 1 )
			throw new IllegalArgumentException("Learn rate must be 0 <= rate <= 1");
		if( threshold <= 0 )
			throw new IllegalArgumentException("threshold must be > 0");
	}
}
