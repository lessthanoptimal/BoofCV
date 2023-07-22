/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

public class TestGrayF32 extends StandardSingleBandTests<GrayF32> {

	@Override
	public GrayF32 createImage( int width, int height ) {
		return new GrayF32(width, height);
	}

	@Override
	public GrayF32 createImage() {
		return new GrayF32();
	}

	@Override
	public Number randomNumber() {
		return rand.nextFloat();
	}

	@Test void forEachPixel() {
		var image = new GrayF32(10, 15);
		setRandom(image);

		image.forEachPixel(( x, y, v ) -> {
			assertEquals(image.get(x, y), v);
			image.set(x, y, y*image.width + x);
		});

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				assertEquals(y*image.width + x, image.get(x, y));
			}
		}
	}
}
