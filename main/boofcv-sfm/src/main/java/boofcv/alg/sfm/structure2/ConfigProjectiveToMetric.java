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

import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Configuration for {@link ProjectiveToMetricReconstruction}
 *
 * @author Peter Abeles
 */
public class ConfigProjectiveToMetric implements Configuration {
	/** Known aspect ratio of the pixels. Pixels are 99.999% square these days so this should be 1.0 */
	public double aspectRatio = 1.0;
	/** Converge tolerance for SBA */
	public ConfigConverge sbaConverge = new ConfigConverge(1e-8, 1e-8, 50);

	@Override public void checkValidity() {
		assertBoof(aspectRatio > 0.0);
		sbaConverge.checkValidity();
	}

	public void setTo( ConfigProjectiveToMetric src ) {
		this.aspectRatio = src.aspectRatio;
		this.sbaConverge.setTo(src.sbaConverge);
	}
}
