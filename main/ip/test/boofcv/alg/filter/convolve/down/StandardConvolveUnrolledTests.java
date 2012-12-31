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

package boofcv.alg.filter.convolve.down;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue;

/**
 * Standard unit tests to perform on unrolled convolution classes
 *
 * @author Peter Abeles
 */
public abstract class StandardConvolveUnrolledTests {

	int numUnrolled=-1;
	Class<?> target;
	Class<?>[] param1D;
	Class<?>[] param2D;

	CompareToStandardConvolveDownNoBorder compareToStandard = new CompareToStandardConvolveDownNoBorder(target);

	@Test
	public void convolve() throws NoSuchMethodException {
		testMethod("convolve",param2D);
	}

	@Test
	public void horizontal() throws NoSuchMethodException {
		testMethod("horizontal",param1D);
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		testMethod("vertical",param1D);
	}

	private void testMethod( String methodName , Class<?> param[] ) throws NoSuchMethodException {
		assertTrue(numUnrolled>0);

		for( int enlarge = 0; enlarge < 2; enlarge++ ) {
			for( int skip = 1; skip <= 4; skip++ ) {
				for( int kernel = 1; kernel <= 2; kernel++ ) {
					compareToStandard.setSkip(skip);
					compareToStandard.setKernelRadius(kernel);
					compareToStandard.setImageDimention(20+enlarge,25+enlarge);
					for (int i = 0; i < numUnrolled; i++) {
						Method m = target.getMethod(methodName, param );

						compareToStandard.compareMethod(m, methodName , i + 1);
					}
				}
			}
		}
	}
}
