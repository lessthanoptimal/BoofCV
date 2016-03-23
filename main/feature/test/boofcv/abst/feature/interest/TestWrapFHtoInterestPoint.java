/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.interest;

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestWrapFHtoInterestPoint {

	// some reasonable input algorithms
	NonMaxSuppression extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, 1, 5, true));
	FastHessianFeatureDetector detector = new FastHessianFeatureDetector(extractor,150,
			1,9, 4,4, 6);

	@Test
	public void standard() {
		WrapFHtoInterestPoint alg = new WrapFHtoInterestPoint(detector);

		new GeneralInterestPointDetectorChecks(alg,false,true,GrayU8.class){}.performAllTests();
	}

}
