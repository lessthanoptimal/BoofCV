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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.Configuration;

/**
 * Configuration for all single point features, e.g. corners or single scale blobs.
 *
 * @author Peter Abeles
 */
public class ConfigPointDetector implements Configuration {
	/**
	 * Which detector is used.
	 */
	public PointDetectorTypes type = PointDetectorTypes.SHI_TOMASI;
	/**
	 * If a scale-invariant descriptor is used this specified what radius/scale the returned point feature will specify.
	 */
	public double scaleRadius = 10.0;

	/**
	 * Configurations common to all point detectors
	 */
	public final ConfigGeneralDetector general = new ConfigGeneralDetector();
	/**
	 * Configuration only used with Harris
	 */
	public final ConfigHarrisCorner harris = new ConfigHarrisCorner();
	/**
	 * Configuration only used with Shi-Tomasi
	 */
	public final ConfigShiTomasi shiTomasi = new ConfigShiTomasi();
	/**
	 * Configuration only used with FAST
	 */
	public final ConfigFastCorner fast = new ConfigFastCorner();

	@Override
	public void checkValidity() {
		if (scaleRadius <= 0)
			throw new IllegalArgumentException("Radius must be a positive number");
	}

	public ConfigPointDetector setTo( ConfigPointDetector src ) {
		this.type = src.type;
		this.scaleRadius = src.scaleRadius;
		this.general.setTo(src.general);
		this.harris.setTo(src.harris);
		this.shiTomasi.setTo(src.shiTomasi);
		this.fast.setTo(src.fast);
		return this;
	}

	public ConfigPointDetector copy() {
		return new ConfigPointDetector().setTo(this);
	}
}
