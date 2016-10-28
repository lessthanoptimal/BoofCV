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

package boofcv.alg.misc;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
// TODO add unit tests for Planar images
public class TestGImageStatistics extends BaseGClassChecksInMisc {

	public TestGImageStatistics() {
		super(GImageStatistics.class, ImageStatistics.class);
	}

	@Test
	public void compareToPixelMath() {
		performTests(9);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> param[] = validation.getParameterTypes();
		String name = candidate.getName();

		ImageBase inputA = GeneralizedImageOps.createImage((Class)param[0], width, height, numBands);
		ImageBase inputB = null;

		Object[][] ret = new Object[1][param.length];

		if( name.equals("maxAbs")) {
			ret[0][0] = inputA;
		} else if( name.equals("max")) {
			ret[0][0] = inputA;
		} else if( name.equals("min")) {
			ret[0][0] = inputA;
		} else if( name.equals("sum")) {
			ret[0][0] = inputA;
		} else if( name.equals("mean")) {
			ret[0][0] = inputA;
		} else if( name.equals("variance")) {
			ret[0][0] = inputA;
			ret[0][1] = 3;
		} else if( name.equals("meanDiffSq")) {
			inputB = GeneralizedImageOps.createImage((Class)param[1], width, height, numBands);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
		} else if( name.equals("meanDiffAbs")) {
			inputB = GeneralizedImageOps.createImage((Class)param[1], width, height, numBands);
			ret[0][0] = inputA;
			ret[0][1] = inputB;
		} else if( name.equals("histogram")) {
			int histogramSize = 10;
			if( inputA.getImageType().getDataType().isSigned() )
				histogramSize += 11;
			ret[0][0] = inputA;
			ret[0][1] = -10;
			ret[0][2] = new int[histogramSize];
		}

		fillRandom(inputA);
		fillRandom(inputB);

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		if( targetResult != null ) {
			double valueT = ((Number) targetResult).doubleValue();
			double valueV = ((Number) validationResult).doubleValue();

			assertTrue(valueT == valueV);
		}
	}
}
