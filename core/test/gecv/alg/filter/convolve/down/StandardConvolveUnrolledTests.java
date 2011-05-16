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

package gecv.alg.filter.convolve.down;

import org.junit.Test;

import java.lang.reflect.Method;

/**
 * Standard unit tests to perform on unrolled convolution classes
 *
 * @author Peter Abeles
 */
public class StandardConvolveUnrolledTests {

	int numUnrolled;
	Class<?> target;
	Class<?>[] param1D;
	Class<?>[] param2D;

	CompareToStandardConvolveDown compareToStandard = new CompareToStandardConvolveDown(target);

	public StandardConvolveUnrolledTests() {
	}

	@Test
	public void convolve() throws NoSuchMethodException {
		for (int i = 0; i < numUnrolled; i++) {
			Method m = target.getMethod("convolve", param2D);

			compareToStandard.compareMethod(m, "convolve", i + 1);
		}
	}

	@Test
	public void horizontal() throws NoSuchMethodException {
		for (int i = 0; i < numUnrolled; i++) {
			Method m = target.getMethod("horizontal", param1D);

			compareToStandard.compareMethod(m, "horizontal", i + 1);
		}
	}

	@Test
	public void vertical() throws NoSuchMethodException {
		for (int i = 0; i < numUnrolled; i++) {
			Method m = target.getMethod("vertical", param1D);

			compareToStandard.compareMethod(m, "vertical", i + 1);
		}
	}
}
