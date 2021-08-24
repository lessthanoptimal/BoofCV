/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import boofcv.testing.BoofStandardJUnit;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestChessboardCornerClusterToGrid extends BoofStandardJUnit {
	@Test void convert_nochange() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		alg.setRequireCornerSquares(true);

		convert(alg, 2, 2, false);
		convert(alg, 2, 3, false);
		convert(alg, 3, 2, false);
		convert(alg, 3, 3, false);
	}

	/**
	 * Node order has been randomized
	 */
	@Test void convert_randomized() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		alg.setRequireCornerSquares(true);

		// do a few loops to test more random cases
		for (int i = 0; i < 10; i++) {
			convert(alg, 2, 2, true);
			convert(alg, 2, 3, true);
			convert(alg, 3, 2, true);
			convert(alg, 3, 3, true);
			convert(alg, 3, 4, true);
			convert(alg, 4, 4, true);
		}
	}

	/**
	 * The input will be a rectangle, but with an extra corner that should be removed
	 */
	@Test void convert_extra() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		alg.setRequireCornerSquares(true);

		ChessboardCornerGraph graph = new ChessboardCornerGraph();
		graph.corners = createGrid(4, 5, true);
		// add an extra node to the graph
		Node n = graph.corners.get(graph.corners.size - 1);
		Node extra = graph.corners.grow();
		extra.corner = new ChessboardCorner();
		extra.corner.orientation = n.getOrientation()*-1;
		extra.corner.x = n.getX() + 30;
		extra.corner.y = n.getY();
		extra.index = graph.corners.size - 1;
		n.edges[0] = extra;
		extra.edges[2] = n;

		// See if it gets removed
		GridInfo info = new GridInfo();
		assertTrue(alg.clusterToSparse(graph));
		assertTrue(alg.sparseToGrid(info));
		assertTrue(info.hasCornerSquare);

		assertEquals(5, info.cols);
		assertEquals(4, info.rows);
		assertEquals(5*4, info.nodes.size());
	}

	void convert( ChessboardCornerClusterToGrid alg, int rows, int cols, boolean randomized ) {
		ChessboardCornerGraph graph = new ChessboardCornerGraph();
		graph.corners = createGrid(rows, cols, true);

		if (randomized) {
			graph.corners.shuffle(rand);
			for (int i = 0; i < graph.corners.size; i++) {
				Node n = graph.corners.get(i);
				shuffle(n.edges);
				n.index = graph.corners.indexOf(n);
			}
		}

		GridInfo info = new GridInfo();
		assertTrue(alg.clusterToSparse(graph));
		assertTrue(alg.sparseToGrid(info));
		assertTrue(info.hasCornerSquare);

		// test shape
		if (rows == info.rows) {
			assertEquals(cols, info.cols);
		} else {
			assertEquals(rows, info.cols);
			assertEquals(cols, info.rows);
		}
		assertEquals(rows*cols, info.nodes.size());

		// first should be a corner
		Node n0 = info.get(0, 0);
		assertEquals(2, n0.countEdges());

		// it should also have coordinate (0,0)
		assertEquals(0, n0.getX(), UtilEjml.TEST_F64);
		assertEquals(0, n0.getY(), UtilEjml.TEST_F64);

		// test right handled
		for (int i = 0; i < 4; i++) {
			int j = (i + 1)%4;
			if (n0.edges[i] != null && n0.edges[j] != null) {
				double angle0 = direction(n0, n0.edges[i]);
				double angle1 = direction(n0, n0.edges[j]);
				assertEquals(Math.PI/2.0, UtilAngle.distanceCCW(angle0, angle1), 0.001);
				break;
			}
		}
	}

	static double direction( Node a, Node b ) {
		return Math.atan2(b.getY() - a.getY(), b.getX() - a.getX());
	}

	@Test void selectCorner() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		alg.setRequireCornerSquares(true);
		assertEquals(0, alg.selectCorner(createGridInfo(2, 2, true)));
		assertEquals(0, alg.selectCorner(createGridInfo(2, 3, true)));
		assertEquals(0, alg.selectCorner(createGridInfo(3, 3, true)));
	}

	/**
	 * Feed it a cluster with and without corner squares
	 */
	@Test void selectCorner_NoCornerSquare() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();

		// all corners are candidates
		int idx = alg.selectCorner(createGridInfo(2, 2, false));
		assertEquals(0, alg.cornerList.get(idx).index); // 4 non corner squares
		idx = alg.selectCorner(createGridInfo(2, 3, false));
		assertEquals(2, alg.cornerList.get(idx).index); // 2 corner squares

		// tell it to only consider proper corners
		alg.setRequireCornerSquares(true);
		idx = alg.selectCorner(createGridInfo(2, 2, false));  // 4 non corner squares
		assertEquals(-1, idx);
		idx = alg.selectCorner(createGridInfo(2, 3, false));  // 2 corner squares
		assertEquals(2, alg.cornerList.get(idx).index);
	}

	@Test void isCornerValidOrigin_2x2() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		DogArray<Node> nodes = createGrid(2, 2, true);

