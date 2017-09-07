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

package boofcv.alg.fiducial.calib.squares;

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ejml.UtilEjml;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSquareGraph {

	@Test
	public void computeNodeInfo() {
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(-1,1,  2,1,  2,-1,  -1,-1);

		SquareGraph alg = new SquareGraph();

		alg.computeNodeInfo(a);

		assertEquals(3,a.sideLengths[0], UtilEjml.TEST_F64);
		assertEquals(2,a.sideLengths[1], UtilEjml.TEST_F64);
		assertEquals(3,a.sideLengths[2], UtilEjml.TEST_F64);
		assertEquals(2,a.sideLengths[3], UtilEjml.TEST_F64);

		assertEquals(3,a.largestSide, UtilEjml.TEST_F64);
		assertEquals(2,a.smallestSide, UtilEjml.TEST_F64);

		assertTrue(a.center.distance(0.5,0)<UtilEjml.TEST_F64);

	}

	@Test
	public void findSideIntersect() {
		LineSegment2D_F64 line = new LineSegment2D_F64();
		LineSegment2D_F64 storage = new LineSegment2D_F64();
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);

		SquareGraph alg = new SquareGraph();
		Point2D_F64 intersection = new Point2D_F64();

		line.b.set(0,2);
		assertEquals(0,alg.findSideIntersect(a,line,intersection,storage));
		line.b.set(0, -2);
		assertEquals(2, alg.findSideIntersect(a, line, intersection,storage));
		line.b.set(2, 0);
		assertEquals(1,alg.findSideIntersect(a,line,intersection,storage));
		line.b.set(-2, 0);
		assertEquals(3, alg.findSideIntersect(a, line, intersection,storage));
	}

	@Test
	public void almostParallel() {
		almostParallel(false);
		almostParallel(true);
	}

	private void almostParallel(boolean changeClock) {
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);
		SquareNode b = new SquareNode();
		b.square = new Polygon2D_F64( 1,1,  3,1,  3,-1,   1,-1);

		int adj = 1;
		if( changeClock ) {
			UtilPolygons2D_F64.flip(a.square);
			UtilPolygons2D_F64.flip(b.square);
			adj = 3;
		}

		SquareGraph alg = new SquareGraph();
		for (int i = 0; i < 4; i++) {
			assertTrue(alg.almostParallel(a, i, b, i));
			assertTrue(alg.almostParallel(a, i, b, (i + 2) % 4));
			assertFalse(alg.almostParallel(a, i, b, (i + 1) % 4));
		}

		// give it some slant
		double angle0 = 0.1;
		double angle1 = Math.PI/4;
		double cos0 = Math.cos(angle0);
		double sin0 = Math.sin(angle0);
		double cos1 = Math.cos(angle1);
		double sin1 = Math.sin(angle1);

		a.square.get(adj).set(-1 + 2 * cos0, 1 + 2 * sin0);
		assertTrue(alg.almostParallel(a, 0, b, 0));
		assertFalse(alg.almostParallel(a, 1, b, 0));
		a.square.get(adj).set(-1 + 2 * cos1, 1 + 2 * sin1);
		assertTrue(alg.almostParallel(a, 0, b, 0));
		assertFalse(alg.almostParallel(a, 1, b, 0));
	}

	@Test
	public void acuteAngle() {
		acuteAngle(true);
		acuteAngle(false);
	}

	private void acuteAngle(boolean changeClock) {
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(-1, 1, 1, 1, 1, -1, -1, -1);
		SquareNode b = new SquareNode();
		b.square = new Polygon2D_F64(1, 1, 3, 1, 3, -1, 1, -1);

		if( changeClock ) {
			UtilPolygons2D_F64.flip(a.square);
			UtilPolygons2D_F64.flip(b.square);
		}

		SquareGraph alg = new SquareGraph();

		assertEquals(0,alg.acuteAngle(a,0,b,0),1e-8);
		assertEquals(0,alg.acuteAngle(a,0,b,2),1e-8);
		assertEquals(0,alg.acuteAngle(a,2,b,0),1e-8);
		assertEquals(0,alg.acuteAngle(a,2,b,2),1e-8);

		assertEquals(0,alg.acuteAngle(a,1,b,1),1e-8);
		assertEquals(0,alg.acuteAngle(a,1,b,3),1e-8);
		assertEquals(0,alg.acuteAngle(a,3,b,1),1e-8);
		assertEquals(0,alg.acuteAngle(a,3,b,3),1e-8);

		assertEquals(Math.PI/2,alg.acuteAngle(a,0,b,1),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,0,b,3),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,2,b,1),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,2,b,3),1e-8);

		assertEquals(Math.PI/2,alg.acuteAngle(a,1,b,0),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,3,b,0),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,1,b,2),1e-8);
		assertEquals(Math.PI/2,alg.acuteAngle(a,3,b,2),1e-8);
	}

	@Test
	public void detachEdge() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();

		SquareGraph alg = new SquareGraph();

		alg.connect(a, 1, b, 2, 2.5);
		SquareEdge e = a.edges[1];

		alg.detachEdge(e);

		assertEquals(1, alg.edgeManager.getUnused().size());
		assertTrue(a.edges[1] == null);
		assertTrue(b.edges[2] == null);
	}

	@Test
	public void connect() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();

		SquareGraph alg = new SquareGraph();

		alg.connect(a,1,b,2,2.5);

		assertTrue(a.edges[1] == b.edges[2]);
		SquareEdge e = a.edges[1];
		assertEquals(e.distance,2.5,1e-8);
		assertEquals(1, e.sideA);
		assertEquals(2, e.sideB);
	}

	@Test
	public void checkConnect() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();

		SquareGraph alg = new SquareGraph();

		// check case where there are no prior connection
		alg.checkConnect(a,2,b,0,2);
		assertConnected(a, 2, b, 0, 2);

		// prior connection on A, which is better then proposed
		alg.checkConnect(a, 2, c, 1, 3);
		assertNotConnected(a, c);

		// prior connection on A, which is worse then proposed
		alg.checkConnect(a, 2, c, 1, 1);
		assertNotConnected(a, b);
		assertConnected(a, 2, c, 1, 1);

		// prior connection on B, which is better then proposed
		alg.checkConnect(b,3,c,1,3);
		assertNotConnected(b, c);

		// prior connection on B, which is worse then proposed
		alg.checkConnect(b, 3, c, 1, 0.5);
		assertNotConnected(a, c);
		assertConnected(b, 3, c, 1, 0.5);
	}

	public static void assertConnected(SquareNode a , int indexA , SquareNode b , int indexB , double distance)
	{
		assertTrue(a.edges[indexA]==b.edges[indexB]);
		assertEquals(distance,a.edges[indexA].distance,1e-8);
	}

	public static void assertNotConnected(SquareNode a ,  SquareNode b )
	{
		for (int i = 0; i < 4; i++) {
			if( a.edges[i] != null ) {
				SquareEdge e = a.edges[i];
				assertFalse(e.a==b);
				assertFalse(e.b==b);
			}
			if( b.edges[i] != null ) {
				SquareEdge e = b.edges[i];
				assertFalse(e.a==a);
				assertFalse(e.b==a);
			}
		}
	}
}
