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

package boofcv.alg.feature.detect.squares;

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSquaresIntoClusters {

	@Test
	public void recycle() {
		fail("implement");
	}

	@Test
	public void computeNodeInfo() {
		fail("implement");
	}

	@Test
	public void connectNodes() {
		fail("implement");
	}

	/**
	 * The usual case.  They should attach
	 */
	@Test
	public void considerAttach_nominal() {
		fail("implement");
	}

	/**
	 * Everything's good, but they are offset from each other by too much
	 */
	@Test
	public void considerAttach_shape_offset() {
		fail("implement");
	}

	/**
	 * Every thing's good, but the size difference of the squares is too much
	 */
	@Test
	public void considerAttach_shape_size() {
		fail("implement");
	}

	@Test
	public void findSideIntersect() {
		LineSegment2D_F64 line = new LineSegment2D_F64();
		LineSegment2D_F64 storage = new LineSegment2D_F64();
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);

		SquaresIntoClusters alg = new SquaresIntoClusters(2);

		line.b.set(0,2);
		assertEquals(0,alg.findSideIntersect(a,line,storage));
		line.b.set(0,-2);
		assertEquals(2,alg.findSideIntersect(a,line,storage));
		line.b.set(2,0);
		assertEquals(1,alg.findSideIntersect(a,line,storage));
		line.b.set(-2,0);
		assertEquals(3,alg.findSideIntersect(a,line,storage));
	}

	@Test
	public void areSidesParallel() {
		areSidesParallel(true);
		areSidesParallel(false);
	}

	private void areSidesParallel(boolean changeClock) {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);
		SquareNode b = new SquareNode();
		b.corners = new Polygon2D_F64( 1,1,  3,1,  3,-1,   1,-1);

		if( changeClock ) {
			UtilPolygons2D_F64.flip(a.corners);
			UtilPolygons2D_F64.flip(a.corners);
		}

		SquaresIntoClusters alg = new SquaresIntoClusters(2);
		for (int i = 0; i < 4; i++) {
			assertTrue(alg.areSidesParallel(a,i,b,i));
			assertFalse(alg.areSidesParallel(a, i, b, (i + 1) % 4));
		}

		// test jsut above and below the threshold
		double cos0 = Math.cos(alg.acuteAngleTol * 0.99);
		double sin0 = Math.sin(alg.acuteAngleTol * 0.99);
		double cos1 = Math.cos(alg.acuteAngleTol * 1.01);
		double sin1 = Math.sin(alg.acuteAngleTol * 1.01);

		a.corners.get(1).set(-1+2*cos0,1+2*sin0);
		assertTrue(alg.areSidesParallel(a, 0, b, 0));

		a.corners.get(1).set(-1 + 2 * cos1, 1 + 2 * sin1);
		assertFalse(alg.areSidesParallel(a, 0, b, 0));
	}

	@Test
	public void areMiddlePointsClose() {
		Point2D_F64 p0 = new Point2D_F64(-2,3);
		Point2D_F64 p1 = new Point2D_F64(-1,3);
		Point2D_F64 p2 = new Point2D_F64( 1,3);
		Point2D_F64 p3 = new Point2D_F64( 2,3);

		SquaresIntoClusters alg = new SquaresIntoClusters(2);
		double maxDistance = 1.0*alg.distanceTol;

		assertTrue(alg.areMiddlePointsClose(p0,p1,p2,p3));
		p1.set(-1, 3 + maxDistance * 0.99);
		assertTrue(alg.areMiddlePointsClose(p0, p1, p2, p3));
		p1.set(-1, 3 + maxDistance * 1.01);
		assertFalse(alg.areMiddlePointsClose(p0, p1, p2, p3));

		p1.set(-1, 3);
		p2.set( 1, 3 + maxDistance * 0.99);
		assertTrue(alg.areMiddlePointsClose(p0, p1, p2, p3));
		p2.set( 1, 3 + maxDistance * 1.01);
		assertFalse(alg.areMiddlePointsClose(p0, p1, p2, p3));
	}

	@Test
	public void checkConnect() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();

		SquaresIntoClusters alg = new SquaresIntoClusters(0.1);

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

	private void assertConnected(SquareNode a , int indexA , SquareNode b , int indexB , double distance)
	{
		assertTrue(a.edges[indexA]==b.edges[indexB]);
		assertEquals(distance,a.edges[indexA].distance,1e-8);
	}

	private void assertNotConnected(SquareNode a ,  SquareNode b )
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

	@Test
	public void detachEdge() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();

		SquaresIntoClusters alg = new SquaresIntoClusters(0.1);

		alg.connect(a, 1, b, 2, 2.5);
		SquareEdge e = a.edges[1];

		alg.detachEdge(e);

		assertEquals(1, alg.edges.getUnused().size());
		assertTrue(a.edges[1] == null);
		assertTrue(b.edges[2] == null);

	}

	@Test
	public void connect() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();

		SquaresIntoClusters alg = new SquaresIntoClusters(0.1);

		alg.connect(a,1,b,2,2.5);

		assertTrue(a.edges[1] == b.edges[2]);
		SquareEdge e = a.edges[1];
		assertEquals(e.distance,2.5,1e-8);
		assertEquals(1, e.sideA);
		assertEquals(2, e.sideB);
	}
}