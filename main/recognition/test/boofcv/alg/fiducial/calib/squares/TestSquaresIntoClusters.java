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

package boofcv.alg.fiducial.calib.squares;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSquaresIntoClusters {

	@Test
	public void detachEdge() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();

		SquaresIntoClusters alg = new SquaresIntoClusters();

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

		SquaresIntoClusters alg = new SquaresIntoClusters();

		alg.connect(a,1,b,2,2.5);

		assertTrue(a.edges[1] == b.edges[2]);
		SquareEdge e = a.edges[1];
		assertEquals(e.distance,2.5,1e-8);
		assertEquals(1, e.sideA);
		assertEquals(2, e.sideB);
	}
}
