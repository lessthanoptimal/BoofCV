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

import georegression.misc.test.GeometryUnitTest;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestExtractOrderedTargetPoints {

	@Test
	public void process_positive() {
		// declare square and create a graph
		List<SquareBlob> blobs = new ArrayList<SquareBlob>();
		blobs.add( createBlob(5,5,10));
		blobs.add( createBlob(50,4,10));
		blobs.add( createBlob(5,50,10));
		blobs.add( createBlob(50,55,10));

		TestConnectGridSquares.connect(0,1,blobs);
		TestConnectGridSquares.connect(0,2,blobs);
		TestConnectGridSquares.connect(2,3,blobs);
		TestConnectGridSquares.connect(1,3,blobs);

		ExtractOrderedTargetPoints alg = new ExtractOrderedTargetPoints();
		try {
			alg.process(blobs);
		} catch (InvalidTarget invalidTarget) {
			fail("Failed");
		}

		List<Point2D_I32> quad = alg.getQuadrilateral();
		List<Point2D_I32> points = alg.getTargetPoints();

		// high level checks
		assertEquals(2,alg.getNumCols());
		assertEquals(2,alg.getNumRows());
		assertEquals(16,points.size());
		assertEquals(4,quad.size());

		// CCW ordering of extreme points
		GeometryUnitTest.assertEquals(-5,-5,quad.get(0));
		GeometryUnitTest.assertEquals(60,-6,quad.get(1));
		GeometryUnitTest.assertEquals(60,65,quad.get(2));
		GeometryUnitTest.assertEquals(-5,60,quad.get(3));

		// just check some of the points
		GeometryUnitTest.assertEquals(-5,-5,points.get(0));
		GeometryUnitTest.assertEquals(60,65,points.get(15));
	}

	@Test
	public void orderedBlobsIntoPoints() {
		List<SquareBlob> blobs = new ArrayList<SquareBlob>();
		List<Point2D_I32> points = new ArrayList<Point2D_I32>();
		
		blobs.add( createBlob(5,5,10));
		blobs.add( createBlob(50,4,10));

		ExtractOrderedTargetPoints.orderedBlobsIntoPoints(blobs,points,2);

		// add first row
		GeometryUnitTest.assertEquals(-5,15,points.get(0));
		GeometryUnitTest.assertEquals(15,15,points.get(1));
		GeometryUnitTest.assertEquals(40,14,points.get(2));
		GeometryUnitTest.assertEquals(60,14,points.get(3));
		// add second row, and remember they are added in circular order, so some indexes are swapped
		GeometryUnitTest.assertEquals(-5,-5,points.get(4));
		GeometryUnitTest.assertEquals(15,-5,points.get(5));
	}

	public static SquareBlob createBlob( int x0 , int y0 , int r )
	{
		return createBlob(x0-r,y0+r,   x0+r,y0+r,   x0+r,y0-r,  x0-r,y0-r);
	}

	public static SquareBlob createBlob( int x0 , int y0 , int x1 , int y1 ,
										  int x2 , int y2 , int x3 , int y3 )
	{
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();
		corners.add( new Point2D_I32(x0,y0));
		corners.add( new Point2D_I32(x1,y1));
		corners.add( new Point2D_I32(x2,y2));
		corners.add( new Point2D_I32(x3,y3));

		return new SquareBlob(corners,corners);
	}
}
