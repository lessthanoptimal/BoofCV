/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.color;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestHistogram_F64 {
	@Test
	public void constructor() {
		Histogram_F64 hist = new Histogram_F64(2,3,4);

		assertEquals(2*3*4,hist.value.length);
		assertEquals(2,hist.getLength(0));
		assertEquals(3,hist.getLength(1));
		assertEquals(4,hist.getLength(2));
	}

	@Test
	public void isRangeSet() {
		Histogram_F64 hist = new Histogram_F64(2,3,4);

		assertFalse(hist.isRangeSet());
		hist.setRange(1,-1,1);
		assertFalse(hist.isRangeSet());
		hist.setRange(0, -1, 1);
		assertFalse(hist.isRangeSet());
		hist.setRange(2, -1, 1);
		assertTrue(hist.isRangeSet());
	}

	@Test
	public void getDimensions() {
		Histogram_F64 hist = new Histogram_F64(2,3,4);

		assertEquals(3, hist.getDimensions());
	}

	@Test
	public void setRange() {
		Histogram_F64 hist = new Histogram_F64(2,3,4);

		assertEquals(0,hist.getMinimum(1),1e-8);
		assertEquals(0,hist.getMaximum(1),1e-8);

		hist.setRange(1,-1,1);

		assertEquals(-1,hist.getMinimum(1),1e-8);
		assertEquals(1,hist.getMaximum(1),1e-8);
	}

	@Test
	public void getDimensionIndex_double() {
		Histogram_F64 hist = new Histogram_F64(2,6,4);

		hist.setRange(1,-1,1);

		double period = 2.0/6.0;

		assertEquals(0, hist.getDimensionIndex(1, -1.0));
		assertEquals(5, hist.getDimensionIndex(1, 1.0));
		assertEquals(0, hist.getDimensionIndex(1, period * 0.5 - 1.0));
		assertEquals(1, hist.getDimensionIndex(1, period * 1.5 - 1.0));
		assertEquals(2, hist.getDimensionIndex(1, period * 2.5 - 1.0));
	}

	@Test
	public void getDimensionIndex_int() {
		Histogram_F64 hist = new Histogram_F64(2,256,4);

		hist.setRange(1, 0, 255);

		assertEquals(0, hist.getDimensionIndex(1, 0));
		assertEquals(255, hist.getDimensionIndex(1,255));
		assertEquals(254, hist.getDimensionIndex(1,254));
		assertEquals(101, hist.getDimensionIndex(1,101));

		hist = new Histogram_F64(2,100,4);
		hist.setRange(1, 0, 255);

		assertEquals(0, hist.getDimensionIndex(1,0));
		assertEquals(99, hist.getDimensionIndex(1,255));
		assertEquals(99, hist.getDimensionIndex(1,254));
		assertEquals(15, hist.getDimensionIndex(1,40));
	}

	@Test
	public void getIndex_two() {
		Histogram_F64 hist = new Histogram_F64(2,6);

		assertEquals(0,hist.getIndex(0, 0));
		assertEquals(2*6-1,hist.getIndex(1, 5));
		assertEquals(6,hist.getIndex(1, 0));
		assertEquals(1,hist.getIndex(0, 1));
	}

	@Test
	public void getIndex_three() {
		Histogram_F64 hist = new Histogram_F64(2,6,4);

		assertEquals(0,hist.getIndex(0, 0, 0));
		assertEquals(2*6*4-1,hist.getIndex(1, 5, 3));
		assertEquals(6*4, hist.getIndex(1, 0, 0));
		assertEquals(4, hist.getIndex(0, 1, 0));
		assertEquals(1, hist.getIndex(0, 0, 1));
	}

	@Test
	public void getIndex_N() {
		Histogram_F64 hist = new Histogram_F64(2,6,4);

		assertEquals(0,hist.getIndex(new int[]{0, 0, 0}));
		assertEquals(2*6*4-1,hist.getIndex(new int[]{1, 5, 3}));
		assertEquals(6*4, hist.getIndex(new int[]{1, 0, 0}));
		assertEquals(4, hist.getIndex(new int[]{0, 1, 0}));
		assertEquals(1, hist.getIndex(new int[]{0, 0, 1}));

		hist = new Histogram_F64(5);
		assertEquals(2, hist.getIndex(new int[]{2}));
	}

	@Test
	public void get_two() {
		Histogram_F64 hist = new Histogram_F64(2,6);

		hist.value[hist.getIndex(1,2)] = 2;
		assertEquals(2,hist.get(1,2),1e-8);
	}

	@Test
	public void get_three() {
		Histogram_F64 hist = new Histogram_F64(2,6,4);

		hist.value[hist.getIndex(1,2,1)] = 2;
		assertEquals(2, hist.get(1, 2,1), 1e-8);
	}

	@Test
	public void get_N() {
		Histogram_F64 hist = new Histogram_F64(2,6,4);

		hist.value[hist.getIndex(1,2,1)] = 2;
		assertEquals(2, hist.get(new int[]{1, 2, 1}), 1e-8);
	}
}
