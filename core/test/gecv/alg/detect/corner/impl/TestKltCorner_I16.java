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
import gecv.alg.misc.ImageTestingOps;
import gecv.alg.filter.derivative.GradientSobel;
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
				KltCorner_I16 alg = new KltCorner_I16(width,height,1);
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

		GradientSobel.process(img, derivX, derivY, true);

		GecvTesting.checkSubImage(this, "compareToNaive", true, derivX, derivY);
	}

	public void compareToNaive(ImageSInt16 derivX, ImageSInt16 derivY) {
		SsdCornerNaive_I16 naive = new SsdCornerNaive_I16(width, height, 3);
		naive.process(derivX, derivY);

		KltCorner_I16 fast = new KltCorner_I16(width, height, 3);
		fast.process(derivX, derivY);

		GecvTesting.assertEquals(naive.getIntensity(), fast.getIntensity());
	}
}
