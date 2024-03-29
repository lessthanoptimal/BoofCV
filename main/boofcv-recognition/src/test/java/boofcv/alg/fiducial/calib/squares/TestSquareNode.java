/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSquareNode extends BoofStandardJUnit {

	@Test void distanceSqCorner() {
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(4);
		a.square.get(0).setTo(-2,-2);
		a.square.get(1).setTo( 2,-2);
		a.square.get(2).setTo( 2, 2);
		a.square.get(3).setTo(-2, 2);

		assertEquals(0, a.distanceSqCorner(new Point2D_F64(-2, -2)), 1e-8);
		assertEquals(0, a.distanceSqCorner(new Point2D_F64( 2, 2)),1e-8);
		assertEquals(1, a.distanceSqCorner(new Point2D_F64(-3, 2)),1e-8);
		assertEquals(4, a.distanceSqCorner(new Point2D_F64(-4, 2)),1e-8);
	}

	@Test void getNumberOfConnections() {
		SquareNode a = new SquareNode();
		a.square = new Polygon2D_F64(4);

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
