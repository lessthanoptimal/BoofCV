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
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvertImage {

	Random rand = new Random(34);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void convert_int8_float32() {
		ImageFloat32 conv = new ImageFloat32(imgWidth, imgHeight);

		// test it with a signed image
		ImageInt8 orig = new ImageInt8(imgWidth, imgHeight, true);
		BasicDrawing_I8.randomize(orig, rand);

		GecvTesting.checkSubImage(this, "convert_int8_float32", true, orig, conv);

		// test it with an unsigned image
		orig = new ImageInt8(imgWidth, imgHeight, false);
		BasicDrawing_I8.randomize(orig, rand);
		GecvTesting.checkSubImage(this, "convert_int8_float32", true, orig, conv);
	}

	public void convert_int8_float32(ImageInt8 orig, ImageFloat32 conv) {
		ConvertImage.convert(orig, conv);

		int numPositive = 0;
		int numNegative = 0;

		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				float f = conv.get(j, i);
				if (f >= 0)
					numPositive++;
				else
					numNegative++;

				assertEquals(f, (float) orig.get(j, i), 1e-8);
			}
		}

		assertTrue(numPositive > 0);
		if( orig.isSigned())
			assertTrue(numNegative > 0);
		else
			assertTrue(numNegative == 0);
	}

	@Test
	public void convert_int16_float32() {
		// test it against a signed image
		ImageInt16 orig = new ImageInt16(imgWidth, imgHeight, true);
		UtilImageInt16.randomize(orig, rand, -20, 20);

		ImageFloat32 conv = new ImageFloat32(imgWidth, imgHeight);

		GecvTesting.checkSubImage(this, "convert_int16_float32", true, orig, conv);

		// test it against an unsigned image
		orig = new ImageInt16(imgWidth, imgHeight, false);
		UtilImageInt16.randomize(orig, rand, 0, 40);
		GecvTesting.checkSubImage(this, "convert_int16_float32", true, orig, conv);
	}

	public void convert_int16_float32(ImageInt16 orig, ImageFloat32 conv) {
		ConvertImage.convert(orig, conv);

		int numPositive = 0;
		int numNegative = 0;

		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				float f = conv.get(j, i);
				if (f >= 0)
					numPositive++;
				else
					numNegative++;

				assertEquals(f, (float) orig.get(j, i), 1e-8);
			}
		}

		assertTrue(numPositive > 0);
		if( orig.isSigned() )
			assertTrue(numNegative > 0);
		else
			assertTrue(numNegative == 0);
	}

	@Test
	public void convert_int16_int8() {
		// test it against a signed image
		ImageInt16 orig = new ImageInt16(imgWidth, imgHeight, true);
		UtilImageInt16.randomize(orig, rand, -20, 20);

		ImageInt8 conv = new ImageInt8(imgWidth, imgHeight,true);

		GecvTesting.checkSubImage(this, "convert_int16_int8", true, orig, conv);

		// test it against an unsigned image
		orig = new ImageInt16(imgWidth, imgHeight, false);
		UtilImageInt16.randomize(orig, rand, 0, 40);
		GecvTesting.checkSubImage(this, "convert_int16_int8", true, orig, conv);
	}

	public void convert_int16_int8(ImageInt16 orig, ImageInt8 conv) {
		ConvertImage.convert(orig, conv);

		int numPositive = 0;
		int numNegative = 0;

		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int f = conv.get(j, i);
				if (f >= 0)
					numPositive++;
				else
					numNegative++;

				assertEquals(f, orig.get(j, i), 1e-8);
			}
		}

		assertTrue(numPositive > 0);
		if( orig.isSigned() )
			assertTrue(numNegative > 0);
		else
			assertTrue(numNegative == 0);
	}

	@Test
	public void convert_float32_int8() {
		ImageFloat32 orig = new ImageFloat32(imgWidth, imgHeight);
		UtilImageFloat32.randomize(orig, rand, -100, 100);

		ImageInt8 conv = new ImageInt8(imgWidth, imgHeight,true);

		GecvTesting.checkSubImage(this, "convert_float32_int8", true, orig, conv);
	}

	public void convert_float32_int8(ImageFloat32 orig, ImageInt8 conv) {
		ConvertImage.convert(orig, conv);

		// quick sanity check to make sure randomize worked
		assertTrue(orig.get(0, 0) != 0);

		// see if the conversion was done correctly
		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int b = conv.get(j, i);

				assertEquals(b, (int) orig.get(j, i), 1e-8);
			}
		}
	}

	@Test
	public void convert_float32_int16() {
		ImageFloat32 orig = new ImageFloat32(imgWidth, imgHeight);
		UtilImageFloat32.randomize(orig, rand, -100, 100);

		ImageInt16 conv = new ImageInt16(imgWidth, imgHeight, true);

		GecvTesting.checkSubImage(this, "convert_float32_int16", true, orig, conv);
	}

	public void convert_float32_int16(ImageFloat32 orig, ImageInt16 conv) {
		ConvertImage.convert(orig, conv);

		// quick sanity check to make sure randomize worked
		assertTrue(orig.get(0, 0) != 0);

		// see if the conversion was done correctly
		for (int i = 0; i < imgHeight; i++) {
			for (int j = 0; j < imgWidth; j++) {
				int b = conv.get(j, i);

				assertEquals(b, (int) orig.get(j, i), 1e-8);
			}
		}
	}
}
