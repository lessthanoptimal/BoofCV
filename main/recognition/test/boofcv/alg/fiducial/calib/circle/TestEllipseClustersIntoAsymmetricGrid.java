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

package boofcv.alg.fiducial.calib.circle;

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.Grid;
import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.NodeInfo;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.findClosestEdge;
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
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(2, 1);

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
	public void checkDuplicates() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows, cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);

		List<List<NodeInfo>> gridLists = convertIntoGridOfLists(0, rows, cols, alg);

		// everything should be unique here
		assertFalse( alg.checkDuplicates(gridLists));

		// test a negative now
		gridLists.get(1).set(2, gridLists.get(0).get(0));
		assertTrue( alg.checkDuplicates(gridLists));
	}

	public List<List<NodeInfo>> convertIntoGridOfLists( int startIndex ,
														int rows, int cols,
														EllipseClustersIntoAsymmetricGrid alg) {
		List<List<NodeInfo>> gridLists = new ArrayList<>();
		for (int row = 0; row < rows; row++) {
			List<NodeInfo> l = new ArrayList<>();
			gridLists.add(l);
			for (int col = 0; col < cols; col++) {
				l.add( alg.listInfo.get(startIndex + row*cols + col));
			}
		}
		return gridLists;
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

	@Test
	public void findClosestEdge_() {
		NodeInfo n = setNodeInfo(null,-2,0);
		n.edges.grow().target = setNodeInfo(null,2,2);
		n.edges.grow().target = setNodeInfo(null,2,0);
		n.edges.grow().target = setNodeInfo(null,-2,-2);

		assertTrue( n.edges.get(0).target == findClosestEdge(n,new Point2D_F64(2,1.5)));
		assertTrue( n.edges.get(1).target == findClosestEdge(n,new Point2D_F64(1.9,0)));
		assertTrue( n.edges.get(2).target == findClosestEdge(n,new Point2D_F64(-2,-1)));
	}

	@Test
	public void selectSeedNext() {
		// create a grid from which a known solution can be easily extracted
		int rows = 5; int cols = 4;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows,cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);


		NodeInfo found = EllipseClustersIntoAsymmetricGrid.selectSeedNext(
				alg.listInfo.get(0),alg.listInfo.get(1),alg.listInfo.get(cols));

		assertTrue( found == alg.listInfo.get(cols+1));
	}

	@Test
	public void findLine() {
		// create a grid from which a known solution can be easily extracted
		int rows = 5; int cols = 4;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows,cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();
		alg.computeNodeInfo(grid.data1,grid.data0);

		List<NodeInfo> line;
		line = EllipseClustersIntoAsymmetricGrid.findLine(alg.listInfo.get(0),alg.listInfo.get(1),5*4);

		assertEquals(4, line.size());
		for (int i = 0; i < cols; i++) {
			assertEquals( line.get(i).ellipse.center.x , i , 1e-6 );
			assertEquals( line.get(i).ellipse.center.y , 0 , 1e-6 );
		}
	}

	@Test
	public void selectSeedCorner() {
		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		NodeInfo best = new NodeInfo();
		best.angleBetween = 3.0*Math.PI/2.0;

		for (int i = 0; i < 10; i++) {
			NodeInfo n = new NodeInfo();
			n.angleBetween = 2.0*Math.PI*i/10.0 + 0.01;
			alg.contour.add( n );

			if( i == 4 )
				alg.contour.add( best );
		}

		NodeInfo found = alg.selectSeedCorner();

		assertTrue(found == best);
	}

	@Test
	public void findContour() {
		// create a grid from which a known solution can be easily extracted
		int rows = 5;
		int cols = 4;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows,cols);

		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		// use internal algorithm to set up its data structure.  Correct of this function is
		// directly tested elsewhere
		alg.computeNodeInfo(grid.data1,grid.data0);

		// now find the contour
		assertTrue(alg.findContour());

		assertEquals(cols*2+(rows-2)*2, alg.contour.size);
	}

	private Tuple2<List<Node>,List<EllipseRotated_F64>> createAsymGrid( int rows , int cols ) {
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

	/**
	 * Creates a regular grid of nodes and sets up the angle and neighbors correctly
	 */
	private Tuple2<List<Node>,List<EllipseRotated_F64>> createRegularGrid( int rows , int cols ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int row = 0, i = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++) {
				ellipses.add( new EllipseRotated_F64(col,row,0.1,0.1,0) );
			}
		}

		return connectEllipses(ellipses, 1.8 );
	}

	private Tuple2<List<Node>, List<EllipseRotated_F64>> connectEllipses(List<EllipseRotated_F64> ellipses, double distance ) {
		List<Node> cluster = new ArrayList<>();

		for (int i = 0; i < ellipses.size(); i++) {
			cluster.add( new Node() );
			cluster.get(i).which = i;
		}

		for (int i = 0; i < ellipses.size(); i++) {
			Node n0 = cluster.get(i);
			EllipseRotated_F64 e0 = ellipses.get(i);

			for (int j = i+1; j < ellipses.size(); j++) {
				Node n1 = cluster.get(j);
				EllipseRotated_F64 e1 = ellipses.get(j);

				if( e1.center.distance(e0.center) <= distance ) {
					n0.connections.add(j);
					n1.connections.add(i);
				}
			}
		}

		return new Tuple2<>(cluster,ellipses);
	}

	/**
	 * This test just checks to see if a node info is created for each node passed in and that
	 * the ellipse is assinged to it.  The inner functions are tested elsewhere
	 */
	@Test
	public void computeNodeInfo() {
		List<Node> nodes = new ArrayList<>();
		nodes.add( createNode(0, 1,2,3));
		nodes.add( createNode(1, 0,2,4));
		nodes.add( createNode(2, 0,1));
		nodes.add( createNode(3, 0));
		nodes.add( createNode(4));

		List<EllipseRotated_F64> ellipses = new ArrayList<>();
		for (int i = 0; i < nodes.size(); i++) {
			ellipses.add( new EllipseRotated_F64());
		}
		
		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		alg.computeNodeInfo(ellipses,nodes);

		assertEquals( nodes.size(), alg.listInfo.size);
		for (int i = 0; i < nodes.size(); i++) {
			assertTrue( ellipses.get(i) == alg.listInfo.get(i).ellipse);
		}
	}

	/**
	 * Combines these two functions into a single test.  This was done to ensure that their behavior is consistent
	 * with each other.
	 */
	@Test
	public void addEdgesToInfo_AND_findLargestAnglesForAllNodes() {
		EllipseClustersIntoAsymmetricGrid alg = new EllipseClustersIntoAsymmetricGrid();

		setNodeInfo(alg.listInfo.grow(), 0 , 0);
		setNodeInfo(alg.listInfo.grow(),-1 , 0);
		setNodeInfo(alg.listInfo.grow(), 3 , 1);
		setNodeInfo(alg.listInfo.grow(), 0 , 1);
		setNodeInfo(alg.listInfo.grow(), 1 , 2);

		List<Node> cluster = new ArrayList<>();
		cluster.add( createNode(0, 1,2,3));
		cluster.add( createNode(1, 0,2,4));
		cluster.add( createNode(2, 0,1));
		cluster.add( createNode(3, 0));
		cluster.add( createNode(4));

		alg.addEdgesToInfo(cluster);

		checkEdgeInfo(alg.listInfo.get(0), 3);
		checkEdgeInfo(alg.listInfo.get(1), 3);
		checkEdgeInfo(alg.listInfo.get(2), 2);
		checkEdgeInfo(alg.listInfo.get(3), 1);
		checkEdgeInfo(alg.listInfo.get(4), 0);

		// check results against hand selected solutions
		alg.findLargestAnglesForAllNodes();

		checkLargestAngle(alg.listInfo.get(0),alg.listInfo.get(1),alg.listInfo.get(2));
		checkLargestAngle(alg.listInfo.get(1),alg.listInfo.get(4),alg.listInfo.get(0));
		checkLargestAngle(alg.listInfo.get(2),alg.listInfo.get(0),alg.listInfo.get(1));
		checkLargestAngle(alg.listInfo.get(3),null,null);
		checkLargestAngle(alg.listInfo.get(4),null,null);
	}

	@Test
	public void grid_getIndexOfEllipse() {
		grid_getIndexOfEllipse(1,1);
		grid_getIndexOfEllipse(1,4);
		grid_getIndexOfEllipse(4,1);
		grid_getIndexOfEllipse(4,4);
		grid_getIndexOfEllipse(4,5);
		grid_getIndexOfEllipse(5,4);
		grid_getIndexOfEllipse(5,5);
		grid_getIndexOfEllipse(5,6);
	}

	private void grid_getIndexOfEllipse( int numRows , int numCols ) {
		Grid g = new Grid();
		g.rows = numRows;
		g.columns = numCols;

		int index[] = new int[g.rows*g.columns];

		int totalEllipses = 0;
		for (int row = 0; row < g.rows; row++) {
			for (int col = 0; col < g.columns; col++) {
				if( row%2==0 && col%2==1 ) {
					g.ellipses.add(null);
				} else if( row%2==1 && col%2==0 ) {
					g.ellipses.add(null);
				} else {
					index[totalEllipses++] = row*g.columns + col;
					g.ellipses.add( new EllipseRotated_F64());
				}
			}
		}
		assertEquals(totalEllipses, g.getNumberOfEllipses());

		for (int i = 0; i < totalEllipses; i++) {
			int row = index[i]/g.columns;
			int col = index[i]%g.columns;
			assertEquals(row+" "+col, i , g.getIndexOfEllipse(row,col));
		}
	}

	/**
	 * Checks to see if the two nodes farthest apart is correctly found and the angle computed
	 */
	private static void checkLargestAngle(NodeInfo info , NodeInfo left , NodeInfo right ) {
		assertTrue( info.left == left);
		assertTrue( info.right == right);

		if( left != null ) {
			double angle0 = Math.atan2(left.ellipse.center.y - info.ellipse.center.y,
					left.ellipse.center.x - info.ellipse.center.x);
			double angle1 = Math.atan2(right.ellipse.center.y - info.ellipse.center.y,
					right.ellipse.center.x - info.ellipse.center.x);

			double expected = UtilAngle.distanceCCW(angle0, angle1);

			assertEquals(expected, info.angleBetween, GrlConstants.DOUBLE_TEST_TOL);
		}
	}

	/**
	 * Makes sure expected number of edges is found and that the edges are correctly sorted by angle
	 */
	private static void checkEdgeInfo(NodeInfo info , int numEdges ) {
		assertEquals(info.edges.size, numEdges);

		if( numEdges == 0 )
			return;

		int numNotZero = 0;
		for (int i = 0; i < numEdges; i++) {
			if( info.edges.get(i).angle != 0 )
				numNotZero++;
		}
		assertTrue( numNotZero >= 1);

		// should be ordered in increasing CCW direction
		for (int i = 1, j = 0; i < numEdges; j=i,i++) {
			EllipseClustersIntoAsymmetricGrid.Edge e0 = info.edges.get(j);
			EllipseClustersIntoAsymmetricGrid.Edge e1 = info.edges.get(i);

			assertTrue( e0.angle <= e1.angle);
		}

	}

	private static NodeInfo setNodeInfo( NodeInfo node , double x , double y ) {
		if( node == null ) node = new NodeInfo();
		node.ellipse = new EllipseRotated_F64(x,y,1,1,0);
		return node;
	}

	private static Node createNode( int which , int ...connections) {
		Node n = new Node();
		n.which = which;
		n.connections.addAll(connections,0,connections.length);
		return n;
	}
}
