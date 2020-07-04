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

package boofcv.alg.sfm.structure2;

import boofcv.factory.geo.ConfigBundleAdjustment;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.ConfigTrifocal;
import boofcv.factory.geo.ConfigTrifocalError;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link ProjectiveReconstructionFromPairwiseGraph}
 *
 * @author Peter Abeles
 */
public class ConfigProjectiveReconstruction implements Configuration {
	/** Configurations for running RANSAC */
	public ConfigRansac ransac = new ConfigRansac(500,1);
	/** How trifocal tensor error is computed inside of RANSAC */
	public ConfigTrifocal ransacTrifocal = new ConfigTrifocal();
	/** How trifocal tensor error is computed inside of RANSAC */
	public ConfigTrifocalError ransacError = new ConfigTrifocalError();
	/** Optimization parameters for SBA */
	public ConfigBundleAdjustment sba = new ConfigBundleAdjustment();
	/** Converge tolerance for SBA */
	public ConfigConverge sbaConverge = new ConfigConverge(1e-8,1e-8,50);
	/** Toggles scaling inputs for SBA */
	public boolean sbaScale = false;

	@Override
	public void checkValidity() {
		ransac.checkValidity();
		ransacTrifocal.checkValidity();
		ransacError.checkValidity();
		sbaConverge.checkValidity();
	}

	public void setTo(ConfigProjectiveReconstruction config) {
		ransac.setTo(config.ransac);
		ransacTrifocal.setTo(config.ransacTrifocal);
		ransacError.setTo(config.ransacError);
		sba.setTo(config.sba);
		sbaConverge.setTo(config.sbaConverge);
		sbaScale = config.sbaScale;
	}
}
