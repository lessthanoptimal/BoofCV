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
import boofcv.core.image.ConvertImage;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.ImageBorder1D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplShiTomasiCornerWeighted_F32 {

	int width = 15;
	int height = 15;

	@Test
	public void genericTests() {
		GenericCornerIntensityTests generic = new GenericCornerIntensityGradientTests(){

			@Override
			public void computeIntensity( GrayF32 intensity ) {
				ImplShiTomasiCornerWeighted_F32 alg = new ImplShiTomasiCornerWeighted_F32(1);
				alg.process(derivX_F32,derivY_F32,intensity);
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
		GrayU8 img = new GrayU8(width, height);
		ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

		GrayS16 derivX_I = new GrayS16(img.getWidth(), img.getHeight());
		GrayS16 derivY_I = new GrayS16(img.getWidth(), img.getHeight());

		GradientSobel.process(img, derivX_I, derivY_I, new ImageBorder1D_S32(BorderIndex1D_Extend.class));

		GrayF32 derivX_F = ConvertImage.convert(derivX_I, (GrayF32) null);
		GrayF32 derivY_F = ConvertImage.convert(derivY_I, (GrayF32)null);

		BoofTesting.checkSubImage(this, "compareToNaive", true, derivX_F, derivY_F);
	}

	public void compareToNaive(GrayF32 derivX_F, GrayF32 derivY_F) {
		GrayF32 expected = new GrayF32(derivX_F.width,derivX_F.height);
		GrayF32 found = new GrayF32(derivX_F.width,derivX_F.height);

		ImplSsdCornerNaive<GrayF32> ssd_I = new ImplSsdCornerNaive<>(width, height, 3, true);
		ssd_I.process(derivX_F, derivY_F,expected);

		ImplShiTomasiCornerWeighted_F32 ssd_F = new ImplShiTomasiCornerWeighted_F32( 3);
		ssd_F.process(derivX_F, derivY_F,found);

		BoofTesting.assertEqualsInner(expected, found, 1f, 3, 3, true);
	}
}
