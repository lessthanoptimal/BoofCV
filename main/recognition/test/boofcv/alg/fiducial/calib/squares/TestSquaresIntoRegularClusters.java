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

package boofcv.alg.fiducial.calib.squares;

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSquaresIntoRegularClusters {

	/**
	 * Very basic test of the entire class.  Pass in squares which should be two clusters and see if two clusters
	 * come out.
	 */
	@Test
	public void process() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		double width = 1;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++) {
				squares.add( createSquare(i*2*width,j*2*width,0,width));
				squares.add( createSquare(i*2*width+100,j*2*width,0,width));
			}
		}

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(1.0,6, 1.35);
		List<List<SquareNode>> clusters = alg.process(squares);
		assertEquals(2,clusters.size());

		// second pass, see if it messes up
		clusters = alg.process(squares);
		assertEquals(2,clusters.size());
	}

	@Test
	public void computeNodeInfo() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add(new Polygon2D_F64(-1, 1, 1, 1, 1, -1, -1, -1));
		squares.add(new Polygon2D_F64(2, 1, 4, 1, 4, -1, 2, -1));

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);
		alg.computeNodeInfo(squares);

		assertEquals(2, alg.nodes.size());
		SquareNode a = alg.nodes.get(0);
		SquareNode b = alg.nodes.get(1);
		assertTrue(a.center.distance(new Point2D_F64(0,0))<=1e-8);
		assertTrue(b.center.distance(new Point2D_F64(3,0))<=1e-8);

		assertEquals(0, a.getNumberOfConnections());
		assertEquals(0, b.getNumberOfConnections());

		assertEquals(2, a.largestSide,1e-8);
		assertEquals(2, b.largestSide,1e-8);

		assertEquals(SquareNode.RESET_GRAPH, a.graph);
		assertEquals(SquareNode.RESET_GRAPH, b.graph);

		for (int i = 0; i < 4; i++) {
			assertEquals(2, a.sideLengths[i],1e-8);
			assertEquals(2, b.sideLengths[i],1e-8);
		}
	}

	/**
	 * Very easy scenario where a rectangular grid should be perefectly connected
	 */
	@Test
	public void connectNodes_oneCluster() {

		List<Polygon2D_F64> squares = new ArrayList<>();
		double width = 1;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++) {
				squares.add( createSquare(i*2*width,j*2*width,0,width));
			}
		}

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(1.0,6, 1.35);
		alg.computeNodeInfo(squares);
		alg.connectNodes();

		assertEquals(2 * 4 + (2 + 4) * 3 + (1 * 2 * 4), countConnections(alg.nodes.toList()));
	}

	/**
	 * Two clusters.  They won't connect because they are spaced so far apart
	 */
	@Test
	public void connectNodes_twoCluster() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		double width = 1;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 4; j++) {
				squares.add( createSquare(i*2*width,j*2*width,0,width));
				squares.add( createSquare(i*2*width+100,j*2*width,0,width));
			}
		}

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(1.0,6, 1.35);
		alg.computeNodeInfo(squares);
		alg.connectNodes();

		int oneGrid = 2 * 4 + (2 + 4) * 3 + (1 * 2 * 4);
		assertEquals(oneGrid*2, countConnections(alg.nodes.toList()));
	}

	private Polygon2D_F64 createSquare( double x , double y , double yaw , double width ) {
		Se2_F64 motion = new Se2_F64(x,y,yaw);

		double r = width/2;
		Polygon2D_F64 poly = new Polygon2D_F64(4);
		poly.get(0).set(-r, r);
		poly.get(1).set( r, r);
		poly.get(2).set( r,-r);
		poly.get(3).set(-r,-r);

		for (int i = 0; i < 4; i++) {
			SePointOps_F64.transform(motion,poly.get(i),poly.get(i));
		}

		return poly;
	}

	private int countConnections( List<SquareNode> nodes ) {
		int total = 0;
		for (int i = 0; i < nodes.size(); i++) {
			total += nodes.get(i).getNumberOfConnections();
		}
		return total;
	}

	/**
	 * The usual case.  They should attach
	 */
	@Test
	public void considerConnect_nominal() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add(new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1));
		squares.add(new Polygon2D_F64(2, 1, 4, 1, 4, -1, 2, -1));

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);
		alg.computeNodeInfo(squares);
		SquareNode a = alg.nodes.get(0);
		SquareNode b = alg.nodes.get(1);

		alg.considerConnect(a, b);
		assertConnected(a, 1, b, 3, 3);
	}

	/**
	 * Everything is good, but they are offset from each other by too much
	 */
	@Test
	public void considerConnect_shape_offset() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add(new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1));
		squares.add(new Polygon2D_F64( 3,2,  5,1,  5,-1,   3,-2));

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);
		alg.computeNodeInfo(squares);
		SquareNode a = alg.nodes.get(0);
		SquareNode b = alg.nodes.get(1);

		alg.considerConnect(a, b);
		assertNotConnected(a, b);
	}

	/**
	 * Every thing's good, but the size difference of the squares is too much
	 */
	@Test
	public void considerConnect_shape_size() {
		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add(new Polygon2D_F64(-1, 1, 1, 1, 1, -1, -1, -1));
		squares.add(new Polygon2D_F64(2, 4, 10, 4, 10, -4, 2, -4));

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);
		alg.computeNodeInfo(squares);
		SquareNode a = alg.nodes.get(0);
		SquareNode b = alg.nodes.get(1);

		alg.considerConnect(a, b);
		assertNotConnected(a, b);
	}

	@Test
	public void findSideIntersect() {
		LineSegment2D_F64 line = new LineSegment2D_F64();
		LineSegment2D_F64 storage = new LineSegment2D_F64();
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);

		line.b.set(0,2);
		assertEquals(0,alg.findSideIntersect(a,line,storage));
		line.b.set(0, -2);
		assertEquals(2, alg.findSideIntersect(a, line, storage));
		line.b.set(2, 0);
		assertEquals(1,alg.findSideIntersect(a,line,storage));
		line.b.set(-2, 0);
		assertEquals(3, alg.findSideIntersect(a, line, storage));
	}

	@Test
	public void mostParallel() {
		mostParallel(false);
		mostParallel(true);
	}

	private void mostParallel(boolean changeClock) {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(-1,1,  1,1,  1,-1,  -1,-1);
		SquareNode b = new SquareNode();
		b.corners = new Polygon2D_F64( 1,1,  3,1,  3,-1,   1,-1);

		int adj = 1;
		if( changeClock ) {
			UtilPolygons2D_F64.flip(a.corners);
			UtilPolygons2D_F64.flip(b.corners);
			adj = 3;
		}

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);
		for (int i = 0; i < 4; i++) {
			assertTrue(alg.mostParallel(a, i, b, i));
			assertTrue(alg.mostParallel(a, i, b, (i + 2) % 4));
			assertFalse(alg.mostParallel(a, i, b, (i + 1) % 4));
		}

		// give it some slant
		double angle0 = 0.1;
		double angle1 = Math.PI/4;
		double cos0 = Math.cos(angle0);
		double sin0 = Math.sin(angle0);
		double cos1 = Math.cos(angle1);
		double sin1 = Math.sin(angle1);

		a.corners.get(adj).set(-1 + 2 * cos0, 1 + 2 * sin0);
		assertTrue(alg.mostParallel(a, 0, b, 0));
		assertFalse(alg.mostParallel(a, 1, b, 0));
		a.corners.get(adj).set(-1 + 2 * cos1, 1 + 2 * sin1);
		assertTrue(alg.mostParallel(a, 0, b, 0));
		assertFalse(alg.mostParallel(a, 1, b, 0));
	}

	@Test
	public void acuteAngle() {
		acuteAngle(true);
		acuteAngle(false);
	}

	private void acuteAngle(boolean changeClock) {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(-1, 1, 1, 1, 1, -1, -1, -1);
		SquareNode b = new SquareNode();
		b.corners = new Polygon2D_F64(1, 1, 3, 1, 3, -1, 1, -1);

		if( changeClock ) {
			UtilPolygons2D_F64.flip(a.corners);
			UtilPolygons2D_F64.flip(b.corners);
		}

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);

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
	public void areMiddlePointsClose() {
		Point2D_F64 p0 = new Point2D_F64(-2,3);
		Point2D_F64 p1 = new Point2D_F64(-1,3);
		Point2D_F64 p2 = new Point2D_F64( 1,3);
		Point2D_F64 p3 = new Point2D_F64( 2,3);

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(2,6, 1.35);

		assertTrue(alg.areMiddlePointsClose(p0,p1,p2,p3));


		double off = 0.1;
		double thresh = 0.1*3;// threshold which will pass all checks inside
		p1.set(-1, 3 + off);
		alg.distanceTol = thresh*1.01;
		assertTrue(alg.areMiddlePointsClose(p0, p1, p2, p3));
		alg.distanceTol = thresh*0.99;
		assertFalse(alg.areMiddlePointsClose(p0, p1, p2, p3));

		p1.set(-1, 3);
		p2.set( 1, 3 + off);
		alg.distanceTol = thresh*1.01;
		assertTrue(alg.areMiddlePointsClose(p0, p1, p2, p3));
		alg.distanceTol = thresh*0.99;
		assertFalse(alg.areMiddlePointsClose(p0, p1, p2, p3));
	}

	@Test
	public void checkConnect() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();

		SquaresIntoRegularClusters alg = new SquaresIntoRegularClusters(0.1,6, 1.35);

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
}