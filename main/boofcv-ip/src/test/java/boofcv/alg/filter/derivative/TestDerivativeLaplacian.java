/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.BoofTesting;
import boofcv.alg.filter.convolve.ConvolveImage;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDerivativeLaplacian extends BoofStandardJUnit {

	private final int width = 4;
	private final int height = 5;

	@Test void process_U8_S16() {
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 100);

		GrayS16 deriv = new GrayS16(width, height);
		BoofTesting.checkSubImage(this, "process_U8_S16", true, img, deriv);
	}

	public void process_U8_S16(GrayU8 img, GrayS16 deriv) {
		ImageBorder_S32<GrayU8> border = (ImageBorder_S32)FactoryImageBorder.single(BorderType.EXTENDED, GrayU8.class);
		DerivativeLaplacian.process(img, deriv, border);

		GrayS16 expected = deriv.createSameShape();
		ConvolveImage.convolve(DerivativeLaplacian.kernel_I32,img,expected,border);

		BoofTesting.assertEquals(expected,deriv,0);
	}

	@Test void process_I8_F32() {
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 100);

		GrayF32 deriv = new GrayF32(width, height);
		BoofTesting.checkSubImage(this, "process_U8_F32", true, img, deriv);
	}

	public void process_U8_F32(GrayU8 img, GrayF32 deriv) {
		DerivativeLaplacian.process(img, deriv);

		int expected = -4 * img.get(1, 1) + img.get(0, 1) + img.get(1, 0)
				+ img.get(2, 1) + img.get(1, 2);

		assertEquals(expected, deriv.get(1, 1), 1e-5);
	}

	@Test void process_F32() {
		GrayF32 img = new GrayF32(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 1);

		GrayF32 deriv = new GrayF32(width, height);
		BoofTesting.checkSubImage(this, "process_F32", true, img, deriv);
	}

	public void process_F32(GrayF32 img, GrayF32 deriv) {
		ImageBorder_F32 border = (ImageBorder_F32)FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);
		DerivativeLaplacian.process(img, deriv, border);

		GrayF32 expected = deriv.createSameShape();
		ConvolveImage.convolve(DerivativeLaplacian.kernel_F32,img,expected,border);

		BoofTesting.assertEquals(expected,deriv, UtilEjml.TEST_F32);
	}
}
