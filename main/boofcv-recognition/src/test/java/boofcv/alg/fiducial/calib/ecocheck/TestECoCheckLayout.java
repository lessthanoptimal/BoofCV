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

package boofcv.alg.fiducial.calib.ecocheck;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestECoCheckLayout extends BoofStandardJUnit {
	/**
	 * Compare against hand constructed order
	 */
	@Test void grid3x3() {
		var order = new DogArray_I32();
		new ECoCheckLayout().selectSnake(3, order);
		assertEquals(9, order.size);

		assertEquals(0, order.get(0));
		assertEquals(3, order.get(1));
		assertEquals(6, order.get(2));
		assertEquals(7, order.get(3));
		assertEquals(4, order.get(4));
		assertEquals(1, order.get(5));
		assertEquals(2, order.get(6));
		assertEquals(5, order.get(7));
		assertEquals(8, order.get(8));
	}

	@Test void grid4x4() {
		var order = new DogArray_I32();
		new ECoCheckLayout().selectSnake(4, order);
		assertEquals(16, order.size);

		assertEquals(0, order.get(0));
		assertEquals(4, order.get(1));
		assertEquals(8, order.get(2));
		assertEquals(12, order.get(3));
		assertEquals(13, order.get(4));
		assertEquals(9, order.get(5));
		assertEquals(5, order.get(6));
		assertEquals(1, order.get(7));
		assertEquals(2, order.get(8));
		assertEquals(6, order.get(9));
		assertEquals(10, order.get(10));
		assertEquals(14, order.get(11));
		assertEquals(15, order.get(12));
		assertEquals(11, order.get(13));
		assertEquals(7, order.get(14));
		assertEquals(3, order.get(15));
	}

	@Test void grid5x5() {
		var order = new DogArray_I32();
		new ECoCheckLayout().selectSnake(5, order);
		assertEquals(25, order.size);

		assertEquals(index(0,0,5), order.get(0));
		assertEquals(index(1,0,5), order.get(1));
		assertEquals(index(2,0,5), order.get(2));
		assertEquals(index(3,0,5), order.get(3));
		assertEquals(index(3,1,5), order.get(4));
		assertEquals(index(2,1,5), order.get(5));
		assertEquals(index(1,1,5), order.get(6));
		assertEquals(index(0,1,5), order.get(7));
		assertEquals(index(0,2,5), order.get(8));
		assertEquals(index(1,2,5), order.get(9));
		assertEquals(index(2,2,5), order.get(10));
		assertEquals(index(3,2,5), order.get(11));
		assertEquals(index(3,3,5), order.get(12));
		assertEquals(index(2,3,5), order.get(13));
		assertEquals(index(1,3,5), order.get(14));
		assertEquals(index(0,3,5), order.get(15));
		assertEquals(index(0,4,5), order.get(16));
		assertEquals(index(1,4,5), order.get(17));
		assertEquals(index(2,4,5), order.get(18));
		assertEquals(index(3,4,5), order.get(19));
		assertEquals(index(4,4,5), order.get(20));
		assertEquals(index(4,3,5), order.get(21));
		assertEquals(index(4,2,5), order.get(22));
		assertEquals(index(4,1,5), order.get(23));
		assertEquals(index(4,0,5), order.get(24));
	}

	@Test void grid6x6() {
		var order = new DogArray_I32();
		new ECoCheckLayout().selectSnake(6, order);
		assertEquals(36, order.size);

		// Check select points
		assertEquals(index(0, 0, 6), order.get(0));
		assertEquals(index(1, 0, 6), order.get(1));
		assertEquals(index(2, 0, 6), order.get(2));
		assertEquals(index(3, 0, 6), order.get(3));

		assertEquals(index(3, 5, 6), order.get(20));
		assertEquals(index(2, 5, 6), order.get(21));
		assertEquals(index(1, 5, 6), order.get(22));
		assertEquals(index(0, 5, 6), order.get(23));

		assertEquals(index(4, 1, 6), order.get(32));
		assertEquals(index(5, 1, 6), order.get(33));
		assertEquals(index(5, 0, 6), order.get(34));
		assertEquals(index(4, 0, 6), order.get(35));
	}

	int index(int row, int col, int size) {
		return row*size+col;
	}
}
