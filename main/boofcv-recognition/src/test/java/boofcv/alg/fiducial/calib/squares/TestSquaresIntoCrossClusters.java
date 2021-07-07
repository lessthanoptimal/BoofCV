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

package boofcv.alg.fiducial.calib.squares;

import boofcv.alg.shapes.polygon.DetectPolygonFromContour;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestSquaresIntoCrossClusters extends BoofStandardJUnit {

	/**
	 * Create a simple perfect cluster. Do a crude test based on number of edge histogram
	 */
	@Test void process_simple() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.05, -1);

		List<DetectPolygonFromContour.Info> squares = new ArrayList<>();
		squares.add(createSquare(7, 8));
		squares.add(createSquare(9, 8));
		squares.add(createSquare(8, 9));
		squares.add(createSquare(7, 10));
		squares.add(createSquare(9, 10));

		List<List<SquareNode>> clusters = alg.process(squares);

		assertEquals(1, clusters.size());

		List<SquareNode> cluster = clusters.get(0);

		int[] connections = new int[5];
		for (SquareNode n : cluster) {
			connections[n.getNumberOfConnections()]++;
		}

		assertEquals(0, connections[0]);
		assertEquals(4, connections[1]);
		assertEquals(0, connections[2]);
		assertEquals(0, connections[3]);
		assertEquals(1, connections[4]);
	}

	/**
	 * Tests shapes with corners that touch the image border
	 */
	@Test void shapesOnBorder() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.05, -1);

		List<DetectPolygonFromContour.Info> squares = new ArrayList<>();
		squares.add(createSquare(7, 8));
		squares.add(createSquare(9, 8));
		squares.add(createSquare(8, 9));
		squares.add(createSquare(7, 10));
		squares.add(createSquare(9, 10));

		markTouch(squares.get(0), true, false, true, true);
		markTouch(squares.get(1), false, true, true, false);
		markTouch(squares.get(3), true, true, false, true);

		List<List<SquareNode>> clusters = alg.process(squares);

		assertEquals(1, clusters.size());
		assertEquals(5, clusters.get(0).size());
	}

	private void markTouch( DetectPolygonFromContour.Info info, boolean... marks ) {
		for (boolean b : marks) {
			info.borderCorners.add(b);
		}
	}

	/**
	 * Tests the corner distance threshold. two nodes should be barely within tolerance of each other with the 3rd
	 * barely not in tolerance
	 */
	@Test void process_connect_threshold() {
		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(0.2, -1);

		List<DetectPolygonFromContour.Info> squares = new ArrayList<>();
		squares.add(createSquare(5, 6));
		squares.add(createSquare(6.20001, 7));
		squares.add(createSquare(6.1999999, 5));

		List<List<SquareNode>> clusters = alg.process(squares);

		assertEquals(2, clusters.size());
	}

	private DetectPolygonFromContour.Info createSquare( double x, double y ) {
		DetectPolygonFromContour.Info info = new DetectPolygonFromContour.Info();
		info.reset();
		info.polygon = new Polygon2D_F64(4);

		info.polygon.get(0).setTo(x, y);
		info.polygon.get(1).setTo(x + 1, y);
		info.polygon.get(2).setTo(x + 1, y - 1);
		info.polygon.get(3).setTo(x, y - 1);

		return info;
	}

	@Test void getCornerIndex() {
		SquareNode node = new SquareNode();
		node.square = new Polygon2D_F64(4);
		node.square.get(0).setTo(5, 6);
		node.square.get(1).setTo(6, 7);
		node.square.get(2).setTo(7, 8);
		node.square.get(3).setTo(8, 9);

		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(5, -1);

		assertEquals(0, alg.getCornerIndex(node, 5, 6));
		assertEquals(1, alg.getCornerIndex(node, 6, 7));
		assertEquals(2, alg.getCornerIndex(node, 7, 8));
		assertEquals(3, alg.getCornerIndex(node, 8, 9));
	}

	@Test void candidateIsMuchCloser() {
		SquareNode node0 = new SquareNode();
		SquareNode node1 = new SquareNode();

		node0.largestSide = 2;
		node1.largestSide = 1;

		SquaresIntoCrossClusters alg = new SquaresIntoCrossClusters(5, -1);

		// test obvious cases
		assertTrue(alg.candidateIsMuchCloser(node0, node1, 0));
		assertFalse(alg.candidateIsMuchCloser(node0, node1, 20));

		double frac = alg.tooFarFraction;
		node1.square = createSquare(12, 10).polygon;
		// the closest neighboring node should be 1 away
		assertTrue(alg.candidateIsMuchCloser(node0, node1, Math.pow(2*frac - 1e-6, 2)));
		assertFalse(alg.candidateIsMuchCloser(node0, node1, Math.pow(2*frac + 1e-6, 2)));
	}
}
