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

package gecv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestMultiSpectralInt8 {

	int imgWidth = 10;
	int imgHeight = 20;
	int numBands = 2;

	@Test
	public void constructor_full() {
		MultiSpectralInt8 img = new MultiSpectralInt8(imgWidth, imgHeight, numBands);

		assertEquals(numBands, img.bands.length);
		assertTrue(ImageInt8.class == img.type);
		for (int i = 0; i < numBands; i++) {
			assertEquals(imgWidth, img.bands[i].width);
			assertEquals(imgHeight, img.bands[i].height);
		}
	}

	@Test
	public void constructor_partial() {
		MultiSpectralInt8 img = new MultiSpectralInt8(numBands);

		assertEquals(numBands, img.bands.length);
		assertTrue(ImageInt8.class == img.type);
	}

	@Test
	public void get() {
		MultiSpectralInt8 img = new MultiSpectralInt8(imgWidth, imgHeight, numBands);

		// see if it has the expected initial value
		byte[] pixel = img.get(1, 2, null);
		for (int i = 0; i < numBands; i++) {
			assertEquals(0, pixel[i]);
		}

		// now give it a different value
		for (int i = 0; i < numBands; i++) {
			img.bands[i].set(1, 2, (byte) (i + 1));
		}

		// check for this value
		img.get(1, 2, pixel);
		for (int i = 0; i < numBands; i++) {
			assertEquals(i + 1, pixel[i]);
		}
	}

	@Test
	public void set() {
		MultiSpectralInt8 img = new MultiSpectralInt8(imgWidth, imgHeight, numBands);

		// see if it has the expected initial value
		byte[] pixel = img.get(1, 2, null);
		for (int i = 0; i < numBands; i++) {
			assertEquals(0, pixel[i]);
		}

		// now give it a different value
		for (int i = 0; i < numBands; i++) {
			pixel[i] = (byte) (i + 1);
		}

		// check for this value
		img.set(1, 2, pixel);
		for (int i = 0; i < numBands; i++) {
			assertEquals(i + 1, img.bands[i].get(1, 2));
		}
	}
}
