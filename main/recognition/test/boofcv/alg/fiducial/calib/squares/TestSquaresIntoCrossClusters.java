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

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSquaresIntoCrossClusters {

	/**
	 * Create a simple perfect cluster.  Do a crude test based on number of edge histogram
	 */
	@Test
	public void process_simple() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.05, -1);

		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add( createSquare(7,8));
		squares.add( createSquare(9,8));
		squares.add( createSquare(8,9));
		squares.add( createSquare(7,10));
		squares.add( createSquare(9,10));

		List<BinaryPolygonDetector.Info> squareInfo = createInfo(squares);

		List<List<SquareNode>> clusters = alg.process(squares,squareInfo);

		assertEquals(1,clusters.size());

		List<SquareNode> cluster = clusters.get(0);

		int connections[] = new int[5];
		for( SquareNode n : cluster ) {
			connections[n.getNumberOfConnections()]++;
		}

		assertEquals(0,connections[0]);
		assertEquals(4,connections[1]);
		assertEquals(0,connections[2]);
		assertEquals(0,connections[3]);
		assertEquals(1,connections[4]);
	}

	private List<BinaryPolygonDetector.Info> createInfo(List<Polygon2D_F64> squares) {
		List<BinaryPolygonDetector.Info> squareInfo = new ArrayList<>();
		for (int i = 0; i < squares.size(); i++) {
			squareInfo.add( new BinaryPolygonDetector.Info() );
		}
		return squareInfo;
	}

	/**
	 * Tests shapes with corners that touch the image border
	 */
	@Test
	public void shapesOnBorder() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.05, -1);

		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add( createSquare(7,8));
		squares.add( createSquare(9,8));
		squares.add( createSquare(8,9));
		squares.add( createSquare(7,10));
		squares.add( createSquare(9,10));

		List<BinaryPolygonDetector.Info> squareInfo = createInfo(squares);

		markTouch(squareInfo.get(0),true,false,true,true);
		markTouch(squareInfo.get(1),false,true,true,false);
		markTouch(squareInfo.get(3),true,true,false,true);

		List<List<SquareNode>> clusters = alg.process(squares,squareInfo);

		assertEquals(1,clusters.size());
		assertEquals(5,clusters.get(0).size());

	}

	private void markTouch( BinaryPolygonDetector.Info info , boolean ...marks ) {
		for( boolean b : marks ) {
			info.borderCorners.add(b);
		}
	}

	/**
	 * Tests the corner distance threshold.  two nodes should be barely within tolerance of each other with the 3rd
	 * barely not in tolerance
	 */
	@Test
	public void process_connect_threshold() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.2,-1);

		List<Polygon2D_F64> squares = new ArrayList<>();
		squares.add( createSquare(5,6));
		squares.add( createSquare(6.20001,7));
		squares.add( createSquare(6.1999999,5));

		List<BinaryPolygonDetector.Info> squareInfo = createInfo(squares);

		List<List<SquareNode>> clusters = alg.process(squares,squareInfo);

		assertEquals(2,clusters.size());
	}

	private Polygon2D_F64 createSquare( double x , double y ) {
		Polygon2D_F64 out = new Polygon2D_F64(4);

		out.get(0).set(x,y);
		out.get(1).set(x+1,y);
		out.get(2).set(x+1,y-1);
		out.get(3).set(x,y-1);

		return out;
	}

	@Test
	public void getCornerIndex() {
		SquareNode node = new SquareNode();
		node.corners = new Polygon2D_F64(4);
		node.corners.get(0).set(5,6);
		node.corners.get(1).set(6,7);
		node.corners.get(2).set(7,8);
		node.corners.get(3).set(8,9);

		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(5,-1);

		assertEquals(0,alg.getCornerIndex(node,5,6));
		assertEquals(1,alg.getCornerIndex(node,6,7));
		assertEquals(2,alg.getCornerIndex(node,7,8));
		assertEquals(3,alg.getCornerIndex(node,8,9));
	}

	@Test
	public void candidateIsMuchCloser() {
		SquareNode node0 = new SquareNode();
		SquareNode node1 = new SquareNode();

		node0.largestSide = 2;
		node1.largestSide = 1;

		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(5,-1);

		// test obvious cases
		assertTrue(alg.candidateIsMuchCloser(node0,node1,0));
		assertFalse(alg.candidateIsMuchCloser(node0,node1,20));

		double frac = alg.tooFarFraction;
		node1.corners = createSquare(12,10);
		// the closest neighboring node should be 1 away
		assertTrue(alg.candidateIsMuchCloser(node0,node1,Math.pow(2*frac-1e-6,2)));
		assertFalse(alg.candidateIsMuchCloser(node0,node1,Math.pow(2*frac+1e-6,2)));
	}

	@Test
	public void considerConnect() {
		SquareNode node0 = new SquareNode();
		SquareNode node1 = new SquareNode();

		// first do it with no connections
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(5,-1);

		alg.considerConnect(node0,0,node1,0,4);

		assertTrue(node0.edges[0]==node1.edges[0]);
		assertTrue(node0.edges[0].a==node0);
		assertTrue(node0.edges[0].b==node1);

		// try to connect when its worse
		alg.considerConnect(node0,0,node1,1,4.5);
		assertTrue(node0.edges[0].a==node0);
		assertTrue(node1.edges[1]==null);
		alg.considerConnect(node1,1,node0,0,4.5);
		assertTrue(node0.edges[0].a==node0);
		assertTrue(node1.edges[1]==null);


		// have one be better
		alg.considerConnect(node0,0,node1,1,3.5);
		assertTrue(node0.edges[0]==node1.edges[1]);
		assertTrue(node0.edges[0].a==node0);
		assertTrue(node0.edges[0].b==node1);
		assertTrue(node1.edges[0]==null);
	}

}
