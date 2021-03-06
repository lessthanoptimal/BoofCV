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

import boofcv.struct.Configuration;

/**
 * Cofiguration for {@link boofcv.alg.sfm.structure.GeneratePairwiseImageGraph}.
 *
 * @author Peter Abeles
 */
public class ConfigGeneratePairwiseImageGraph implements Configuration {
	/** Configuration for how quality of 3D information between two views is scored */
	public final ConfigEpipolarScore3D score = new ConfigEpipolarScore3D();

	@Override public void checkValidity() {
		score.checkValidity();
	}

	public void setTo( ConfigGeneratePairwiseImageGraph src ) {
		this.score.setTo(src.score);
	}
}
