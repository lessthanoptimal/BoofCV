/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.calib.CameraPinholeRadial;

/**
 * Configuration parameters for estimating an essential matrix robustly.
 *
 * @author Peter Abeles
 */
public class ConfigEssential implements Configuration {

	/**
	 * Which algorithm should it use.  Only use essential matrix ones.
	 */
	public EnumEssential which = EnumEssential.NISTER_5;

	/**
	 * How many points should be used to resolve ambiguity in the solutions?
	 */
	public int numResolve = 2;

	/**
	 * Intrinsic camera parameters.  Used to compute error in pixels.
	 */
	public CameraPinholeRadial intrinsic;

	public ConfigEssential(CameraPinholeRadial intrinsic) {
		this.intrinsic = intrinsic;
	}

	public ConfigEssential() {
	}

	@Override
	public void checkValidity() {

	}
}
