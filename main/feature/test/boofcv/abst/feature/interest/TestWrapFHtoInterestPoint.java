/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.FeatureExtractor;
import boofcv.abst.feature.detect.interest.WrapFHtoInterestPoint;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestWrapFHtoInterestPoint {

	// some reasonable input algorithms
	FeatureExtractor extractor = FactoryFeatureExtractor.nonmax(2, 1, 5, true);
	FastHessianFeatureDetector detector = new FastHessianFeatureDetector(extractor,150,
			1,9, 4,4);

	OrientationIntegral orientation = FactoryOrientationAlgs.image_ii(5,1,4,0, ImageSInt32.class);

	@Test
	public void NO_Orientation() {
		WrapFHtoInterestPoint alg = new WrapFHtoInterestPoint(detector);

		new GeneralInterestPointDetectorChecks(alg,false,true,ImageUInt8.class).performAllTests();
	}

	@Test
	public void With_Orientation() {
		WrapFHtoInterestPoint alg = new WrapFHtoInterestPoint(detector,orientation);

		new GeneralInterestPointDetectorChecks(alg,true,true,ImageUInt8.class).performAllTests();
	}
}
