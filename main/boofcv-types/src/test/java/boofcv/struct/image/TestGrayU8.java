/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.image;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestGrayU8 extends StandardImageIntegerTests<GrayU8> {
	public TestGrayU8() {
		super(false);
	}

	@Override public GrayU8 createImage(int width, int height) {
		return new GrayU8(width, height);
	}

	@Override public GrayU8 createImage() {
		return new GrayU8();
	}

	@Override public Number randomNumber() {
		return (byte)rand.nextInt(255);
	}

	@Test void forEachPixel() {
		var image = new GrayU8(10, 15);
		setRandom(image);

		image.forEachPixel(( x, y, v ) -> assertEquals(image.get(x, y), v));
	}


	@Test void applyEachPixel() {
		var image = new GrayU8(10, 15);
		setRandom(image);

		image.applyEachPixel(( x, y, v ) -> y*image.width + x);

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				assertEquals(y*image.width + x, image.get(x, y));
			}
		}
	}

}
