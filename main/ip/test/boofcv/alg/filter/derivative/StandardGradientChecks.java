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

package boofcv.alg.filter.derivative;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderIndex1D_Wrap;
import boofcv.core.image.border.ImageBorder1D_F32;
import boofcv.core.image.border.ImageBorder1D_S32;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class StandardGradientChecks {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	/**
	 * Tests to see if the XY and YX second derivatives are equal to each other
	 */
	public void secondDerivativeTest( Class<?> gradientTarget , int numExpected ) {
		int total = 0;

		Method methods[] = gradientTarget.getMethods();

		for( Method m : methods ) {
			if( !m.getName().equals("process"))
				continue;

			// see if there is a function that can compute the second derivative
			Class<?> params[] = m.getParameterTypes();
			Method m2;
			try {
				m2 = gradientTarget.getMethod("process",params[1],params[1],params[1],params[3]);
			} catch (NoSuchMethodException e) {
				continue;
			}

			testSecondDerivative(m,m2);
			total++;
		}

		assertEquals(numExpected,total);
	}

	/**
	 * The XY and YX second derivatives should be indential
	 */
	private void testSecondDerivative(Method m1 , Method m2) {
		Class params[] = m1.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(params[0], width, height);
		ImageGray derivX = GeneralizedImageOps.createSingleBand(params[1], width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(params[2], width, height);
		ImageGray derivXX = GeneralizedImageOps.createSingleBand(params[1], width, height);
		ImageGray derivYY = GeneralizedImageOps.createSingleBand(params[2], width, height);
		ImageGray derivXY = GeneralizedImageOps.createSingleBand(params[1], width, height);
		ImageGray derivYX = GeneralizedImageOps.createSingleBand(params[1], width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 40);

		Object border;
		if( params[3] == ImageBorder_F32.class ) {
			border = new ImageBorder1D_F32(BorderIndex1D_Wrap.class);
		} else {
			border = new ImageBorder1D_S32(BorderIndex1D_Wrap.class);
		}

		try {
			m1.invoke(null,input,derivX,derivY,border);
			m2.invoke(null,derivX,derivXX,derivXY,border);
			m2.invoke(null,derivY,derivYX,derivYY,border);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

//		BoofTesting.printDiff(derivXY,derivYX);
		BoofTesting.assertEquals(derivXY, derivYX, 1e-3f);
	}
}
