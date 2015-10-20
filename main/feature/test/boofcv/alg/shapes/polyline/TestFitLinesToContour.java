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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestFitLinesToContour {
	@Test
	public void fitAnchored_perfect_input() {
		fail("implement");
	}

	@Test
	public void fitAnchored_easy_optimization() {
		fail("implement");
	}

	@Test
	public void sanityCheckCornerOrder() {
		fail("implement");
	}

	@Test
	public void linesIntoCorners() {
		fail("implement");
	}

	@Test
	public void fitLinesUsingCorners() {
		fail("implement");
	}

	@Test
	public void fitLine() {
		FitLinesToContour alg = new FitLinesToContour();

		alg.contour = createSquare( 10,12 , 30,40);
		GrowQueue_I32 corners = createSquareCorners(10,12 , 30,40);

		LineGeneral2D_F64 line = new LineGeneral2D_F64();
		for (int i = 0,j=corners.size()-1; i < corners.size(); j=i,i++) {
			alg.fitLine(corners.get(j),corners.get(i),line);

			// see if the line lies perfectly along hat side
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
		List<Point2D_I32> output = new ArrayList<Point2D_I32>();

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
