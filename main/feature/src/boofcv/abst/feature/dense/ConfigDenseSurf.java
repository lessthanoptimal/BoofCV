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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.struct.Configuration;

/**
 * @author Peter Abeles
 */
public class ConfigDenseSurf implements Configuration {

	/**
	 * Standard configuration for SURF
	 */
	ConfigSurfDescribe surf;

	/**
	 * Space between the center of each descriptor region along the image's rows.
	 */
	int periodRows = 20;
	/**
	 * Space between the center of each descriptor region along the image's columns.
	 */
	int periodColumns = 20;
	/**
	 * The scale at which each feature is to be computed at
	 */
	double scale = 1;

	@Override
	public void checkValidity() {

	}
}
