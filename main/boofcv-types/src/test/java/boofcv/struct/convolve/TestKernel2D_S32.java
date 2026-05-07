/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.convolve;

import boofcv.testing.BoofStandardJUnit;
import org.ejml.MatrixPrintFormat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestKernel2D_S32 extends BoofStandardJUnit {
	@Test void format() {
		var format = new MatrixPrintFormat(0, ", ", ",\n", "{", "}", "{", "}");

		var a = new Kernel2D_S32(3, new int[]{1, 2, -3, 4, -5, 6, -7, 800, 9});

		// By default, it will align columns
		assertEquals("{{1     , 2     , -3    },\n {4     , -5    , 6     },\n {-7    , 800   , 9     }}", a.format(format));

		format.setAligned(false);
		String found = a.format(format);
		assertEquals("{{1, 2, -3},\n{4, -5, 6},\n{-7, 800, 9}}", found);
	}

	@Test void countZeroBorder() {
		var c0 = new Kernel2D_S32(3);
		assertEquals(2, c0.countZeroBorder());
		c0.set(0, 0, 1);
		assertEquals(0, c0.countZeroBorder());
		c0.set(0, 0, 0);
		c0.set(2, 2, 1);
		assertEquals(0, c0.countZeroBorder());
		c0.set(2, 2, 0);
		c0.set(1, 1, 1);
		assertEquals(1, c0.countZeroBorder());
		c0.set(1, 1, 0);
		c0.set(2, 2, 1);
		assertEquals(0, c0.countZeroBorder());

		var c1 = new Kernel2D_S32(5);
		assertEquals(3, c1.countZeroBorder());
		c1.set(0, 1, 1);
		assertEquals(0, c1.countZeroBorder());
		c1.set(0, 1, 0);
		c1.set(1, 0, 1);
		assertEquals(0, c1.countZeroBorder());
		c1.set(1, 0, 0);
		c1.set(2, 2, 1);
		assertEquals(2, c1.countZeroBorder());
	}

	@Test void trimBorder() {
		var in = new Kernel2D_S32(5);
		for (int i = 0; i < in.width; i++) {
			in.set(i, i, i + 1);
		}

		Kernel2D_S32 found = in.trimBorder(1);
		assertEquals(3, found.width);
		for (int i = 0; i < found.width; i++) {
			assertEquals(i + 2, found.get(i, i));
		}
	}
}
