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
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.fiducial.calib.circle.EllipseClustersIntoGrid.findClosestEdge;
import static org.junit.Assert.*;

/**
/**
 * @author Peter Abeles
 */
public class TestEllipseClustersIntoGrid {

	@Test
	public void checkDuplicates() {
		// create a grid in the expected format
		int rows = 4;
		int cols = 3;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows, cols);

		EllipseClustersIntoGrid alg = new HelperAlg();
		alg.computeNodeInfo(grid.data1,grid.data0);

		List<List<NodeInfo>> gridLists = convertIntoGridOfLists(0, rows, cols, alg);

		// everything should be unique here
		assertFalse( alg.checkDuplicates(gridLists));

		// test a negative now
		gridLists.get(1).set(2, gridLists.get(0).get(0));
		assertTrue( alg.checkDuplicates(gridLists));
	}

	public static List<List<NodeInfo>> convertIntoGridOfLists( int startIndex ,
															   int rows, int cols,
															   EllipseClustersIntoGrid alg) {
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

		EllipseClustersIntoGrid alg = new HelperAlg();
		alg.computeNodeInfo(grid.data1,grid.data0);

		alg.listInfo.get(0).marked = true;
		alg.listInfo.get(1).marked = true;
		NodeInfo found = EllipseClustersIntoGrid.selectSeedNext(
				alg.listInfo.get(0),alg.listInfo.get(1),alg.listInfo.get(cols),true);

		assertTrue( found == alg.listInfo.get(cols+1));
	}

	@Test
	public void findLine() {
		// create a grid from which a known solution can be easily extracted
		int rows = 5; int cols = 4;
		Tuple2<List<Node>,List<EllipseRotated_F64>> grid = createRegularGrid(rows,cols);

		EllipseClustersIntoGrid alg = new HelperAlg();
		alg.computeNodeInfo(grid.data1,grid.data0);

		alg.listInfo.get(0).marked = true;
		alg.listInfo.get(1).marked = true;
		List<NodeInfo> line;
		line = EllipseClustersIntoGrid.findLine(alg.listInfo.get(0),alg.listInfo.get(1),5*4, null,true);

		assertEquals(4, line.size());
		for (int i = 0; i < cols; i++) {
			assertEquals( line.get(i).ellipse.center.x , i , 1e-6 );
			assertEquals( line.get(i).ellipse.center.y , 0 , 1e-6 );
		}
	}

	@Test
	public void selectSeedCorner() {
		EllipseClustersIntoGrid alg = new HelperAlg();

		NodeInfo best = new NodeInfo();
		best.angleBetween = 3.0*Math.PI/2.0;

		for (int i = 0; i < 10; i++) {
			NodeInfo n = new NodeInfo();
			n.angleBetween = best.angleBetween*(i/10.0);
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

		EllipseClustersIntoGrid alg = new HelperAlg();

		// use internal algorithm to set up its data structure.  Correct of this function is
		// directly tested elsewhere
		alg.computeNodeInfo(grid.data1,grid.data0);

		// now find the contour
		assertTrue(alg.findContour(true));

		assertEquals(cols*2+(rows-2)*2, alg.contour.size);
	}

	/**
	 * Creates a regular grid of nodes and sets up the angle and neighbors correctly
	 */
	static Tuple2<List<Node>,List<EllipseRotated_F64>> createRegularGrid(int rows , int cols ) {
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int row = 0, i = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++) {
				ellipses.add( new EllipseRotated_F64(col,row,0.1,0.1,0) );
			}
		}

		return connectEllipses(ellipses, 1.8 );
	}

	static Tuple2<List<Node>, List<EllipseRotated_F64>> connectEllipses(List<EllipseRotated_F64> ellipses, double distance ) {
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

		EllipseClustersIntoGrid alg = new HelperAlg();

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
		EllipseClustersIntoGrid alg = new HelperAlg();

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
	public void grid_getIndexOfHexEllipse() {
		grid_getIndexOfHexEllipse(1,1);
		grid_getIndexOfHexEllipse(1,4);
		grid_getIndexOfHexEllipse(4,1);
		grid_getIndexOfHexEllipse(4,4);
		grid_getIndexOfHexEllipse(4,5);
		grid_getIndexOfHexEllipse(5,4);
		grid_getIndexOfHexEllipse(5,5);
		grid_getIndexOfHexEllipse(5,6);
	}

	private void grid_getIndexOfHexEllipse(int numRows , int numCols ) {
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
//		assertEquals(totalEllipses, g.getNumberOfEllipses());

		for (int i = 0; i < totalEllipses; i++) {
			int row = index[i]/g.columns;
			int col = index[i]%g.columns;
			assertEquals(row+" "+col, i , g.getIndexOfHexEllipse(row,col));
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

			assertEquals(expected, info.angleBetween, GrlConstants.TEST_F64);
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
			EllipseClustersIntoGrid.Edge e0 = info.edges.get(j);
			EllipseClustersIntoGrid.Edge e1 = info.edges.get(i);

			assertTrue( e0.angle <= e1.angle);
		}

	}

	static NodeInfo setNodeInfo(NodeInfo node , double x , double y ) {
		if( node == null ) node = new NodeInfo();
		node.ellipse = new EllipseRotated_F64(x,y,1,1,0);
		return node;
	}

	static Node createNode(int which , int ...connections) {
		Node n = new Node();
		n.which = which;
		n.connections.addAll(connections,0,connections.length);
		return n;
	}

	private static class HelperAlg extends EllipseClustersIntoGrid {

		@Override
		public void process(List<EllipseRotated_F64> ellipses, List<List<Node>> clusters) {

		}
	}
}