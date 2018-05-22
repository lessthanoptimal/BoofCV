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
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.TestEllipseClustersIntoGrid.connectEllipses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestEllipseClustersIntoHexagonalGrid {

	private static final double hexY = Math.sqrt(3.0/4.0);

	/**
	 * See if it can handle a very easy case
	 */
	@Test
	public void process() {

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();

//		process(3, 3, alg); // too small. Simplies the code by igniring this case
		process(4, 3, alg);
		process(4, 4, alg);
		process(4, 5, alg);
		process(5, 5, alg);
		process(5, 8, alg);
	}

	private void process(int rows, int cols, EllipseClustersIntoHexagonalGrid alg) {
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createHexagonalGrid(rows, cols, 0.5, 1);
		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		processAndCheck(rows, cols, alg, grid, nodes);
	}


	private void checkShape(int rows, int cols, Grid found) {
		if( rows == found.rows ) {
			assertEquals(rows , found.rows);
			assertEquals(cols, found.columns);
		} else {
			assertEquals(rows , found.columns);
			assertEquals(cols , found.rows);
		}
	}

	/**
	 * Apply some affine distortion so that the grid isn't perfect
	 */
	@Test
	public void process_affine() {
		// scale different amounts along each axis and translate for fun
		Affine2D_F64 affine0 = new Affine2D_F64(1.05,0,0,0.95,1,2);
		// rotate a bit
		Affine2D_F64 affine1 = ConvertTransform_F64.convert(new Se2_F64(0,0,0.5),(Affine2D_F64)null);

//		process_affine( 3,3,affine0);
		process_affine( 4,3,affine0);
		process_affine( 4,4,affine0);
		process_affine( 4,5,affine0);
		process_affine( 5,5,affine0);

//		process_affine( 3,3,affine1);
		process_affine( 4,3,affine1);
		process_affine( 4,4,affine1);
		process_affine( 4,5,affine1);
		process_affine( 5,5,affine1);
	}

	private void process_affine( int rows , int cols , Affine2D_F64 affine ) {
		// create a grid in the expected format
		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();

		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createHexagonalGrid(rows, cols, 0.5, 1);

		for( EllipseRotated_F64 e : grid.data1 ) {
			AffinePointOps_F64.transform(affine, e.center, e.center);
		}

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		processAndCheck(rows, cols, alg, grid, nodes);
	}

	private void processAndCheck(int rows, int cols, EllipseClustersIntoHexagonalGrid alg, Tuple2<List<Node>, List<EllipseRotated_F64>> grid, List<List<Node>> nodes) {
		alg.process(grid.data1, nodes);

		FastQueue<Grid> found = alg.getGrids();

		assertEquals( 1 , found.size() );
		checkShape(rows, cols, found.get(0));
	}

	/**
	 * Multiple grids in view at the same time
	 */
	@Test
	public void process_multiple_grids() {
		// create two grids
		int rows = 4; int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid0 = createHexagonalGrid(rows, cols, 0.5, 1);
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid1 = createHexagonalGrid(rows, cols, 0.5, 1);

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

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();

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
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createHexagonalGrid(rows, cols, 0.5, 1);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		processAndCheck(rows, cols, alg, grid, nodes);

		// process it a second time with a different grid
		rows = 4; cols = 3;
		grid = createHexagonalGrid(rows, cols, 0.5, 1);
		alg = new EllipseClustersIntoHexagonalGrid();
		nodes.clear();
		nodes.add( grid.data0 );

		processAndCheck(rows, cols, alg, grid, nodes);
	}

	/**
	 * Give it input with a grid that's too small and see if it blows up
	 */
	@Test
	public void process_too_small() {
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = TestEllipseClustersIntoGrid.createRegularGrid(2, 1);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();

		List<List<Node>> nodes = new ArrayList<>();
		nodes.add( grid.data0 );

		alg.process(grid.data1, nodes);

		assertEquals(0, alg.getGrids().size);
	}

	/**
	 * There's a longer row to add
	 */
	@Test
	public void bottomTwoColumns_case0() {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int i = 0; i < 3; i++) {
			ellipses.add(new EllipseRotated_F64(i , 0, 1, 1, 0));
			ellipses.add(new EllipseRotated_F64(i + 0.5, hexY, 1, 1, 0));
			ellipses.add(new EllipseRotated_F64(i , hexY*2, 1, 1, 0));
		}

		Tuple2<List<Node>,List<EllipseRotated_F64>> input = connectEllipses(ellipses,hexY*2.1);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();
		alg.computeNodeInfo(input.data1,input.data0);
		alg.findContour(true);

		List<NodeInfo> column0 = new ArrayList<>();
		List<NodeInfo> column1 = new ArrayList<>();

		// first two on top row
		NodeInfo corner = alg.listInfo.get(2);
		NodeInfo next = alg.listInfo.get(5);

		corner.marked = next.marked = true;
		alg.bottomTwoColumns(corner, next,column0, column1);

		assertEquals(3,column0.size());
		assertEquals(3,column1.size());

		for (int i = 0; i < 3; i++) {
			assertTrue(column0.get(i).ellipse.center.distance(i,hexY*2) < 1e-4);
			assertTrue(column1.get(i).ellipse.center.distance(i+0.5,hexY) < 1e-4);
			assertTrue(column0.get(i).marked);
			assertTrue(column1.get(i).marked);
		}
	}

	/**
	 * The second column has only one element
	 */
	@Test
	public void bottomTwoColumns_case1() {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int i = 0; i < 2; i++) {
			ellipses.add(new EllipseRotated_F64(i, 0, 1, 1, 0));
			ellipses.add(new EllipseRotated_F64(i, hexY * 2, 1, 1, 0));
			if (i == 0) {
				ellipses.add(new EllipseRotated_F64(i + 0.5, hexY, 1, 1, 0));
				ellipses.add(new EllipseRotated_F64(i + 0.5, 1.732, 1, 1, 0));
			}
		}

		Tuple2<List<Node>,List<EllipseRotated_F64>> input = connectEllipses(ellipses,1.1);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();
		alg.computeNodeInfo(input.data1,input.data0);
		alg.findContour(true);

		List<NodeInfo> column0 = new ArrayList<>();
		List<NodeInfo> column1 = new ArrayList<>();

		NodeInfo corner = alg.listInfo.get(4);
		NodeInfo next = alg.listInfo.get(0);

		corner.marked = next.marked = true;
		alg.bottomTwoColumns(corner,next, column0, column1);

		assertEquals(2,column0.size());
		assertEquals(1,column1.size());

		assertTrue(column0.get(0).ellipse.center.distance(1,0) < 1e-4);
		assertTrue(column0.get(1).ellipse.center.distance(0,0) < 1e-4);
		assertTrue(column1.get(0).ellipse.center.distance(0.5,hexY) < 1e-4);
	}

	@Test
	public void selectClosest() {

		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		ellipses.add( new EllipseRotated_F64(0,0,1,1,0) );
		ellipses.add( new EllipseRotated_F64(0.5,0.866,1,1,0) );
		ellipses.add( new EllipseRotated_F64(0.5,1.2,1,1,0) );
		ellipses.add( new EllipseRotated_F64(1,0,1,1,0) );

		Tuple2<List<Node>,List<EllipseRotated_F64>> input = connectEllipses(ellipses,2);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();
		alg.computeNodeInfo(input.data1,input.data0);

		NodeInfo found = EllipseClustersIntoHexagonalGrid.selectClosestN(alg.listInfo.get(0),alg.listInfo.get(1));

		assertTrue(found != null);

		assertTrue( found.ellipse == ellipses.get(3));

		// move the node out of range it nothing should be accepted

		alg.listInfo.get(3).ellipse.set(0,1,1,1,0);
		input = connectEllipses(ellipses,1.1);
		alg.computeNodeInfo(input.data1,input.data0);
		found = EllipseClustersIntoHexagonalGrid.selectClosestN(alg.listInfo.get(0),alg.listInfo.get(1));
		assertTrue(found == null);

	}

	@Test
	public void selectClosestSide() {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		ellipses.add( new EllipseRotated_F64(0,0,1,1,0) );
		ellipses.add( new EllipseRotated_F64(1,0,1,1,0) );
		ellipses.add( new EllipseRotated_F64(0.5,0.866,1,1,0) );
		ellipses.add( new EllipseRotated_F64(0,0.866*2,1,1,0) );
		ellipses.add( new EllipseRotated_F64(1,0.866*2,1,1,0) );

		Tuple2<List<Node>,List<EllipseRotated_F64>> input = connectEllipses(ellipses,2);

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();
		alg.computeNodeInfo(input.data1,input.data0);

		NodeInfo found = EllipseClustersIntoHexagonalGrid.selectClosestSide(alg.listInfo.get(2),alg.listInfo.get(0));

		assertTrue(found != null);

		assertTrue( found.ellipse == ellipses.get(3));

		// move the node out of range it nothing should be accepted

		alg.listInfo.get(3).ellipse.set(0,1,1,1,0);
		input = connectEllipses(ellipses,1.1);
		alg.computeNodeInfo(input.data1,input.data0);
		found = EllipseClustersIntoHexagonalGrid.selectClosestN(alg.listInfo.get(0),alg.listInfo.get(1));
		assertTrue(found == null);
	}

	@Test
	public void saveResults() {
		// construct a dummy graph
		List<List<NodeInfo>> graph = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			graph.add( new ArrayList<NodeInfo>());
		}

		for (int i = 0; i < 2; i++) {
			graph.get(0).add( new NodeInfo());
			graph.get(2).add( new NodeInfo());
		}
		graph.get(1).add( new NodeInfo());
		for (List<NodeInfo> row : graph ) {
			for( NodeInfo n : row ) {
				n.ellipse = new EllipseRotated_F64();
			}
		}

		EllipseClustersIntoHexagonalGrid alg = new EllipseClustersIntoHexagonalGrid();
		alg.saveResults(graph);

		Grid g = alg.getGrids().get(0);
		assertEquals(3,g.columns);
		assertEquals(3,g.rows);
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				if( row%2 == col%2 ) {
					assertTrue(g.get(row,col)!=null);
				} else {
					assertTrue(g.get(row,col)==null);
				}
			}
		}
	}

	private Tuple2<List<Node>,List<EllipseRotated_F64>> createHexagonalGrid(int rows, int cols, double diameter, double distance) {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		double spaceX = distance/2.0;
		double spaceY = spaceX*hexY;

		double r = diameter/2.0;

		for (int row = 0; row < rows; row++) {
			double y = row*spaceY;
			for (int col = row%2; col < cols; col+=2) {
				double x = col*spaceX;

				ellipses.add( new EllipseRotated_F64(x,y,r,r,0) );
			}
		}

		return connectEllipses(ellipses, distance*2 );
	}
}
