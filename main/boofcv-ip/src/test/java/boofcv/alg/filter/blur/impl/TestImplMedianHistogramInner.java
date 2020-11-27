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

package boofcv.alg.filter.blur.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplMedianHistogramInner extends BoofStandardJUnit {

	@Test
	void compareToSort() {
		GrayU8 input = new GrayU8(20, 30);
		ImageMiscOps.fillUniform(input, new Random(234), 0, 100);

		GrayU8 found = input.createSameShape();
		GrayU8 expected = input.createSameShape();

		BoofTesting.checkSubImage(this, "compareToSort", true, input, found, expected);
	}

	public void compareToSort( GrayU8 image, GrayU8 found, GrayU8 expected ) {
		GrowArray<DogArray_I32> work = new GrowArray<>(DogArray_I32::new);

		for (int radiusX = 1; radiusX <= 3; radiusX++) {
			int radiusY = radiusX + 1;
			ImageMiscOps.fill(found, 0);
			ImageMiscOps.fill(expected, 0);

			ImplMedianHistogramInner.process(image, found, radiusX, radiusY, work);
			ImplMedianSortNaive.process(image, expected, radiusX, radiusY, work);

			BoofTesting.assertEqualsInner(expected, found, 0, radiusX, radiusY, false);
		}
	}
}
