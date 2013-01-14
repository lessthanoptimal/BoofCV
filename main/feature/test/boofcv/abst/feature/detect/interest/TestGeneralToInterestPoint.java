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

package boofcv.abst.feature.detect.interest;

import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestGeneralToInterestPoint {

	ImageFloat32 input = new ImageFloat32(10,20);

	/**
	 * Several basic functionality tests
	 */
	public void various() {
		Helper detector = new Helper();
		detector.maximum = true;
		GeneralToInterestPoint<ImageFloat32,ImageFloat32> alg =
				new GeneralToInterestPoint<ImageFloat32,ImageFloat32>(detector,2.5, ImageFloat32.class,ImageFloat32.class);

		alg.detect(input);

		assertEquals(6,alg.getNumberOfFeatures());
		for( int i = 0; i < alg.getNumberOfFeatures(); i++) {
			assertEquals(2.5, alg.getScale(i),1e-8);
			assertEquals(0, alg.getOrientation(i),1e-8);
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
		GeneralToInterestPoint<ImageFloat32,ImageFloat32> alg =
				new GeneralToInterestPoint<ImageFloat32,ImageFloat32>(detector,2.5, ImageFloat32.class,ImageFloat32.class);

		// both turned off
		alg.detect(input);
		assertEquals(0,alg.getNumberOfFeatures());

		// just minimums
		detector.minimum = true;
		alg.detect(input);
		assertEquals(5,alg.getNumberOfFeatures());

		// both minimums and maximums
		detector.maximum = true;
		alg.detect(input);
		assertEquals(11,alg.getNumberOfFeatures());

		// just maximums
		detector.minimum = false;
		alg.detect(input);
		assertEquals(6,alg.getNumberOfFeatures());
	}

	public static class Helper extends GeneralFeatureDetector<ImageFloat32,ImageFloat32> {

		public int calledProcess = 0;
		public boolean minimum = false;
		public boolean maximum = false;


		@Override
		public void process(ImageFloat32 image,
							ImageFloat32 derivX, ImageFloat32 derivY,
							ImageFloat32 derivXX, ImageFloat32 derivYY, ImageFloat32 derivXY) {
			calledProcess++;
		}

		@Override
		public QueueCorner getMinimums() {
			QueueCorner ret = new QueueCorner();
			for( int i = 0; i < 5; i++ )
				ret.add(1,2);
			return ret;
		}

		@Override
		public QueueCorner getMaximums() {
			QueueCorner ret = new QueueCorner();
			for( int i = 0; i < 6; i++ )
				ret.add(1,2);
			return ret;
		}

		@Override
		public boolean isDetectMinimums() {
			return minimum;
		}

		@Override
		public boolean isDetectMaximums() {
			return maximum;
		}

		@Override
		public boolean getRequiresGradient() {
			return false;
		}

		@Override
		public boolean getRequiresHessian() {
			return false;
		}
	}
}
