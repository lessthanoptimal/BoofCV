/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.misc;

import boofcv.BoofTesting;
import boofcv.alg.filter.misc.impl.ImplAverageDownSample;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings({"rawtypes", "unchecked"})
public class TestAverageDownSampleOps extends BoofStandardJUnit {

	// The original image is too large and needs to be down sampled
	@Test void downMaxPixels_Larger() {
		GrayF32 full = new GrayF32(30, 40);
		GrayF32 scaled = new GrayF32(1, 1);

		assertSame(scaled, AverageDownSampleOps.downMaxPixels(full, scaled, 6*8));
		assertEquals(6, scaled.width);
		assertEquals(8, scaled.height);
	}

	// The original image is too small and should not be modified
	@Test void downMaxPixels_Smaller() {
		GrayF32 full = new GrayF32(30, 40);
		GrayF32 scaled = new GrayF32(1, 1);

		assertSame(full, AverageDownSampleOps.downMaxPixels(full, scaled, 100*100));
	}

	/**
	 * Down sample with just two inputs. Compare to results from raw implementation.
	 */
	@Test void down_2inputs() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Class[] input = new Class[]{GrayU8.class, GrayU16.class, GrayF32.class, GrayF64.class};
		Class[] middle = new Class[]{GrayF32.class, GrayF32.class, GrayF32.class, GrayF64.class};

		for (int i = 0; i < input.length; i++) {
			ImageGray in = GeneralizedImageOps.createSingleBand(input[i], 17, 14);
			ImageGray mid = GeneralizedImageOps.createSingleBand(middle[i], 3, 14);
			ImageGray found = GeneralizedImageOps.createSingleBand(input[i], 3, 4);
			ImageGray expected = GeneralizedImageOps.createSingleBand(input[i], 3, 4);

			GImageMiscOps.fillUniform(in, rand, 0, 100);

			Method horizontal = ImplAverageDownSample.class.getDeclaredMethod("horizontal", input[i], middle[i]);
			Method vertical = BoofTesting.findMethod(ImplAverageDownSample.class, "vertical", middle[i], input[i]);

			horizontal.invoke(null, in, mid);
			vertical.invoke(null, mid, expected);

			AverageDownSampleOps.down(in, found);

			BoofTesting.assertEquals(expected, found, 1e-4);
		}
	}

	/** Make sure it adjusts the number of bands in the output image */
	@Test void down_Planar_NumBands() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class, 400, 300, 3);
		Planar<GrayU8> output = new Planar<>(GrayU8.class, 200, 140, 1);

		AverageDownSampleOps.down(input, output);
		assertEquals(3, output.getNumBands());
	}
}
