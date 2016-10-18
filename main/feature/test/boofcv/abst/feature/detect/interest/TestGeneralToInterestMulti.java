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

package boofcv.abst.feature.detect.interest;

import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static boofcv.abst.feature.detect.interest.TestGeneralToInterestPoint.Helper;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGeneralToInterestMulti {

	GrayF32 input = new GrayF32(10,20);

	/**
	 * Several basic functionality tests
	 */
	public void various() {
		Helper detector = new Helper();
		detector.maximum = true;
		GeneralToInterestMulti<GrayF32,GrayF32> alg =
				new GeneralToInterestMulti<>(detector, 2.5, GrayF32.class, GrayF32.class);

		alg.detect(input);

		assertEquals(1,alg.getNumberOfSets());
		FoundPointSO set = alg.getFeatureSet(0);
		assertEquals(6,set.getNumberOfFeatures());
		for( int i = 0; i < set.getNumberOfFeatures(); i++ ) {
			assertEquals(2.5, set.getRadius(i),1e-8);
			assertEquals(0, set.getOrientation(i),1e-8);
		}

		assertEquals(1, detector.calledProcess);
		assertEquals(6, detector.getMaximums().size);
	}

	/**
	 * Makes sure both minimums and maximums are added
	 */
	@Test
	public void checkMinimumsMaximums() {
		Helper detector = new Helper();
		detector.minimum = true;
		GeneralToInterestMulti<GrayF32,GrayF32> alg;

		// just minimums
		alg = new GeneralToInterestMulti<>(detector, 2.5, GrayF32.class, GrayF32.class);
		assertEquals(1,alg.getNumberOfSets());
		alg.detect(input);
		assertEquals(5,alg.getFeatureSet(0).getNumberOfFeatures());

		// both minimums and maximums
		detector.maximum = true;
		alg = new GeneralToInterestMulti<>(detector, 2.5, GrayF32.class, GrayF32.class);
		assertEquals(2,alg.getNumberOfSets());
		alg.detect(input);
		assertEquals(5,alg.getFeatureSet(0).getNumberOfFeatures());
		assertEquals(6,alg.getFeatureSet(1).getNumberOfFeatures());

		// just maximums
		detector.minimum = false;
		alg = new GeneralToInterestMulti<>(detector, 2.5, GrayF32.class, GrayF32.class);
		assertEquals(1, alg.getNumberOfSets());
		alg.detect(input);
		assertEquals(6,alg.getFeatureSet(0).getNumberOfFeatures());
	}
}
