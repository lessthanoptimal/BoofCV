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

public class TestInterleavedS32 extends StandardImageInterleavedTests<InterleavedS32> {


	@Override
	public InterleavedS32 createImage(int width, int height, int numBands) {
		return new InterleavedS32(width, height, numBands);
	}

	@Override
	public InterleavedS32 createImage() {
		return new InterleavedS32();
	}

	@Override
	public Number randomNumber() {
		return rand.nextInt(3000)-1500;
	}

	@Override
	public Number getNumber( Number value) {
		return value.intValue();
	}

	@Test void forEachPixel() {
		var img = new InterleavedS32(2, 3, 4);
		setRandom(img);

		img.forEachPixel(( x, y, found ) -> {
			for (int i = 0; i < 4; i++) {
				assertEquals(found[i], img.getBand(x, y, i));
			}
		});
	}
}
