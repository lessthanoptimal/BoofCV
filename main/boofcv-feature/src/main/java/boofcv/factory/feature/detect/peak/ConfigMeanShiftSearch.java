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

package boofcv.factory.feature.detect.peak;

import boofcv.struct.Configuration;

/**
 * Configuration for performing a mean-shift search for a local peak
 *
 * @author Peter Abeles
 */
public class ConfigMeanShiftSearch implements Configuration {

	/**
	 * Maximum number of mean shift iterations it will perform
	 */
	public int maxIterations = 15;

	/**
	 * When motion along both axises is less than this hold the search
	 */
	public double convergenceTol = 1e-3;

	/**
	 * If the region will have an "odd" region. Odd regions have a width that is an odd number, i.e. 2*radius+1
	 * and start sampling at -radius. Even regions have an even width (2*radius) and start sampling at -(radius-0.5).
	 * Odd is used if there's a single sharp peak, even is used if the peak is two pixels.
	 */
	public boolean odd = true;

	/**
	 * If mean-shift will only consider positive values in the intensity function
	 */
	public boolean positiveOnly = false;

	public ConfigMeanShiftSearch( int maxIterations, double convergenceTol ) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
	}

	public ConfigMeanShiftSearch() {}

	public ConfigMeanShiftSearch setTo( ConfigMeanShiftSearch src ) {
		this.maxIterations = src.maxIterations;
		this.convergenceTol = src.convergenceTol;
		this.odd = src.odd;
		this.positiveOnly = src.positiveOnly;
		return this;
	}

	@Override public void checkValidity() {}
}
