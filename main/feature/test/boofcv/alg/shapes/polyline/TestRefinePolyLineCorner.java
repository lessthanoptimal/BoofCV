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
import georegression.geometry.UtilLine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestRefinePolyLineCorner {

	Random rand = new Random(234);

	/**
	 * Fit to a square
	 */
	@Test
	public void fit_quad() {
		int x0 = 10,y0 = 15;
		int x1 = 60,y1 = 99;

		List<Point2D_I32> points = new ArrayList<>();
		addPoints(x0,y0,x1,y0,points);
		addPoints(x1,y0,x1,y1,points);
		addPoints(x1,y1,x0,y1,points);
		addPoints(x0,y1,x0,y0,points);

		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);

		GrowQueue_I32 corners = new GrowQueue_I32();
		corners.add(0);
		corners.add(50);
		corners.add(50+84);
		corners.add(50+84+50);

		assertTrue(alg.fit(points, corners));

		assertEquals(0,   corners.get(0));
		assertEquals(50,  corners.get(1));
		assertEquals(134, corners.get(2));
		assertEquals(184, corners.get(3));
	}

	/**
	 * Fit to a square, but only a disconnected polyline on 3 sides
	 */
	@Test
	public void fit_quad_segment() {
		int x0 = 10,y0 = 15;
		int x1 = 60,y1 = 99;

		List<Point2D_I32> points = new ArrayList<>();
		addPoints(x0,y0,x1,y0,points);
		addPoints(x1,y0,x1,y1,points);
		addPoints(x1,y1,x0,y1,points);

		RefinePolyLineCorner alg = new RefinePolyLineCorner(false);

		for( int i = 0; i < 10; i++ ) {
			GrowQueue_I32 corners = new GrowQueue_I32();
			corners.add(0);
			corners.add(50  + rand.nextInt(6)-3);
			corners.add(50 + 84 + rand.nextInt(6)-3);
			corners.add(points.size()-1);

			assertTrue(alg.fit(points, corners));

			assertEquals(0, corners.get(0));
			assertEquals(50, corners.get(1));
			assertEquals(134, corners.get(2));
			assertEquals(points.size()-1, corners.get(3));
		}
	}

	/**
	 * Fit six sided shape
	 */
	@Test
	public void fit_six_sides() {
		List<Point2D_I32> points = new ArrayList<>();
		addPoints(0,0,20,0,points);
		addPoints(20,0,20,20,points);
		addPoints(20,20,40,20,points);
		addPoints(40,20,40,40, points);
		addPoints(40,40, 0,40, points);
		addPoints(0 ,40, 0,0, points);

		GrowQueue_I32 corners = new GrowQueue_I32();
		corners.add(0);
		corners.add(20);
		corners.add(40);
		corners.add(60);
		corners.add(80);
		corners.add(120);

		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);
		for (int i = 0; i < 10; i++) {
			// noise up the inputs
			for (int j = 0; j < corners.size(); j++) {
				corners.data[j] = CircularIndex.addOffset(corners.data[j], rand.nextInt(10) - 5, points.size());
			}

			assertTrue(alg.fit(points, corners));

			assertEquals(0, corners.get(0));
			assertEquals(20, corners.get(1));
			assertEquals(40,corners.get(2));
			assertEquals(60,corners.get(3));
			assertEquals(80,corners.get(4));
			assertEquals(120,corners.get(5));
		}
	}

	/**
	 * easy straight forward case
	 */
	@Test
	public void optimize_easy() {
		List<Point2D_I32> contour = new ArrayList<>();
		addPoints(0, 0, 20, 0, contour);
		addPoints(20, 0, 20, 20, contour);

		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);
		alg.searchRadius = 5;
		int found = alg.optimize(contour,2,16,36);
		assertEquals(20,found);
	}

	/**
	 * Test case where a local minimum will cause it to get stuck if a local search is performed
	 */
	@Test
	public void optimize_kinky() {
		List<Point2D_I32> contour = new ArrayList<>();
		addPoints(0, 0, 20, 0, contour);
		addPoints(20, 0, 20, 20, contour);

		// add a kink which could throw it off locally
		contour.get(17).set(17, 1);
		contour.get(18).set(18, 2);

		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);
		alg.searchRadius = 5;
		int found = alg.optimize(contour,2,16,36);
		assertEquals(20,found);
	}

	@Test
	public void distanceSum() {
		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);

		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			contour.add( new Point2D_I32(i,2));
		}
		LineGeneral2D_F64 line = UtilLine2D_F64.convert(new LineParametric2D_F64(0,0,1,0),(LineGeneral2D_F64)null);

		// normal case
		assertEquals(2 * 16, alg.distanceSum(line, 2, 17, contour), 1e-8);

		// boundary case
		assertEquals(2 * 15, alg.distanceSum(line, 10, 4, contour), 1e-8);
	}

	private void addPoints( int x0 , int y0 , int x1 , int y1 , List<Point2D_I32> points ) {
		if( x0 == x1 ) {
			int length = Math.abs(y1-y0);
			int dir = y1>y0 ? 1 : -1;
			for (int y = 0; y < length; y++) {
				points.add(new Point2D_I32(x0,y0+dir*y));
			}
		} else {
			int length = Math.abs(x1-x0);
			int dir = x1>x0 ? 1 : -1;
			for (int x = 0; x < length; x++) {
				points.add(new Point2D_I32(x0+dir*x,y0));
			}
		}
	}

	/**
	 * Test to see if it gracefully handles the case where there are too few points
	 */
	@Test
	public void tooFewPoints() {
		RefinePolyLineCorner alg = new RefinePolyLineCorner(true);

		GrowQueue_I32 corners = new GrowQueue_I32();
		List<Point2D_I32> contour = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			assertFalse(alg.fit(contour, corners));
			corners.add(i);
			contour.add(new Point2D_I32(i, 2));
		}
		assertTrue(alg.fit(contour, corners));
	}

}