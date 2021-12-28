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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Interface for automatic interest point detection in an image. Optional support is
 * provided for scale and orientation.
 *
 * Features can belong to multiple set. A feature set indicates that the features were some how detected using
 * mutually exclusive methods. A classical example comes from blob detectors where there will naturally be
 * two sets composed of dark and white blobs.
 *
 * @author Peter Abeles
 */
// TODO Rename to DetectorInterestPoint? or DetectorPointSO
public interface InterestPointDetector<T extends ImageBase> extends FoundPointSO, FeatureSets {

	/**
	 * Detects interest points inside the provided image.
	 *
	 * @param input Input features are detected inside of.
	 */
	void detect( T input );

	/**
	 * Does the interest point detector have scale information. This made available through the radius.
	 *
	 * @return true if it has scale information and false otherwise
	 */
	boolean hasScale();

	/**
	 * If the interest point detector estimates the feature's orientation
	 *
	 * @return true if it estimates the orientation
	 */
	boolean hasOrientation();

	/**
	 * Get the expected input image type
	 */
	ImageType<T> getInputType();
}
