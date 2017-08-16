/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.TestEllipseClustersIntoGrid.*;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEllipseClustersIntoAsymmetricGrid {

	/**
	 * See if it can handle a very easy case
	 */
	@Test
	public void process() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows, cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		checkProcess(rows, cols, grid, alg, nodes);
	}

	private void checkProcess(int rows, int cols, Tuple2<List<Node>, List<EllipseRotated_F64>> grid, EllipseClustersIntoAsymmetricGrid alg, List<List<Node>> nodes) {
		alg.process(grid.data1, nodes);

		FastQueue<Grid> found = alg.getGrids();

		assertEquals( 1 , found.size() );
		checkShape(rows, cols, found.get(0));
	}

	private void checkShape(int rows, int cols, Grid found) {
		if( rows*2 - 1 == found.rows ) {
			assertEquals(rows * 2 - 1, found.rows);
			assertEquals(cols * 2 - 1, found.columns);
		} else {
			assertEquals(rows * 2 - 1, found.columns);
			assertEquals(cols * 2 - 1, found.rows);
		}
	}

	/**
	 * Apply some affine distortion so that the grid isn't perfect
	 */
	@Test
	public void process_affine() {
		// scale different amounts along each axis and translate for fun
		process_affine( new Affine2D_F64(1.05,0,0,0.95,1,2));

		// rotate a bit
		Affine2D_F64 rotate = ConvertTransform_F64.convert(new Se2_F64(0,0,0.5),(Affine2D_F64)null);
		process_affine( rotate );
	}

	private void process_affine( Affine2D_F64 affine ) {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows, cols);

		for( EllipseRotated_F64 e : grid.data1 ) {
			AffinePointOps_F64.transform(affine, e.center, e.center);
		}

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		checkProcess(rows, cols, grid, alg, nodes);
	}

	/**
	 * Multiple grids in view at the same time
	 */
	@Test
	public void process_multiple_grids() {
		// create two grids
		int rows = 4; int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid0 = createAsymGrid(rows, cols);
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid1 = createAsymGrid(rows, cols);

		List<List<Node>> nodes = new ArrayList<>();
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		nodes.add( grid0.data0 );
		nodes.add( grid1.data0 );
		ellipses.addAll( grid0.data1 );
		ellipses.addAll( grid1.data1 );

		// adjust indexing for second grid
		for( Node n : grid1.data0 ) {
			n.cluster = 1;
			n.which += grid0.data1.size();
			for (int i = 0; i < n.connections.size(); i++) {
				n.connections.data[i] += grid0.data1.size();
			}
		}

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		alg.process(ellipses, nodes);

		FastQueue<Grid> found = alg.getGrids();

		assertEquals( 2 , found.size() );
		checkShape(rows, cols, found.get(0));
		checkShape(rows, cols, found.get(1));
	}

	/**
	 * Call process multiple times and see if it blows up
	 */
	@Test
	public void process_multiple_calls() {
		// create a grid in the expected format
		int rows = 4; int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows, cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		checkProcess(rows, cols, grid, alg, nodes);

		// process it a second time with a different grid
		rows = 4; cols = 3;
		grid = createAsymGrid(rows, cols);
		alg = new EllipseClustersIntoAsymmetricGrid();
		nodes.clear();
		nodes.add( grid.data0 );

		checkProcess(rows, cols, grid, alg, nodes);
	}

	/**
	 * Give it input with a grid that's too small and see if it blows up
	 */
	@Test
	public void process_too_small() {
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = TestEllipseClustersIntoGrid.createRegularGrid(2, 1);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		alg.process(grid.data1, nodes);

		assertEquals(0, alg.getGrids().size);
	}

	@Test
	public void combineGrids() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows, cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);

		// split into the two grids
		List<List<NodeInfo>> outer = convertIntoGridOfLists(0, rows, cols, alg);
		List<List<NodeInfo>> inner = convertIntoGridOfLists(rows*cols, rows-1, cols-1, alg);

		alg.combineGrids(outer,inner);

		Grid found = alg.getGrids().get(0);

		assertEquals(rows*2-1, found.rows);
		assertEquals(cols*2-1, found.columns);

		for (int row = 0; row < found.rows; row++) {
			if( row % 2 == 0 ) {
				for (int col = 0; col < found.columns; col += 2) {
					int index = row*found.columns + col;
					assertTrue( outer.get(row/2).get(col/2).ellipse == found.ellipses.get(index));
					if( index+1 < found.rows*found.columns)
						assertTrue( null == found.ellipses.get(index + 1));
				}
			} else {
				for (int col = 1; col < found.columns; col += 2) {
					int index = row*found.columns + col;
					assertTrue( null == found.ellipses.get(index - 1));
					assertTrue( inner.get(row/2).get(col/2).ellipse == found.ellipses.get(index));
				}
			}
		}
	}

	@Test
	public void checkGridSize() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows, cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);

		// split into the two grids
		List<List<NodeInfo>> outer = convertIntoGridOfLists(0, rows, cols, alg);
		List<List<NodeInfo>> inner = convertIntoGridOfLists(rows*cols, rows-1, cols-1, alg);

		// test the function
		int expectedSize = rows*cols + (rows-1)*(cols-1);
		assertTrue(EllipseClustersIntoAsymmetricGrid.checkGridSize(outer,inner,expectedSize));

		inner.get(1).remove(1);
		assertFalse(EllipseClustersIntoAsymmetricGrid.checkGridSize(outer,inner,expectedSize));
	}

	@Test
	public void findInnerGrid() {
		int rows = 3;
		int cols = 4;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createAsymGrid(rows,cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);

		List<List<NodeInfo>> outerGrid = new ArrayList<>();

		// the outer grid should be contained in the first rows*cols elements
		for (int i = 0, index=0; i < rows; i++) {
			List<NodeInfo> row = new ArrayList<>();
			for (int j = 0; j < cols; j++, index++) {
				row.add(alg.listInfo.get(index));
			}
			outerGrid.add(row);
		}

		List<List<NodeInfo>> found = alg.findInnerGrid(outerGrid,rows*cols + (rows-1)*(cols-1));

		// see if it's the expected size
		int size = 0;
		for (int i = 0; i < found.size(); i++) {
			size += found.get(i).size();
		}
		assertEquals(size, (rows-1)*(cols-1));

		// make sure none of the found elements are in the outer grid
		for (int i = 0; i < found.size(); i++) {
			List<NodeInfo> l = found.get(i);
			for (int j = 0; j < l.size(); j++) {
				NodeInfo n = l.get(j);
				boolean matched = false;
				for (int k = 0; k < rows * cols; k++) {
					if( n == alg.listInfo.get(k)) {
						matched = true;
						break;
					}
				}
				assertFalse(matched);
			}
		}
	}

	@Test
	public void selectInnerSeed() {
		NodeInfo n00 = setNodeInfo(null,-2,0);
		NodeInfo n01 = setNodeInfo(null,-2,1);
		NodeInfo n11 = setNodeInfo(null,-1,1);
		NodeInfo n10 = setNodeInfo(null,-1,0);

		NodeInfo n = setNodeInfo(null,-1.5,0.5); // solution
		NodeInfo f = setNodeInfo(null,-0.5,0.8); // some noise

		n00.edges.grow().target = n;
		n00.edges.grow().target = f;
		n01.edges.grow().target = n;
		n01.edges.grow().target = f;
		n11.edges.grow().target = n;
		n11.edges.grow().target = f;
		n10.edges.grow().target = n;
		n10.edges.grow().target = f;

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		NodeInfo found = alg.selectInnerSeed(n00,n01,n10,n11);
		assertTrue( found == n );
	}

	private Tuple2<List<Node>,List<EllipseRotated_F64>> createAsymGrid(int rows , int cols ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				ellipses.add( new EllipseRotated_F64(col,row,0.1,0.1,0) );
			}
		}
		for (int row = 0; row < rows-1; row++) {
			for (int col = 0; col < cols-1; col++) {
				ellipses.add( new EllipseRotated_F64(col+0.5,row+0.5,0.1,0.1,0) );
			}
		}

		return connectEllipses(ellipses, 1.1 );
	}
}
