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

package boofcv.abst.scene;

import boofcv.factory.feature.describe.ConfigConvertTupleDesc;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.scene.FactorySceneRecognition;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestWrapFeatureToSceneRecognition extends BoofStandardJUnit {
	/**
	 * Default settings with a few minor tweaks for speed
	 */
	@Nested class MostlyDefault extends GenericSceneRecognitionChecks<GrayU8> {
		public MostlyDefault() {
			super(ImageType.SB_U8);
		}

		@Override protected SceneRecognition<GrayU8> createAlg() {
			var config = new ConfigFeatureToSceneRecognition();
			config.recognizeNister2006.tree.branchFactor = 3;
			config.recognizeNister2006.tree.maximumLevel = 3;
			config.features.detectFastHessian.extract.radius = 6;
			return FactorySceneRecognition.createFeatureToScene(config, imageType);
		}
	}

	/**
	 * Use binary features and see if it still works
	 */
	@Nested class Binary extends GenericSceneRecognitionChecks<GrayU8> {
		public Binary() {
			super(ImageType.SB_U8);
		}

		@Override protected SceneRecognition<GrayU8> createAlg() {
			var config = new ConfigFeatureToSceneRecognition();
			config.recognizeNister2006.tree.branchFactor = 5;
			config.recognizeNister2006.tree.maximumLevel = 3;
			config.features.typeDescribe = ConfigDescribeRegion.Type.BRIEF;
			config.features.describeBrief.fixed = false;
			config.features.detectFastHessian.extract.radius = 12;
			config.features.convertDescriptor.outputData = ConfigConvertTupleDesc.DataType.NATIVE;
			return FactorySceneRecognition.createFeatureToScene(config, imageType);
		}
	}
}
