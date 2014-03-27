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

package boofcv.alg.feature.detect.grid;

import boofcv.alg.feature.detect.InvalidCalibrationTarget;
import boofcv.alg.feature.detect.quadblob.QuadBlob;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConnectGridSquares {

	/**
	 * Creates a copy of the sub-graph and makes sure only references to nodes in
	 * the sub-graph are saved.
	 */
	@Test
	public void copy() {
		List<QuadBlob> all = new ArrayList<QuadBlob>();
		all.add(createSquare(50,60,10,12));
		all.add(createSquare(51,61,11,13));
		all.add(createSquare(52, 62, 12, 14));
		all.add(createSquare(53, 63, 13, 15));
		connect(0,1,all);
		connect(0,2,all);
		connect(0,3,all);
		connect(1,3,all);
		connect(2,3,all);

		List<QuadBlob> sub = new ArrayList<QuadBlob>();
		sub.add( all.get(0));
		sub.add( all.get(1));
		sub.add( all.get(2));

		List<QuadBlob> found = ConnectGridSquares.copy(sub);

		checkSquare(found.get(0),50,60,10,12,2);
		checkSquare(found.get(1),51,61,11,13,1);
		checkSquare(found.get(2),52,62,12,14,1);
	}

	@Test
	public void findIsland() {
		List<QuadBlob> all = new ArrayList<QuadBlob>();
		all.add(createSquare(50,60,10,12));
		all.add(createSquare(51,61,11,13));
		all.add(createSquare(52, 62, 12, 14));
		all.add(createSquare(53, 63, 13, 15));
		all.add(createSquare(70, 63, 15, 15));
		connect(0,1,all);
		connect(0,2,all);
		connect(0,3,all);
		connect(1,3,all);
		connect(2,3,all);
		
		List<QuadBlob> found = ConnectGridSquares.findIsland(all.remove(1),all);
		assertEquals(4,found.size());
	}

	@Test
	public void basic() throws InvalidCalibrationTarget {
		List<QuadBlob> blobs = new ArrayList<QuadBlob>();

		blobs.add( createBlob(5,7,1));
		blobs.add( createBlob(10,7,1));
		blobs.add( createBlob(15,7,1));
		blobs.add( createBlob(5,2,1));
		blobs.add( createBlob(10,2,1));
		blobs.add( createBlob(15,2,1));

		ConnectGridSquares.connect(blobs,1);

		// see if they have the expected number of connections
		assertEquals(2, blobs.get(0).conn.size());
		assertEquals(3, blobs.get(1).conn.size());
		assertEquals(2, blobs.get(2).conn.size());
		assertEquals(2, blobs.get(3).conn.size());
		assertEquals(3, blobs.get(4).conn.size());
		assertEquals(2, blobs.get(5).conn.size());

		// sanity check one of the nodes specific connections
		assertTrue(blobs.get(2).conn.contains(blobs.get(1)));
		assertTrue(blobs.get(2).conn.contains(blobs.get(5)));
	}

	private void checkSquare( QuadBlob square , double largestSide , double smallestSide,
							  int x , int y , int numConnections )
	{
		assertEquals(largestSide,square.largestSide,1e-8);
		assertEquals(smallestSide,square.smallestSide,1e-8);
		assertEquals(x,square.center.x);
		assertEquals(y,square.center.y);
		assertEquals(numConnections,square.conn.size());
	}

	public static void connect( int a , int b , List<QuadBlob> input ) {
		input.get(a).conn.add( input.get(b) );
		input.get(b).conn.add( input.get(a) );
	}

	public static QuadBlob createBlob( int x0 , int y0 , int r )
	{
		return createBlob(x0-r,y0+r,   x0+r,y0+r,   x0+r,y0-r,  x0-r,y0-r);
	}

	public static QuadBlob createBlob( int x0 , int y0 , int x1 , int y1 ,
									   int x2 , int y2 , int x3 , int y3 )
	{
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();
		corners.add( new Point2D_I32(x0,y0));
		corners.add( new Point2D_I32(x1,y1));
		corners.add( new Point2D_I32(x2,y2));
		corners.add( new Point2D_I32(x3,y3));

		List<Point2D_F64> subpixel = new ArrayList<Point2D_F64>();
		subpixel.add( new Point2D_F64(x0,y0));
		subpixel.add( new Point2D_F64(x1,y1));
		subpixel.add( new Point2D_F64(x2,y2));
		subpixel.add( new Point2D_F64(x3,y3));

		QuadBlob ret = new QuadBlob(corners,corners);
		ret.subpixel = subpixel;

		return ret;
	}

	private QuadBlob createSquare( double largestSide , double smallestSide,
									 int x , int y )
	{
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();
		List<Point2D_I32> corners = new ArrayList<Point2D_I32>();

		for( int i = 0; i < 4; i++ ) {
			contour.add( new Point2D_I32(1,2));
			corners.add( new Point2D_I32(1,2));
		}

		QuadBlob ret = new QuadBlob(contour,corners);

		ret.largestSide = largestSide;
		ret.smallestSide = smallestSide;
		ret.center.x = x;
		ret.center.y = y;

		return ret;
	}

}
