/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.ImageBorder1D_I32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestImplShiTomasiCorner_F32 {

	int width = 15;
	int height = 15;

	@Test
	public void genericTests() {
		GenericCornerIntensityTests generic = new GenericCornerIntensityGradientTests(){

			@Override
			public void computeIntensity( ImageFloat32 intensity ) {
				ImplShiTomasiCorner_F32 alg = new ImplShiTomasiCorner_F32(1);
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
		ImageUInt8 img = new ImageUInt8(width, height);
		ImageMiscOps.fillUniform(img, new Random(0xfeed), 0, 100);

		ImageSInt16 derivX_I = new ImageSInt16(img.getWidth(), img.getHeight());
		ImageSInt16 derivY_I = new ImageSInt16(img.getWidth(), img.getHeight());

		GradientSobel.process(img, derivX_I, derivY_I, new ImageBorder1D_I32(BorderIndex1D_Extend.class));

		ImageFloat32 derivX_F = ConvertImage.convert(derivX_I, (ImageFloat32)null);
		ImageFloat32 derivY_F = ConvertImage.convert(derivY_I, (ImageFloat32)null);

		BoofTesting.checkSubImage(this, "compareToNaive", true, derivX_F, derivY_F);
	}

	public void compareToNaive( ImageFloat32 derivX_F, ImageFloat32 derivY_F) {
		ImageFloat32 expected = new ImageFloat32(derivX_F.width,derivX_F.height);
		ImageFloat32 found = new ImageFloat32(derivX_F.width,derivX_F.height);

		ImplSsdCornerNaive<ImageFloat32> ssd_I = new ImplSsdCornerNaive<ImageFloat32>(width, height, 3, false);
		ssd_I.process(derivX_F, derivY_F,expected);

		ImplShiTomasiCorner_F32 ssd_F = new ImplShiTomasiCorner_F32( 3);
		ssd_F.process(derivX_F, derivY_F,found);

		BoofTesting.assertEquals(expected, found, 1f);
	}
}
