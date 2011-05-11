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

package gecv.alg.detect.corner.impl;

import gecv.alg.detect.corner.GenericCornerIntensityGradientTests;
import gecv.alg.detect.corner.GenericCornerIntensityTests;
import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.core.image.ConvertImage;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestKltCorner_F32 {

	int width = 15;
	int height = 15;


	@Test
	public void genericTests() {
		GenericCornerIntensityTests generic = new GenericCornerIntensityGradientTests(){

			@Override
			public ImageFloat32 computeIntensity() {
				KltCorner_F32 alg = new KltCorner_F32(width,height,1);
				alg.process(derivX_F32,derivY_F32);
				return alg.getIntensity();
			}
		};

		generic.performAllTests();
	}

	/**
	 * Sees if the integer version and this version produce the same results.
	 * <p/>
	 * Creates a random image and looks for corners in it.  Sees if the naive
	 * and fast algorithm produce exactly the same results.
	 */
	@Test
	public void compareToNaive() {
		ImageUInt8 img = new ImageUInt8(width, height);
		BasicDrawing_I8.randomize(img, new Random(0xfeed));

		ImageSInt16 derivX_I = new ImageSInt16(img.getWidth(), img.getHeight());
		ImageSInt16 derivY_I = new ImageSInt16(img.getWidth(), img.getHeight());

		GradientSobel.process(img, derivX_I, derivY_I);

		ImageFloat32 derivX_F = ConvertImage.convert(derivX_I, (ImageFloat32)null);
		ImageFloat32 derivY_F = ConvertImage.convert(derivY_I, (ImageFloat32)null);

		GecvTesting.checkSubImage(this, "compareToNaive", true, derivX_I, derivY_I, derivX_F, derivY_F);
	}

	public void compareToNaive(ImageSInt16 derivX_I, ImageSInt16 derivY_I, ImageFloat32 derivX_F, ImageFloat32 derivY_F) {
		SsdCornerNaive_I16 ssd_I = new SsdCornerNaive_I16(width, height, 3);
		ssd_I.process(derivX_I, derivY_I);

		KltCorner_F32 ssd_F = new KltCorner_F32(width, height, 3);
		ssd_F.process(derivX_F, derivY_F);

		GecvTesting.assertEquals(ssd_I.getIntensity(), ssd_F.getIntensity(), 0, 1f);
	}
}
