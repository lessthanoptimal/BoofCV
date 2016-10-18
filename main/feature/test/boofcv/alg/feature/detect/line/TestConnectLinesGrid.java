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

package boofcv.alg.feature.detect.line;

import boofcv.struct.feature.MatrixOfList;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestConnectLinesGrid {

	/**
	 * Very basic check which sees if lines are being connected in the same region
	 */
	@Test
	public void connectInSameRegion() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(1, 1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,3));
		grid.get(0,0).add(new LineSegment2D_F32(2,3,4,6));

		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);

		List<LineSegment2D_F32> list = grid.createSingleList();

		assertEquals(1,list.size());
		LineSegment2D_F32 l = list.get(0);
		assertEquals(0,l.a.x,1e-8);
		assertEquals(0,l.a.y,1e-8);
		assertEquals(4,l.b.x,1e-8);
		assertEquals(6,l.b.y,1e-8);
	}

	/**
	 * Orientation of two lines should be compared using the half circle angle.
	 *
	 * The test is designed to test that both angles are computed using atan() and compared
	 * between 0 and 180 degrees
	 */
	@Test
	public void checkHalfCircleAngle() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(1, 1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,0,2));
		grid.get(0,0).add(new LineSegment2D_F32(0,0,0.001f,-2));

		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);

		List<LineSegment2D_F32> list = grid.createSingleList();
		assertEquals(1, list.size());
	}

	/**
	 * Very basic check to see if lines are connected between regions.
	 */
	@Test
	public void connectToNeighborRegion() {
		// check all the neighbors around 1,1 and see if they get connected
		checkConnectNeighbor(0,0);
		checkConnectNeighbor(1,0);
		checkConnectNeighbor(2,0);
		checkConnectNeighbor(2,1);
		checkConnectNeighbor(2,2);
		checkConnectNeighbor(1,2);
		checkConnectNeighbor(0,2);
		checkConnectNeighbor(0,1);
	}

	/**
	 * Makes sure the angle tolerance parameter is correctly set and processed
	 */
	@Test
	public void checkAngleTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(1, 1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(2,0,4,4));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(Math.PI,1,1);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());

	}

	/**
	 * Makes sure the tangent distance tolerance parameter is correctly set and processed
	 */
	@Test
	public void checkTangentTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(1, 1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(2,1,4,1));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(2,0.1,2);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(2,1.1,2);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());
	}

	/**
	 * Makes sure the parallel distance tolerance parameter is correctly set and processed
	 */
	@Test
	public void checkParallelTolerance() {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(1, 1);

		grid.get(0,0).add(new LineSegment2D_F32(0,0,2,0));
		grid.get(0,0).add(new LineSegment2D_F32(3,0,5,0));

		// have the tolerance be too tight
		ConnectLinesGrid app = new ConnectLinesGrid(2,2,0.1);
		app.process(grid);
		assertEquals(2,grid.createSingleList().size());

		// now make the tolerance broader
		app = new ConnectLinesGrid(2,2,1.1);
		app.process(grid);
		assertEquals(1,grid.createSingleList().size());
	}

	private void checkConnectNeighbor( int x , int y ) {
		MatrixOfList<LineSegment2D_F32> grid = new MatrixOfList<>(3, 3);

		grid.get(1,1).add(new LineSegment2D_F32(0,0,2,3));
		grid.get(x,y).add(new LineSegment2D_F32(2,3,4,6));

		ConnectLinesGrid app = new ConnectLinesGrid(0.1,1,1);
		app.process(grid);

		List<LineSegment2D_F32> list = grid.createSingleList();

		assertEquals(1,list.size());
		LineSegment2D_F32 l = list.get(0);
		if( l.a.x == 4 ) {
			Point2D_F32 temp = l.a;
			l.a = l.b;
			l.b = temp;
		}
		assertEquals(0,l.a.x,1e-8);
		assertEquals(0,l.a.y,1e-8);
		assertEquals(4,l.b.x,1e-8);
		assertEquals(6,l.b.y,1e-8);
	}
}
