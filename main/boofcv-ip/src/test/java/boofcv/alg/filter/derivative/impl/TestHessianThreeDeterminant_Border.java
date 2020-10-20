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

package boofcv.alg.filter.derivative.impl;

import boofcv.BoofTesting;
import boofcv.alg.filter.convolve.ConvolveImage;
import boofcv.alg.filter.derivative.HessianThree;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

public class TestHessianThreeDeterminant_Border extends BoofStandardJUnit {

	private final int width = 8;
	private final int height = 9;

	@Test
	void process_U8_S16() {
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 100);

		GrayS16 deriv = new GrayS16(width, height);
		BoofTesting.checkSubImage(this, "process_U8_S16", true, img, deriv);
	}

	public void process_U8_S16(GrayU8 img, GrayS16 deriv) {
		ImageBorder_S32<GrayU8> border = (ImageBorder_S32) FactoryImageBorder.single(BorderType.EXTENDED, GrayU8.class);
		HessianThreeDeterminant_Border.process(img, deriv, border);

		GrayS16 expected = hessianDetFromConv_U8_S16(img);
		BoofTesting.assertEqualsBorder(expected,deriv,0,2,2);
	}

	@Test
	void process_U8_F32() {
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 100);

		GrayF32 deriv = new GrayF32(width, height);
		BoofTesting.checkSubImage(this, "process_U8_F32", true, img, deriv);
	}

	public void process_U8_F32(GrayU8 img, GrayF32 deriv) {
		ImageBorder_S32<GrayU8> border = (ImageBorder_S32) FactoryImageBorder.single(BorderType.EXTENDED, GrayU8.class);
		HessianThreeDeterminant_Border.process(img, deriv, border);

		GrayS16 expected = hessianDetFromConv_U8_S16(img);
		BoofTesting.assertEqualsBorder(expected,deriv,1e-4,2,2);
	}

	@Test
	void process_F32() {
		GrayF32 img = new GrayF32(width, height);
		ImageMiscOps.fillUniform(img, rand, 0, 1);

		GrayF32 deriv = new GrayF32(width, height);
		BoofTesting.checkSubImage(this, "process_F32", true, img, deriv);
	}

	public void process_F32(GrayF32 img, GrayF32 deriv) {
		ImageBorder_F32 border = (ImageBorder_F32) FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);
		HessianThreeDeterminant_Border.process(img, deriv, border);

		GrayF32 expected = hessianDetFromConv_F32_F32(img);
		BoofTesting.assertEqualsBorder(expected,deriv,1e-4,2,2);
	}

	public static GrayS16 hessianDetFromConv_U8_S16(GrayU8 img) {
		ImageBorder_S32<GrayU8> border = (ImageBorder_S32) FactoryImageBorder.single(BorderType.EXTENDED, GrayU8.class);
		GrayS16 Lxx = new GrayS16(img.width,img.height);
		GrayS16 Lyy = new GrayS16(img.width,img.height);
		GrayS16 Lxy = new GrayS16(img.width,img.height);

		ConvolveImage.horizontal(HessianThree.kernelXXYY_I32,img,Lxx,border);
		ConvolveImage.vertical(HessianThree.kernelXXYY_I32,img,Lyy,border);
		ConvolveImage.convolve(HessianThree.kernelCross_I32,img,Lxy,border);

		GrayS16 expected =new GrayS16(img.width,img.height);

		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				expected.data[expected.getIndex(x,y)] = (short)(Lxx.get(x,y)*Lyy.get(x,y) - Lxy.get(x,y)*Lxy.get(x,y));
			}
		}
		return expected;
	}

	public static GrayF32 hessianDetFromConv_F32_F32(GrayF32 img) {
		ImageBorder_F32 border = (ImageBorder_F32) FactoryImageBorder.single(BorderType.EXTENDED, GrayF32.class);
		GrayF32 Lxx = new GrayF32(img.width,img.height);
		GrayF32 Lyy = new GrayF32(img.width,img.height);
		GrayF32 Lxy = new GrayF32(img.width,img.height);

		ConvolveImage.horizontal(HessianThree.kernelXXYY_F32,img,Lxx,border);
		ConvolveImage.vertical(HessianThree.kernelXXYY_F32,img,Lyy,border);
		ConvolveImage.convolve(HessianThree.kernelCross_F32,img,Lxy,border);

		GrayF32 expected = new GrayF32(img.width,img.height);

		for (int y = 0; y < img.height; y++) {
			for (int x = 0; x < img.width; x++) {
				expected.data[expected.getIndex(x,y)] = 4*(Lxx.get(x,y)*Lyy.get(x,y) - Lxy.get(x,y)*Lxy.get(x,y));
			}
		}
		return expected;
	}
}
