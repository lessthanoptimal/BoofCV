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

package boofcv.factory.geo;

import boofcv.struct.Configuration;

/**
 * Configuration parameters for estimating a homography
 *
 * @author Peter Abeles
 */
public class ConfigHomography implements Configuration {

	/**
	 * If the input is in pixel coordinates then this should be true.  If in normalized image coordinates
	 * then it can be false.
	 */
	public boolean normalize = true;

	public ConfigHomography(boolean normalize) {
		this.normalize = normalize;
	}

	public ConfigHomography() {
	}

	@Override
	public void checkValidity() {

	}
}
