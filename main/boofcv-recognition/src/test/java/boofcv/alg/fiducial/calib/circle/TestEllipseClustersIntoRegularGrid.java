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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.NodeInfo;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.curve.EllipseRotated_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.Tuple2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.TestEllipseClustersIntoGrid.convertIntoGridOfLists;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestEllipseClustersIntoRegularGrid extends BoofStandardJUnit {

	@Test void process_various() {
		process(4,3, false);
		process(3,3, false);
		process(4,1, true);
		process(1,4, true);
		process(2,2, false);
		process(2,4, false);
		process(1,1, true);
	}

	public void process( int rows , int cols , boolean fail ) {
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = TestEllipseClustersIntoGrid.createRegularGrid(rows, cols);

		List<List<Node>> clusters = new ArrayList<>();
		clusters.add( grid.d0 );

		EllipseClustersIntoRegularGrid alg = new EllipseClustersIntoRegularGrid();

		alg.process(grid.d1,clusters);

		DogArray<Grid> found = alg.getGrids();

		if( fail ) {
			assertEquals(0,found.size);
		} else {
			assertEquals(1, found.size);
			Grid g = found.get(0);

			assertTrue((g.rows == rows && g.columns == cols) || (g.rows == cols && g.columns == rows));
		}
	}

	@Test void checkGridSize() {
		// create a grid in the expected format
		int rows = 4, cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = TestEllipseClustersIntoGrid.createRegularGrid(rows, cols);

		EllipseClustersIntoRegularGrid alg = new EllipseClustersIntoRegularGrid();
		alg.computeNodeInfo(grid.d1,grid.d0);

		// split into the two grids
		List<List<NodeInfo>> input = convertIntoGridOfLists(0, rows, cols, alg);

		assertTrue(EllipseClustersIntoRegularGrid.checkGridSize(input,rows*cols));
		assertFalse(EllipseClustersIntoRegularGrid.checkGridSize(input,rows*cols+1));
	}

	@Test void createRegularGrid() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = TestEllipseClustersIntoGrid.createRegularGrid(rows, cols);

		EllipseClustersIntoRegularGrid alg = new EllipseClustersIntoRegularGrid();
		alg.computeNodeInfo(grid.d1,grid.d0);

		// split into the two grids
		List<List<NodeInfo>> input = convertIntoGridOfLists(0, rows, cols, alg);

		Grid g = new Grid();
		alg.createRegularGrid(input,g);

		assertEquals(rows,g.rows);
		assertEquals(cols,g.columns);

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				assertTrue(input.get(row).get(col).ellipse == g.get(row,col));
			}
		}
	}
}
