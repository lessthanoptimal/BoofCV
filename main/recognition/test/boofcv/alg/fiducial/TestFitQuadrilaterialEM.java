/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import georegression.geometry.UtilLine2D_F64;
import georegression.geometry.UtilPoint2D_F64;
import georegression.metric.Distance2D_F64;
import georegression.misc.GrlConstants;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestFitQuadrilaterialEM {

	Random rand = new Random(234);

	/**
	 * Run the whole thing with a very simple square.
	 */
	@Test
	public void fit() {
		int x0 = 10,y0 = 15;
		int x1 = 60,y1 = 99;

		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		addPoints(x0,y0,x1,y0,points);
		addPoints(x1,y0,x1,y1,points);
		addPoints(x1,y1,x0,y1,points);
		addPoints(x0, y1, x0, y0, points);

		FitQuadrilaterialEM alg = new FitQuadrilaterialEM();

		GrowQueue_I32 corners = new GrowQueue_I32();
		corners.add(0);
		corners.add(10); // extra corner
		corners.add(50);
		corners.add(50+84);
		corners.add(50+84+50);

		Quadrilateral_F64 result = new Quadrilateral_F64();
		alg.fit(points,corners,result);

		List<Point2D_F64> expected = new ArrayList<Point2D_F64>();
		expected.add( new Point2D_F64(x0,y0));
		expected.add( new Point2D_F64(x1,y0));
		expected.add( new Point2D_F64(x1,y1));
		expected.add( new Point2D_F64(x0,y1));

		assertTrue(findMatch(result.a,expected));
		assertTrue(findMatch(result.b,expected));
		assertTrue(findMatch(result.c,expected));
		assertTrue(findMatch(result.d,expected));
	}

	@Test
	public void performLineEM() {
		// perfect case
		checkPerformLineEM(0,0);

		// a little bit off
		checkPerformLineEM(2,2);

		// a lot more off
		checkPerformLineEM(10,8);
	}

	private void checkPerformLineEM( int offX , int offY ) {
		int x0 = 10,y0 = 15;
		int x1 = 60,y1 = 99;

		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		addPoints(x0,y0,x1,y0,points);
		addPoints(x1,y0,x1,y1,points);
		addPoints(x1,y1,x0,y1,points);
		addPoints(x0,y1,x0,y0,points);

		FitQuadrilaterialEM alg = new FitQuadrilaterialEM();
		alg.lines[0] = line(x0,y0,x1,y0+offY);
		alg.lines[1] = line(x1,y0,x1+offX,y1);
		alg.lines[2] = line(x1,y1-offY,x0,y1);
		alg.lines[3] = line(x0,y1,x0-offX,y0);

		alg.performLineEM(points);

		for (int i = 0; i < 4; i++) {
			System.out.println(i+" "+alg.lines[i].C);
		}

		assertEquals(15,Math.abs(alg.lines[0].C),1e-8);
		assertEquals(60,Math.abs(alg.lines[1].C),1e-8);
		assertEquals(99,Math.abs(alg.lines[2].C),1e-8);
		assertEquals(10,Math.abs(alg.lines[3].C),1e-8);
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

	@Test
	public void distance() {
		LineGeneral2D_F64 line = new LineGeneral2D_F64(3,-5,2.5);
		Point2D_F64 p = new Point2D_F64(-7,-2.3);

		double expected = Distance2D_F64.distance(line,p);
		line.normalize();
		double found = FitQuadrilaterialEM.distance(line,p);

		assertEquals(expected,found,1e-8);
	}

	@Test
	public void bubbleSortLines() {
		for (int i = 0; i < 10; i++) {
			FastQueue<FitQuadrilaterialEM.Segment> segments =
					new FastQueue<FitQuadrilaterialEM.Segment>(FitQuadrilaterialEM.Segment.class,true);

			int original[] = new int[7];
			for (int j = 0; j < 7; j++) {
				original[j] = segments.grow().index0 = rand.nextInt(100);
			}

			FitQuadrilaterialEM.bubbleSortLines(segments);

			// make sure it doesn't touch the others
			for (int j = 4; j < original.length; j++) {
				assertEquals(original[j],segments.get(j).index0);
			}

			// check ordering if first 4
			for (int j = 1; j < 4; j++) {
				int a = segments.get(j-1).index0;
				int b = segments.get(j).index0;

				assertTrue(a <= b);
			}
		}

	}

	/**
	 * Create random lines from 4 points.  See if resulting quad has the same 4 points.
	 */
	@Test
	public void convert() {
		List<Point2D_F64> points = UtilPoint2D_F64.random(-2,2,4,rand);
		points = UtilPoint2D_F64.orderCCW(points);

		List<LineGeneral2D_F64> lines = new ArrayList<LineGeneral2D_F64>();
		for (int j = 0; j < 4; j++) {
			Point2D_F64 a = points.get(j);
			Point2D_F64 b = points.get((j+1)%4);

			lines.add(line(a.x,a.y,b.x,b.y));
		}

		Quadrilateral_F64 quad = new Quadrilateral_F64();
		FitQuadrilaterialEM.convert(lines.toArray(new LineGeneral2D_F64[4]),quad);

		assertTrue(findMatch(quad.a,points));
		assertTrue(findMatch(quad.b,points));
		assertTrue(findMatch(quad.c,points));
		assertTrue(findMatch(quad.d,points));

		// see what happens if the order is reversed
		List<LineGeneral2D_F64> alt = new ArrayList<LineGeneral2D_F64>();
		for (int j = 0; j < 4; j++) {
			alt.add( lines.get(4-j-1));
		}
		lines= alt;

		FitQuadrilaterialEM.convert(lines.toArray(new LineGeneral2D_F64[4]),quad);

		assertTrue(findMatch(quad.a,points));
		assertTrue(findMatch(quad.b,points));
		assertTrue(findMatch(quad.c,points));
		assertTrue(findMatch(quad.d,points));
	}

	private LineGeneral2D_F64 line( double x0 , double y0 , double x1 , double y1 ) {
		LineSegment2D_F64 ls = new LineSegment2D_F64(x0,y0,x1,y1);
		LineGeneral2D_F64 g = UtilLine2D_F64.convert(ls, (LineGeneral2D_F64) null);
		g.normalize();
		return g;
	}

	private boolean findMatch( Point2D_F64 p , List<Point2D_F64> points ) {
		for( Point2D_F64 a : points ) {
			if( a.distance(p) <= GrlConstants.DOUBLE_TEST_TOL ) {
				return true;
			}
		}
		return false;
	}
}