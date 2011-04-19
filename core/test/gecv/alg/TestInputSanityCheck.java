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

package gecv.alg;

import gecv.struct.image.ImageInt8;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestInputSanityCheck {

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void checkShape_two() {
		ImageInt8 a = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 b = new ImageInt8(imgWidth, imgHeight);

		// InputSanityCheck test
		InputSanityCheck.checkSameShape(a, b);

		// negative test
		try {
			b = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			b = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	public void checkShape_three() {
		ImageInt8 a = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 b = new ImageInt8(imgWidth, imgHeight);
		ImageInt8 c = new ImageInt8(imgWidth, imgHeight);

		// InputSanityCheck test
		InputSanityCheck.checkSameShape(a, b, c);

		// negative test
		try {
			b = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			b = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
		b = new ImageInt8(imgWidth, imgHeight);
		try {
			c = new ImageInt8(imgWidth + 1, imgHeight);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}

		try {
			c = new ImageInt8(imgWidth, imgHeight + 1);
			InputSanityCheck.checkSameShape(a, b, c);
			fail("Didn't throw an exception");
		} catch (IllegalArgumentException e) {
		}
	}
}
