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

package boofcv.alg.filter.convolve;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareEquivalentFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static boofcv.alg.filter.convolve.noborder.TestImplConvolveMean.createTableKernel;

/**
 * @author Peter Abeles
 */
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

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> params[] = m.getParameterTypes();

		if( params.length < 4 || params.length > 6)
			return false;

		return ImageGray.class.isAssignableFrom(params[0]);
	}

	@Override
	protected boolean isEquivalent(Method validation, Method target) {

		Class<?>[] v = validation.getParameterTypes();
		Class<?>[] c = target.getParameterTypes();

		if( !target.getName().equals(validation.getName()))
			return false;

		if( c[0] != v[1] || c[1] != v[2])
			return false;

		if( target.getName().equals("vertical")) {
			if( ImageBorder.class.isAssignableFrom(c[4]) ) {
				return v.length >= 4 && ImageBorder.class.isAssignableFrom(v[3]);
			} else if( v.length != 3 ){
				return false;
			}
		} else if( (c.length == 4) ^ (v.length == 3)) {
			return false;
		}

		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class[] c = candidate.getParameterTypes();

		ImageGray input = GeneralizedImageOps.createSingleBand(c[0], width, height);
		ImageGray output = GeneralizedImageOps.createSingleBand(c[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 100);

		ImageBorder border = FactoryImageBorder.generic(BorderType.REFLECT,input.getImageType());

		Object[][] ret = new Object[2][];
		if( c.length == 4 ) {
			ret[0] = new Object[]{input, output, offset1, length1};
			ret[1] = new Object[]{input, output, offset2, length2};
		} else if( c.length == 5 ) {
			ret[0] = new Object[]{input, output, offset1, length1, null};
			ret[1] = new Object[]{input, output, offset2, length2, null};
			if( ImageBorder.class.isAssignableFrom(c[4]) ) {
				ret[0][4] = border;
				ret[1][4] = border;
			}
		} else {
			ret[0] = new Object[]{input, output, offset1, length1, border, null};
			ret[1] = new Object[]{input, output, offset2, length2, border, null};
		}

		return ret;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Class<?>[] params = m.getParameterTypes();
		Object kernel = createTableKernel(params[0],(Integer)targetParam[2],(Integer)targetParam[3]);

		ImageGray output = (ImageGray)((ImageGray)targetParam[1]).clone();

		if( ImageBorder.class.isAssignableFrom(params[params.length-1])) {
			return new Object[]{kernel, targetParam[0], output, targetParam[4]};
		} else {
			return new Object[]{kernel, targetParam[0], output};
		}
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

		if (validationParam.length == 3) {
			ImageGray expected = (ImageGray) validationParam[2];
			ImageGray found = (ImageGray) targetParam[1];

			BoofTesting.assertEquals(expected, found, 1e-4);
		} else {
			ImageGray expected = (ImageGray) validationParam[2];
			ImageGray found = (ImageGray) targetParam[1];

			BoofTesting.assertEquals(expected, found, 1e-4);
		}
	}
}
