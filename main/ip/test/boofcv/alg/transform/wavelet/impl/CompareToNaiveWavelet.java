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

package boofcv.alg.transform.wavelet.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderIndex1D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.struct.wavelet.WlBorderCoef;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Compares the output optimized algorithms to the naive wavelet algorithms
 *
 * @author Peter Abeles
 */
public class CompareToNaiveWavelet {
	int numExpected;

	boolean isFloat;
	Class<?> typeInput;
	Class<?> typeOutput;
	String functionName;
	Class<?> testClass;

	public CompareToNaiveWavelet(int numExpected, Class<?> testClass) {
		this.numExpected = numExpected;
		this.testClass = testClass;
	}

	protected void checkAll( String functionName , String testMethodName ) {
		this.functionName = functionName;
		Method methods[] = testClass.getMethods();
		Method testMethod;

		try {
			testMethod = getClass().getMethod(testMethodName,Method.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( m.getName().compareTo(functionName) != 0)
				continue;

			Class<?>[] p = m.getParameterTypes();
			typeInput = p[p.length-2];
			isFloat = GeneralizedImageOps.isFloatingPoint(typeInput);
			typeOutput = p[p.length-1];

//			System.out.println(typeInput.getSimpleName()+" "+typeOutput.getSimpleName());

			try {
				testMethod.invoke(this,m);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	/**
	 * Invokes test validation methods using reflections
	 */
	protected abstract class BaseCompare extends PermuteWaveletCompare {
		protected BaseCompare() {
			super(typeInput, typeOutput);
		}

		@Override
		public void applyValidation(WaveletDescription<?> desc, ImageGray input, ImageGray output) {
			Method m;
			Object args[];
			if( functionName.contains("Inverse")) {
				BorderIndex1D border = desc.getBorder();
				WlBorderCoef<?> inv = desc.getInverse();
				m = BoofTesting.findMethod(ImplWaveletTransformNaive.class,functionName,border.getClass(),inv.getClass(),input.getClass(),output.getClass());
				args = new Object[]{border,inv,input,output};
			} else {
				Class<?> borderClass = desc.border.getClass();
				Class<?> coefClass = desc.getForward().getClass();
				m = BoofTesting.findMethod(ImplWaveletTransformNaive.class,functionName,borderClass,coefClass,input.getClass(),output.getClass());
				args = new Object[]{desc.border,desc.getForward(),input,output};
			}
			try {
				m.invoke(null,args);
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

	}

}
