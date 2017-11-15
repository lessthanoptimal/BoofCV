/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import georegression.metric.Distance2D_F64;
import georegression.misc.GrlConstants;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.shapes.polyline.keypoint.TestContourInterestPointDetector.rect;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPolylineSplitMerge {

	@Test
	public void process_perfectSquare() {
		fail("Implement");
	}

	@Test
	public void savePolyline() {
		fail("Implement");
	}

	@Test
	public void computeScore() {
		fail("Implement");
	}

	@Test
	public void findInitialTriangle() {
		fail("Implement");
	}

	@Test
	public void increaseNumberOfSidesByOne() {
		fail("Implement");
	}

	@Test
	public void selectCornerToSplit() {
		fail("Implement");
	}

	@Test
	public void considerAndRemoveCorner() {
		fail("Implement");
	}

	@Test
	public void getCurrentPolylineScore() {
		fail("Implement");
	}

	@Test
	public void findCornerSeed() {
		fail("Implement");
	}

	@Test
	public void computePotentialSplitScore() {
		fail("Implement");
	}

	@Test
	public void computeSideError_exhaustive() {

		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.maxNumberOfSideSamples = 30; // have it exhaustively sample all pixels

		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,0));
		}
		assertEquals(0,alg.computeSideError(contour,0,19), GrlConstants.TEST_F64);
		for (int i = 1; i < 19; i++) {
			contour.get(i).y = 5;
		}
		contour.get(10).y = 0; // need this to be zero so that two lines are the same
//		// average SSE
		double expected = (5.0*5.0*17)/19.0;
		assertEquals(expected,alg.computeSideError(contour,0,19), GrlConstants.TEST_F64);
		// the error should have this property to not bias it based on the number of sides
		expected = (5*5*9)/10.0 + (5*5*8)/9.0;
		double split = alg.computeSideError(contour,0,10)+alg.computeSideError(contour,10,19);
		assertEquals(expected,split, GrlConstants.TEST_F64);

		//----------- Now in the reverse direction
		expected = (5*5*8)/10.0;
		assertEquals(expected,alg.computeSideError(contour,10,0), GrlConstants.TEST_F64);
	}

	@Test
	public void computeSideError_skip() {
		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.maxNumberOfSideSamples = 5; // it will sub sample

		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,0));
		}
		assertEquals(0,alg.computeSideError(contour,0,19), GrlConstants.TEST_F64);
		for (int i = 1; i < 19; i++) {
			contour.get(i).y = 5;
		}
		contour.get(10).y = 0;

		// see if it is within the expected by some error margin
		double expected = (5.0*5.0*17)/19.0;
		assertEquals(expected,alg.computeSideError(contour,0,19), expected*0.15);

		//----------- Now in the reverse direction
		expected = (5*5*8)/10.0;
		assertEquals(expected,alg.computeSideError(contour,10,0), expected*0.15);
	}

	@Test
	public void addCorner() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		assertEquals(0,alg.list.size());
		alg.addCorner(3);
		assertEquals(1,alg.list.size());
		alg.addCorner(4);
		assertEquals(2,alg.list.size());
		assertEquals(3,alg.list.getElement(0,true).object.index);
		assertEquals(4,alg.list.getElement(1,true).object.index);
	}

	@Test
	public void canBeSplit() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		for (int i = 0; i < 10; i++) {
			PolylineSplitMerge.Corner c = alg.corners.grow();
			c.index = i*5;
			alg.list.pushTail(c);
		}

		alg.setMinimumSideLength(5);
		alg.setThresholdSideSplitScore(0); // turn off this test

		assertTrue(alg.canBeSplit(50,alg.list.getElement(5,true)));
		assertTrue(alg.canBeSplit(50,alg.list.getElement(9,true)));

		alg.setMinimumSideLength(6);
		assertFalse(alg.canBeSplit(50,alg.list.getElement(5,true)));
		assertFalse(alg.canBeSplit(50,alg.list.getElement(9,true)));

		// test side split score
		alg.setMinimumSideLength(5);
		alg.setThresholdSideSplitScore(1);

		alg.list.getElement(5,true).object.sideScore = 1;
		assertTrue(alg.canBeSplit(50,alg.list.getElement(5,true)));
		alg.list.getElement(5,true).object.sideScore = 0.9999999;
		assertFalse(alg.canBeSplit(50,alg.list.getElement(5,true)));
	}

	@Test
	public void next() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		PolylineSplitMerge.Corner a = alg.corners.grow();
		PolylineSplitMerge.Corner b = alg.corners.grow();
		PolylineSplitMerge.Corner c = alg.corners.grow();

		alg.list.pushTail(a);
		alg.list.pushTail(b);
		alg.list.pushTail(c);

		assertTrue(c==alg.next(alg.list.find(b)).object);
		assertTrue(a==alg.next(alg.list.find(c)).object);
	}

	@Test
	public void previous() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		PolylineSplitMerge.Corner a = alg.corners.grow();
		PolylineSplitMerge.Corner b = alg.corners.grow();
		PolylineSplitMerge.Corner c = alg.corners.grow();

		alg.list.pushTail(a);
		alg.list.pushTail(b);
		alg.list.pushTail(c);

		assertTrue(a==alg.previous(alg.list.find(b)).object);
		assertTrue(c==alg.previous(alg.list.find(a)).object);
	}

	/**
	 * Create a rectangle and feed it every point in the rectangle and see if it has the expected response
	 */
	@Test
	public void sanityCheckConvex_positive() {
		List<Point2D_I32> contour = rect(5,6,12,20);

		for (int i = 0; i < contour.size(); i++) {
			int farthest = -1;
			double distance = -1;

			for (int j = 0; j < contour.size(); j++) {
				double d = contour.get(i).distance(contour.get(j));
				if( d > distance ) {
					distance = d;
					farthest = j;
				}
			}

			assertTrue( PolylineSplitMerge.sanityCheckConvex(contour,i,farthest));
		}
	}

	/**
	 * Give it a scenario where it should fail
	 */
	@Test
	public void sanityCheckConvex_negative() {
		List<Point2D_I32> contour = rect(5,6,12,20);
		assertFalse( PolylineSplitMerge.sanityCheckConvex(contour,2,3));
	}

	@Test
	public void distanceSq() {
		Point2D_I32 a = new Point2D_I32(2,4);
		Point2D_I32 b = new Point2D_I32( 10,-3);

		int expected = a.distance2(b);
		double found = PolylineSplitMerge.distanceSq(a,b);

		assertEquals(expected,found,GrlConstants.TEST_F64);
	}

	@Test
	public void assignLine() {
		List<Point2D_I32> contour = new ArrayList<>();

		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,2));
		}

		// make these points offset from all the others. That way if it grabs the wrong points the line will be wrong
		contour.get(1).set(1,5);
		contour.get(9).set(9,5);

		LineParametric2D_F64 line = new LineParametric2D_F64();

		PolylineSplitMerge.assignLine(contour,1,9,line);

		assertEquals(0, Distance2D_F64.distanceSq(line,1,5), GrlConstants.TEST_F64);
		assertEquals(0, Distance2D_F64.distanceSq(line,9,5), GrlConstants.TEST_F64);

	}
}