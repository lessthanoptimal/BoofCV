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

package boofcv.alg.filter.blur.impl;

import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplMedianSortEdgeNaive extends CompareEquivalentFunctions {

	Random rand = new Random(234234);
	int width = 20;
	int height = 20;

	int radius = 2;

	public TestImplMedianSortEdgeNaive() {
		super(ImplMedianSortEdgeNaive.class, ImplMedianSortNaive.class);
	}

	@Test
	public void compareToFullNaive() {

		performTests(2);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		return m.getName().equals("process");
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method validation) {
		Class<?> c[] = candidate.getParameterTypes();
		Class<?> v[] = validation.getParameterTypes();

		return c.length == v.length && c[0] == v[1];
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class c[] = candidate.getParameterTypes();

		ImageSingleBand input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageSingleBand output = GeneralizedImageOps.createSingleBand(c[1], width, height);


		Object[][] ret = new Object[1][ c.length ];
		ret[0][0] = input;
		ret[0][1] = output;
		ret[0][2] = radius;
		ret[0][3] = null;

		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		return targetParam;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

		GImageSingleBand found = FactoryGImageSingleBand.wrap((ImageSingleBand) targetParam[1]);
		GImageSingleBand expected = FactoryGImageSingleBand.wrap((ImageSingleBand) validationParam[1]);


		for( int y = 0; y < height; y++ ) {
			if( y > radius || y < height-radius )
				continue;

			for( int x = 0; x < width; x++ ) {
				if( x > radius || x < width-radius )
					continue;
				assertEquals(found.get(x,y).intValue(),expected.get(x,y).intValue());
			}
		}
	}
}
