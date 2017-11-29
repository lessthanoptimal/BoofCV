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

import boofcv.alg.shapes.polyline.splitmerge.PolylineSplitMerge.Corner;
import georegression.metric.Distance2D_F64;
import georegression.misc.GrlConstants;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.LinkedList.Element;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPolylineSplitMerge {

	@Test
	public void process_perfectSquare() {
		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.setCornerScorePenalty(0.1);
		alg.setMinimumSideLength(5);
		alg.setMaxNumberOfSideSamples(10);
		alg.setConvex(true);

		List<Point2D_I32> contour = rect(10,12,20,24);

		alg.process(contour);

		PolylineSplitMerge.CandidatePolyline result = alg.getBestPolyline();

//		for (int i = 0; i < result.splits.size; i++) {
//			System.out.println(contour.get(result.splits.get(i)));
//		}

		assertEquals(4,result.splits.size);
		assertEquals(0.1*4,result.score, 1e-8);
	}

	@Test
	public void savePolyline() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		alg.getPolylines().grow();
		alg.getPolylines().grow();
		alg.getPolylines().grow();
		alg.getPolylines().grow();

		alg.addCorner(0).object.sideError = 10;
		alg.addCorner(0);
		alg.addCorner(0);
		alg.addCorner(0);

		alg.savePolyline();

		assertTrue(alg.getPolylines().get(1).score > 0);
		assertEquals(4,alg.getPolylines().get(1).splits.size);

		fail("update to check if saved or not");
	}

	@Test
	public void computeScore() {
		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.addCorner(0).object.sideError = 5;
		alg.addCorner(0).object.sideError = 6;
		alg.addCorner(0).object.sideError = 1;

		double expected = 12/3.0 + 0.5*3;
		double found = PolylineSplitMerge.computeScore(alg.list,0.5);

		assertEquals(expected,found,1e-8);
	}

	/**
	 * Give it an obvious triangle and see if it finds it
	 */
	@Test
	public void findInitialTriangle() {
		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			contour.add(new Point2D_I32(i,i));
		}
		for (int i = 0; i < 10; i++) {
			contour.add(new Point2D_I32(9-i,9));
		}
		for (int i = 0; i < 8; i++) {
			contour.add(new Point2D_I32(0,8-i));
		}

		PolylineSplitMerge alg = new PolylineSplitMerge();

		assertTrue(alg.findInitialTriangle(contour));

		assertEquals(3,alg.list.size());

		// the order was specially selected knowing what the current algorithm is
		// te indexes are what it should be no matter what
		Element<Corner> e = alg.list.getHead();
		assertEquals(9,e.object.index);e = e.next;
		assertEquals(19,e.object.index);e = e.next;
		assertEquals(0,e.object.index);
	}

	/**
	 * Case where it should add a corner and then remove a corner
	 */
	@Test
	public void increaseNumberOfSidesByOne() {
		List<Point2D_I32> contour = rect(10,12,20,22);

		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.addCorner(5);
		alg.addCorner(10);
		alg.addCorner(20);
		alg.addCorner(30);

		// set up polyline variables
		Element<Corner> e = alg.list.getHead();
		while( e != null ) {
			e.object.splitable = false;
			e = e.next;
		}
		e = alg.list.getTail();
		e.object.splitable = true;
		e.object.sideError = alg.computeSideError(contour,e.object.index,5);
		alg.setSplitVariables(contour,e,alg.list.getHead());


		assertTrue(alg.increaseNumberOfSidesByOne(contour));

		assertEquals(4,alg.list.size());
		e = alg.list.getHead();
		assertEquals(0,e.object.sideError,1e-8);
		assertEquals(10,e.object.index);e = e.next;
		assertEquals(0,e.object.sideError,1e-8);
		assertEquals(20,e.object.index);e = e.next;
		assertEquals(0,e.object.sideError,1e-8);
		assertEquals(30,e.object.index);e = e.next;
		assertEquals(0,e.object.sideError,1e-8);
		assertEquals(0,e.object.index);
	}

	@Test
	public void selectCornerToSplit() {
		PolylineSplitMerge alg = new PolylineSplitMerge();
		Element<Corner> c0 = alg.addCorner(0);
		Element<Corner> c1 = alg.addCorner(10);
		Element<Corner> c2 = alg.addCorner(20);
		Element<Corner> c3 = alg.addCorner(30);
		Element<Corner> c4 = alg.addCorner(40);

		// net reduction of 2
		c2.object.splitable = true;
		c2.object.sideError = 6;
		c2.object.splitError0 = 1;
		c2.object.splitError1 = 3;
		// net reduction of 1
		c3.object.splitable = true;
		c3.object.sideError = 6;
		c3.object.splitError0 = 5;
		c3.object.splitError1 = 0;
		// small split error but an increase
		c1.object.splitable = true;
		c1.object.sideError = 2;
		c1.object.splitError0 = 1;
		c1.object.splitError1 = 2;
		//c0 is no change
		// massive reduction but marked as not splittable
		c4.object.splitable = false;
		c4.object.sideError = 20;

		assertTrue(c2==alg.selectCornerToSplit());

	}

	/**
	 * Test case where the corner is removed
	 */
	@Test
	public void selectAndRemoveCorner_positive() {
		List<Point2D_I32> contour = rect(10,12,20,18);

		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.setCornerScorePenalty(0.5);
		alg.addCorner(0);
		alg.addCorner(5);
		alg.addCorner(9);
		alg.addCorner(16);
		Element<Corner> e1 = alg.list.getHead();
		Element<Corner> e2 = e1.next;
		alg.getPolylines().grow(); // need to give a polyline to store the results in

		e1.object.sideError = 0;
		e2.object.sideError = 0;

		alg.selectAndRemoveCorner(contour);
		assertEquals(3,alg.list.size());
		fail("update. no longer given an edge");
	}

	/**
	 * Test case where the corner is NOT removed
	 */
	@Test
	public void selectAndRemoveCorner_negative() {
		List<Point2D_I32> contour = rect(10,12,20,18);

		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.setCornerScorePenalty(0.5);
		alg.addCorner(0);
		alg.addCorner(5);
		alg.addCorner(9);
		alg.addCorner(16);
		Element<Corner> e1 = alg.list.getHead().next;
		Element<Corner> e2 = e1.next;
		alg.getPolylines().grow(); // need to give a polyline to store the results in

		e1.object.sideError = 0;
		e2.object.sideError = 0;

		alg.selectAndRemoveCorner(contour);
		assertEquals(4,alg.list.size());
		fail("update. no longer given an edge");
	}

