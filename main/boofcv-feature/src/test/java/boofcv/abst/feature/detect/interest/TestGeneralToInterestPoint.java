/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGeneralToInterestPoint extends BoofStandardJUnit {

	GrayF32 input = new GrayF32(10,20);

	/**
	 * Several basic functionality tests
	 */
	@Test void various() {
		Helper detector = new Helper();
		detector.maximum = true;
		GeneralToInterestPoint<GrayF32,GrayF32> alg =
				new GeneralToInterestPoint<>(detector, 2.5);

		alg.detect(input);
		assertEquals(1, alg.getNumberOfSets());

		assertEquals(6,alg.getNumberOfFeatures());
		for( int i = 0; i < alg.getNumberOfFeatures(); i++) {
			assertEquals(2.5, alg.getRadius(i),1e-8);
			assertEquals(0, alg.getOrientation(i),1e-8);
			assertEquals(0,alg.getSet(i));
		}

		assertEquals(1, detector.calledProcess);
		assertEquals(6, detector.getMaximums().size);
	}

	/**
	 * Makes sure both minimums and maximums are added
	 */
	@Test void checkMinimumsMaximums() {
		Helper detector = new Helper();

		// both turned off
		var alg = new GeneralToInterestPoint<>(detector, 2.5);
		alg.detect(input);
		assertEquals(0, alg.getNumberOfSets());
		assertEquals(0, alg.getNumberOfFeatures());

		// just minimums
		detector.minimum = true;
		alg = new GeneralToInterestPoint<>(detector, 2.5);
		alg.detect(input);
		assertEquals(1, alg.getNumberOfSets());
		assertEquals(5, alg.getNumberOfFeatures());
		for (int i = 0; i < alg.getNumberOfFeatures(); i++) {
			assertEquals(0,alg.getSet(i));
		}

		// both minimums and maximums
		detector.maximum = true;
		alg = new GeneralToInterestPoint<>(detector, 2.5);
		alg.detect(input);
		assertEquals(2, alg.getNumberOfSets());
		assertEquals(11,alg.getNumberOfFeatures());
		for (int i = 0; i < 6; i++) {
			assertEquals(0,alg.getSet(i));
		}
		for (int i = 6; i < alg.getNumberOfFeatures(); i++) {
			assertEquals(1,alg.getSet(i));
		}

		// just maximums
		detector.minimum = false;
		alg = new GeneralToInterestPoint<>(detector, 2.5);
		alg.detect(input);
		assertEquals(1, alg.getNumberOfSets());
		assertEquals(6,alg.getNumberOfFeatures());
		for (int i = 0; i < alg.getNumberOfFeatures(); i++) {
			assertEquals(0,alg.getSet(i));
		}
	}

	public static class Helper extends GeneralFeatureDetector<GrayF32,GrayF32> {

		public int calledProcess = 0;
		public boolean minimum = false;
		public boolean maximum = false;

		@Override public void process(GrayF32 image,
									  GrayF32 derivX, GrayF32 derivY,
									  GrayF32 derivXX, GrayF32 derivYY, GrayF32 derivXY) {
			calledProcess++;
		}

		@Override public QueueCorner getMinimums() {
			QueueCorner ret = new QueueCorner();
			for( int i = 0; i < 5; i++ )
				ret.append(1,2);
			return ret;
		}

		@Override public QueueCorner getMaximums() {
			QueueCorner ret = new QueueCorner();
			for( int i = 0; i < 6; i++ )
				ret.append(1,2);
			return ret;
		}

		@Override public boolean isDetectMinimums() { return minimum; }
		@Override public boolean isDetectMaximums() { return maximum; }
		@Override public boolean getRequiresGradient() { return false; }
		@Override public boolean getRequiresHessian() { return false; }
		@Override public Class<GrayF32> getImageType() { return GrayF32.class; }
		@Override public Class<GrayF32> getDerivType() { return GrayF32.class; }
	}
}
