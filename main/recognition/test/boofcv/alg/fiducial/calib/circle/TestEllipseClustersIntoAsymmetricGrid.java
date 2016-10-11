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
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
	public void selectSeedNext() {
		fail("implement");
	}

	@Test
	public void findLine() {
		fail("implement");
	}

	@Test
	public void selectSeedCorner() {
		fail("implement");
	}

	@Test
	public void findContour() {
		fail("implement");
	}

	@Test
	public void computeClusterInfo() {
		fail("implement");
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

	private static void setNodeInfo( NodeInfo node , double x , double y ) {
		node.ellipse = new EllipseRotated_F64(x,y,1,1,0);
	}

	private static Node createNode( int which , int ...connections) {
		Node n = new Node();
		n.which = which;
		n.connections.addAll(connections,0,connections.length);
		return n;
	}
}
