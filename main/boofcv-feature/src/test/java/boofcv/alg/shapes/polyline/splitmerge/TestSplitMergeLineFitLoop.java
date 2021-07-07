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

package boofcv.alg.shapes.polyline.splitmerge;

import boofcv.struct.ConfigLength;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.RectangleLength2D_I32;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.shapes.polygon.TestContourEdgeIntensity.rectToContour;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSplitMergeLineFitLoop extends BoofStandardJUnit {

	public static final ConfigLength MINIMUM_LENGTH = ConfigLength.relative(0.01,0);
	public static final ConfigLength MINIMUM_LENGTH_5X = ConfigLength.relative(0.05,0);

	DogArray_I32 splits = new DogArray_I32();
	
	/**
	 * Tests contours with zero and one points in them
	 */
	@Test void checkZeroOne() {
		List<Point2D_I32> contour = new ArrayList<>();
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.15, MINIMUM_LENGTH,100);
		alg.process(contour,splits);
		assertEquals(0,splits.size);

		contour.add( new Point2D_I32(2,3));
		alg.process(contour,splits);
		assertEquals(0,splits.size);
	}

	/**
	 * Sees if it can segment a square.
	 */
	@Test void simpleSquareAll() {
		List<Point2D_I32> contour = rectToContour(new RectangleLength2D_I32(0,0,10,5));

		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.15, MINIMUM_LENGTH,100);
		alg.process(contour,splits);
		matchSplitsToExpected(new int[]{8, 12, 21, 25}, splits);
	}

	@Test void selectFarthest() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.15, MINIMUM_LENGTH,100);
		List<Point2D_I32> contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			contour.add( new Point2D_I32(i,0));
		for( int i = 0; i < 10; i++ )
			contour.add( new Point2D_I32(9-i,1));

		int found = alg.selectFarthest(contour);
		assertEquals(0,found);

		for( int offset = 1; offset < 5; offset++ ) {
			List<Point2D_I32> adjusted = shiftContour(contour,offset);
			found = alg.selectFarthest(adjusted);
			assertEquals(9-offset,found);
		}
	}

	/**
	 * Segment where no splitting is required
	 */
	@Test void splitPixels_nosplit() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.15,MINIMUM_LENGTH_5X,100);
		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.N = alg.contour.size();
		alg.splits = splits;

		alg.splitPixels(0,5);
		assertEquals(0,alg.splits.size);

		alg.splitPixels(0,9);
		assertEquals(0,alg.splits.size);

		alg.splitPixels(5,1);
		assertEquals(0,alg.splits.size);

		alg.splitPixels(5,9);
		assertEquals(0,alg.splits.size);
	}

	/**
	 * Basic tests with a single split
	 */
	@Test void splitPixels() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.01, MINIMUM_LENGTH,100);
		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.N = alg.contour.size();
		alg.contour.get(4).y = 6;// set it just above the threshold
		alg.splits = splits;

		// tests which require splits on recursive calls
		alg.splitPixels(0,3);
		assertEquals(0, alg.splits.size);
		alg.splitPixels(0, 4);
		assertEquals(1, alg.splits.size);
		assertEquals(3,alg.splits.data[0]);

		// will get a hit from its recursive call.
		// gets split on both sides of the impulse because the impulse is so far from all the other lines
		alg.splits.reset();
		alg.splitPixels(0, 9);
		assertEquals(3, alg.splits.size);
		assertEquals(3,alg.splits.data[0]);
		assertEquals(4,alg.splits.data[1]);
		assertEquals(5,alg.splits.data[2]);

		// Test a few edge cases
		alg.splits.reset();
		alg.splitPixels(0,1);
		assertEquals(0,alg.splits.size);
		alg.splitPixels(9,1);
		assertEquals(0,alg.splits.size);
	}

	/**
	 * Multiple splits are required
	 */
	@Test void splitPixels_multiple() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.001, MINIMUM_LENGTH,100);
		alg.contour = new ArrayList<>();
		alg.contour.add( new Point2D_I32(0,0));
		alg.contour.add( new Point2D_I32(10,10));
		alg.contour.add( new Point2D_I32(20,20));
		alg.contour.add( new Point2D_I32(30,30));
		alg.contour.add( new Point2D_I32(50,20));
		alg.contour.add( new Point2D_I32(60,10));
		alg.contour.add( new Point2D_I32(70,0));
		alg.N = alg.contour.size();

		alg.splits = splits;
		alg.splitPixels(0,alg.N-1);
		assertEquals(2,alg.splits.size);
		assertEquals(3,alg.splits.data[0]);
		assertEquals(4,alg.splits.data[1]);

		alg.contour = shiftContour(alg.contour,2);
		alg.splits.reset();
		alg.splitPixels(alg.N-2,alg.N-1);
		assertEquals(2,alg.splits.size);
		assertEquals(1,alg.splits.data[0]);
		assertEquals(2,alg.splits.data[1]);
	}


	@Test void mergeLines() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.05, MINIMUM_LENGTH,100);
		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(9-i,3));
		alg.N = alg.contour.size();

		alg.splits = splits;
		alg.splits.add(0);
		alg.splits.add(5);
		alg.splits.add(9);
		alg.splits.add(10);
		alg.splits.add(15);
		alg.splits.add(19);

		assertTrue(alg.mergeSegments());
		assertTrue(matchSplitsToExpected(new int[]{0, 9, 10, 19}, alg.splits));

		// merge on split 0, special case
		alg.splits.reset();
		alg.splits.add(5);
		alg.splits.add(9);
		alg.splits.add(10);
		alg.splits.add(15);
		alg.splits.add(19);
		alg.splits.add(0);
		assertTrue(alg.mergeSegments());
		assertTrue(matchSplitsToExpected(new int[]{9, 10, 19, 0}, alg.splits));
	}

	@Test void splitSegments() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.001, MINIMUM_LENGTH,100);
		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.contour.get(4).y=5;
		alg.N = alg.contour.size();

		alg.splits = splits;
		alg.splits.add(0);
		alg.splits.add(6);

		assertTrue(alg.splitSegments());
		assertTrue(matchSplitsToExpected(new int[]{0, 4, 6}, alg.splits));
	}

	@Test void circularDistance() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.15,MINIMUM_LENGTH_5X,100);
		alg.N = 15;

		assertEquals(0, alg.circularDistance(0, 0));
		assertEquals(1, alg.circularDistance(0, 1));
		assertEquals(14,alg.circularDistance(0,14));

		assertEquals(0,alg.circularDistance(5,5));
		assertEquals(1,alg.circularDistance(5,6));
		assertEquals(14,alg.circularDistance(5,4));
	}

	@Test void selectSplitOffset() {

		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(9.0/9.0, MINIMUM_LENGTH,100);

		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.contour.get(4).y = 10;
		alg.N = alg.contour.size();
		alg.line.slope.x = 1;


		// no wrapping around
		int found = alg.selectSplitOffset(0,9);
		assertEquals(4,found);
		found = alg.selectSplitOffset(0,5);
		assertEquals(4, found);
		found = alg.selectSplitOffset(0,4);
		assertEquals(-1, found);
		found = alg.selectSplitOffset(0,3);
		assertEquals(-1, found);
		found = alg.selectSplitOffset(3,5);
		assertEquals(1, found);

		// wrapping around
		found = alg.selectSplitOffset(5,6);
		assertEquals(-1,found);
		found = alg.selectSplitOffset(5,8);
		assertEquals(-1, found);
		found = alg.selectSplitOffset(9,8);
		assertEquals(5, found);

		// test the threshold
		alg.setSplitFraction(10.1/9.00);
		found = alg.selectSplitOffset(0,9);
		assertEquals(-1,found);
		alg.setSplitFraction(9.9/9.00);
		found = alg.selectSplitOffset(0,9);
		assertEquals(4,found);
	}

	/**
	 * Makes sure the selectSplitOffset is obeying the minimumSideLengthPixel parameter
	 */
	@Test void selectSplitOffset_minimumSideLengthPixel() {
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(9.99/9.0, MINIMUM_LENGTH,100);

		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.contour.get(5).y = 10;
		alg.N = alg.contour.size();
		alg.line.slope.x = 1;

		// check the default of 1 pixel
		selectSplitOffset_minimumSideLengthPixel(alg,1);

		// user specified value of 2
		alg.minimumSideLengthPixel = 2;
		selectSplitOffset_minimumSideLengthPixel(alg,2);
	}

	private void selectSplitOffset_minimumSideLengthPixel(SplitMergeLineFitLoop alg, int minimum ) {
		// test positive cases
		int found = alg.selectSplitOffset(5-minimum,9);
		assertEquals(minimum,found);
		found = alg.selectSplitOffset(0, 5+minimum);
		assertEquals(5,found);

		// negative cases that should be within the threshold
		found = alg.selectSplitOffset(5-minimum+1,9);
		assertEquals(-1,found);
		found = alg.selectSplitOffset(0,5+minimum-1);
		assertEquals(-1,found);
	}

	/**
	 * Checks to make sure the minimum side length is correctly set
	 */
	@Test void set_minimumSideLengthPixel() {
		List<Point2D_I32> contour = new ArrayList<>();

		for (int i = 0; i < 30; i++) {
			contour.add(new Point2D_I32(i, 0));
		}

		ConfigLength cl = ConfigLength.relative(0.2,0);
		SplitMergeLineFitLoop alg = new SplitMergeLineFitLoop(0.001,cl,100);
		alg.process(contour,splits);

		assertEquals(contour.size()/5,alg.minimumSideLengthPixel);
	}

	public static List<Point2D_I32> shiftContour( List<Point2D_I32> contour , int offset ) {
		List<Point2D_I32> ret = new ArrayList<>();
		for( int i = 0; i < contour.size(); i++ ) {
			ret.add( contour.get( (i+offset)%contour.size()));
		}
		return ret;
	}

	public static boolean matchSplitsToExpected(int[] expected, DogArray_I32 found) {
		assertEquals(expected.length,found.size());

		for (int i = 0; i < expected.length; i++) {
			boolean match = true;
			for (int j = 0; j < expected.length; j++) {
				if( expected[j] != found.get((i+j)%4)) {
					match = false;
					break;
				}
			}
			if( match )
				return true;
		}
		return false;
	}
}
