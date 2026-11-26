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
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.override.BOverrideManager;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder1D_F32;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestBOverrideConvolveImage extends BoofStandardJUnit {
	GrayF32 image = new GrayF32(10, 8);
	Kernel1D_F32 kernel1 = FactoryKernel.table1D_F32(1, false);
	Kernel2D_F32 kernel2 = FactoryKernel.random2D_F32(3, 1, 0, 1, rand);
	ImageBorder reflect = FactoryImageBorder.single(BorderType.REFLECT, GrayF32.class);
	ImageBorder custom = new ImageBorder1D_F32(BorderIndex1D_Extend::new);

	List<BorderType> found = new ArrayList<>();

	@AfterEach void clear() {BOverrideManager.clear();}

	@Test void borderType() {
		BOverrideConvolveImage.horizontal = ( k, input, output, border ) -> found.add(border);
		BOverrideConvolveImage.vertical = ( k, input, output, border ) -> found.add(border);
		BOverrideConvolveImage.convolve = ( k, input, output, border ) -> found.add(border);

		assertTrue(BOverrideConvolveImage.invokeNativeHorizontal(kernel1, image, image, reflect));
		assertTrue(BOverrideConvolveImage.invokeNativeVertical(kernel1, image, image, reflect));
		assertTrue(BOverrideConvolveImage.invokeNativeConvolve(kernel2, image, image, reflect));
		assertFalse(BOverrideConvolveImage.invokeNativeHorizontal(kernel1, image, image, custom));
		assertFalse(BOverrideConvolveImage.invokeNativeVertical(kernel1, image, image, custom));
		assertFalse(BOverrideConvolveImage.invokeNativeConvolve(kernel2, image, image, custom));
		assertEquals(List.of(BorderType.REFLECT, BorderType.REFLECT, BorderType.REFLECT), found);
	}
}
