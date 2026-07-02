/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.struct.Configuration;
import lombok.Setter;
import lombok.experimental.Accessors;

/// Specifies constraints when calibrating a pinhole/brown camera model
@Setter
@Accessors(chain = true, fluent = true)
public class ConfigCalibratePinhole implements Configuration {
	/// If true it will assume the skew is zero
	public boolean zeroSkew = true;
	/// Number of radial distortion terms it needs to consider.
	public int numRadial = 3;
	/// If true it will include tangential distortional terms
	public boolean tangential = false;
	/// Aspect ratio, if > 0 then `fx = ratio*fy``
	public double aspectRatio = -1;

	@Override public void checkValidity() {}

	public ConfigCalibratePinhole setTo( ConfigCalibratePinhole src ) {
		this.zeroSkew = src.zeroSkew;
		this.numRadial = src.numRadial;
		this.tangential = src.tangential;
		this.aspectRatio = src.aspectRatio;
		return this;
	}
}
