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

package boofcv.alg.feature.detect.interest;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEasyGeneralFeatureDetector {

	int width = 20;
	int height = 30;

	GrayU8 image = new GrayU8(width,height);

	@Test
	public void requiresGradient() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(true, false);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		assertTrue(alg.derivX != null);
		assertTrue(alg.derivY != null);
		assertTrue(alg.derivXX == null);
		assertTrue(alg.derivXY == null);
		assertTrue(alg.derivYY == null);

		alg.detect(image,null);

		assertTrue(detector.excludeIsNull);

	}

	@Test
	public void requiresHessian() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(false, true);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		// It uses the gradient to compute the hessian faster
		assertTrue(alg.derivX != null);
		assertTrue(alg.derivY != null);
		assertTrue(alg.derivXX != null);
		assertTrue(alg.derivXY != null);
		assertTrue(alg.derivYY != null);

		alg.detect(image,null);

		assertTrue(detector.excludeIsNull);
	}

	@Test
	public void checkExclude() {
		Helper<GrayU8,GrayS16> detector = new Helper<>(true, true);
		EasyGeneralFeatureDetector<GrayU8,GrayS16> alg =
				new EasyGeneralFeatureDetector<>(detector, GrayU8.class, GrayS16.class);

		alg.detect(image,new QueueCorner(10));

		assertFalse(detector.excludeIsNull);
	}

	private static class Helper<I extends ImageGray, D extends ImageGray>
			extends GeneralFeatureDetector<I,D> {

		boolean gradient;
		boolean hessian;
		boolean excludeIsNull;

		private Helper(boolean gradient, boolean hessian) {
			this.gradient = gradient;
			this.hessian = hessian;
		}

		@Override
		public void setExcludeMaximum(QueueCorner exclude) {
			excludeIsNull = exclude==null;
		}

		@Override
		public void process(I image, D derivX, D derivY, D derivXX, D derivYY, D derivXY) {
			if( gradient ) {
				assertTrue(derivX != null );
				assertTrue(derivY != null );
			}
			if( hessian ) {
				assertTrue(derivXX != null );
				assertTrue(derivYY != null );
				assertTrue(derivXY != null );
			}
		}

		@Override
		public QueueCorner getMaximums() {
			return foundMaximum;
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