//		assertTrue(alg.isCornerValidOrigin(nodes.get(0)));
		assertTrue(alg.isCornerValidOrigin(nodes.get(1)));
		assertTrue(alg.isCornerValidOrigin(nodes.get(2)));
		assertTrue(alg.isCornerValidOrigin(nodes.get(3)));
	}

	@Test void isCornerValidOrigin_2x3() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		DogArray<Node> nodes = createGrid(2, 3, true);

		assertTrue(alg.isCornerValidOrigin(nodes.get(0)));
		assertFalse(alg.isCornerValidOrigin(nodes.get(2)));
		assertTrue(alg.isCornerValidOrigin(nodes.get(3)));
		assertFalse(alg.isCornerValidOrigin(nodes.get(5)));
	}

	@Test void isCornerValidOrigin_3x3() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		DogArray<Node> nodes = createGrid(3, 3, true);

		assertTrue(alg.isCornerValidOrigin(nodes.get(0)));
		assertFalse(alg.isCornerValidOrigin(nodes.get(2*3)));
		assertFalse(alg.isCornerValidOrigin(nodes.get(2)));
		assertTrue(alg.isCornerValidOrigin(nodes.get(2*3 + 2)));
	}

	@Test void isWhiteSquare() {
		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		DogArray<Node> nodes = createGrid(3, 3, true);

		int o = 3 + 1; // offset from a code. +1 x and +1 y

		// Test all nodes it can test. All the others are outside the allowed bounds
		assertFalse(alg.isWhiteSquareOrientation(nodes.get(0), nodes.get(o)));
		assertTrue(alg.isWhiteSquareOrientation(nodes.get(1), nodes.get(1 + o)));
		assertTrue(alg.isWhiteSquareOrientation(nodes.get(3), nodes.get(3 + o)));
		assertFalse(alg.isWhiteSquareOrientation(nodes.get(4), nodes.get(4 + o)));
	}

	@Test void orderNodes() {
		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				for (int i = 0; i < 10; i++) {
					DogArray<Node> corners = createGrid(rows, cols, true);

					// randomize the order so that it needs to do something interesting
					corners.shuffle(rand);
					orderNodes(corners, rows, cols);
				}
			}
		}
	}

	void orderNodes( DogArray<Node> corners, int rows, int cols ) {
		GridInfo info = new GridInfo();
		info.reset();

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		assertTrue(alg.orderNodes(corners, info));

		// rows and cols could be swapped arbitrarily
		if (rows == info.rows) {
			assertEquals(cols, info.cols);
		} else {
			assertEquals(rows, info.cols);
			assertEquals(cols, info.rows);
		}
	}

	@Test void isRightHanded() {
		Node n = new Node();
		n.corner = new ChessboardCorner();
		n.corner.x = 10;
		n.corner.y = 20;

		Node a = new Node();
		a.corner = new ChessboardCorner();
		Node b = new Node();
		b.corner = new ChessboardCorner();

		a.corner.x = n.getX() + 10; // this will be the column
		a.corner.y = n.getY();

		b.corner.x = n.getX();
		b.corner.y = n.getY() + 10; // row

		n.edges[0] = a;
		n.edges[1] = b;

		assertFalse(ChessboardCornerClusterToGrid.isRightHanded(n, 0, 1));
		assertTrue(ChessboardCornerClusterToGrid.isRightHanded(n, 1, 0));
	}

	@Test void alignEdges() {
		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				DogArray<Node> corners = createGrid(rows, cols, true);

				// Shift the edges so that they won't be aligned
				// They are already in order
				Node[] tmp = new Node[4];
				for (Node n : corners.toList()) {
					int amount = rand.nextInt(3);

					System.arraycopy(n.edges, 0, tmp, 0, 4);
					for (int i = 0; i < 4; i++) {
						n.edges[(i + amount)%4] = tmp[i];
					}
				}

				ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
				assertTrue(alg.alignEdges(corners));

				// Check using (i+2)%4 rule
				for (Node n : corners.toList()) {
					for (int i = 0; i < 4; i++) {
						if (n.edges[i] == null)
							continue;
						assertSame(n, n.edges[i].edges[(i + 2)%4]);
					}
				}
			}
		}
	}

	private GridInfo createGridInfo( int rows, int cols, boolean cornerSquare ) {
		GridInfo output = new GridInfo();
		output.rows = rows;
		output.cols = cols;
		output.nodes.addAll(createGrid(rows, cols, cornerSquare).toList());
		return output;
	}

	private DogArray<Node> createGrid( int rows, int cols, boolean cornerSquare ) {
		DogArray<Node> corners = new DogArray<>(Node::new);

		// declare the grid
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Node n = corners.grow();
				n.index = row*cols + col;
				n.corner = new ChessboardCorner();
				n.corner.x = col*30;
				n.corner.y = row*30;
				n.corner.orientation = Math.PI/4;
				if ((row%2 + col%2) == 1)
					n.corner.orientation -= Math.PI/2;
				if (!cornerSquare)
					n.corner.orientation *= -1;

				if (row > 0) {
					Node p = corners.get((row - 1)*cols + col);
					p.edges[1] = n;
					n.edges[3] = p;
				}
				if (col > 0) {
					Node p = corners.get(row*cols + col - 1);
					p.edges[0] = n;
					n.edges[2] = p;
				}
			}
		}
		return corners;
	}

	@Test void sortEdgesCCW() {
		DogArray<Node> corners = new DogArray<>(Node::new);

		for (int nodeIdx = 0; nodeIdx < 6; nodeIdx++) {
			Node target = corners.grow();
			target.corner = new ChessboardCorner();
			target.corner.x = 10;
			target.corner.y = 12;

			// always 3 edges 90 degrees apart
			for (int i = 0; i < 3; i++) {
				double theta = -(i + nodeIdx)*Math.PI/2.0 + nodeIdx;
				double c = Math.cos(theta);
				double s = Math.sin(theta);

				double r = 4;
				Node n = new Node();
				n.corner = new ChessboardCorner();
				n.corner.x = target.getX() + r*c;
				n.corner.y = target.getY() + r*s;
				target.edges[i + 1] = n;
			}
			shuffle(target.edges);
		}

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();

		alg.sortEdgesCCW(corners);

		for (Node n : corners.toList()) {
			int tested = 0;
			for (int i = 0; i < 4; i++) {
				int j = (i + 1)%4;
				if (n.edges[i] == null || n.edges[j] == null)
					continue;
				Node m0 = n.edges[i];
				Node m1 = n.edges[j];

				double theta0 = Math.atan2(m0.getY() - n.getY(), m0.getX() - n.getX());
				double theta1 = Math.atan2(m1.getY() - n.getY(), m1.getX() - n.getX());

				double diff = UtilAngle.distanceCCW(theta0, theta1);
				assertTrue(UtilAngle.dist(diff, Math.PI/2.0) < 0.01);
				tested++;
			}
			assertEquals(2, tested);
		}
	}

	/**
	 * Need to shift elements after sorting to ensure edges are every 90 degrees
	 */
	@Test void sortEdgesCCW_shift() {
		DogArray<Node> corners = new DogArray<>(Node::new);

		// Add nodes with 3 edges
		for (int jump = 0; jump < 3; jump++) {
			Node target = corners.grow();
			target.corner = new ChessboardCorner();
			target.corner.x = 10;
			target.corner.y = 12;

			for (int i = 0; i < 3; i++) {
				double theta = i*Math.PI/2;
				if (i > jump)
					theta += Math.PI/2;
				double c = Math.cos(theta);
				double s = Math.sin(theta);

				double r = 4;
				Node n = new Node();
				n.corner = new ChessboardCorner();
				n.corner.x = target.getX() + r*c;
				n.corner.y = target.getY() + r*s;
				target.edges[i] = n;
			}
			// shuffle to make it a better test
			shuffle(target.edges);
		}

		// add Nodes with two edges
		for (int count = 0; count < 10; count++) {
			Node target = corners.grow();
			target.corner = new ChessboardCorner();
			target.corner.x = 10;
			target.corner.y = 12;

			for (int i = 0; i < 2; i++) {
				double theta = i*Math.PI/2;
				double c = Math.cos(theta);
				double s = Math.sin(theta);

				double r = 4;
				Node n = new Node();
				n.corner = new ChessboardCorner();
				n.corner.x = target.getX() + r*c;
				n.corner.y = target.getY() + r*s;
				target.edges[i] = n;
			}
			// shuffle to make it a better test
			shuffle(target.edges);
		}

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();

		alg.sortEdgesCCW(corners);

		for (Node n : corners.toList()) {
			Node m0 = n.edges[0];
			double theta0 = Math.atan2(m0.getY() - n.getY(), m0.getX() - n.getX());

			for (int i = 0; i < 4; i++) {
				Node m = n.edges[i];
				if (m == null)
					continue;

				double thetaI = Math.atan2(m.getY() - n.getY(), m.getX() - n.getX());

				assertEquals(i*Math.PI/2.0, UtilAngle.distanceCCW(theta0, thetaI), 0.001);
			}
		}
	}

	private void shuffle( Node[] edges ) {
		for (int i = 0; i < 4; i++) {
			int src = rand.nextInt(4 - i);
			Node a = edges[src];
			Node b = edges[4 - i - 1];
			edges[4 - i - 1] = a;
			edges[src] = b;
		}
	}
}
