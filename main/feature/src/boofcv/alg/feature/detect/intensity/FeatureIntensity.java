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

package boofcv.alg.feature.detect.intensity;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Base interface for classes which extract intensity images for image feature detection. In the
 * intensity image higher values indicate that a pixel is more "feature like".  All intensity
 * images are {@link GrayF32}.
 * </p>
 *
 * @author Peter Abeles
 */
public interface FeatureIntensity<T extends ImageGray> {

	/**
	 * Returns the radius of the feature being computed.  Features are square in shape with a width = 2*radius+1.
	 *
	 * @return Radius of detected features.
	 */
	public int getRadius();

	/**
	 * Size of the region surrounding the image's border in which pixels are not processed.
	 *
	 * @return The ignore border around the image.
	 */
	public int getIgnoreBorder();
}
