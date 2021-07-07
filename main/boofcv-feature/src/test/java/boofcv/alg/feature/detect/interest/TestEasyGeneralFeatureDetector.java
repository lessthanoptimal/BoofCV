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

package boofcv.alg.feature.detect.interest;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEasyGeneralFeatureDetector extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	GrayU8 image = new GrayU8(width,height);

	@Test void requiresGradient() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(true, false);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		assertNotNull(alg.derivX);
		assertNotNull(alg.derivY);
		assertNull(alg.derivXX);
		assertNull(alg.derivXY);
		assertNull(alg.derivYY);

		alg.detect(image,null);

		assertTrue(detector.excludeIsNull);

	}

	@Test void requiresHessian() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(false, true);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		// It uses the gradient to compute the hessian faster
		assertNotNull(alg.derivX);
		assertNotNull(alg.derivY);
		assertNotNull(alg.derivXX);
		assertNotNull(alg.derivXY);
		assertNotNull(alg.derivYY);

		alg.detect(image,null);

		assertTrue(detector.excludeIsNull);
	}

	@Test void checkExclude() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(true, true);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		alg.detect(image,new QueueCorner(10));

		assertFalse(detector.excludeIsNull);
	}

	private static class Helper<I extends ImageGray<I>, D extends ImageGray<D>>
			extends GeneralFeatureDetector<I,D> {

		boolean gradient;
		boolean hessian;
		boolean excludeIsNull;

		private Helper(boolean gradient, boolean hessian) {
			this.gradient = gradient;
			this.hessian = hessian;
		}

		@Override
		public void setExclude(QueueCorner exclude) {
			excludeIsNull = exclude==null;
		}

		@Override
		public void process(I image, D derivX, D derivY, D derivXX, D derivYY, D derivXY) {
			if( gradient ) {
				assertNotNull(derivX);
				assertNotNull(derivY);
			}
			if( hessian ) {
				assertNotNull(derivXX);
				assertNotNull(derivYY);
				assertNotNull(derivXY);
			}
		}

		@Override
		public QueueCorner getMaximums() {
			return maximums;
		}

		@Override
		public boolean getRequiresGradient() {
			return gradient;
		}

		@Override
		public boolean getRequiresHessian() {
			return hessian;
		}
	}
}
