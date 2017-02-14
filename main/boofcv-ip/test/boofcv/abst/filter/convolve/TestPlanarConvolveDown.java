/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.convolve;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.convolve.FactoryConvolveDown;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestPlanarConvolveDown {

	Random rand = new Random(234);

	@Test
	public void compareToSingleBand() {
		Kernel1D_S32 kernel = FactoryKernelGaussian.gaussian1D(GrayU8.class, -1, 3);
		ConvolveDown<GrayU8,GrayU8> downU8 = FactoryConvolveDown.convolveSB(
				kernel, BorderType.NORMALIZED, true,2, GrayU8.class, GrayU8.class);

		Planar<GrayU8> original = new Planar<>(GrayU8.class,20,30,3);
		Planar<GrayU8> found = new Planar<>(GrayU8.class,10,30,3);

		GImageMiscOps.fillUniform(original, rand, 0, 100);

		GrayU8[] expected = new GrayU8[original.getNumBands()];
		for (int i = 0; i < expected.length; i++) {
			expected[i] = new GrayU8(found.width, found.height);

			downU8.process(original.getBand(i), expected[i]);
		}

		PlanarConvolveDown<GrayU8,GrayU8> alg = new PlanarConvolveDown<>(downU8, original.getNumBands());

		alg.process(original,found);
		for (int i = 0; i < expected.length; i++) {
			BoofTesting.assertEquals(expected[i], found.getBand(i), 1e-4);
		}
	}
}
