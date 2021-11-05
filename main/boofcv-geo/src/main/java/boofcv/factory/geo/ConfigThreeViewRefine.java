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

package boofcv.factory.geo;

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for
 *
 * @author Peter Abeles
 */
public class ConfigThreeViewRefine implements Configuration {
	/**
	 * If true pixel coordinates will be normalized using {@link LowLevelMultiViewOps}. Set to false
	 * only if pixels have already been scaled.
	 */
	public boolean normalizePixels = true;

	/**
	 * Convergence criteria
	 */
	public ConfigConverge converge = new ConfigConverge(1e-8, 1e-8, 100);

	/**
	 * Specifies which algorithm to apply
	 */
	public Algorithm which = Algorithm.GEOMETRIC;

	public ConfigThreeViewRefine setTo( ConfigThreeViewRefine src ) {
		this.normalizePixels = src.normalizePixels;
		this.converge.setTo(src.converge);
		this.which = src.which;
		return this;
	}

	@Override
	public void checkValidity() {
		converge.checkValidity();
	}

	public enum Algorithm {
		/**
		 * Minimizes geometric error. This is the same as the Gold Standard algorithm in [1]
		 *
		 * @see boofcv.alg.geo.trifocal.RefineThreeViewProjectiveGeometric
		 */
		GEOMETRIC
	}
}
