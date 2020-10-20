/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.misc;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestHistogram2D_S32 extends BoofStandardJUnit {
	@Test
	void reshape() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		assertEquals(0,alg.rows);
		assertEquals(0,alg.cols);

		alg.reshape(2,3);
		assertEquals(2,alg.rows);
		assertEquals(3,alg.cols);
		assertEquals(6,alg.data.length);
		alg.data[0] = 2;
		alg.reshape(1,2);
		assertEquals(2,alg.data[0]);
		assertEquals(1,alg.rows);
		assertEquals(2,alg.cols);
		assertEquals(6,alg.data.length);
	}

	@Test
	void zero() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.zero(); // just see if it blows up here

		alg.reshape(2,3);
		alg.data[2] = 1;
		alg.data[5] = 2;
		alg.zero();
		for (int i = 0; i < alg.size(); i++) {
			assertEquals(0,alg.data[i]);
		}
	}

	@Test
	void increment() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		alg.increment(1,0);
		alg.increment(1,0);
		alg.increment(1,2);

		assertEquals(2,alg.get(1,0));
		assertEquals(1,alg.get(1,2));
	}

	@Test
	void sumRow() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		assertEquals(6,alg.sumRow(0));
		assertEquals(15,alg.sumRow(1));
	}

	@Test
	void sumCol() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		assertEquals(5,alg.sumCol(0));
		assertEquals(9,alg.sumCol(2));
	}

	@Test
	void maximumColIdx() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		assertEquals(2,alg.maximumColIdx(0));
		assertEquals(2,alg.maximumColIdx(1));
	}

	@Test
	void maximumRowIdx() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		alg.data[1] = 10;
		assertEquals(1,alg.maximumRowIdx(0));
		assertEquals(0,alg.maximumRowIdx(1));
		assertEquals(1,alg.maximumRowIdx(2));
	}

	@Test
	void sum() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(6,4);
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		alg.data[7] = 20; // make sure it stays inside the grid boundary
		assertEquals(21,alg.sum());
	}

	@Test
	void get() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int i = 0; i < 6; i++) {
			alg.data[i] = i+1;
		}
		for (int row = 0, i =0; row < 2; row++) {
			for (int col = 0; col < 3; col++, i++) {
				assertEquals(i+1,alg.get(row,col));
			}
		}
	}

	@Test
	void set() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int row = 0; row < 2; row++) {
			for (int col = 0; col < 3; col++) {
				int expected = 10+row+col;
				alg.set(1,2,expected);
				assertEquals(expected,alg.get(1,2));
			}
		}
	}

	@Test
	void indexOf() {
		Histogram2D_S32 alg = new Histogram2D_S32();
		alg.reshape(2,3);
		for (int row = 0, i =0; row < 2; row++) {
			for (int col = 0; col < 3; col++, i++) {
				assertEquals(i,alg.indexOf(row,col));
			}
		}
	}
}
