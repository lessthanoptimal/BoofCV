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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provides a formalism for comparing two sets of functions which should produce identical results.
 *
 * @author Peter Abeles
 */
public abstract class CompareEquivalentFunctions {
	// the class being tested
	Class<?> testClass;
	// class being validated
	Class<?> validationClass;

	protected CompareEquivalentFunctions(Class<?> testClass, Class<?> validationClass) {
		this.testClass = testClass;
		this.validationClass = validationClass;
	}

	public void performTests( int numMethods) {
		Method methods[] = testClass.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;

			Method candidates[] = validationClass.getMethods();
			Method matched = null;
			for( Method c : candidates ) {
				if( isEquivalent(m,c)) {
					matched = c;
					break;
				}
			}

			if( matched == null )
				continue;

			System.out.println("Examining: "+m.getName());
			compareMethods(m, matched);
			numFound++;
		}

		// update this as needed when new functions are added
		if(numMethods != numFound)
			throw new RuntimeException("Unexpected number of methods");
	}

	/**
	 * Allows directly comparing two functions to each other without going through all the other functions
	 *
	 * @param target The method being compared
	 * @param validationName Name of the method in the validation class
	 */
	public void compareMethod( Method target , String validationName ) {
		try {
			Method evaluation = validationClass.getMethod(validationName,target.getParameterTypes());
			compareMethods(target,evaluation);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private void compareMethods( Method target , Method validation ) {
		Object [][]targetParamArray = createInputParam(target);

		for( int i = 0; i < targetParamArray.length; i++  ) {
			compareMethods(target, validation, targetParamArray[i]);
		}
	}

	private void compareMethods(Method target, Method validation, Object[] targetParam) {
		Object []validationParam = reformatForValidation(validation,targetParam);

		Object []targetParamSub = createSubImageInputs(targetParam);
		Object []validationParamSub = createSubImageInputs(validationParam);

		try {
			Object targetResult = target.invoke(null,targetParam);
			Object validationResult = validation.invoke(null,validationParam);

			compareResults(targetResult,targetParam,validationResult,validationParam);

			// see if it can handle sub-images correctly
			targetResult = target.invoke(null,targetParamSub);
			validationResult = validation.invoke(null,validationParamSub);

			compareResults(targetResult,targetParamSub,validationResult,validationParamSub);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected Object[] createSubImageInputs( Object[] param ) {
		Object[] ret = new Object[ param.length ];

		for( int i = 0; i < param.length; i++ ) {
			if( param[i] == null )
				continue;
			if( ImageBase.class.isAssignableFrom(param[i].getClass())) {
				ret[i] = GecvTesting.createSubImageOf((ImageBase)param[i]);
			} else {
				ret[i] = param[i];
			}
		}
		return ret;
	}

	protected abstract boolean isTestMethod( Method m );

	protected abstract boolean isEquivalent( Method evaluation , Method candidate );

	protected abstract Object[][] createInputParam( Method m );

	protected abstract Object[] reformatForValidation( Method m , Object[] targetParam);

	protected abstract void compareResults( Object targetResult, Object[] targetParam,
											Object validationResult, Object[] validationParam );

}
