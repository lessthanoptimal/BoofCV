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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.image.ImageBase;

/**
 * Interface for automatic interest point detection in an image.  Optional support is
 * provided for scale and orientation.
 *
 * @author Peter Abeles
 */
// TODO Rename to DetectorInterestPoint? or DetectorPointSO
public interface InterestPointDetector< T extends ImageBase> extends FoundPointSO {

	/**
	 * Detects interest points inside the provided image.
	 *
	 * @param input Input features are detected inside of.
	 */
	void detect( T input );

	/**
	 * Does the interest point detector have scale information
	 *
	 * @deprecated Likely to be removed since this flag has no use in actual algorithms to date.
	 * @return true if it has scale information and false otherwise
	 */
	public boolean hasScale();

	/**
	 * If the interest point detector estimates the feature's orientation
	 *
	 * @deprecated Likely to be removed since this flag has no use in actual algorithms to date.
	 * @return true if it estimates the orientation
	 */
	public boolean hasOrientation();
}
