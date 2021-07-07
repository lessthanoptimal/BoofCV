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

package boofcv.alg.misc;

import boofcv.BoofTesting;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestGImageStatistics extends BaseGClassChecksInMisc {

	Class[] bandTypes = new Class[]{GrayU8.class, GrayS8.class, GrayU16.class, GrayF32.class};

	public TestGImageStatistics() {
		super(GImageStatistics.class, ImageStatistics.class);
	}

	@Test void compareToPixelMath() {
		performTests(10);
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class<?>[] param = validation.getParameterTypes();
		String name = candidate.getName();

		ImageBase inputA = GeneralizedImageOps.createImage((Class)param[0], width, height, numBands);
		ImageBase inputB = null;

		Object[][] ret = new Object[1][param.length];

		switch (name) {
			case "maxAbs" -> ret[0][0] = inputA;
			case "max" -> ret[0][0] = inputA;
			case "min" -> ret[0][0] = inputA;
			case "sum" -> ret[0][0] = inputA;
			case "mean" -> ret[0][0] = inputA;
			case "variance" -> {
				ret[0][0] = inputA;
				ret[0][1] = 3;
			}
			case "meanDiffSq" -> {
				inputB = GeneralizedImageOps.createImage((Class)param[1], width, height, numBands);
				ret[0][0] = inputA;
				ret[0][1] = inputB;
			}
			case "meanDiffAbs" -> {
				inputB = GeneralizedImageOps.createImage((Class)param[1], width, height, numBands);
				ret[0][0] = inputA;
				ret[0][1] = inputB;
			}
			case "histogram" -> {
				int histogramSize = 10;
				if (inputA.getImageType().getDataType().isSigned())
					histogramSize += 11;
				ret[0][0] = inputA;
				ret[0][1] = -10;
				ret[0][2] = new int[histogramSize];
			}
			case "histogramScaled" -> {
				int histogramSize = 6;
				ret[0][0] = inputA;
				ret[0][1] = -10;
				ret[0][2] = 10;
				ret[0][3] = new int[histogramSize];
			}
		}

		fillRandom(inputA);
		fillRandom(inputB);

		return ret;
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		if (targetResult != null) {
			double valueT = ((Number)targetResult).doubleValue();
			double valueV = ((Number)validationResult).doubleValue();

			assertTrue(valueT == valueV);
		}
	}

	@Test
	void maxAbs_planar() {
		for (Class type : bandTypes) {
			Planar image = new Planar(type, 200, 180, 3);
			if (image.getImageType().getDataType().isSigned())
				GImageMiscOps.fillUniform(image, rand, -100, 100);
			else
				GImageMiscOps.fillUniform(image, rand, 0, 200);

			double found = GImageStatistics.maxAbs(image);

			double expected = -1;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					for (int k = 0; k < image.getNumBands(); k++) {
						double v = Math.abs(GeneralizedImageOps.get(image, x, y, k));
						if (v > expected) {
							expected = v;
						}
					}
				}
			}
			assertEquals(expected, found, UtilEjml.TEST_F64);
		}
	}

	@Test
	void max_planar() {
		for (Class type : bandTypes) {
			Planar image = new Planar(type, 200, 180, 3);
			if (image.getImageType().getDataType().isSigned())
				GImageMiscOps.fillUniform(image, rand, -100, 100);
			else
				GImageMiscOps.fillUniform(image, rand, 0, 200);

			double found = GImageStatistics.max(image);

			double expected = -Double.MAX_VALUE;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					for (int k = 0; k < image.getNumBands(); k++) {
						double v = GeneralizedImageOps.get(image, x, y, k);
						if (v > expected) {
							expected = v;
						}
					}
				}
			}
			assertEquals(expected, found, UtilEjml.TEST_F64);
		}
	}

	@Test
	void min_planar() {
		for (Class type : bandTypes) {
			Planar image = new Planar(type, 200, 180, 3);
			if (image.getImageType().getDataType().isSigned())
				GImageMiscOps.fillUniform(image, rand, -100, 100);
			else
				GImageMiscOps.fillUniform(image, rand, 0, 200);

			double found = GImageStatistics.min(image);

			double expected = Double.MAX_VALUE;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					for (int k = 0; k < image.getNumBands(); k++) {
						double v = GeneralizedImageOps.get(image, x, y, k);
						if (v < expected) {
							expected = v;
						}
					}
				}
			}
			assertEquals(expected, found, UtilEjml.TEST_F64);
		}
	}

	@Test
	void sum_planar() {
		for (Class type : bandTypes) {
			Planar image = new Planar(type, 200, 180, 3);
			if (image.getImageType().getDataType().isSigned())
				GImageMiscOps.fillUniform(image, rand, -100, 100);
			else
				GImageMiscOps.fillUniform(image, rand, 0, 200);

			double found = GImageStatistics.sum(image);

			double expected = 0;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					for (int k = 0; k < image.getNumBands(); k++) {
						expected += GeneralizedImageOps.get(image, x, y, k);
					}
				}
			}


			double tol = BoofTesting.tolerance(image.getBand(0).getDataType());
			assertEquals(expected, found, Math.abs(found)*tol, type.getSimpleName());
		}
	}

	@Test
	void mean_planar() {
		for (Class type : bandTypes) {
			Planar image = new Planar(type, 200, 180, 3);
			if (image.getImageType().getDataType().isSigned())
				GImageMiscOps.fillUniform(image, rand, -100, 100);
			else
				GImageMiscOps.fillUniform(image, rand, 0, 200);

			double found = GImageStatistics.mean(image);

			double expected = 0;
			for (int y = 0; y < image.height; y++) {
				for (int x = 0; x < image.width; x++) {
					for (int k = 0; k < image.getNumBands(); k++) {
						expected += GeneralizedImageOps.get(image, x, y, k);
					}
				}
			}
			expected /= (image.width*image.height*image.getNumBands());

			double tol = BoofTesting.tolerance(image.getBand(0).getDataType());
			assertEquals(expected, found, 10*tol);
		}
	}

	@Test
	void meanDiffSq_planar() {
		for (Class type : bandTypes) {

			Planar imageA = new Planar(type, 200, 180, 3);
			Planar imageB = new Planar(type, 200, 180, 3);

			if (imageA.getImageType().getDataType().isSigned()) {
				GImageMiscOps.fillUniform(imageA, rand, -100, 100);
				GImageMiscOps.fillUniform(imageB, rand, -100, 100);
			} else {
				GImageMiscOps.fillUniform(imageA, rand, 0, 200);
				GImageMiscOps.fillUniform(imageB, rand, 0, 200);
			}

			double found = GImageStatistics.meanDiffSq(imageA, imageB);

			double expected = 0;
			for (int y = 0; y < imageA.height; y++) {
				for (int x = 0; x < imageA.width; x++) {
					for (int k = 0; k < imageA.getNumBands(); k++) {
						double va = GeneralizedImageOps.get(imageA, x, y, k);
						double vb = GeneralizedImageOps.get(imageB, x, y, k);

						expected += (va - vb)*(va - vb);
					}
				}
			}
			expected /= (imageA.width*imageA.height*imageA.getNumBands());

			assertEquals(expected, found, Math.abs(found)*UtilEjml.TEST_F64_SQ);
		}
	}

	@Test
	void meanDiffAbs_planar() {
		for (Class type : bandTypes) {

			Planar imageA = new Planar(type, 200, 180, 3);
			Planar imageB = new Planar(type, 200, 180, 3);

			if (imageA.getImageType().getDataType().isSigned()) {
				GImageMiscOps.fillUniform(imageA, rand, -100, 100);
				GImageMiscOps.fillUniform(imageB, rand, -100, 100);
			} else {
				GImageMiscOps.fillUniform(imageA, rand, 0, 200);
				GImageMiscOps.fillUniform(imageB, rand, 0, 200);
			}

			double found = GImageStatistics.meanDiffAbs(imageA, imageB);

			double expected = 0;
			for (int y = 0; y < imageA.height; y++) {
				for (int x = 0; x < imageA.width; x++) {
					for (int k = 0; k < imageA.getNumBands(); k++) {
						double va = GeneralizedImageOps.get(imageA, x, y, k);
						double vb = GeneralizedImageOps.get(imageB, x, y, k);

						expected += Math.abs(va - vb);
					}
				}
			}
			expected /= (imageA.width*imageA.height*imageA.getNumBands());

			assertEquals(expected, found, Math.abs(found)*UtilEjml.TEST_F64_SQ);
		}
	}
}
