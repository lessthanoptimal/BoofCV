/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.orientation;

import boofcv.struct.Configuration;

/**
 * Configuration for region orientations
 *
 * @author Peter Abeles
 */
public class ConfigOrientation2 implements Configuration {

	public Type type = Type.SLIDING;

	public ConfigSlidingIntegral slidingIntegral = new ConfigSlidingIntegral();
	public ConfigAverageIntegral averageIntegral = new ConfigAverageIntegral();

	// TODO support fixed scale variants
	// TODO have option for fixed orientation where you specify the value or orientation

	public void setTo( ConfigOrientation2 src ) {
		this.type = src.type;
		this.slidingIntegral.setTo(src.slidingIntegral);
		this.averageIntegral.setTo(src.averageIntegral);
	}

	@Override
	public void checkValidity() {
		slidingIntegral.checkValidity();
		averageIntegral.checkValidity();
	}

	public enum Type {
		AVERAGE,
		SLIDING,
		HISTOGRAM
	}
}
