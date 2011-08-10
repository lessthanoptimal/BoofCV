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

package gecv.alg.detect.intensity.impl;

import gecv.alg.detect.intensity.GenericCornerIntensityGradientTests;
import gecv.alg.detect.intensity.GenericCornerIntensityTests;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.alg.misc.ImageTestingOps;
import gecv.core.image.border.BorderIndex1D_Extend;
import gecv.core.image.border.ImageBorder1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestKltCorner_I16 {
	int width = 15;
	int height = 15;

	@Test
	public void genericTests() {
		GenericCornerIntensityTests generic = new GenericCornerIntensityGradientTests(){

			@Override
			public ImageFloat32 computeIntensity() {
				KltCorner_S16 alg = new KltCorner_S16(1);
				alg.process(derivX_I16,derivY_I16);
				return alg.getIntensity();
			}
		};

		generic.performAllTests();
	}

	/**
	 * Creates a random image and looks for corners in it.  Sees if the naive
	 * and fast algorithm produce exactly the same results.
	 */
	@Test
	public void compareToNaive() {
		ImageUInt8 img = new ImageUInt8(width, height);
		ImageTestingOps.randomize(img, new Random(0xfeed), 0, 100);

		ImageSInt16 derivX = new ImageSInt16(img.getWidth(), img.getHeight());
		ImageSInt16 derivY = new ImageSInt16(img.getWidth(), img.getHeight());

		GradientSobel.process(img, derivX, derivY, new ImageBorder1D_I32(BorderIndex1D_Extend.class));

		GecvTesting.checkSubImage(this, "compareToNaive", true, derivX, derivY);
	}

	public void compareToNaive(ImageSInt16 derivX, ImageSInt16 derivY) {
		SsdCornerNaive_S16 naive = new SsdCornerNaive_S16(width, height, 3);
		naive.process(derivX, derivY);

		KltCorner_S16 fast = new KltCorner_S16(3);
		fast.process(derivX, derivY);

		GecvTesting.assertEquals(naive.getIntensity(), fast.getIntensity());
	}
}
