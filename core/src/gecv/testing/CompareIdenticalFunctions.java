/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.testing;

import gecv.struct.image.ImageBase;

import java.lang.reflect.Method;

/**
 * Provides common implementations of functions in CompareEquivalentFunctions.  Assumes only
 * images are modified and tests those.
 *
 * @author Peter Abeles
 */
public abstract class CompareIdenticalFunctions extends CompareEquivalentFunctions {

	protected CompareIdenticalFunctions(Class<?> testClass, Class<?> validationClass) {
		super(testClass, validationClass);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> e[] = m.getParameterTypes();

		for( Class<?> c : e ) {
			if( ImageBase.class.isAssignableFrom(c))
				return true;
		}
		return false;
	}

	@Override
	protected boolean isEquivalent(Method evaluation, Method candidate) {
		if( evaluation.getName().compareTo(candidate.getName()) != 0 )
			return false;

		Class<?> e[] = evaluation.getParameterTypes();
		Class<?> c[] = candidate.getParameterTypes();

		if( e.length != c.length )
			return false;

		for( int i = 0; i < e.length; i++ ) {
			if( e[i] != c[i])
				return false;
		}
		return true;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret = new Object[ targetParam.length ];

		for( int i = 0; i < targetParam.length; i++ ) {
			if( ImageBase.class.isAssignableFrom(targetParam[i].getClass()) ) {
				ret[i] = ((ImageBase)targetParam[i]).clone();
			} else {
				ret[i] = targetParam[i];
			}
		}

		return ret;
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
		for( int i = 0; i < targetParam.length; i++ ) {
			if( !ImageBase.class.isAssignableFrom(targetParam[i].getClass()) )
				continue;

			ImageBase t = (ImageBase)targetParam[i];
			ImageBase v = (ImageBase)validationParam[i];

			GecvTesting.assertEqualsGeneric(v,t,1,1e-4);// todo is this tolerance too big?  some operations with a slightly different ordering seem to require it
		}
	}
}
