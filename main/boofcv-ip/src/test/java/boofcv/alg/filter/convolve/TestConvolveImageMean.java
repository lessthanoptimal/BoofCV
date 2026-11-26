/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve;

import boofcv.BoofTesting;
import boofcv.alg.filter.convolve.noborder.TestImplConvolveBox;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestConvolveImageMean extends CompareEquivalentFunctions {

	Random rand = new Random(0xFF);

	static int width = 10;
	static int height = 12;
	static int offset1 = 2;
	static int offset2 = 6;
	static int length1 = 5;
	static int length2 = 13; // kernel will be larger than the image

	public TestConvolveImageMean() {
		super(ConvolveImageMean.class, ConvolveImageNormalized.class);
	}

	@Test void compareToStandard() {
		performTests(20);
	}

	/// A null border isn't processed. Inside the image it's the same as the functions without a border
	@Test void nullBorder() throws Exception {
		int count = 0;
		for (Method m : ConvolveImageMean.class.getMethods()) {
			Class<?>[] params = m.getParameterTypes();
			if (params.length != 5 || !ImageGray.class.isAssignableFrom(params[0]))
				continue;
			Method noBorder = ConvolveImageMean.class.getMethod(m.getName(), params[0], params[1], int.class, int.class);
			ImageGray input = GeneralizedImageOps.createSingleBand((Class)params[0], width, height);
			GImageMiscOps.fillUniform(input, rand, 0, 100);
			ImageGray expected = GeneralizedImageOps.createSingleBand((Class)params[1], width, height);
			ImageGray found = GeneralizedImageOps.createSingleBand((Class)params[1], width, height);
			GImageMiscOps.fill(found, 3);

			noBorder.invoke(null, input, expected, offset1, length1);
			m.invoke(null, input, found, offset1, length1, null);

			boolean horizontal = m.getName().equals("horizontal");
			int offsetR = length1 - offset1 - 1;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int i = horizontal ? x : y, size = horizontal ? width : height;
					double value = i >= offset1 && i < size - offsetR ? GeneralizedImageOps.get(expected, x, y) : 3;
					assertEquals(value, GeneralizedImageOps.get(found, x, y), 0.0);
				}
			}
			count++;
		}
		assertEquals(10, count);
	}

	@Disabled("Needs a way to observe which implementation the concurrency system ran")
	@Test void switchSingleToConcurrent() {
		// TODO verify CONCURRENT_MIN_PIXELS_HORIZONTAL/VERTICAL and USE_CONCURRENT select ImplConvolveMean_MT
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		Class<?> params[] = m.getParameterTypes();

		if (params.length != 4 && params.length != 5)
			return false;

		return ImageGray.class.isAssignableFrom(params[0]);
	}

	@Override
	protected boolean isEquivalent( Method validation, Method target ) {
		Class<?>[] v = validation.getParameterTypes();
		Class<?>[] c = target.getParameterTypes();

		if (!target.getName().equals(validation.getName()))
			return false;

		if (c[0] != v[1] || c[1] != v[2])
			return false;

		// functions with an ImageBorder (5 arguments) against ConvolveImageNormalized with one (4 arguments)
		int mean = Math.max(v.length, c.length), normalized = Math.min(v.length, c.length);
		return mean == 5 ? normalized == 4 : normalized == 3;
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {

		Class[] c = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(c[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		ImageBorder border = FactoryImageBorder.generic(BorderType.REFLECT, input.getImageType());

		Object[][] ret = new Object[2][];
		if (c.length == 4) {
			ret[0] = new Object[]{input, output, offset1, length1};
			ret[1] = new Object[]{input, output, offset2, length2};
		} else {
			ret[0] = new Object[]{input, output, offset1, length1, border};
			ret[1] = new Object[]{input, output, offset2, length2, border};
		}

		return ret;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Class<?>[] params = m.getParameterTypes();
		Object kernel = TestImplConvolveBox.tableKernel((ImageGray)targetParam[0], (Integer)targetParam[2],
				(Integer)targetParam[3], true);

		ImageGray output = (ImageGray)((ImageGray)targetParam[1]).clone();

		if (ImageBorder.class.isAssignableFrom(params[params.length - 1])) {
			return new Object[]{kernel, targetParam[0], output, targetParam[4]};
		} else {
			return new Object[]{kernel, targetParam[0], output};
		}
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		ImageGray expected = (ImageGray)validationParam[2];
		ImageGray found = (ImageGray)targetParam[1];
		try {
			BoofTesting.assertEquals(expected, found, 1e-4);
		} catch (Exception e) {
			expected.print();
			System.out.println("----");
			found.print();
			throw e;
		}
	}
}
