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

public class TestInterleavedS8 extends StandardImageInterleavedTests<InterleavedS8> {


	@Override
	public InterleavedS8 createImage(int width, int height, int numBands) {
		return new InterleavedS8(width, height, numBands);
	}

	@Override
	public InterleavedS8 createImage() {
		return new InterleavedS8();
	}

	@Override
	public Number randomNumber() {
		return (byte)(rand.nextInt(255)-Byte.MIN_VALUE);
	}

	@Override
	public Number getNumber( Number value) {
		return value.byteValue();
	}

	@Test void forEachPixel() {
		var img = new InterleavedS8(2, 3, 4);
		setRandom(img);

		img.forEachPixel(( x, y, found ) -> {
			for (int i = 0; i < 4; i++) {
				assertEquals(found[i], img.getBand(x, y, i));
			}
		});
	}
}
