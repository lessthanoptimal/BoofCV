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

package boofcv.factory.geo;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Configuration for {@link boofcv.alg.geo.selfcalib.SelfCalibrationEssentialGuessAndCheck}.
 *
 * @author Peter Abeles
 */
public class ConfigSelfCalibEssentialGuess implements Configuration {
	/** Specifies the lower and upper limit for focal lengths it will consider. Relative to image max(width,height) */
	public double sampleMin = 0.3, sampleMax = 3;

	/** Number of focal length values it will sample for each camera. 200 is better but is slow */
	public int numberOfSamples = 50;

	/** if true the focus is assumed to be the same for the first two images */
	public boolean fixedFocus = true;

	@Override
	public void checkValidity() {
		checkTrue(sampleMin > 0, "Minimum focal length must be more than 0");
		checkTrue(sampleMin < sampleMax, "Minimum focal length must less than the maximum");
		BoofMiscOps.checkTrue(numberOfSamples >= 1);
	}

	public ConfigSelfCalibEssentialGuess setTo( ConfigSelfCalibEssentialGuess src ) {
		this.sampleMin = src.sampleMin;
		this.sampleMax = src.sampleMax;
		this.numberOfSamples = src.numberOfSamples;
		this.fixedFocus = src.fixedFocus;
		return this;
	}
}
