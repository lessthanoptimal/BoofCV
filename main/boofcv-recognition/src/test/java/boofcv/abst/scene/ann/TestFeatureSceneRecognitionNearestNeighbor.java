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

package boofcv.abst.scene.ann;

import boofcv.abst.scene.FeatureSceneRecognition;
import boofcv.abst.scene.GenericFeatureSceneRecognitionChecks;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.struct.feature.TupleDesc_F32;

/**
 * @author Peter Abeles
 */
public class TestFeatureSceneRecognitionNearestNeighbor extends GenericFeatureSceneRecognitionChecks<TupleDesc_F32> {
	@Override public FeatureSceneRecognition<TupleDesc_F32> createAlg() {
		return FactorySceneRecognition.createSceneNearestNeighbor(null, ()->new TupleDesc_F32(64));
	}

	@Override public TupleDesc_F32 createDescriptor( int seed ) {
		var desc = new TupleDesc_F32(64);
		for (int i = 0; i < 64; i++) {
			desc.data[i] = seed+i;
		}
		return desc;
	}
}
