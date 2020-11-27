/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.census.CensusNaive;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class TestImplCensusTransformInner extends BoofStandardJUnit {
	final private int w = 25, h = 40;

	@Test
	void checkAll() {
		int numExpected = 3*4;
		Method[] methods = ImplCensusTransformInner.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if (!isTestMethod(m))
				continue;
			try {
//				System.out.println(m.getName());
				if (m.getName().compareTo("dense3x3") == 0) {
					dense3x3(m);
				} else if (m.getName().compareTo("dense5x5") == 0) {
					dense5x5(m);
				} else if (m.getName().compareTo("sample_S64") == 0) {
					sample_S64(m);
				} else if (m.getName().compareTo("sample_IU16") == 0) {
					sample_IU16(m);
				} else {
					throw new RuntimeException("Unknown function. " + m.getName());
				}
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if (numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found " + numFound + "  expected " + numExpected);
	}

	private boolean isTestMethod( Method m ) {
		Class<?> param[] = m.getParameterTypes();

		if (param.length < 1)
			return false;

		for (int i = 0; i < param.length; i++) {
			if (ImageBase.class.isAssignableFrom(param[i]))
				return true;
		}
		return false;
	}

	void dense3x3( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, w, h);
		GrayU8 found = new GrayU8(w, h);
		GrayU8 expected = new GrayU8(w, h);

		fillUniform(input);

		m.invoke(null, input, found);
		CensusNaive.region3x3(input, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, 1, 1, 1, 1, false);
	}

	void dense5x5( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, w, h);
		GrayS32 found = new GrayS32(w, h);
		GrayS32 expected = new GrayS32(w, h);

		fillUniform(input);

		m.invoke(null, input, found);
		CensusNaive.region5x5(input, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, 2, 2, 2, 2, false);
	}

	void sample_S64( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, w, h);
		GrayS64 found = new GrayS64(w, h);
		GrayS64 expected = new GrayS64(w, h);

		fillUniform(input);

		int r = 3;
		DogArray<Point2D_I32> samples = createSamples(r);
		DogArray_I32 indexes = samplesToIndexes(input, samples);

		m.invoke(null, input, r, indexes, found);
		CensusNaive.sample(input, samples, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, r, r, r, r, false);
	}

	void sample_IU16( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType, w, h);
		InterleavedU16 found = new InterleavedU16(w, h, 2);
		InterleavedU16 expected = new InterleavedU16(w, h, 2);

		fillUniform(input);

		DogArray<Point2D_I32> samples5x5 = createSamples(2);
		DogArray_I32 indexes = samplesToIndexes(input, samples5x5);

		m.invoke(null, input, 2, indexes, found);
		CensusNaive.sample(input, samples5x5, expected);

		BoofTesting.assertEqualsInner(expected, found, 0, 2, 2, 2, 2, false);
	}

	private void fillUniform( ImageGray input ) {
		if (input.getDataType().isInteger()) {
			int maxValue = (int)input.getDataType().getMaxValue();
			GImageMiscOps.fillUniform(input, rand, 0, maxValue);
		} else {
			GImageMiscOps.fillUniform(input, rand, -5, 5);
		}
	}

	public static DogArray_I32 samplesToIndexes( ImageGray input, DogArray<Point2D_I32> samples ) {
		DogArray_I32 indexes = new DogArray_I32();
		for (int i = 0; i < samples.size; i++) {
			Point2D_I32 p = samples.get(i);
			indexes.add(p.y*input.stride + p.x);
		}
		return indexes;
	}

	public static DogArray<Point2D_I32> createSamples( int r ) {
		DogArray<Point2D_I32> samples = new DogArray<>(Point2D_I32::new);
		for (int y = -r; y <= r; y++) {
			for (int x = -r; x <= r; x++) {
				samples.grow().setTo(x, y);
			}
		}
		return samples;
	}
}
