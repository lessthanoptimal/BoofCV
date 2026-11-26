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

package boofcv.alg.filter.convolve;

import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestBOverrideConvolveImageMean extends BoofStandardJUnit {
	GrayF32 image = new GrayF32(10, 8);
	ImageBorder reflect = FactoryImageBorder.single(BorderType.REFLECT, GrayF32.class);
	ImageBorder custom = new ImageBorder1D_F32(BorderIndex1D_Extend::new);

	List<BorderType> found = new ArrayList<>();

	@AfterEach void clear() {BOverrideManager.clear();}

	@Test void borderType() {
		BOverrideConvolveImageMean.horizontal = ( input, output, offset, length, border ) -> found.add(border);
		BOverrideConvolveImageMean.vertical = ( input, output, offset, length, border ) -> found.add(border);

		assertTrue(BOverrideConvolveImageMean.invokeNativeHorizontal(image, image, 1, 3));
		assertTrue(BOverrideConvolveImageMean.invokeNativeVertical(image, image, 1, 3));
		assertTrue(BOverrideConvolveImageMean.invokeNativeHorizontalBorder(image, image, 1, 3, reflect));
		assertTrue(BOverrideConvolveImageMean.invokeNativeVerticalBorder(image, image, 1, 3, reflect));
		assertTrue(BOverrideConvolveImageMean.invokeNativeHorizontalBorder(image, image, 1, 3, null));
		assertTrue(BOverrideConvolveImageMean.invokeNativeVerticalBorder(image, image, 1, 3, null));
		assertFalse(BOverrideConvolveImageMean.invokeNativeHorizontalBorder(image, image, 1, 3, custom));
		assertFalse(BOverrideConvolveImageMean.invokeNativeVerticalBorder(image, image, 1, 3, custom));
		BorderType N = BorderType.NORMALIZED, R = BorderType.REFLECT, S = BorderType.SKIP;
		assertEquals(List.of(N, N, R, R, S, S), found);
	}
}
