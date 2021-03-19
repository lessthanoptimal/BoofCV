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

package boofcv.alg.filter.binary.impl;

import boofcv.BoofTesting;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("rawtypes")
public class TestImplThresholdImageOps extends BoofStandardJUnit {
	int width = 20;
	int height = 30;

	Method[] list = ImplThresholdImageOps.class.getMethods();

	@Test
	void localMean() {
		int total = 0;

		for (Method m : list) {
			if (!m.getName().equals("localMean"))
				continue;

			Class[] param = m.getParameterTypes();

			ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			GrayU8 output = new GrayU8(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this, "performLocalMean", true, m, input, output);
			total++;
		}

		assertEquals(3, total);
	}

	public void performLocalMean( Method m, ImageGray input, GrayU8 output )
			throws InvocationTargetException, IllegalAccessException {
		ImageGray storage1 = (ImageGray)input.createSameShape();
		ImageGray storage2 = (ImageGray)input.createSameShape();

		GrayU8 expected = new GrayU8(output.width, output.height);

		for (int radius = 1; radius <= 5; radius++) {
			ConfigLength width = ConfigLength.fixed(radius*2 + 1);

			for (int indexScale = 0; indexScale < 4; indexScale++) {
				float scale = (float)(0.8 + 0.4*(indexScale/3.0));
				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected, rand, 0, 200);
				m.invoke(null, input, output, width, scale, true, storage1, storage2, null);
				naiveLocalMean(input, expected, radius, scale, true);

				BoofTesting.assertEquals(expected, output, 0);

				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected, rand, 0, 200);
				m.invoke(null, input, output, width, scale, false, storage1, storage2, null);
				naiveLocalMean(input, expected, radius, scale, false);

				BoofTesting.assertEquals(expected, output, 0);
			}
		}
	}

	void naiveLocalMean( ImageGray input, GrayU8 output,
						 int radius, double scale, boolean down ) {
		ImageGray blur;
		boolean isInt;
		if (input instanceof GrayU8) {
			isInt = true;
			blur = BlurImageOps.mean((GrayU8)input, null, radius, null, null);
		} else if (input instanceof GrayU16) {
			isInt = true;
			blur = BlurImageOps.mean((GrayU16)input, null, radius, null, null);
		} else {
			isInt = false;
			blur = BlurImageOps.mean((GrayF32)input, null, radius, null, null);
		}

		float fscale = (float)scale;

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {

				double threshold = GeneralizedImageOps.get(blur, x, y);
				double v = GeneralizedImageOps.get(input, x, y);

				boolean one;
				if (down) {
					if (isInt) {
						one = (int)v <= ((int)threshold)*fscale;
					} else {
						one = v <= threshold*fscale;
					}
				} else {
					if (isInt) {
						one = ((int)v)*fscale > (int)threshold;
					} else {
						one = v*fscale > threshold;
					}
				}

				if (one) {
					output.set(x, y, 1);
				} else {
					output.set(x, y, 0);
				}
			}
		}
	}

	@Test
	void localGaussian() {
		int total = 0;

		for (Method m : list) {
			if (!m.getName().equals("localGaussian"))
				continue;

			Class[] param = m.getParameterTypes();

			ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			GrayU8 output = new GrayU8(width, height);

			GImageMiscOps.fillUniform(input, rand, 0, 200);

			BoofTesting.checkSubImage(this, "performLocalGaussian", true, m, input, output);
			total++;
		}

		assertEquals(3, total);
	}

	public void performLocalGaussian( Method m, ImageGray input, GrayU8 output )
			throws InvocationTargetException, IllegalAccessException {
		ImageGray storage1 = (ImageGray)input.createSameShape();
		ImageGray storage2 = (ImageGray)input.createSameShape();

		GrayU8 expected = new GrayU8(output.width, output.height);

		for (int radius = 1; radius <= 5; radius++) {
			for (int indexScale = 0; indexScale < 4; indexScale++) {
				ConfigLength width = ConfigLength.fixed(radius*2 + 1);
				float scale = (float)(0.8 + 0.4*(indexScale/3.0));

				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected, rand, 0, 200);
				m.invoke(null, input, output, width, scale, true, storage1, storage2);
				naiveLocalGaussian(input, expected, radius, scale, true);

				BoofTesting.assertEquals(expected, output, 0);

				ImageMiscOps.fillUniform(output, rand, 0, 200);
				ImageMiscOps.fillUniform(expected, rand, 0, 200);
				m.invoke(null, input, output, width, scale, false, storage1, storage2);
				naiveLocalGaussian(input, expected, radius, scale, false);

				BoofTesting.assertEquals(expected, output, 0);
			}
		}
	}

	void naiveLocalGaussian( ImageGray input, GrayU8 output,
							 int radius, double scale, boolean down ) {

		ImageGray blur;
		boolean isInt;
		if (input instanceof GrayU8) {
			isInt = true;
			blur = BlurImageOps.gaussian((GrayU8)input, null, -1, radius, null);
		} else if (input instanceof GrayU16) {
			isInt = true;
			blur = BlurImageOps.gaussian((GrayU16)input, null, -1, radius, null);
		} else {
			isInt = false;
			blur = BlurImageOps.gaussian((GrayF32)input, null, -1, radius, null);
		}

		float fscale = (float)scale;

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				double threshold = GeneralizedImageOps.get(blur, x, y);
				double v = GeneralizedImageOps.get(input, x, y);

				boolean one;
				if (down) {
					if (isInt) {
						one = (int)v <= ((int)threshold)*fscale;
					} else {
						one = v <= threshold*fscale;
					}
				} else {
					if (isInt) {
						one = ((int)v)*fscale > (int)threshold;
					} else {
						one = v*fscale > threshold;
					}
				}

				if (one) {
					output.set(x, y, 1);
				} else {
					output.set(x, y, 0);
				}
			}
		}
	}

	@Test
	void threshold() {

		int total = 0;

		for (Method m : list) {
			if (!m.getName().equals("threshold"))
				continue;

			Class[] param = m.getParameterTypes();

			ImageGray input = GeneralizedImageOps.createSingleBand(param[0], width, height);
			GrayU8 output = new GrayU8(width, height);

			GImageGray a = FactoryGImageGray.wrap(input);
			for (int y = 0; y < input.height; y++) {
				for (int x = 0; x < input.width; x++) {
					a.set(x, y, x);
				}
			}

			BoofTesting.checkSubImage(this, "performThreshold", true, m, input, output);
			total++;
		}

		assertEquals(6, total);
	}

	public void performThreshold( Method m, ImageGray input, GrayU8 output )
			throws InvocationTargetException, IllegalAccessException {
		int areaBelow = 8*input.height;
		int areaAbove = input.width*input.height - areaBelow;


		m.invoke(null, input, output, 7, true);
		assertEquals(areaBelow, GImageStatistics.sum(output), 1e-4);

		m.invoke(null, input, output, 7, false);
		assertEquals(areaAbove, GImageStatistics.sum(output), 1e-4);
	}
}
