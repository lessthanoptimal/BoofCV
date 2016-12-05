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

package boofcv.abst.tracker;

import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.tracker.tld.TldParameters;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.tracker.tld.TldTracker} as wrapped inside of {@link Tld_to_TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class ConfigTld implements Configuration {

	/**
	 * Configuration parameters
	 */
	public TldParameters parameters = new TldParameters();

	/**
	 * Specifies the type of interpolation.  More stable with bilinear, but nearest-neighbor can be
	 * used to maximize speed.
	 */
	public InterpolationType interpolate = InterpolationType.BILINEAR;

	@Override
	public void checkValidity() {

	}

	public ConfigTld( boolean stable ) {
		if( !stable ) {
			interpolate = InterpolationType.NEAREST_NEIGHBOR;
			parameters.scaleSpread = 0; // turns off a good chunk of scale invariance
			parameters.maximumCascadeConsider = 25;
			parameters.numNegativeFerns = 400;
		}
	}

	public ConfigTld() {
	}
}
