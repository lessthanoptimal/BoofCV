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
public class TestInterpolate1D_F32 {

	@Test
	public void bisectionSearch() {
		Debug debug = new Debug(3, 20);

		debug.bisectionSearch(0, 0, 20);
		assertEquals(0, debug.index0);

		debug.bisectionSearch(0.1f, 0, 20);
		assertEquals(0, debug.index0);

		debug.bisectionSearch(16.5f, 0, 20);
		assertEquals(14, debug.index0);

		debug.bisectionSearch(19.5f, 0, 20);
		assertEquals(16, debug.index0);
	}

	@Test
	public void hunt() {
		Debug debug = new Debug(3, 20);
		debug.bisectionSearch(0, 0, 20);
		debug.hunt(16.5f);
		assertEquals(14, debug.index0);
		debug.hunt(0);
		assertEquals(0, debug.index0);
		debug.hunt(19.5f);
		assertEquals(16, debug.index0);
	}

	@Test
	public void changeDegree() {
		Debug debug = new Debug(3, 20);
		assertEquals(4, debug.M);
		debug.changeDegree(2);
		assertEquals(3, debug.M);
	}

	private static class Debug extends Interpolate1D_F32 {
		boolean computeCalled = false;

		public Debug(int degree, int size) {
			super(degree);

			float[] x = new float[size];
			float[] y = new float[size];

			for (int i = 0; i < size; i++) {
				x[i] = i;
			}

			setInput(x, y, size);
		}


		@Override
		protected float compute(float sampleX) {
			computeCalled = true;
			return 0;
		}

		@Override
		public void bisectionSearch(float val, int lowerLimit, int upperLimit) {
			super.bisectionSearch(val, lowerLimit, upperLimit);
		}

		@Override
		protected void hunt(float val) {
			super.hunt(val);
		}
	}
}
