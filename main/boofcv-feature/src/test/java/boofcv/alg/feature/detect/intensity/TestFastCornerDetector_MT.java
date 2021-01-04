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

package boofcv.alg.feature.detect.intensity;

import boofcv.BoofTesting;
import boofcv.alg.feature.detect.intensity.impl.ImplFastCorner9_U8;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestFastCornerDetector_MT extends BoofStandardJUnit {

	protected int width = 20;
	protected int height = 21;

	protected GrayU8 image = new GrayU8(width, height);

	@Test void compareToSingle() {
		ImageMiscOps.fillUniform(image, rand, 0, 15);
		FastCornerDetector<GrayU8> single = new FastCornerDetector<>(new ImplFastCorner9_U8(5));
		FastCornerDetector<GrayU8> concurrent = new FastCornerDetector_MT<>(new ImplFastCorner9_U8(5));

		single.process(image);
		concurrent.process(image);

		compare(single.candidatesLow, concurrent.candidatesLow);
		compare(single.candidatesHigh, concurrent.candidatesHigh);
	}

	@Test void compareToSingle_intensity() {
		ImageMiscOps.fillUniform(image, rand, 0, 15);
		FastCornerDetector<GrayU8> single = new FastCornerDetector<>(new ImplFastCorner9_U8(5));
		FastCornerDetector<GrayU8> concurrent = new FastCornerDetector_MT<>(new ImplFastCorner9_U8(5));

		GrayF32 expected = new GrayF32(width, height);
		GrayF32 found = new GrayF32(width, height);

		single.process(image, expected);
		concurrent.process(image, found);

		compare(single.candidatesLow, concurrent.candidatesLow);
		compare(single.candidatesHigh, concurrent.candidatesHigh);

		BoofTesting.assertEquals(expected, found, 1e-8);
	}

	void compare( ListIntPoint2D a , ListIntPoint2D b ) {
		assertEquals(a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			assertEquals(a.getPoints().get(i), b.getPoints().get(i));
		}
	}
}

