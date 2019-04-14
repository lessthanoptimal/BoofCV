/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestChessboardCornerClusterToGrid
{
	Random rand = new Random(234);

	@Test
	void convert_nochange() {
		fail("Implement");
	}

	@Test
	void convert_randomized() {
		fail("Implement");
	}

	@Test
	void selectCorner() {
		fail("Implement");
	}

	@Test
	void isCornerValidOrigin() {
		fail("Implement");
	}

	@Test
	void orderNodes() {
		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				FastQueue<Node> corners = createGrid(rows, cols);

				// randomize the order so that it needs to do something interesting
				Collections.shuffle(corners.toList(),rand);

				orderNodes(corners,rows,cols);
			}
		}
	}

	void orderNodes(FastQueue<Node> corners , int rows, int cols ) {
		GridInfo info = new GridInfo();
		info.reset();

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
		assertTrue(alg.orderNodes(corners,info));

		// rows and cols could be swapped arbitrarily
		if( rows == info.rows ) {
			assertEquals(cols, info.cols);
		} else {
			assertEquals(rows, info.cols);
			assertEquals(cols, info.rows);
		}
	}

	@Test
	void isRightHanded() {
		Node n = new Node();
		n.x = 10;
		n.y = 20;

		Node a = new Node();
		Node b = new Node();

		a.x = n.x + 10; // this will be the column
		a.y = n.y;

		b.x = n.x;
		b.y = n.y + 10; // row

		n.edges[0] = a;
		n.edges[1] = b;

		assertFalse(ChessboardCornerClusterToGrid.isRightHanded(n,0,1));
		assertTrue(ChessboardCornerClusterToGrid.isRightHanded(n,1,0));
	}

	@Test
	void alignEdges() {
		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				FastQueue<Node> corners = createGrid(rows, cols);

				// Shift the edges so that they won't be aligned
				// They are already in order
				Node[] tmp = new Node[4];
				for( Node n : corners.toList() ) {
					int amount = rand.nextInt(3);

					System.arraycopy(n.edges,0,tmp,0,4);
					for (int i = 0; i < 4; i++) {
						n.edges[(i+amount)%4] = tmp[i];
					}
				}

				ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();
				assertTrue(alg.alignEdges(corners));

				// Check using (i+2)%4 rule
				for( Node n : corners.toList() ) {
					for (int i = 0; i < 4; i++) {
						if( n.edges[i] == null )
							continue;
						assertSame(n, n.edges[i].edges[(i + 2) % 4]);
					}
				}
			}
		}
	}

	private FastQueue<Node> createGrid(int rows, int cols) {
		FastQueue<Node> corners = new FastQueue<>(Node.class,true);

		// declare the grid
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				Node n = corners.grow();
				n.index = row*cols+col;
				n.x = col*30;
				n.y = row*30;
				if( row > 0 ) {
					Node p = corners.get((row-1)*cols+col);
					p.edges[1] = n;
					n.edges[3] = p;
				}
				if( col > 0 ) {
					Node p = corners.get(row*cols+col-1);
					p.edges[0] = n;
					n.edges[2] = p;
				}
			}
		}
		return corners;
	}

	@Test
	void sortEdgesCCW() {
		FastQueue<Node> corners = new FastQueue<>(Node.class,true);

		for (int nodeIdx = 0; nodeIdx < 6; nodeIdx++) {
			Node target = corners.grow();
			target.x = 10;
			target.y = 12;

			for (int i = 0; i < 3; i++) {
				double theta = -(i+nodeIdx)*0.5+nodeIdx;
				double c = Math.cos(theta);
				double s = Math.sin(theta);

				double r = 4;
				Node n = new Node();
				n.x = target.x + r*c;
				n.y = target.y + r*s;
				target.edges[i+1] = n;
			}
		}

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();

		alg.sortEdgesCCW(corners);

		for( Node n : corners.toList() ) {
			for (int i = 1; i < 3; i++) {
				Node m0 = n.edges[i-1];
				Node m1 = n.edges[i];

				double theta0 = Math.atan2(m0.y-n.y,m0.x-n.x);
				double theta1 = Math.atan2(m1.y-n.y,m1.x-n.x);

				double diff = UtilAngle.minus(theta1,theta0);
				assertTrue(diff>0.1);
			}
			assertNull(n.edges[3]);
		}
	}

	/**
	 * Need to shift elements after sorting to ensure edges are every 90 degrees
	 */
	@Test
	void sortEdgesCCW_shift() {
		FastQueue<Node> corners = new FastQueue<>(Node.class,true);
		for (int jump = 0; jump < 3; jump++) {
			Node target = corners.grow();
			target.x = 10;
			target.y = 12;

			for (int i = 0; i < 3; i++) {
				double theta = i*Math.PI/2;
				if( i > jump )
					theta += Math.PI/2;
				double c = Math.cos(theta);
				double s = Math.sin(theta);

				double r = 4;
				Node n = new Node();
				n.x = target.x + r*c;
				n.y = target.y + r*s;
				target.edges[i] = n;
			}
		}

		ChessboardCornerClusterToGrid alg = new ChessboardCornerClusterToGrid();

		alg.sortEdgesCCW(corners);

		for( Node n : corners.toList() ) {
			System.out.println();
			Node m0 = n.edges[0];
			double theta0 = Math.atan2(m0.y-n.y,m0.x-n.x);

			for (int i = 0; i < 4; i++) {
				Node m = n.edges[i];
				if( m == null )
					continue;

				double thetaI = Math.atan2(m.y-n.y,m.x-n.x);

				assertEquals(i*Math.PI/2.0,UtilAngle.distanceCCW(theta0,thetaI),0.001);
			}
		}
	}

	@Test
	void rotateCCW() {
		fail("Implement");
	}
}