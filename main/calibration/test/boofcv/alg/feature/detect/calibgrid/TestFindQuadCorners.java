/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFindQuadCorners {

	/**
	 * Overall test which tests all functions at once
	 */
	@Test
	public void process() {
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		
		for( int i = 0; i < 10; i++ ) {
			contour.add( new Point2D_I32(i,0));
			contour.add( new Point2D_I32(i,9));
		}
		for( int i = 1; i < 9; i++ ) {
			contour.add( new Point2D_I32(0,i));
			contour.add( new Point2D_I32(9,i));
		}

		// remove any structure from the input
		Collections.shuffle(contour,new Random(1234));
		
		FindQuadCorners alg = new FindQuadCorners(0.5,10);

		List<Point2D_I32> corners = alg.process(contour);

		// check the solution and make sure its in the correct order
		Point2D_I32 a = corners.get(0);
		Point2D_I32 b = corners.get(1);
		Point2D_I32 c = corners.get(2);
		Point2D_I32 d = corners.get(3);

		assertEquals(0,a.x);
		assertEquals(0,a.y);

		assertEquals(9,b.x);
		assertEquals(0,b.y);

		assertEquals(9,c.x);
		assertEquals(9,c.y);

		assertEquals(0,d.x);
		assertEquals(9,d.y);
	}

	@Test
	public void findFarthest() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		list.add( new Point2D_I32(1,2));
		list.add( new Point2D_I32(2,23));
		list.add( new Point2D_I32(8,8));

		int index = FindQuadCorners.findFarthest(new Point2D_I32(1,3),list);

		assertEquals(1,index);
	}

	@Test
	public void findAverage() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		list.add( new Point2D_I32(1,2));
		list.add( new Point2D_I32(5,3));
		list.add( new Point2D_I32(2,8));
		
		Point2D_I32 c = FindQuadCorners.findAverage(list);
		
		assertEquals(2,c.x);
		assertEquals(4, c.y);
	}

	@Test
	public void sortByAngle() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();

		for( int i = 0; i < 10; i++ )
			list.add( new Point2D_I32(i,0));

		Point2D_I32 c = new Point2D_I32(5,-10);

		// randomize the order
		Collections.shuffle(list, new Random(8234));
		
		// sanity check
		assertTrue(0 != list.get(0).x);

		FindQuadCorners.sortByAngleCCW(c, list);

		for( int i = 0; i < 10; i++ )
			assertEquals(9-i, list.get(i).x);
	}

	@Test
	public void countInliers() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();
		
		list.add( new Point2D_I32(0,0));
		list.add( new Point2D_I32(1,0));
		list.add( new Point2D_I32(2,0));
		list.add( new Point2D_I32(4,0));
		list.add( new Point2D_I32(3,0));
		list.add( new Point2D_I32(3,10));
		list.add( new Point2D_I32(5,0));
		list.add( new Point2D_I32(6,0));
		list.add( new Point2D_I32(10,0));
		
		assertEquals(5,FindQuadCorners.countInliers(1,7,1,list,1));
		assertEquals(5,FindQuadCorners.countInliers(7,1,-1,list,1));
	}

	@Test
	public void refineCorner() {
		List<Point2D_I32> list = new ArrayList<Point2D_I32>();

		// create a list with a 90 degree corner in it
		list.add( new Point2D_I32(-2,0));
		list.add( new Point2D_I32(-1,0));
		list.add( new Point2D_I32(0,0));
		list.add( new Point2D_I32(1,0));
		list.add( new Point2D_I32(2,0));
		list.add( new Point2D_I32(3,0));
		list.add( new Point2D_I32(4,0));
		list.add( new Point2D_I32(4,1));
		list.add( new Point2D_I32(4,2));
		list.add( new Point2D_I32(4,3));
		list.add( new Point2D_I32(4,4));
		list.add( new Point2D_I32(4,5));
		list.add( new Point2D_I32(4,6));

		// have the center be at a sub-optimal location
		int found = FindQuadCorners.refineCorner(5,2,list);

		// see if it picked the crux of the corner
		assertEquals(6,found);
	}
}
