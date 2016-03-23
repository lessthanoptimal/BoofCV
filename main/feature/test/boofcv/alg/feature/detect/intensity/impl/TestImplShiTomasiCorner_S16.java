/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.feature.detect.intensity.GenericCornerIntensityGradientTests;
import boofcv.alg.feature.detect.intensity.GenericCornerIntensityTests;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImplShiTomasiCorner_S16 {
	int width = 15;
	int height = 20;

	@Test
	public void genericTests() {
		GenericCornerIntensityTests generic = new GenericCornerIntensityGradientTests(){

			@Override
			public void computeIntensity( GrayF32 intensity ) {
				ImplShiTomasiCorner_S16 alg = new ImplShiTomasiCorner_S16(1);
				alg.process(derivX_I16,derivY_I16,intensity);
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
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

		GrayS16 derivX = new GrayS16(img.getWidth(), img.getHeight());
		GrayS16 derivY = new GrayS16(img.getWidth(), img.getHeight());

		GradientSobel.process(img, derivX, derivY, new ImageBorder1D_S32(BorderIndex1D_Extend.class));

		BoofTesting.checkSubImage(this, "compareToNaive", true, derivX, derivY);
	}

	public void compareToNaive(GrayS16 derivX, GrayS16 derivY) {
		GrayF32 expected = new GrayF32(derivX.width,derivX.height);
		GrayF32 found = new GrayF32(derivX.width,derivX.height);

		ImplSsdCornerNaive naive = new ImplSsdCornerNaive(width, height, 3, false);
		naive.process(derivX, derivY,expected);

		ImplShiTomasiCorner_S16 fast = new ImplShiTomasiCorner_S16(3);
		fast.process(derivX, derivY,found);

		BoofTesting.assertEquals(expected, found,1e-4);
	}

	@Test
	public void checkOverflow() {
		ImplShiTomasiCorner_S16 detector = new ImplShiTomasiCorner_S16(1);

		detector.totalXX = (1<<18)+10;
		detector.totalYY = (1<<20)+50;
		detector.totalXY = (1<<16)+5;

		assertTrue(detector.computeIntensity() > 0);
	}
}
