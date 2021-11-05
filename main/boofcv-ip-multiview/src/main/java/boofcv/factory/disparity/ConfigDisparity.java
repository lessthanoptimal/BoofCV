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

package boofcv.factory.disparity;

import boofcv.struct.Configuration;

/**
 * Generic configuration for any dense stereo disparity algorithm.
 *
 * @author Peter Abeles
 */
public class ConfigDisparity implements Configuration {

	/** Specifies which approach to use */
	public Approach approach = Approach.BLOCK_MATCH;

	/** Configuration for Block Matching approach */
	public final ConfigDisparityBM approachBM = new ConfigDisparityBM();
	/** Configuration for Block Matching Best-5 approach */
	public final ConfigDisparityBMBest5 approachBM5 = new ConfigDisparityBMBest5();
	/** Configuration for Semi Global Matching (SGM) appraoch */
	public final ConfigDisparitySGM approachSGM = new ConfigDisparitySGM();

	@Override public void checkValidity() {
		approachBM.checkValidity();
		approachBM5.checkValidity();
		approachSGM.checkValidity();
	}

	public ConfigDisparity setTo( ConfigDisparity src ) {
		this.approach = src.approach;
		this.approachBM.setTo(src.approachBM);
		this.approachBM5.setTo(src.approachBM5);
		this.approachSGM.setTo(src.approachSGM);
		return this;
	}

	/**
	 * List of avaliable approaches
	 */
	public enum Approach {
		BLOCK_MATCH,
		BLOCK_MATCH_5,
		SGM
	}
}
