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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
class TestChessboardCornerGraph {
	@Test
	void findClosest() {
		fail("Implement");
	}

	@Test
	void reset() {
		fail("Implement");
	}

	@Test
	void node_putEdgesIntoList() {
		fail("Implement");
	}

	@Test
	void node_rotateEdgesDown() {
		Node[] tmp = new Node[4];

		tmp[1] = new Node();
		tmp[2] = new Node();
		tmp[3] = new Node();

		Node n = new Node();
		System.arraycopy(tmp,0,n.edges,0,4);

		n.rotateEdgesDown();

		for (int i = 0; i < 4; i++) {
			assertSame(tmp[(i+1)%4],n.edges[i]);
		}
	}

	@Test
	void node_reset() {
		fail("Implement");
	}

	@Test
	void node_countEdges() {
		fail("Implement");
	}
}