//	@Test
//	public void getCurrentPolylineScore() {
//		PolylineSplitMerge.CandidatePolyline a;
//
//		PolylineSplitMerge alg = new PolylineSplitMerge();
//		alg.addCorner(0);
//		alg.addCorner(0);
//		alg.addCorner(0);
//		alg.getPolylines().grow().score = 2;
//
//		assertEquals(2,alg.getCurrentPolylineScore(),1e-8);
//
//		alg.getPolylines().grow().score = 3;
//		assertEquals(2,alg.getCurrentPolylineScore(),1e-8);
//		alg.addCorner(0);
//		assertEquals(3,alg.getCurrentPolylineScore(),1e-8);
//	}

	@Test
	public void findCornerSeed() {
		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,0));
		}
		contour.add( new Point2D_I32(2,1));
		contour.add( new Point2D_I32(2,2));

		assertEquals(19,PolylineSplitMerge.findCornerSeed(contour));
	}

	@Test
	public void computePotentialSplitScore() {
		PolylineSplitMerge alg = new PolylineSplitMerge();
		alg.setMinimumSideLength(5);
		alg.setThresholdSideSplitScore(0);

		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,0));
		}

		// add some texture
		contour.get(3).y = 5;
		contour.get(15).y = 5;
		contour.get(10).y = 20; // this will be selected as the corner since it's the farthest away

		alg.addCorner(0);
		alg.addCorner(19);
		Element<Corner> e = alg.list.getHead();
		e.object.sideError =20;

		alg.computePotentialSplitScore(contour,e,false);

		assertTrue(e.object.splitable);
		assertTrue(e.object.splitError0 >0);
		assertTrue(e.object.splitError1 >0);
		assertEquals(10,e.object.splitLocation);
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
			Corner c = alg.corners.grow();
			c.index = i*5;
			alg.list.pushTail(c);
		}

		alg.setMinimumSideLength(5);
		alg.setThresholdSideSplitScore(0); // turn off this test

