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

package boofcv.factory.structure;

import boofcv.abst.disparity.ConfigSpeckleFilter;
import boofcv.factory.disparity.ConfigDisparity;
import boofcv.factory.sfm.ConfigMultiviewStereo;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.structure.SparseSceneToDenseCloud}
 *
 * @author Peter Abeles
 */
public class ConfigSparseToDenseCloud implements Configuration {

	/** Specifies which stereo disparity and how to configure */
	public final ConfigDisparity disparity = new ConfigDisparity();

	/** Specifies which disparity smoothing approach to use */
	public final ConfigSpeckleFilter smoother = new ConfigSpeckleFilter();

	/** Specifies how to create the stereo graph */
	public final ConfigGenerateStereoGraph graph = new ConfigGenerateStereoGraph();

	/** Specifies how multiple stereo views are combined into a single stereo view */
	public final ConfigMultiviewStereo mvs = new ConfigMultiviewStereo();

	{
		// much faster than SGM and much better than regular block matching
		disparity.approach = ConfigDisparity.Approach.BLOCK_MATCH_5;
	}

	@Override public void checkValidity() {
		disparity.checkValidity();
		smoother.checkValidity();
		graph.checkValidity();
		mvs.checkValidity();
	}

	public ConfigSparseToDenseCloud setTo( ConfigSparseToDenseCloud src ) {
		this.disparity.setTo(src.disparity);
		this.smoother.setTo(src.smoother);
		this.graph.setTo(src.graph);
		this.mvs.setTo(src.mvs);
		return this;
	}
}
