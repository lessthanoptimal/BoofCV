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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
public interface FastCornerInterface<T extends ImageGray<T>> {

	void setImage( T image , int offsets[] );

	/**
	 * Sets the lower and upper thresholds relative to the current pixel value
	 */
	int checkPixel( int index );

	float scoreLower( int index );

	float scoreUpper( int index );

	void setThreshold( int index );
}