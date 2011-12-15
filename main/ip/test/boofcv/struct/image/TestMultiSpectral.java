/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestMultiSpectral {

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void constructor() {
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,imgWidth, imgHeight, 3);

		assertTrue(ImageUInt8.class == img.getType());
		assertTrue(3 == img.bands.length);
		assertTrue(3 == img.getNumBands());
		assertTrue(imgWidth == img.width);
		assertTrue(imgHeight == img.height);
		for (int i = 0; i < 3; i++) {
			assertTrue(img.bands[i] != null);
		}
	}


	@Test
	public void getBand() {
		MultiSpectral<ImageUInt8> img = new MultiSpectral<ImageUInt8>(ImageUInt8.class,imgWidth, imgHeight, 3);

		assertTrue(img.getBand(0) != null);

		try {
			img.getBand(-1);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
		try {
			img.getBand(3);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
	}

	@Test
	public void subimage() {
		fail("implement");
	}

	@Test
	public void reshape() {
		fail("implement");
	}
}
