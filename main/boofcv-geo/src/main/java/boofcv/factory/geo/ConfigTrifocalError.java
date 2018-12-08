/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.robust.DistanceTrifocalTransferSq;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for trifocal error computation
 *
 * @author Peter Abeles
 */
public class ConfigTrifocalError implements Configuration {

	/**
	 * How to compute the error
	 */
	public Model model = Model.REPROJECTION;

	/**
	 * If an iterative method was selected, this specifies the convergence criteria
	 */
	public ConfigConverge converge = new ConfigConverge(1e-8,1e-8,10);

	@Override
	public void checkValidity() {

	}

	public enum Model {
		/**
		 * Computes camera matrices and triangulates a point in 3D space. Reprojects the points and returns the error
		 *
		 * inlier units: Pixels
		 *
		 * @see boofcv.alg.geo.robust.DistanceTrifocalReprojectionSq
		 */
		REPROJECTION,
		/**
		 * Point transfer from view 0 into 2 and 0 into 3.
		 *
		 * inlier units: Pixels
		 *
		 * @see DistanceTrifocalTransferSq
		 */
		POINT_TRANSFER
	}
}
