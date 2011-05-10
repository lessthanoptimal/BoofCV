/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.core.image;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestConvertRaster {

	Random rand = new Random(234);

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void bufferedToGray_U8_ByteInterleaved() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_U8_ByteInterleaved", false, result);
	}

	public void bufferedToGray_U8_ByteInterleaved(ImageUInt8 result) {
		// check with a 3 byte image
		BufferedImage origImg = createByteBuff(imgWidth, imgHeight, 3, rand);
		ConvertRaster.bufferedToGray((ByteInterleavedRaster) origImg.getRaster(), result);

		GecvTesting.checkEquals(origImg, result);

		// check with a 1 byte image
		origImg = createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertRaster.bufferedToGray((ByteInterleavedRaster) origImg.getRaster(), result);
		GecvTesting.checkEquals(origImg, result);
	}

	@Test
	public void bufferedToGray_F32_ByteInterleaved() {
		ImageFloat32 result = new ImageFloat32(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_F32_ByteInterleaved", false, result);
	}

	public void bufferedToGray_F32_ByteInterleaved(ImageFloat32 result) {
		// check with a 3 byte image
		BufferedImage origImg = createByteBuff(imgWidth, imgHeight, 3, rand);
		ConvertRaster.bufferedToGray((ByteInterleavedRaster) origImg.getRaster(), result);

		GecvTesting.checkEquals(origImg, result,1e-4f);

		// check with a 1 byte image
		origImg = createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertRaster.bufferedToGray((ByteInterleavedRaster) origImg.getRaster(), result);
		GecvTesting.checkEquals(origImg, result,1e-4f);
	}

	@Test
	public void bufferedToGray_U8_IntegerInterleaved() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_U8_IntegerInterleaved", false, result);
	}

	public void bufferedToGray_U8_IntegerInterleaved(ImageUInt8 result) {
		BufferedImage origImg = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.bufferedToGray((IntegerInterleavedRaster) origImg.getRaster(), result);

		GecvTesting.checkEquals(origImg, result);
	}

	@Test
	public void bufferedToGray_F32_IntegerInterleaved() {
		ImageFloat32 result = new ImageFloat32(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_F32_IntegerInterleaved", false, result);
	}

	public void bufferedToGray_F32_IntegerInterleaved(ImageFloat32 result) {
		BufferedImage origImg = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.bufferedToGray((IntegerInterleavedRaster) origImg.getRaster(), result);

		GecvTesting.checkEquals(origImg, result,1e-4f);
	}

	@Test
	public void bufferedToGray_U8_BufferedImage() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_U8_BufferedImage", false, result);
	}

	public void bufferedToGray_U8_BufferedImage(ImageUInt8 result) {
		BufferedImage origImg = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.bufferedToGray(origImg, result);

		GecvTesting.checkEquals(origImg, result);
	}

	@Test
	public void bufferedToGray_F32_BufferedImage() {
		ImageFloat32 result = new ImageFloat32(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "bufferedToGray_F32_BufferedImage", false, result);
	}

	public void bufferedToGray_F32_BufferedImage(ImageFloat32 result) {
		BufferedImage origImg = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.bufferedToGray(origImg, result);

		GecvTesting.checkEquals(origImg, result, 1e-3f);
	}

	@Test
	public void grayToBuffered_ByteInterleaved() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);
		BasicDrawing_I8.randomize(result, rand);

		GecvTesting.checkSubImage(this, "grayToBuffered_ByteInterleaved", true, result);
	}

	public void grayToBuffered_ByteInterleaved(ImageUInt8 origImg) {
		// check with a 3 byte image
		BufferedImage result = createByteBuff(imgWidth, imgHeight, 3, rand);
		ConvertRaster.grayToBuffered(origImg, (ByteInterleavedRaster) result.getRaster());

		GecvTesting.checkEquals(result, origImg);

		// check with a 1 byte image
		result = createByteBuff(imgWidth, imgHeight, 1, rand);
		ConvertRaster.grayToBuffered(origImg, (ByteInterleavedRaster) result.getRaster());
		GecvTesting.checkEquals(result, origImg);
	}

	@Test
	public void grayToBuffered_IntegerInterleaved() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);
		BasicDrawing_I8.randomize(result, rand);

		GecvTesting.checkSubImage(this, "grayToBuffered_IntegerInterleaved", true, result);
	}

	public void grayToBuffered_IntegerInterleaved(ImageUInt8 origImg) {
		BufferedImage result = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.grayToBuffered(origImg, (IntegerInterleavedRaster) result.getRaster());

		GecvTesting.checkEquals(result, origImg);
	}

	@Test
	public void grayToBuffered_BufferedImage() {
		ImageUInt8 result = new ImageUInt8(imgWidth, imgHeight);
		BasicDrawing_I8.randomize(result, rand);

		GecvTesting.checkSubImage(this, "grayToBuffered_BufferedImage", true, result);
	}

	public void grayToBuffered_BufferedImage(ImageUInt8 origImg) {
		BufferedImage result = createIntBuff(imgWidth, imgHeight, rand);
		ConvertRaster.grayToBuffered(origImg, result);

		GecvTesting.checkEquals(result, origImg);
	}

	public static BufferedImage createByteBuff(int width, int height, int numBands, Random rand) {
		BufferedImage ret;

		if (numBands == 1) {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		} else {
			ret = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}

		randomize(ret, rand);

		return ret;
	}

	public static BufferedImage createIntBuff(int width, int height, Random rand) {
		BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		randomize(ret, rand);
		return ret;
	}

	public static void randomize(BufferedImage img, Random rand) {
		for (int i = 0; i < img.getWidth(); i++) {
			for (int j = 0; j < img.getHeight(); j++) {
				img.setRGB(i, j, rand.nextInt() & 0xFFFFFF );
			}
		}
	}
}
