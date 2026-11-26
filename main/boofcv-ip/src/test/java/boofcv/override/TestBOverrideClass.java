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

package boofcv.override;

import boofcv.core.image.ImageBorderValue;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.InterleavedF64;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestBOverrideClass extends BoofStandardJUnit {
	@Test void canOverride() {
		assertTrue(BOverrideClass.canOverride(null));
		for (BorderType type : new BorderType[]{BorderType.EXTENDED, BorderType.REFLECT, BorderType.WRAP, BorderType.ZERO}) {
			assertTrue(BOverrideClass.canOverride(FactoryImageBorder.single(type, GrayU8.class)));
			assertTrue(BOverrideClass.canOverride(FactoryImageBorder.interleaved(type, InterleavedF64.class)));
		}

		assertFalse(BOverrideClass.canOverride(new ImageBorder1D_F32(BorderIndex1D_Extend::new)));
		assertFalse(BOverrideClass.canOverride(FactoryImageBorder.singleValue(0, GrayF32.class)));
	}

	@Test void canOverride_zeroChanged() {
		var border = (ImageBorderValue.Value_F32)FactoryImageBorder.single(BorderType.ZERO, GrayF32.class);
		border.value = 2;
		assertFalse(BOverrideClass.canOverride(border));

		var borderIL = (ImageBorderValue.Value_IL_F64)FactoryImageBorder.interleaved(BorderType.ZERO, InterleavedF64.class);
		borderIL.value = 2;
		assertFalse(BOverrideClass.canOverride(borderIL));
	}

	@Test void overrideBorderOrSkip() {
		assertSame(BorderType.SKIP, BOverrideClass.overrideBorderOrSkip(null));
		assertSame(BorderType.REFLECT, BOverrideClass.overrideBorderOrSkip(FactoryImageBorder.single(BorderType.REFLECT, GrayU8.class)));
	}
}
