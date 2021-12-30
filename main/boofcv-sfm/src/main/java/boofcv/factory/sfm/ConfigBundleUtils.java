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

import boofcv.factory.geo.ConfigTriangulation;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.abst.geo.bundle.MetricBundleAdjustmentUtils}
 *
 * @author Peter Abeles
 */
public class ConfigBundleUtils implements Configuration {
	/** Configures convergence criteria for SBA */
	public final ConfigConverge converge = new ConfigConverge(1e-5, 1e-5, 30);

	/** Toggles on and off scaling parameters */
	public boolean scale = false;

	/** Should it use homogenous coordinates for points or 3D Cartesian? */
	public boolean homogenous = true;

	/** Optional second pass where outliers observations. Fraction specifies that the best X fraction are kept. */
	public double keepFraction = 1.0;

	/** Specifies which triangulation approach to use */
	public final ConfigTriangulation triangulation = ConfigTriangulation.GEOMETRIC();

	@Override public void checkValidity() {
		converge.checkValidity();
		triangulation.checkValidity();
		BoofMiscOps.checkFraction(keepFraction, "keepFraction");
	}

	public ConfigBundleUtils setTo( ConfigBundleUtils src ) {
		this.converge.setTo(src.converge);
		this.scale = src.scale;
		this.homogenous = src.homogenous;
		this.keepFraction = src.keepFraction;
		this.triangulation.setTo(src.triangulation);
		return this;
	}
}
