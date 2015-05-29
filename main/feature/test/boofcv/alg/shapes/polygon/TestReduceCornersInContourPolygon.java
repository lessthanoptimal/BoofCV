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

package boofcv.alg.shapes.polygon;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static georegression.metric.Distance2D_F64.distance;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestReduceCornersInContourPolygon {

	Random rand = new Random(234);

	/**
	 * Give it 4 perfect vertexes that match the contour
	 */
	@Test
	public void all_4() {
		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4,2,true);

		GrowQueue_I32 vertexes = new GrowQueue_I32();

		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		vertexes.add(0);
		contour.add( new Point2D_I32(30,35));
		vertexes.add(addTo(contour,new Point2D_I32(60,37)));
		vertexes.add(addTo(contour,new Point2D_I32(58,12)));
		vertexes.add(addTo(contour,new Point2D_I32(28,9)));
		addTo(contour, new Point2D_I32(30, 34));

		for (int i = 0; i < 10; i++) {

			alg.process(contour, vertexes);

			Polygon2D_F64 found = alg.getOutput();

			assertTrue(maxDistance(contour, found) < 1);

			// shift everything over by one
			// not really sure if this actually stresses anything
			List<Point2D_I32> shifted = new ArrayList<Point2D_I32>();
			shifted.add( contour.get( contour.size()-1));
			for (int j = 0; j < contour.size() - 1; j++) {
				shifted.add(contour.get(j));
			}
			contour = shifted;
			for (int j = 0; j < vertexes.size(); j++) {
				vertexes.data[j] = (vertexes.data[j] + 1 ) % contour.size();
			}
		}
	}

	/**
	 * Test the situation where there are more than 4 vertexes and it's a prefect shape
	 */
	@Test
	public void moreThanFour_perfect() {
		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4,2,true);

		GrowQueue_I32 vertexes = new GrowQueue_I32();

		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		int v0 = 0;
		contour.add( new Point2D_I32(30,35));
		int v1 = addTo(contour,new Point2D_I32(40,35));
		int v2 = addTo(contour,new Point2D_I32(40,60));
		int v3 = addTo(contour,new Point2D_I32(30,60));

		vertexes.add(v0);
		vertexes.add(v0+1);
		vertexes.add(v1);
		vertexes.add(v1+5);
		vertexes.add(v2);
		vertexes.add(v3);

		alg.process(contour, vertexes);

		Polygon2D_F64 found = alg.getOutput();

		assertTrue(maxDistance(contour, found) < 1);
	}

	/**
	 * More than 4 vertixes when the contour isn't a perfect quad
	 */
	@Test
	public void moreThanFour_kink() {
		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4,2,true);

		GrowQueue_I32 vertexes = new GrowQueue_I32();

		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		int v0 = 0;
		contour.add(           new Point2D_I32(30,35));
		int v1 = addTo(contour, new Point2D_I32(40, 37)); // here's the kink
		int v2 = addTo(contour,new Point2D_I32(50,35));
		int v3 = addTo(contour,new Point2D_I32(50,60));
		int v4 = addTo(contour,new Point2D_I32(30,60));

		vertexes.add(v0);
		vertexes.add(v1);
		vertexes.add(v2);
		vertexes.add(v3);
		vertexes.add(v4);

		alg.process(contour, vertexes);

		Polygon2D_F64 found = alg.getOutput();

		assertTrue(maxDistance(contour, found) < 1);
	}


	@Test
	public void bubbleSortLines() {
		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4, 10, true);

		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			indexes.add(i);
		}

		for (int i = 0; i < 20; i++) {
			Collections.shuffle(indexes, rand);

			alg.segments.reset();
			for (Integer index : indexes) {
				alg.segments.grow().index0 = index;
			}

			ReduceCornersInContourPolygon.bubbleSortLines(alg.segments);

			assertEquals( 0, alg.segments.get(0).index0);
			assertEquals( 1, alg.segments.get(1).index0);
			assertEquals( 2, alg.segments.get(2).index0);
			assertEquals( 3, alg.segments.get(3).index0);
		}
	}

	@Test
	public void checkPolygonCornerDistance() {
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();

		for (int i = 0; i < 20; i++) {
			contour.add(new Point2D_I32(rand.nextInt(),rand.nextInt()));
		}

		contour.get(3).set(10,10);
		contour.get(4).set(13,10);
		contour.get(8).set(20,10);
		contour.get(12).set(20,20);
		contour.get(18).set(10,20);

		GrowQueue_I32 corners = new GrowQueue_I32();
		corners.add(3);
		corners.add(4);
		corners.add(8);
		corners.add(12);
		corners.add(18);

		Polygon2D_F64 poly = new Polygon2D_F64(10,10,  20,10,  20,20,  10,20);

		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4,2,true);

		// perfect fit
		assertTrue(alg.checkPolygonCornerDistance(poly,contour,corners));

		// move one to threshold
		contour.get(3).set(8,10);
		assertTrue(alg.checkPolygonCornerDistance(poly,contour,corners));

		// now move it outside the threshold
		contour.get(3).set(7,10);
		assertFalse(alg.checkPolygonCornerDistance(poly, contour, corners));

	}

	private double maxDistance( List<Point2D_I32> contour , Polygon2D_F64 poly ) {
		double max = 0;
		for( Point2D_I32 c : contour ) {
			max = Math.max(max, distance(poly, new Point2D_F64(c.x, c.y)));
		}
		return max;
	}

	private int addTo( List<Point2D_I32> contour , Point2D_I32 target ) {
		Point2D_I32 p = contour.get( contour.size()-1 );

		int N = (int)Math.ceil(p.distance(target));

		for (int i = 1; i < N; i++) {
			int x = (target.x-p.x)*i/N + p.x;
			int y = (target.y-p.y)*i/N + p.y;
			contour.add(new Point2D_I32(x,y));
		}
		contour.add(target);
		return contour.size()-1;
	}
}