/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detdesc;

import boofcv.alg.feature.describe.DescribePointSift;
import boofcv.alg.feature.detect.interest.SiftDetector;
import boofcv.alg.feature.detect.interest.SiftImageScaleSpace;
import boofcv.alg.feature.orientation.OrientationHistogramSift;
import boofcv.factory.feature.detect.interest.FactoryInterestPointAlgs;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * High level functionality is tested by {@link boofcv.abst.feature.detdesc.TestWrapDetectDescribeSift}.
 * Only very basic accessor tests are here.
 *
 * @author Peter Abeles
 */
public class TestDetectDescribeSift {

	DetectDescribeSift alg;

	public TestDetectDescribeSift() {
		SiftImageScaleSpace ss = new SiftImageScaleSpace(1.6f, 5,4,false);
		SiftDetector detector = FactoryInterestPointAlgs.siftDetector(null);
		OrientationHistogramSift orientation = new OrientationHistogramSift(32,2.5,1.5);
		DescribePointSift describe = new DescribePointSift(4,8,8,0.5, 2.5);

		alg = new DetectDescribeSift(ss,detector,orientation,describe);
	}

	@Test
	public void getDescriptorLength() {
		assertTrue(alg.describe.getDescriptorLength() == alg.getDescriptorLength());
	}

	@Test
	public void getFeatures() {
		assertTrue(alg.features == alg.getFeatures());
	}

	@Test
	public void getFeatureScales() {
		assertTrue(alg.featureScales == alg.getFeatureScales());
	}

	@Test
	public void getFeatureAngles() {
		assertTrue(alg.featureAngles == alg.getFeatureAngles());
	}

	@Test
	public void getLocation() {
		assertTrue(alg.location == alg.getLocation());
	}
}
