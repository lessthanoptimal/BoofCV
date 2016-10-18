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

package boofcv.testing;

import boofcv.struct.image.ImageGray;

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

	// the method being tested
	protected Method methodTest;
	// the method being used to validate the test
	protected Method methodValidation;

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

			// check for equivalence for each match
			Method candidates[] = validationClass.getMethods();
			boolean foundMatch = false;
			for( Method c : candidates ) {
				if( isEquivalent(c, m)) {
//					System.out.println("Examining: "+m.getName());
					foundMatch = true;
					compareMethods(m, c);
				}
			}

			if( !foundMatch ) {
				System.out.println("Can't find an equivalent function in validation class. "+m.getName());
			} else {
				numFound++;
			}
		}

		// update this as needed when new functions are added
		if(numMethods != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numMethods);
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

		methodTest = target;
		methodValidation = validation;

		Object [][]targetParamArray = createInputParam(target,validation);

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
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected Object[] createSubImageInputs( Object[] param ) {
		Object[] ret = new Object[ param.length ];

		for( int i = 0; i < param.length; i++ ) {
			if( param[i] == null )
				continue;
			if( ImageGray.class.isAssignableFrom(param[i].getClass())) {
				ret[i] = BoofTesting.createSubImageOf((ImageGray)param[i]);
			} else {
				ret[i] = param[i];
			}
		}
		return ret;
	}

	/**
	 * Checks to see if the provided method from the test class is a method that should be tested
	 */
	protected abstract boolean isTestMethod( Method m );

	/**
	 * Returns two if the two methods provide results that should be compared
	 */
	protected abstract boolean isEquivalent(Method candidate, Method validation);

	/**
	 * Creates the set of input parameters for the functions.  Tests on multiple inputs are
	 * handled by returning multiple sets of parameters
	 */
	protected abstract Object[][] createInputParam( Method candidate, Method validation );

	/**
	 * Adjusts the input for the validation method.  Allows methods with different parameter
	 * sets to be used.
	 */
	protected abstract Object[] reformatForValidation( Method m , Object[] targetParam);

	/**
	 * Compares the two sets of results from the target and validation methods.
	 */
	protected abstract void compareResults( Object targetResult, Object[] targetParam,
											Object validationResult, Object[] validationParam );

}
