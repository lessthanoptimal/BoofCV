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

package boofcv.alg.filter.binary.impl;

import boofcv.struct.image.GrayU8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBinaryInnerOps {
	@Test
	public void test() {
		CompreToBinaryNaiveInner tests = new CompreToBinaryNaiveInner(ImplBinaryInnerOps.class);
		tests.performTests(7);
	}

	private static class CompreToBinaryNaiveInner extends CompareToBinaryNaive {

		public CompreToBinaryNaiveInner(Class<?> testClass) {
			super(false,testClass);
		}

		@Override
		protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {
			GrayU8 t = (GrayU8) targetParam[1];
			GrayU8 v = (GrayU8) validationParam[1];

			for (int y = 1; y < t.height - 1; y++) {
				for (int x = 1; x < t.width - 1; x++) {
					assertEquals(t.get(x, y), v.get(x, y));
				}
			}
		}
	}
}
