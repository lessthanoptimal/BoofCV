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
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestChessboardCornerGraph extends BoofStandardJUnit {
	@Test
	void findClosest() {
		ChessboardCornerGraph alg = new ChessboardCornerGraph();
		for (int i = 0; i < 10; i++) {
			Node n = alg.corners.grow();
			n.corner = new ChessboardCorner();
			n.corner.setTo(i*5, 1);
		}
		Node n = alg.findClosest(10.1, 0.5);
		assertEquals(0, n.corner.distance(10, 1), UtilEjml.TEST_F64);
	}

	@Test
	void reset() {
		ChessboardCornerGraph alg = new ChessboardCornerGraph();
		for (int i = 0; i < 10; i++) {
			Node n = alg.corners.grow();
			n.corner = new ChessboardCorner();
			n.corner.setTo(i*5, 1);
		}
		assertTrue(alg.corners.size > 0);
		alg.reset();
		assertEquals(0, alg.corners.size);
	}

	@Test
	void node_putEdgesIntoList() {
		Node n = new Node();
		List<Node> found = new ArrayList<>();
		found.add(new Node());
		n.putEdgesIntoList(found);
		assertEquals(0, found.size());

		n.edges[1] = new Node();
		n.putEdgesIntoList(found);
		assertEquals(1, found.size());
	}

	@Test
	void node_rotateEdgesDown() {
		Node[] tmp = new Node[4];

		tmp[1] = new Node();
		tmp[2] = new Node();
		tmp[3] = new Node();

		Node n = new Node();
		System.arraycopy(tmp, 0, n.edges, 0, 4);

		n.rotateEdgesDown();

		for (int i = 0; i < 4; i++) {
			assertSame(tmp[(i + 1)%4], n.edges[i]);
		}
	}

	@Test
	void node_reset() {
		Node n = new Node();
		n.corner = new ChessboardCorner();
		n.corner.setTo(1, 2);
		n.index = 3;
		n.corner.orientation = 9;
		n.edges[1] = new Node();
		n.reset();
		assertEquals(-1, n.index);
		for (int i = 0; i < 4; i++) {
			assertNull(n.edges[i]);
		}
		assertNull(n.corner);
	}

	@Test
	void node_countEdges() {
		Node n = new Node();
		assertEquals(0, n.countEdges());
		n.edges[1] = new Node();
		assertEquals(1, n.countEdges());
		n.edges[3] = new Node();
		assertEquals(2, n.countEdges());
		n.edges[0] = new Node();
		n.edges[2] = new Node();
		assertEquals(4, n.countEdges());
	}
}
