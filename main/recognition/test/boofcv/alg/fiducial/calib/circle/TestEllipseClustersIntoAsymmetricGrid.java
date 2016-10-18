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

import boofcv.alg.fiducial.calib.circle.EllipseClustersIntoAsymmetricGrid.NodeInfo;
import boofcv.alg.fiducial.calib.circle.EllipsesIntoClusters.Node;
import georegression.metric.UtilAngle;
import georegression.misc.GrlConstants;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
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
		fail("implement");
	}

	/**
	 * Call process multiple times and see if it blows up
	 */
	@Test
	public void process_multiple_calls() {
		fail("implement");
	}

	/**
	 * Give it input with a grid that's too small and see if it blows up
	 */
	@Test
	public void process_too_small() {
		fail("implement");
	}

	@Test
	public void combineGrids() {
		fail("implement");
	}

	@Test
	public void checkGridSize() {
		fail("implement");
	}

	@Test
	public void checkDuplicates() {
		fail("implement");
	}

	@Test
	public void findInnerGrid() {
		fail("implement");
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
		best.angleBetween = Math.PI/2.0;

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

	/**
	 * Creates a regular grid of nodes and sets up the angle and neighbors correctly
	 * @param rows
	 * @param cols
	 * @return
	 */
	private Tuple2<List<Node>,List<EllipseRotated_F64>> createRegularGrid( int rows , int cols ) {
		List<Node> cluster = new ArrayList<>();
		List<EllipseRotated_F64> ellipses = new ArrayList<>();

		for (int row = 0, i = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++) {
				ellipses.add( new EllipseRotated_F64(col,row,0.1,0.1,0) );
				cluster.add( new Node());
				cluster.get(i).which = i;
			}
		}

		for (int row = 0, i=0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++) {
				Node a = cluster.get(i);

				if( col > 0 )
					a.connections.add(i-1);
				if( row > 0 )
					a.connections.add(i-cols);
				if( col < cols-1 )
					a.connections.add(i+1);
				if( row < rows-1 )
					a.connections.add(i+cols);
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
