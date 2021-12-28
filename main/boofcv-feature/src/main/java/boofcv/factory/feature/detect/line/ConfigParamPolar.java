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
 * parameters for {@link boofcv.alg.feature.detect.line.HoughParametersPolar}
 *
 * @author Peter Abeles
 */
public class ConfigParamPolar implements Configuration {
	/**
	 * Resolution of line range in pixels. Try 2
	 */
	public double resolutionRange = 2;
	/**
	 * Number of bins along angle axis. Resolution = 180/binAngle (degrees)
	 */
	public int numBinsAngle = 180;

	public ConfigParamPolar( double resolutionRange, int numBinsAngle ) {
		this.resolutionRange = resolutionRange;
		this.numBinsAngle = numBinsAngle;
	}

	public ConfigParamPolar() {}

	public ConfigParamPolar setTo( ConfigParamPolar src ) {
		this.resolutionRange = src.resolutionRange;
		this.numBinsAngle = src.numBinsAngle;
		return this;
	}

	@Override public void checkValidity() {}
}
