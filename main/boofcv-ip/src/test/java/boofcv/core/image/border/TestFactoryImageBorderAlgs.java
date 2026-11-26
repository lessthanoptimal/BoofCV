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

package boofcv.core.image.border;

import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TestFactoryImageBorderAlgs extends BoofStandardJUnit {
	@Test void borderType() {
		var f64 = new GrayF64(5, 4);
		assertEquals(BorderType.EXTENDED, FactoryImageBorderAlgs.extend(f64).type);
		assertEquals(BorderType.REFLECT, FactoryImageBorderAlgs.reflect(f64).type);
		assertEquals(BorderType.WRAP, FactoryImageBorderAlgs.wrap(f64).type);
		assertNull(FactoryImageBorderAlgs.value(f64, 0).type);

		var f32 = new GrayF32(5, 4);
		assertEquals(BorderType.EXTENDED, FactoryImageBorderAlgs.extend(f32).type);
		assertEquals(BorderType.REFLECT, FactoryImageBorderAlgs.reflect(f32).type);
		assertEquals(BorderType.WRAP, FactoryImageBorderAlgs.wrap(f32).type);
		assertNull(FactoryImageBorderAlgs.value(f32, 0).type);

		var u8 = new GrayU8(5, 4);
		assertEquals(BorderType.EXTENDED, FactoryImageBorderAlgs.extend(u8).type);
		assertEquals(BorderType.REFLECT, FactoryImageBorderAlgs.reflect(u8).type);
		assertEquals(BorderType.WRAP, FactoryImageBorderAlgs.wrap(u8).type);
		assertNull(FactoryImageBorderAlgs.value(u8, 0).type);
	}
}
