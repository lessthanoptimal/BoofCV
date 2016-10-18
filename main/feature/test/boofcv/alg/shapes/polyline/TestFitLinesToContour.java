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

import boofcv.misc.CircularIndex;
import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestFitLinesToContour {

	/**
	 * Easy case were the corners are all in perfect location.  Try all permutations of first anchor and second anchor.
	 */
	@Test
	public void fitAnchored_perfect_input() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.setContour(createSquare( 10,12 , 30,40));
		GrowQueue_I32 corners = createSquareCorners(10,12 , 30,40);

		GrowQueue_I32 found = new GrowQueue_I32();
		for (int anchor0 = 0; anchor0 < 4; anchor0++) {
			for (int j = 0; j < 4; j++) {
				if( j == 1 ) continue; // this case it can't optimize
				int anchor1 = (anchor0+j)%4;
				alg.fitAnchored(anchor0,anchor1,corners,found);

				assertEquals(4,found.size());
				for (int i = 0; i < found.size(); i++) {
					assertEquals(corners.get(i),found.get(i));
				}
			}
		}
	}

	/**
	 * Have only one corner off by a few pixels with extremely clean input data
	 */
	@Test
	public void fitAnchored_easy_optimization() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.setContour(createSquare( 10,12 , 30,40));
		GrowQueue_I32 expected = createSquareCorners(10,12 , 30,40);
		GrowQueue_I32 input = createSquareCorners(10,12 , 30,40);
		GrowQueue_I32 found = new GrowQueue_I32();

		input.set(2, input.get(2) + 3);

		alg.fitAnchored(1, 3, input, found);

		for (int i = 0; i < found.size(); i++) {
			assertEquals(expected.get(i), found.get(i));
		}
	}

	@Test
	public void sanityCheckCornerOrder() {
		FitLinesToContour alg = new FitLinesToContour();
		alg.contour = createSquare( 10,12 , 30,40);
		GrowQueue_I32 corners = new GrowQueue_I32();

		corners.add(6);
		corners.add(12);
		corners.add(20);
		corners.add(41);
		corners.add(1);

		// test positive cases first
		for (int i = 0; i < 5; i++) {
			alg.anchor0 = i;
			assertTrue(alg.sanityCheckCornerOrder(3, corners));
		}

		// should fail
		corners.add(8);
		corners.add(3);
		alg.anchor0 = 3;
		assertFalse(alg.sanityCheckCornerOrder(4, corners));
	}

	@Test
	public void linesIntoCorners() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.contour = createSquare( 10,12 , 30,40);
		GrowQueue_I32 corners = createSquareCorners(10,12 , 30,40);

		// first generate the lines it will fit
		alg.lines.resize(3);
		alg.anchor0 = 1;
		alg.fitLinesUsingCorners(3, corners);

		// now extract the corners
		GrowQueue_I32 found = new GrowQueue_I32(corners.size);
		found.resize(corners.size());
		alg.anchor0 = 1;
		alg.linesIntoCorners(3,found);

		// only corners 2 and 3 should be updated with no change
		for (int i = 2; i < found.size(); i++) {
			assertEquals(corners.get(i), found.get(i));
		}

	}

	@Test
	public void fitLinesUsingCorners() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.contour = createSquare( 10,12 , 30,40);
		GrowQueue_I32 corners = createSquareCorners(10,12 , 30,40);

		alg.lines.resize(3);
		alg.anchor0 = 1;
		alg.fitLinesUsingCorners(3, corners);

		LineGeneral2D_F64 expected = new LineGeneral2D_F64();
		for (int i = 0; i < 3; i++) {
			alg.fitLine(corners.get((i+1)%4),corners.get((i+2)%4),expected);

			LineGeneral2D_F64 found = alg.lines.get(i);

			assertEquals(expected.A,found.A,1e-8);
			assertEquals(expected.B,found.B,1e-8);
			assertEquals(expected.C,found.C,1e-8);
		}
	}

	@Test
	public void fitLine() {
		FitLinesToContour alg = new FitLinesToContour();

		// create the rectangle so that two sizes are less than max samples and the other two more
		int w = alg.maxSamples;
		alg.contour = createSquare( 10,12 , 10+w-1,12+w+4);
		GrowQueue_I32 corners = createSquareCorners(10,12 , 10+w-1,12+w+4);

		LineGeneral2D_F64 line = new LineGeneral2D_F64();
		for (int i = 0,j=corners.size()-1; i < corners.size(); j=i,i++) {
			alg.fitLine(corners.get(j),corners.get(i),line);

			// see if the line lies perfectly along the side
			int contour0 = corners.get(j);
			int contour1 = corners.get(i);

			int length = CircularIndex.distanceP(contour0,contour1,alg.contour.size());

			for (int k = 0; k < length; k++) {
				int contourIndex = CircularIndex.addOffset(contour0,k,alg.contour.size());
				Point2D_I32 p = alg.contour.get(contourIndex);
				double found = Distance2D_F64.distance(line,new Point2D_F64(p.x,p.y));
				assertEquals(0,found,1e-8);
			}
		}
	}

	@Test
	public void closestPoint() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.contour = createSquare( 10,12 , 30,40);

		Point2D_F64 p = new Point2D_F64(15.5,11);

		Point2D_I32 corner = alg.contour.get( alg.closestPoint(p));

		assertEquals(15,corner.x);
		assertEquals(12,corner.y);
	}

	private List<Point2D_I32> createSquare( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> output = new ArrayList<>();

		for (int x = x0; x < x1; x++) {
			output.add( new Point2D_I32(x,y0));
		}

		for (int y = y0; y < y1; y++) {
			output.add( new Point2D_I32(x1,y));
		}

		for (int x = x1; x > x0; x--) {
			output.add( new Point2D_I32(x,y1));
		}

		for (int y = y1; y > y0; y--) {
			output.add( new Point2D_I32(x0,y));
		}

		return output;
	}

	private GrowQueue_I32 createSquareCorners( int x0 , int y0 , int x1 , int y1 ) {
		GrowQueue_I32 corners = new GrowQueue_I32();

		int c0 = 0;
		int c1 = c0 + x1-x0;
		int c2 = c1 + y1-y0;
		int c3 = c2 + x1-x0;

		corners.add(c0);
		corners.add(c1);
		corners.add(c2);
		corners.add(c3);

		return corners;
	}
}
