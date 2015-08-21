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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSquareNode {
	@Test
	public void reset() {
		SquareNode a = new SquareNode();




		a.reset();
		fail("implement");
	}

	@Test
	public void distanceSqCorner() {
		fail("implement");
	}

	@Test
	public void getNumberOfConnections() {
		SquareNode a = new SquareNode();

		assertEquals(0,a.getNumberOfConnections());
		a.edges[2] = new SquareEdge();
		assertEquals(1,a.getNumberOfConnections());
		a.edges[0] = new SquareEdge();
		assertEquals(2,a.getNumberOfConnections());
		a.edges[3] = new SquareEdge();
		assertEquals(3,a.getNumberOfConnections());
		a.edges[1] = new SquareEdge();
		assertEquals(4,a.getNumberOfConnections());
	}
}
