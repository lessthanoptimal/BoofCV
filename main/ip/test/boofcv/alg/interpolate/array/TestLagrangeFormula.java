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

package boofcv.alg.interpolate.array;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestLagrangeFormula {

	/**
	 * See if it can interpolate a linear function
	 */
	@Test
	public void linear_F64() {
		int length = 10;
		double a = 2.5;
		double y0 = 4;

		double x[] = new double[length];
		double y[] = new double[length];

		for (int i = 0; i < length; i++) {
			x[i] = i;
			y[i] = y0 + i * a;
		}

		// try it across the whole data set
		double val = LagrangeFormula.process_F64(3.7, x, y, 0, length - 1);
		assertEquals(3.7 * a + y0, val, 1e-10);

		// now just part of it
		val = LagrangeFormula.process_F64(3.7, x, y, 2, 6);
		assertEquals(3.7 * a + y0, val, 1e-10);
	}

	/**
	 * See if it can interpolate a linear function
	 */
	@Test
	public void linear_F32() {
		int length = 10;
		float a = 2.5f;
		float y0 = 4f;

		float x[] = new float[length];
		float y[] = new float[length];

		for (int i = 0; i < length; i++) {
			x[i] = i;
			y[i] = y0 + i * a;
		}

		// try it across the whole data set
		float val = LagrangeFormula.process_F32(3.7f, x, y, 0, length - 1);
		assertEquals(3.7 * a + y0, val, 1e-5);

		// now just part of it
		val = LagrangeFormula.process_F32(3.7f, x, y, 2, 6);
		assertEquals(3.7 * a + y0, val, 1e-5);
	}
}
