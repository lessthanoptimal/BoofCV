/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.detect.selector.FeatureSelectLimitIntensity;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.selector.ConfigSelectLimit;
import boofcv.factory.feature.detect.selector.FactorySelectLimit;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I16;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestWrapFHtoInterestPoint extends BoofStandardJUnit {

	@Nested
	public class Standard extends GeneralInterestPointDetectorChecks {
		public Standard() {
			NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 1, 5, true));
			FeatureSelectLimitIntensity<Point2D_I16> limitLevels = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
			FeatureSelectLimitIntensity<ScalePoint> limitAll = FactorySelectLimit.intensity(ConfigSelectLimit.selectBestN());
			var detector = new FastHessianFeatureDetector(extractor,limitLevels,limitAll,
					1,9, 4,4, 6);
			detector.maxFeaturesPerScale = 150;
			var alg = new WrapFHtoInterestPoint(detector,GrayU8.class);
			configure(alg,false,true,GrayU8.class);
		}
	}
}
