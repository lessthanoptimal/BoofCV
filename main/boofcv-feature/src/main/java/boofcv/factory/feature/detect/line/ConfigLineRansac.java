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

import boofcv.abst.feature.detect.line.DetectLineSegmentsGridRansac;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link DetectLineSegmentsGridRansac}.
 *
 * @author Peter Abeles
 */
public class ConfigLineRansac implements Configuration {
	/** Size of the region considered. Try 40 and tune. */
	public int regionSize = 40;

	/** Threshold for determining which pixels belong to an edge or not. Try 30 and tune. */
	public double thresholdEdge = 30;

	/** Tolerance in angle for allowing two edgels to be paired up, in radians. Try 2.36 */
	public double thresholdAngle = 2.36;

	/** Should lines be connected and optimized. */
	public boolean connectLines = true;

	public ConfigLineRansac( int regionSize, double thresholdEdge, double thresholdAngle, boolean connectLines ) {
		this.regionSize = regionSize;
		this.thresholdEdge = thresholdEdge;
		this.thresholdAngle = thresholdAngle;
		this.connectLines = connectLines;
	}

	public ConfigLineRansac() {}

	public ConfigLineRansac setTo( ConfigLineRansac src ) {
		this.regionSize = src.regionSize;
		this.thresholdEdge = src.thresholdEdge;
		this.thresholdAngle = src.thresholdAngle;
		this.connectLines = src.connectLines;
		return this;
	}

	@Override public void checkValidity() {}
}
