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

package boofcv.core.image;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageInterleavedTestingOps;
import boofcv.struct.image.InterleavedI8;
import boofcv.struct.image.InterleavedU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageInterleavedTestingOps extends BoofStandardJUnit {

	@Test void fill() {
		InterleavedU8 image = new InterleavedU8(10, 20, 3);

		BoofTesting.checkSubImage(this, "checkFill", true, image);
	}

	public void checkFill(InterleavedI8 image) {
		ImageInterleavedTestingOps.fill(image, (byte) 6, (byte) 7, (byte) 8);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertEquals(image.getBand(x, y, 0), (byte) 6);
				assertEquals(image.getBand(x, y, 1), (byte) 7);
				assertEquals(image.getBand(x, y, 2), (byte) 8);
			}
		}
	}

	@Test void randomize() {
		InterleavedU8 image = new InterleavedU8(10, 20, 3);

		BoofTesting.checkSubImage(this, "checkRandomize", false, image);
	}

	public void checkRandomize(InterleavedI8 image) {
		ImageInterleavedTestingOps.randomize(image, rand);

		int totalZero = 0;
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				for (int k = 0; k < 3; k++)
					if (image.getBand(x, y, k) == 0)
						totalZero++;
			}
		}
		assertTrue(totalZero < 10);
	}
}
