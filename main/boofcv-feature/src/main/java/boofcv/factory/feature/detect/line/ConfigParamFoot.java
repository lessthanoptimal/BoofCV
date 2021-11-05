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

package boofcv.factory.feature.detect.line;

import boofcv.struct.Configuration;

/**
 * parameters for {@link boofcv.alg.feature.detect.line.HoughParametersFootOfNorm}
 *
 * @author Peter Abeles
 */
public class ConfigParamFoot implements Configuration {
	/**
	 * Lines which are this close to the origin of the transformed image are ignored. Try 5.
	 */
	public int minDistanceFromOrigin = 5;

	public ConfigParamFoot setTo( ConfigParamFoot src ) {
		this.minDistanceFromOrigin = src.minDistanceFromOrigin;
		return this;
	}

	@Override public void checkValidity() {}
}
