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

package boofcv.abst.feature.describe;

import boofcv.struct.Configuration;

/**
 * Configuration for BRIEF descriptor.
 *
 * @see boofcv.alg.feature.describe.DescribePointBrief
 * @see boofcv.alg.feature.describe.DescribePointBriefSO
 *
 * @author Peter Abeles
 */
public class ConfigBrief implements Configuration {
	/**
	 * Region's radius.  Typical value is 16.
	 */
	public int radius = 16;
	/**
	 * Number of points sampled.  Typical value is 512.
	 */
	public int numPoints = 512;
	/**
	 * Amount of blur applied to the image before sampling.  Typical value is -1
	 */
	public double blurSigma = -1;
	/**
	 * Amount of blur applied to the image before sampling.  Typical value is 4
	 */
	public int blurRadius = 4;
	/**
	 * If true then a fixed sized descriptor is used.  If false then orientation and scale information
	 * is used, if available. By default this is true.
	 */
	public boolean fixed = true;

	public ConfigBrief(int radius, int numPoints, double blurSigma, int blurRadius, boolean fixed) {
		this.radius = radius;
		this.numPoints = numPoints;
		this.blurSigma = blurSigma;
		this.blurRadius = blurRadius;
		this.fixed = fixed;
	}

	public ConfigBrief( boolean fixed ) {
		this.fixed = fixed;
	}

	public ConfigBrief() {
	}

	@Override
	public void checkValidity() {
	}
}
