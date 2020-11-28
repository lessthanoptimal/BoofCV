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

package boofcv.alg.segmentation.cc;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestConnectedTwoRowSpeckleFiller extends BoofStandardJUnit {
	float valueStep = 0.6f;

	GrayF32 input = new GrayF32(1, 1);
	GrayF32 expected = new GrayF32(1, 1);

	// @formatter:off
	String case0 =
			"200001\n" +
			"000220\n" +
			"002222\n" +
			"002220\n" +
			"000020\n" +
			"000000";

	String expected0 =
			"300001\n" +
			"000330\n" +
			"003333\n" +
			"003330\n" +
			"000030\n" +
			"000000";

	// The 1 prevents the top cluster from being pruned as it connects it to the background
	String case1 =
			"222000\n" +
			"001000\n" +
			"000220\n" +
			"002000\n" +
			"010010\n" +
			"000010";

	String expected1 =
			"222000\n" +
			"001000\n" +
			"000330\n" +
			"003000\n" +
			"010010\n" +
			"000010";

	// Checks to see if speckle on the bottom is handled and max region size
	String case2 =
			"222222\n" +
			"022222\n" +
			"002222\n" +
			"002000\n" +
			"010022\n" +
			"202002";

	String expected2 =
			"222222\n" +
			"022222\n" +
			"002222\n" +
			"002000\n" +
			"010033\n" +
			"303003";

	String case3 =
			"101010\n" +
			"202020\n" +
			"303030\n" +
			"212020\n" +
			"001110\n" +
			"000000";
	// @formatter:on

	@Test void process_case0() {
		// When the fill value is zero it will be all zeros
		set(case0, input);
		ImageMiscOps.fill(expected, 0);
		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.process(input, 20, 1.0f, 0.0f);
		BoofTesting.assertEquals(expected, input, 1e-4);

		// There will be no change when the fill value is equal to the max value
		set(case0, input);
		expected.setTo(input);
		alg.process(input, 20, 1.0f, valueStep*2);
		BoofTesting.assertEquals(expected, input, 1e-4);

		// More interesting when set to a value that's different
		set(case0, input);
		set(expected0, expected);
		alg.process(input, 20, 1.0f, valueStep*3);
		BoofTesting.assertEquals(expected, input, 1e-4);
	}

	@Test void process_case1() {
		set(case1, input);
		set(expected1, expected);
		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.process(input, 20, 1.0f, valueStep*3);
		BoofTesting.assertEquals(expected, input, 1e-4);
	}

	@Test void process_case2() {
		set(case2, input);
		set(expected2, expected);
		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.process(input, 5, 1.0f, valueStep*3);
		BoofTesting.assertEquals(expected, input, 1e-4);
	}

	@Test void process_case3() {
		set(case3, input);
		set(case3, expected);
		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.process(input, 15, 1.0f, valueStep*4);
		// there should be no change since everything is connect to one segment
		BoofTesting.assertEquals(expected, input, 1e-4);
	}

	/**
	 * Call it multiple times and see if it gets the same result
	 */
	@Test void process_MultipleCalls() {
		var image = new GrayF32(40, 30);
		var copy = new GrayF32(40, 30);
		var alg = new ConnectedTwoRowSpeckleFiller();

		for (int trial = 0; trial < 10; trial++) {
			image.reshape(rand.nextInt(10) + 30, rand.nextInt(10) + 30);
			ImageMiscOps.fillUniform(image, rand, 0.0f, 10.0f);
			copy.setTo(image);
			alg.process(image, 20, 1.0f, 0.0f);
			int filled = alg.getTotalFilled();
			alg.process(copy, 20, 1.0f, 0.0f);
			assertEquals(filled, alg.getTotalFilled());
			assertTrue(filled > 2);// sanity check to make sure it's doing something

			BoofTesting.assertEquals(input, copy, 0.0);
		}
	}

	/**
	 * Checks to see if internal checks get triggered. This caught some difficult bugs. Increase 'w'' to make it more
	 * rigorous. Try 100 or 1000
	 */
	@Test void random() {
		System.setOut(systemOut);
		var alg = new ConnectedTwoRowSpeckleFiller();

		int w = 20;
		int r = 5;
		int numTrials = 1000;

		for (int trial = 0; trial < numTrials; trial++) {
			float fillValue = 3.0f;
			var image = new GrayF32(w + rand.nextInt(r), w + rand.nextInt(r));
			ImageMiscOps.fillUniform(image, rand, 0.0f, fillValue);
			alg.process(image, 20, 1.0f, fillValue);
		}
	}

	/** Check row labeling on a normal situation without any edge cases */
	@Test void labelRow() {
		int width = 10;
		float[] row = new float[]{1, 2, 3, 4, 4, 4, 4, 5, 5, 2, 3, 4, 6, 7};
		int[] labels = new int[width];
		int[] counts = new int[width];
		int[] locations = new int[width];

		int regions = ConnectedTwoRowSpeckleFiller.labelRow(row, 1, width, labels, counts, locations, 10.0f, 0.9f);

		assertEquals(6, regions);
		assertArrayEquals(new int[]{0, 1, 2, 2, 2, 2, 3, 3, 4, 5}, labels);
		assertArrayEquals(new int[]{1, 1, 4, 2, 1, 1, 0, 0, 0, 0}, counts);
		assertArrayEquals(new int[]{0, 1, 2, 6, 8, 9, 0, 0, 0, 0}, locations);
	}

	/** See if it handles elements equal to the fill value correctly */
	@Test void labelRow_FillValue() {
		int width = 10;
		float[] row = new float[]{1, 4, 3, 3, 4, 4, 4, 5, 5, 2, 3, 4, 6, 7};
		int[] labels = new int[width];
		int[] counts = new int[width];
		int[] locations = new int[width];

		int regions = ConnectedTwoRowSpeckleFiller.labelRow(row, 1, width, labels, counts, locations, 4.0f, 0.9f);

		assertEquals(4, regions);
		assertArrayEquals(new int[]{-1, 0, 0, -1, -1, -1, 1, 1, 2, 3}, labels);
		assertArrayEquals(new int[]{2, 2, 1, 1, 0, 0, 0, 0, 0, 0}, counts);
		assertArrayEquals(new int[]{1, 6, 8, 9, 0, 0, 0, 0, 0, 0}, locations);
	}

	/** Nominal case with a simple graph */
	@Test void findConnectionsBetweenRows() {
		var image = new GrayF32(7, 4);
		image.data = new float[]{1,
				4, 3, 3, 8, 1, 2, 8,
				4, 9, 2, 8, 8, 8, 8};

		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.image = image;
		alg.fillValue = 10.0f;
		alg.countsA.size = 6;
		alg.countsB.size = 4;
		alg.labelsA.setTo(0, 1, 1, 2, 3, 4, 5);
		alg.labelsB.setTo(0, 1, 2, 3, 3, 3, 3);
		alg.findConnectionsBetweenRows(1, 8, 0.9f);

		assertTrue(alg.connectAtoB.isEquals(0, -1, 3, -1, -1, 3));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1));
	}

	/** A fill value matches one of the element values */
	@Test void findConnectionsBetweenRows_fillValue() {
		var image = new GrayF32(7, 4);
		image.data = new float[]{1,
				4.1f, 3, 3, 4, 1, 2, 8,
				4, 9, 2, 4.1f, 8, 8, 8};

		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.image = image;
		alg.fillValue = 4.0f;
		alg.countsA.size = 5;
		alg.countsB.size = 4;
		alg.labelsA.setTo(0, 1, 1, -1, 2, 3, 4);
		alg.labelsB.setTo(-1, 0, 1, 2, 3, 3, 3);
		alg.findConnectionsBetweenRows(1, 8, 0.9f);

		assertTrue(alg.connectAtoB.isEquals(-1, -1, -1, -1, 3));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1));
	}

	/** Scenario where labels wil need to be merged in B */
	@Test void findConnectionsBetweenRows_merge() {
		var image = new GrayF32(7, 4);
		image.data = new float[]{1,
				4, 3, 4, 8, 8, 8, 8,
				4, 4, 4, 8, 2, 1, 8};

		var alg = new ConnectedTwoRowSpeckleFiller();
		alg.image = image;
		alg.fillValue = 10.0f;
		alg.countsA.size = 4;
		alg.countsB.size = 5;
		alg.labelsA.setTo(0, 1, 2, 3, 3, 3, 3);
		alg.labelsB.setTo(0, 0, 0, 1, 2, 3, 4);
		alg.findConnectionsBetweenRows(1, 8, 0.9f);

		assertTrue(alg.connectAtoB.isEquals(0, -1, 0, 1));
		assertTrue(alg.merge.isEquals(-1, -1, -1, -1, 1));
	}

	/** Give it a non-trivial situation where multiple hops are required to merge */
	@Test void mergeClustersInB() {
		var alg = new ConnectedTwoRowSpeckleFiller();

		alg.labelsB.setTo(0, 0, -1, 1, 2, 3, 4);
		alg.countsB.setTo(1, 2, 3, 4, 5);
		alg.merge.setTo(-1, 0, -1, 4, 1);
		alg.mergeClustersInB();

		assertTrue(alg.countsB.isEquals(12, 0, 3, 0, 0));
		assertTrue(alg.labelsB.isEquals(0, 0, -1, 0, 2, 0, 0));
	}

	@Test void addCountsRowAIntoB() {
		var alg = new ConnectedTwoRowSpeckleFiller();

		alg.connectAtoB.setTo(-1, 1, 0, -1);
		alg.countsA.setTo(1, 2, 3, 4);
		alg.labelsB.setTo(0, 0, -1, 1, 2, 3, 4);
		alg.countsB.setTo(6, 7, 8, 9, 0);
		alg.merge.setTo(-1, 0, -1, -1, -1);
		alg.addCountsRowAIntoB();

		assertTrue(alg.finished.isEquals(0, 3));
		assertTrue(alg.countsB.isEquals(11, 7, 8, 9, 0));
	}

	private void set( String encoded, GrayF32 image ) {
		String[] lines = encoded.split("\n");

		image.reshape(lines[0].length(), lines.length);

		for (int y = 0; y < lines.length; y++) {
			String line = lines[y];
			for (int x = 0; x < line.length(); x++) {
				image.set(x, y, Integer.parseInt("" + line.charAt(x))*valueStep);
			}
		}
	}
}
