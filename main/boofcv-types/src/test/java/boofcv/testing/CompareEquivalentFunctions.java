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
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;

import java.lang.reflect.Method;

/**
 * Provides a formalism for comparing two sets of functions which should produce identical results.
 *
 * @author Peter Abeles
 */
public abstract class CompareEquivalentFunctions extends BoofStandardJUnit {
	// the class being tested
	protected Class<?> testClass;
	// class being validated
	protected Class<?>[] validationClasses;

	// the method being tested
	protected Method methodTest;
	// the method being used to validate the test
	protected Method methodValidation;

	// flag used to indicate if a parameter can be skipped when computing sub-image
	protected boolean[] ignoreSubimage = new boolean[0];

	protected CompareEquivalentFunctions( Class<?> testClass, Class<?>... validationClass ) {
		this.testClass = testClass;
		this.validationClasses = validationClass;
	}

	public void performTests( int numMethods ) {
		Method[] methods = testClass.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if (!isTestMethod(m))
				continue;

//			BoofMiscOps.printMethodInfo(m, System.out);
			boolean foundMatch = false;

			escape:
			for (Class<?> vc : validationClasses) {
				// check for equivalence for each match
				Method[] candidates = vc.getMethods();
				for (Method c : candidates) {
					if (isEquivalent(c, m)) {
//						System.out.println("Examining: "+m.getName()+" param "+c.getParameterCount()+" vs "+m.getParameterCount());
						foundMatch = true;
						compareMethods(m, c);
						break escape;
					}
				}
			}
			if (foundMatch)
				numFound++;

//			if (!foundMatch) {
//				System.out.print("No match: ");
//				BoofMiscOps.printMethodInfo(m, System.out);
//			}
		}

		// update this as needed when new functions are added
		if (numMethods != numFound) {
			throw new RuntimeException("Unexpected number of methods: Found " + numFound + "  expected " + numMethods + " possible " + methods.length);
		}
	}

	/**
	 * Allows directly comparing two functions to each other without going through all the other functions
	 *
	 * @param target The method being compared
	 * @param validationName Name of the method in the validation class
	 */
	public void compareMethod( Class<?> validationClass, Method target, String validationName ) {
		try {
			Method evaluation = validationClass.getMethod(validationName, target.getParameterTypes());
			compareMethods(target, evaluation);
		} catch (NoSuchMethodException e) {
			BoofMiscOps.printMethodInfo(target, System.err);
			e.printStackTrace(); // need more info to debug an erratic error
			throw new RuntimeException(e);
		}
	}

	public void compareMethod( Method target, String validationName ) {
		compareMethod(validationClasses[0], target, validationName);
	}

	protected void compareMethods( Method target, Method validation ) {

		methodTest = target;
		methodValidation = validation;
		// by default don't skip a parameter for sub-image
		ignoreSubimage = new boolean[Math.max(target.getParameterCount(), validation.getParameterCount())];

		Object[][] targetParamArray = createInputParam(target, validation);

		for (int i = 0; i < targetParamArray.length; i++) {
			compareMethods(target, validation, targetParamArray[i]);
		}
	}

	protected void compareMethods( Method target, Method validation, Object[] targetParam ) {
		Object[] validationParam = reformatForValidation(validation, targetParam);

		Object[] targetParamSub = createSubImageInputs(targetParam);
		Object[] validationParamSub = createSubImageInputs(validationParam);

		try {
			Object targetResult = target.invoke(null, targetParam);
			Object validationResult = validation.invoke(null, validationParam);

			compareResults(targetResult, targetParam, validationResult, validationParam);

			// see if it can handle sub-images correctly
			targetResult = target.invoke(null, targetParamSub);
			validationResult = validation.invoke(null, validationParamSub);

			compareResults(targetResult, targetParamSub, validationResult, validationParamSub);
		} catch (Exception e) {
			BoofMiscOps.printMethodInfo(target, System.err);
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
	}

	protected Object[] createSubImageInputs( Object[] param ) {
		Object[] ret = new Object[param.length];

		for (int i = 0; i < param.length; i++) {
			if (param[i] == null)
				continue;
			if (!ignoreSubimage[i] && ImageGray.class.isAssignableFrom(param[i].getClass())) {
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
	protected abstract boolean isEquivalent( Method candidate, Method validation );

	/**
	 * Creates the set of input parameters for the functions. Tests on multiple inputs are
	 * handled by returning multiple sets of parameters
	 */
	protected abstract Object[][] createInputParam( Method candidate, Method validation );

	/**
	 * Adjusts the input for the validation method. Allows methods with different parameter
	 * sets to be used.
	 */
	protected abstract Object[] reformatForValidation( Method m, Object[] targetParam );

	/**
	 * Compares the two sets of results from the target and validation methods.
	 */
	protected abstract void compareResults( Object targetResult, Object[] targetParam,
											Object validationResult, Object[] validationParam );
}
