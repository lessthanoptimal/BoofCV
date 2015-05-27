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
import java.util.List;

import static georegression.metric.Distance2D_F64.distance;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestReduceCornersInContourPolygon {

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
	 * Test the situation where there are more than 4 vertexes
	 */
	@Test
	public void moreThanFour() {
		ReduceCornersInContourPolygon alg = new ReduceCornersInContourPolygon(4,2,true);

		GrowQueue_I32 vertexes = new GrowQueue_I32();

		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		vertexes.add(0);
		contour.add( new Point2D_I32(30,35));
		vertexes.add(addTo(contour,new Point2D_I32(40,35)));
		vertexes.add(addTo(contour,new Point2D_I32(60,37)));
		vertexes.add(addTo(contour,new Point2D_I32(58,12)));
		vertexes.add(addTo(contour,new Point2D_I32(28,9)));
		addTo(contour, new Point2D_I32(30, 34));

		alg.process(contour, vertexes);

		Polygon2D_F64 found = alg.getOutput();

		assertTrue(maxDistance(contour, found) < 1);
	}


	@Test
	public void bubbleSortLines() {
		fail("Implement");
	}

	@Test
	public void checkPolygonCornerDistance() {
		fail("Implement");
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