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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSquareEdge {
	@Test
	public void destination() {
		SquareNode a = new SquareNode();
		SquareNode b = new SquareNode();
		SquareNode c = new SquareNode();

		SquareEdge edge = new SquareEdge();
		edge.a = a;
		edge.b = b;

		assertTrue(edge.b == edge.destination(edge.a));
		assertTrue(edge.a == edge.destination(edge.b));

		try {
			edge.destination(c);
			fail("Should have thrown exception");
		} catch( RuntimeException ignore){}
	}

	@Test
	public void reset() {
		SquareEdge edge = new SquareEdge();
		edge.a = new SquareNode();
		edge.b = new SquareNode();
		edge.sideA = 1;
		edge.sideB = 2;
		edge.distance = 3;

		edge.reset();

		assertTrue(null == edge.a);
		assertTrue(null == edge.b);
		assertTrue(-1 == edge.sideA);
		assertTrue(-1 == edge.sideA);
		assertTrue(-1 == edge.distance);
	}
}
