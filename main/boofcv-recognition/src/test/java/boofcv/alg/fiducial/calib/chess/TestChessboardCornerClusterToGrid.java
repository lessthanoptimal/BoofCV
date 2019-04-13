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

import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import georegression.metric.UtilAngle;
import org.ddogleg.struct.FastQueue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestChessboardCornerClusterToGrid
{
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
		fail("Implement");
	}

	@Test
	void isRightHanded() {
		fail("Implement");
	}

	@Test
	void alignEdges() {
		// TODO write next.
		fail("Implement");
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
	 * Need to shift elements after sorting to ensure cardinal directions.
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