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

package boofcv.alg.sfm;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneStructure;
import boofcv.alg.sfm.structure.PairwiseImageGraph;
import org.ddogleg.struct.Stoppable;

/**
 * Provides an initial estimate of a scenes structure for {@link boofcv.abst.geo.bundle.BundleAdjustment}.
 *
 * @author Peter Abeles
 */
public interface EstimateSceneStructure<Structure extends SceneStructure> extends Stoppable {


	/**
	 * Estimte the 3D structure of each point and view location given the
	 * graph connecting each view
	 * @param graph Describes relationship of each feature between views and epipolar geometry
	 * @return true if successful or false if it failed
	 */
	boolean estimate( PairwiseImageGraph graph );

	/**
	 * Returns the scene structure. Camera models will not be specified since that requires additional information
	 * than is already available
	 *
	 * @return scene
	 */
	Structure getSceneStructure();

	/**
	 * Observations from each view
	 * @return observations
	 */
	SceneObservations getObservations();

	/**
	 * Forgets all added views and cameras
	 */
	void reset();
}
