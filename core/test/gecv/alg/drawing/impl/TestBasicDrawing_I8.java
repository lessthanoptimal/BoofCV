/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.drawing.impl;

import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestBasicDrawing_I8 {
	Random rand = new Random(234234);

	@Test
	public void fill() {
		ImageUInt8 image = new ImageUInt8(10, 20);

		GecvTesting.checkSubImage(this, "checkFill", true, image);
	}

	public void checkFill(ImageUInt8 image) {
		ImageInitialization_I8.fill(image, (byte) 6);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertEquals(image.get(x, y), (byte) 6);
			}
		}
	}

	@Test
	public void rectangle() {
		fail("implement");
	}

	@Test
	public void randomize() {
		ImageUInt8 image = new ImageUInt8(10, 20);

		GecvTesting.checkSubImage(this, "checkRandomize", false, image);
	}

	public void checkRandomize(ImageUInt8 image) {
		ImageInitialization_I8.randomize(image, rand);

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				assertTrue(image.get(x, y) != 0);
			}
		}
	}
}
