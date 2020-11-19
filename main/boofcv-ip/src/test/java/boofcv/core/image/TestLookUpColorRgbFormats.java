/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestLookUpColorRgbFormats extends BoofStandardJUnit {
	@Test void PL_U8() {
		var image = new Planar<>(GrayU8.class,10,12, 3);
		image.set24u8(2,4,0x1245EE);

		var alg = new LookUpColorRgbFormats.PL_U8();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x1245EE, alg.lookupRgb(2,4));
	}

	@Test void PL_F32() {
		var image = new Planar<>(GrayF32.class,10,12, 3);
		image.getBand(0).set(2,4,0x12);
		image.getBand(1).set(2,4,0x45);
		image.getBand(2).set(2,4,0xEE);

		var alg = new LookUpColorRgbFormats.PL_F32();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x1245EE, alg.lookupRgb(2,4));
	}

	@Test void IL_U8() {
		var image = new InterleavedU8(10,12,3);
		image.set24(2,4,0x1245EE);

		var alg = new LookUpColorRgbFormats.IL_U8();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x1245EE, alg.lookupRgb(2,4));
	}

	@Test void IL_F32() {
		var image = new InterleavedF32(10,12,3);
		image.set(2,4,0x12,0x45,0xEE);

		var alg = new LookUpColorRgbFormats.IL_F32();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x1245EE, alg.lookupRgb(2,4));
	}

	@Test void SB_U8() {
		var image = new GrayU8(10,12);
		image.set(2,4,0x12);

		var alg = new LookUpColorRgbFormats.SB_U8();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x121212, alg.lookupRgb(2,4));
	}

	@Test void SB_F32() {
		var image = new GrayF32(10,12);
		image.set(2,4,0x12);

		var alg = new LookUpColorRgbFormats.SB_F32();
		alg.setImage(image);
		assertEquals(0, alg.lookupRgb(2,3));
		assertEquals(0x121212, alg.lookupRgb(2,4));
	}
}
