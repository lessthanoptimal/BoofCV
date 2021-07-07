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

package boofcv.testing;

import boofcv.BoofTesting;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.Planar;
import org.ejml.UtilEjml;

import java.lang.reflect.Method;

/**
 * Provides common implementations of functions in CompareEquivalentFunctions. Assumes only
 * images are modified and tests those.
 *
 * @author Peter Abeles
 */
public abstract class CompareIdenticalFunctions extends CompareEquivalentFunctions {

	protected CompareIdenticalFunctions( Class<?> testClass, Class<?>... validationClass ) {
		super(testClass, validationClass);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		Class<?>[] e = m.getParameterTypes();

		for (Class<?> c : e) {
			if (ImageGray.class.isAssignableFrom(c))
				return true;
			if (ImageInterleaved.class.isAssignableFrom(c))
				return true;
			if (Planar.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	@Override
	protected boolean isEquivalent( Method candidate, Method evaluation ) {
		if (evaluation.getName().compareTo(candidate.getName()) != 0)
			return false;

		Class<?>[] e = evaluation.getParameterTypes();
		Class<?>[] c = candidate.getParameterTypes();

		if (e.length != c.length)
			return false;

		for (int i = 0; i < e.length; i++) {
			if (e[i] != c[i])
				return false;
		}
//		System.out.println("   "+c[1].getSimpleName()+"  "+c[2].getSimpleName());

		return true;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Object[] ret = new Object[targetParam.length];

		for (int i = 0; i < targetParam.length; i++) {
			if (targetParam[i] == null)
				continue;
			if (ImageBase.class.isAssignableFrom(targetParam[i].getClass())) {
				ret[i] = ((ImageBase)targetParam[i]).clone();
			} else {
				ret[i] = targetParam[i];
			}
		}

		return ret;
	}

	@SuppressWarnings({"EmptyCatch"})
	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {

		for (int i = 0; i < targetParam.length; i++) {
			if (targetParam[i] == null) {
				continue;
			}
			if (targetParam[i].getClass() == byte[].class) {
				BoofTesting.assertEquals((byte[])validationParam[i], (byte[])targetParam[i]);
			} else if (targetParam[i].getClass() == short[].class) {
				BoofTesting.assertEquals((short[])validationParam[i], (short[])targetParam[i]);
			} else if (targetParam[i].getClass() == int[].class) {
				BoofTesting.assertEquals((int[])validationParam[i], (int[])targetParam[i]);
			} else if (targetParam[i].getClass() == long[].class) {
				BoofTesting.assertEquals((long[])validationParam[i], (long[])targetParam[i]);
			} else if (targetParam[i].getClass() == float[].class) {
				BoofTesting.assertEquals((float[])validationParam[i], (float[])targetParam[i], UtilEjml.TEST_F32);
			} else if (targetParam[i].getClass() == double[].class) {
				BoofTesting.assertEquals((double[])validationParam[i], (double[])targetParam[i], UtilEjml.TEST_F64);
			}

			if (!ImageBase.class.isAssignableFrom(targetParam[i].getClass()))
				continue;

			ImageBase t = (ImageBase)targetParam[i];
			ImageBase v = (ImageBase)validationParam[i];

			BoofTesting.assertEqualsRelative(v, t, 1e-4);// todo is this tolerance too big?  some operations with a slightly different ordering seem to require it
		}

		if (targetResult != null) {
			try {
				double error = ((Number)targetResult).doubleValue() - ((Number)validationResult).doubleValue();
				if (Math.abs(error) > 1e-3) {
					throw new RuntimeException("Failed");
				}
			} catch (RuntimeException ignore) {
			}
		}
	}
}
