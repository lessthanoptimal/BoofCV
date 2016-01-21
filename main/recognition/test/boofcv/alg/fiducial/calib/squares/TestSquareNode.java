/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSquareNode {

	@Test
	public void distanceSqCorner() {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(4);
		a.corners.get(0).set(-2,-2);
		a.corners.get(1).set( 2,-2);
		a.corners.get(2).set( 2, 2);
		a.corners.get(3).set(-2, 2);

		assertEquals(0, a.distanceSqCorner(new Point2D_F64(-2, -2)), 1e-8);
		assertEquals(0, a.distanceSqCorner(new Point2D_F64( 2, 2)),1e-8);
		assertEquals(1, a.distanceSqCorner(new Point2D_F64(-3, 2)),1e-8);
		assertEquals(4, a.distanceSqCorner(new Point2D_F64(-4, 2)),1e-8);
	}

	@Test
	public void getNumberOfConnections() {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(4);

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
