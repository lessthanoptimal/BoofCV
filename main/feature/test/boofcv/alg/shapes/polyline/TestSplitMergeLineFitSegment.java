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

package boofcv.alg.shapes.polyline;

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSplitMergeLineFitSegment {

	private static final double MIN_SPLIT = 0.1;

	/**
	 * Tests contours with zero and one points in them
	 */
	@Test
	public void checkZeroOne() {
		List<Point2D_I32> contour = new ArrayList<>();
		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.15,MIN_SPLIT,100);
		alg.process(contour);
		assertEquals(0,alg.getSplits().size);

		contour.add( new Point2D_I32(2,3));
		alg.process(contour);
		assertEquals(0,alg.getSplits().size);
	}

	/**
	 * Simple case with a zig-zag pattern
	 */
	@Test
	public void simpleCase() {
		List<Point2D_I32> contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			contour.add( new Point2D_I32(i,0));
		for( int i = 1; i < 5; i++ )
			contour.add( new Point2D_I32(9,i));
		for( int i = 1; i < 10; i++ )
			contour.add( new Point2D_I32(9+i,4));
		for( int i = 2; i < 5; i++ )
			contour.add( new Point2D_I32(18,5-i));

		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.15,MIN_SPLIT,100);
		alg.process(contour);

		GrowQueue_I32 splits = alg.getSplits();
		assertEquals(5 , splits.size );
		assertEquals(0,alg.splits.data[0]);
		assertEquals(9,alg.splits.data[1]);
		assertEquals(13,alg.splits.data[2]);
		assertEquals(22,alg.splits.data[3]);
		assertEquals(25,alg.splits.data[4]);
	}

	@Test
	public void splitSegments() {
		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.15,MIN_SPLIT,100);
		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.contour.get(4).y=5;

		// single split
		alg.splits.add(0);
		alg.splits.add(9);
		assertTrue(alg.splitSegments());
		assertEquals(3,alg.splits.size);
		assertEquals(0,alg.splits.data[0]);
		assertEquals(4,alg.splits.data[1]);
		assertEquals(9,alg.splits.data[2]);

	}

	@Test
	public void selectSplitOffset() {
		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.15,MIN_SPLIT,100);

		alg.contour = new ArrayList<>();
		for( int i = 0; i < 10; i++ )
			alg.contour.add( new Point2D_I32(i,0));
		alg.contour.get(4).y = 10;
		alg.line.slope.x = 5;


		int found = alg.selectSplitBetween(0, 9);
		assertEquals(4,found);
		found = alg.selectSplitBetween(0, 5);
		assertEquals(4, found);
		found = alg.selectSplitBetween(0, 4);
		assertTrue(found < 4);
		found = alg.selectSplitBetween(0, 3);
		assertEquals(-1, found);
		found = alg.selectSplitBetween(5, 9);
		assertEquals(-1, found);
		found = alg.selectSplitBetween(1, 6);
		assertEquals(4, found);
	}

	@Test
	public void mergeSegments() {
		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.001,MIN_SPLIT,100);
		alg.contour = new ArrayList<>();
		alg.contour.add(new Point2D_I32(0,0));
		alg.contour.add(new Point2D_I32(1,0));
		alg.contour.add(new Point2D_I32(3,0));
		alg.contour.add(new Point2D_I32(3,4));
		alg.contour.add(new Point2D_I32(4,4));
		alg.contour.add(new Point2D_I32(5,4));
		for( int i = 0; i < alg.contour.size(); i++ ) {
			alg.splits.add(i);
		}

		assertTrue(alg.mergeSegments());

		assertEquals(3,alg.splits.size);
		assertEquals(0,alg.splits.data[0]);
		assertEquals(2,alg.splits.data[1]);
		assertEquals(5,alg.splits.data[2]);
	}

	/**
	 * Makes sure the selectSplitOffset is obeying the minimumSideLengthPixel parameter
	 */
	@Test
	public void selectSplitBetween_minimumSideLengthPixel() {
		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.001,MIN_SPLIT,100);
		alg.contour = new ArrayList<>();

		// contour is straight with one point that's way off
		for (int i = 0; i < 20; i++) {
			alg.contour.add(new Point2D_I32(i, 0));
		}
		alg.contour.get(10).set(10,10);

		// force it to use the default of 1 pixel
		alg.minimumSideLengthPixel = 0;
		// shouldn't be affect by the min side
		assertEquals(10,alg.selectSplitBetween(0,19));

		int r = 0;
		assertEquals(10,alg.selectSplitBetween(10-r-1,10+r+1));
		assertEquals(-1,alg.selectSplitBetween(10-r,10+r+1));
		assertEquals(-1,alg.selectSplitBetween(10-r-1,10+r));

		alg.minimumSideLengthPixel = 2;
		r = 1;
		assertEquals(10,alg.selectSplitBetween(10-r-1,10+r+1));
		assertEquals(-1,alg.selectSplitBetween(10-r,10+r+1));
		assertEquals(-1,alg.selectSplitBetween(10-r-1,10+r));
	}

	/**
	 * Checks to make sure the minimum side length is correctly set
	 */
	@Test
	public void set_minimumSideLengthPixel() {
		double minSplitFraction = 0.1;
		List<Point2D_I32> contour = new ArrayList<>();

		for (int i = 0; i < 30; i++) {
			contour.add(new Point2D_I32(i, 0));
		}

		SplitMergeLineFitSegment alg = new SplitMergeLineFitSegment(0.001,minSplitFraction,100);
		alg.process(contour);

		assertEquals(contour.size()/10,alg.minimumSideLengthPixel);
	}
}
