/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

@SuppressWarnings("unchecked")
public class TestWrapFHtoInterestPoint extends BoofStandardJUnit {

	@Nested
	public class Standard extends GeneralInterestPointDetectorChecks {
		public Standard() {
			var extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 1, 5, true), 1);
			var limitLevels = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
			var limitAll = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
			var detector = new FastHessianFeatureDetector(extractor,limitLevels,limitAll,
					1,9, 4,4, 6);
			detector.maxFeaturesPerScale = 150;
			var alg = new WrapFHtoInterestPoint(detector,GrayU8.class);
			configure(alg,false,true,GrayU8.class);
		}
	}
}
