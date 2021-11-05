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

import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;

/**
 * Configuration for {@link boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure}.
 *
 * @author Peter Abeles
 */
public class ConfigMultiviewStereo implements Configuration {
	/** {@link boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure#minimumQuality3D} */
	public double minimumQuality3D = 0.05;

	/** {@link boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure#maximumCenterOverlap} */
	public double maximumCenterOverlap = 0.80;

	/** {@link boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure#maxCombinePairs} */
	public int maxCombinePairs = 10;

	@Override public void checkValidity() {
		BoofMiscOps.checkTrue(minimumQuality3D >= 0.0);
		BoofMiscOps.checkTrue(maximumCenterOverlap >= 0.0 && maximumCenterOverlap <= 1.0);
	}

	public ConfigMultiviewStereo setTo( ConfigMultiviewStereo src ) {
		this.minimumQuality3D = src.minimumQuality3D;
		this.maximumCenterOverlap = src.maximumCenterOverlap;
		this.maxCombinePairs = src.maxCombinePairs;
		return this;
	}
}
