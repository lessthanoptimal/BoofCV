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

package boofcv.alg.filter.blur.impl;

import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestImplMedianSortEdgeNaive extends CompareEquivalentFunctions {

	int width = 23;
	int height = 20;

	int radiusX = 2;
	int radiusY = 3;

	public TestImplMedianSortEdgeNaive() {
		super(ImplMedianSortEdgeNaive.class, ImplMedianSortNaive.class);
	}

	@Test void compareToFullNaive() {

		performTests(2);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		return m.getName().equals("process");
	}

	@Override
	protected boolean isEquivalent( Method candidate, Method validation ) {
		Class<?>[] c = candidate.getParameterTypes();
		Class<?>[] v = validation.getParameterTypes();

		return c.length == v.length && c[0] == v[1];
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {

		Class[] c = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(c[1], width, height);


		Object[][] ret = new Object[1][c.length];
		ret[0][0] = input;
		ret[0][1] = output;
		ret[0][2] = radiusX;
		ret[0][3] = radiusY;
		ret[0][4] = null;

		return ret;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		return targetParam;
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {

		GImageGray found = FactoryGImageGray.wrap((ImageGray)targetParam[1]);
		GImageGray expected = FactoryGImageGray.wrap((ImageGray)validationParam[1]);


		for (int y = 0; y < height; y++) {
			if (y > radiusY || y < height - radiusY)
				continue;

			for (int x = 0; x < width; x++) {
				if (x > radiusX || x < width - radiusX)
					continue;
				assertEquals(found.get(x, y).intValue(), expected.get(x, y).intValue());
			}
		}
	}
}
