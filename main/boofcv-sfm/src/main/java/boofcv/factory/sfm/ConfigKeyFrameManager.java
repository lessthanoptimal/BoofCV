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

package boofcv.factory.sfm;

import boofcv.alg.sfm.d3.structure.MaxGeoKeyFrameManager;
import boofcv.alg.sfm.d3.structure.TickTockKeyFrameManager;
import boofcv.struct.Configuration;

/**
 * Configuration for implementations of {@link boofcv.alg.sfm.d3.structure.VisOdomKeyFrameManager}
 *
 * @author Peter Abeles
 */
public class ConfigKeyFrameManager implements Configuration {

	/** Specifies the specific algorithm. */
	public Type type = Type.MAX_GEO;

	/** For MaxGeoKeyFrameManager only. When coverage drops below this number a new keyframe is created. 0.0 to 1.0 */
	public double geoMinCoverage = 0.4;

	/** For TickTockKeyFrameManager only. Period at which new keyframes are created. */
	public int tickPeriod = 2;

	public enum Type {
		/** {@link MaxGeoKeyFrameManager} */
		MAX_GEO,
		/** {@link TickTockKeyFrameManager} */
		TICK_TOCK
	}

	@Override
	public void checkValidity() {
		if (geoMinCoverage < 0 || geoMinCoverage > 1.0)
			throw new IllegalArgumentException("geoMinCoverage must be 0 to 1.0");
		if (tickPeriod <= 0)
			throw new IllegalArgumentException("tickPeriod must be greater than zero");
	}

	public ConfigKeyFrameManager setTo( ConfigKeyFrameManager src ) {
		this.type = src.type;
		this.geoMinCoverage = src.geoMinCoverage;
		this.tickPeriod = src.tickPeriod;
		return this;
	}
}