//		assertTrue(alg.canBeSplit(50,alg.list.getElement(5,true)));
//		assertTrue(alg.canBeSplit(50,alg.list.getElement(9,true)));
//
//		alg.setMinimumSideLength(6);
//		assertFalse(alg.canBeSplit(50,alg.list.getElement(5,true)));
//		assertFalse(alg.canBeSplit(50,alg.list.getElement(9,true)));
//
//		// test side split score
//		alg.setMinimumSideLength(5);
//		alg.setThresholdSideSplitScore(1);
//
//		alg.list.getElement(5,true).object.sideError = 1;
//		assertTrue(alg.canBeSplit(50,alg.list.getElement(5,true)));
//		alg.list.getElement(5,true).object.sideError = 0.9999999;
//		assertFalse(alg.canBeSplit(50,alg.list.getElement(5,true)));
		fail("update");
	}

	@Test
	public void canBeSplit_special_case() {
//		PolylineSplitMerge alg = new PolylineSplitMerge();
//		alg.setMinimumSideLength(5);
//		alg.setThresholdSideSplitScore(0);
//
//		alg.addCorner(0);
//		alg.addCorner(10);
//
//		assertTrue(alg.canBeSplit(11,alg.list.getHead()));
		fail("update");
	}

	@Test
	public void next() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		Corner a = alg.corners.grow();
		Corner b = alg.corners.grow();
		Corner c = alg.corners.grow();

		alg.list.pushTail(a);
		alg.list.pushTail(b);
		alg.list.pushTail(c);

		assertTrue(c==alg.next(alg.list.find(b)).object);
		assertTrue(a==alg.next(alg.list.find(c)).object);
	}

	@Test
	public void previous() {
		PolylineSplitMerge alg = new PolylineSplitMerge();

		Corner a = alg.corners.grow();
		Corner b = alg.corners.grow();
		Corner c = alg.corners.grow();

		alg.list.pushTail(a);
		alg.list.pushTail(b);
		alg.list.pushTail(c);

		assertTrue(a==alg.previous(alg.list.find(b)).object);
		assertTrue(c==alg.previous(alg.list.find(a)).object);
	}

	@Test
	public void ensureTriangleIsCCW() {
		fail("Implement");
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

	public static List<Point2D_I32> rect( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> out = new ArrayList<>();

		out.addAll( line(x0,y0,x1,y0));
		out.addAll( line(x1,y0,x1,y1));
		out.addAll( line(x1,y1,x0,y1));
		out.addAll( line(x0,y1,x0,y0));

		return out;
	}

	private static List<Point2D_I32> line( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> out = new ArrayList<>();

		int lengthY = Math.abs(y1-y0);
		int lengthX = Math.abs(x1-x0);

		int x,y;
		if( lengthY > lengthX ) {
			for (int i = 0; i < lengthY; i++) {
				x = x0 + (x1-x0)*lengthX*i/lengthY;
				y = y0 + (y1-y0)*i/lengthY;
				out.add( new Point2D_I32(x,y));
			}
		} else {
			for (int i = 0; i < lengthX; i++) {
				x = x0 + (x1-x0)*i/lengthX;
				y = y0 + (y1-y0)*lengthY*i/lengthX;
				out.add( new Point2D_I32(x,y));
			}
		}
		return out;
	}
}