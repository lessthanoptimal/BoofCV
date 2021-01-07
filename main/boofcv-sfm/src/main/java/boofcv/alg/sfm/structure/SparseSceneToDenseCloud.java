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

package boofcv.alg.sfm.structure;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure;
import boofcv.alg.mvs.StereoPairGraph;
import boofcv.misc.LookUpImages;
import boofcv.struct.image.ImageGray;

import java.util.List;

/**
 * Takes in a known sparse scene that's in SBA format and converts it into a dense point cloud.
 *
 * @author Peter Abeles
 */
public class SparseSceneToDenseCloud<T extends ImageGray<T>> {


	MultiViewStereoFromKnownSceneStructure<T> alg;

	Class<T> grayType;


	public boolean process( SceneStructureMetric scene, List<String> viewIdx_to_ImageID,
							LookUpImages lookUpImages ) {

		return true;
	}

	public static StereoPairGraph stereoGraphFromScene(SceneStructureMetric scene) {
		return null;
	}
}
