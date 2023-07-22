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

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImageMultiBand extends BoofStandardJUnit {
	@Test void reshapeTo_SingleBand() {
		var mb = new InterleavedU8(1,2,3);
		mb.reshapeTo(new GrayF32(10, 15));

		assertEquals(10, mb.width);
		assertEquals(15, mb.height);
		assertEquals(3, mb.numBands);
	}

	@Test void reshapeTo_MultiBand() {
		var mb = new InterleavedU8(1,2,3);
		mb.reshapeTo(new InterleavedF32(10, 15, 2));

		assertEquals(10, mb.width);
		assertEquals(15, mb.height);
		assertEquals(2, mb.numBands);
	}
}
