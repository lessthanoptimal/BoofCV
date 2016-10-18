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

import boofcv.misc.CircularIndex;
import georegression.struct.shapes.Polygon2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestSquareCrossClustersIntoGrids {
	@Test
	public void processCluster_positive() {
		SquareCrossClustersIntoGrids alg = new SquareCrossClustersIntoGrids();

		for (int rows = 2; rows <= 5; rows++) {
			for (int cols = 2; cols <= 5; cols++) {
//				System.out.println(rows+" "+cols);

				int[] levels = createLevels(rows, cols);
				List<SquareNode> nodes = createCluster(false,levels);

				alg.grids.reset();
				alg.processCluster(nodes);

				assertEquals(1,alg.grids.size());
				SquareGrid grid = alg.grids.get(0);
				assertTrue((rows==grid.rows&&cols==grid.columns)||(cols==grid.rows&&rows==grid.columns));

				for (int i = 0; i < grid.rows; i++) {
					boolean expected = grid.get(i,0) != null;
					if( i > 0 ) {
						assertEquals(!expected,grid.get(i-1,0) != null);
					}
					for (int j = 0; j < grid.columns; j++) {
						assertEquals(expected , (grid.get(i,j) != null));
						expected = !expected;
					}
				}
			}
		}
	}

	private int[] createLevels(int rows, int cols) {
		int levels[] = new int[rows];
		for (int i = 0; i < rows; i++) {
			levels[i] = cols/2 + (i%2==0 ? cols%2 : 0);
		}
		return levels;
	}

	@Test
	public void firstRow1and2() {
		// X X X X X
		//  X X X X
		checkFirstRow1and2(false,5,4);

		//  X X X X
		// X X X X X
		checkFirstRow1and2(true,4,5);

		// X
		//  x
		checkFirstRow1and2(false,1,1);

		//  X
		// x
		checkFirstRow1and2(true,1,1);
	}

	private void checkFirstRow1and2( boolean skip , int top , int bottom  )
	{
		List<SquareNode> cluster = createCluster(skip,top,bottom);
		SquareCrossClustersIntoGrids alg = new SquareCrossClustersIntoGrids();

		// see if any of the nodes in the first row can be the seed
		for (int i = 0; i < top; i++) {
			List<SquareNode> list;
			SquareNode seed = cluster.get(i);
			if( seed.getNumberOfConnections() == 1 ) {
				list = alg.firstRow1(seed);
			} else {
				list = alg.firstRow2(seed);
			}

			assertEquals(top,list.size());

			// Check to see if the edge index ordering is as expected
			for (int j = 0; j < top-1; j++) {
				SquareNode a = list.get(j);
				SquareNode b = list.get(j+1);

				checkConnection(a,b);
			}
			// check to see if the nodes are marked correctly and then reset them
			for (int j = 0; j < top; j++) {
				assertTrue(list.get(j).graph != SquareNode.RESET_GRAPH);
				list.get(j).graph = SquareNode.RESET_GRAPH;
			}
			for (int j = 0; j < bottom; j++) {
				assertTrue(cluster.get(top+j).graph == SquareNode.RESET_GRAPH);
			}
		}
	}

	/**
	 * Seeds if 'a' and 'b' are connected to each other through a common node. The out going
	 * indexes from the common node should be 'i' to 'a' and 'i+1' to 'b'
	 */
	private void checkConnection( SquareNode a , SquareNode b ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge edgeA = a.edges[i];
			if( edgeA == null )
				continue;

			SquareNode common = edgeA.destination(a);
			int commonToA = edgeA.destinationSide(a);
			int commonToB = CircularIndex.addOffset(commonToA,1,4);

			SquareEdge edgeB = common.edges[commonToB];
			if( edgeB != null && edgeB.destination(common) == b ) {
				return;
			}
		}
		fail("Failed");
	}

	@Test
	public void addNextRow() {

		// X X X X X
		//  X X X X
		checkAddNextRow(false,5,4);

		//  X X X X
		// X X X X X
		checkAddNextRow(true,4,5);

		// X
		//  x
		checkAddNextRow(false,1,1);



		//  X
		// x
		checkAddNextRow(true,1,1);
	}

	private void checkAddNextRow( boolean skip , int top , int bottom  )
	{
		List<SquareNode> cluster = createCluster(skip,top,bottom);
		SquareCrossClustersIntoGrids alg = new SquareCrossClustersIntoGrids();
		List<List<SquareNode>> ordered = new ArrayList<>();

		// mark the first row as traversed, which it will be
		for (int i = 0; i < top; i++) {
			cluster.get(i).graph = 0;
		}

		// see if any of the nodes in the first row can be the seed
		for (int i = 0; i < top; i++) {
			assertTrue(alg.addNextRow(cluster.get(i),ordered));

			List<SquareNode> found = ordered.remove( ordered.size()-1);
			assertEquals(bottom,found.size());

			for (int j = 0; j < bottom; j++) {
				assertTrue(j+"",cluster.get(top+j)==found.get(j));

				// try adding a row below the bottom, which should fail, from any of the bottom nodes
				assertFalse(alg.addNextRow(found.get(j),ordered));
				assertEquals(0,ordered.size());
			}

			// reset the markings so that in the next loop it can add them
			for (int j = 0; j < bottom; j++) {
				found.get(j).graph = SquareNode.RESET_GRAPH;
			}
		}
	}

	@Test
	public void lowerEdgeIndex() {
		for( int numCorners = 3; numCorners <= 5; numCorners++ ) {
			for (int first = 0; first < numCorners; first++) {
				int second = (first + 1) % numCorners;

				SquareNode node = new SquareNode();
				node.corners = new Polygon2D_F64(numCorners);
				node.updateArrayLength();
				connect(node, first, new SquareNode(), 0);
				node.edges[first].b.graph = SquareNode.RESET_GRAPH;

				connect(node, second, new SquareNode(), 0);
				node.edges[second].b.graph = SquareNode.RESET_GRAPH;

				assertEquals(first, SquareCrossClustersIntoGrids.lowerEdgeIndex(node));
			}
		}
	}

	@Test
	public void isOpenEdge() {
		SquareNode node = new SquareNode();

		for (int i = 0; i < 4; i++) {
			assertFalse(SquareCrossClustersIntoGrids.isOpenEdge(node,i));
		}
		for (int i = 0; i < 4; i++) {
			connect(node,i,new SquareNode(),i);
		}
		for (int i = 0; i < 4; i++) {
			assertFalse(SquareCrossClustersIntoGrids.isOpenEdge(node,i));
		}
		for (int i = 0; i < 4; i++) {
			node.edges[i].b.graph = SquareNode.RESET_GRAPH;
			assertTrue(SquareCrossClustersIntoGrids.isOpenEdge(node,i));
			node.edges[i].b.graph = 0;
		}
	}

	@Test
	public void addToRow() {
		List<SquareNode> cluster;

		// X
		//  X
		cluster = createCluster(false,1,1);
		checkAdd(cluster,0,2,-1,true,new int[]{});
		checkAdd(cluster,0,2,-1,false,new int[]{1});

		// X X
		//  X
		cluster = createCluster(false,2,1);
		checkAdd(cluster,0,2,-1,true,new int[]{1});
		checkAdd(cluster,0,2,-1,false,new int[]{2});

		// X X
		//  X X
		cluster = createCluster(false,2,2);
		checkAdd(cluster,0,2,-1,true,new int[]{1});
		checkAdd(cluster,0,2,-1,false,new int[]{2,3});

		//  X
		// X
		cluster = createCluster(true,1,1);
		checkAdd(cluster,0,3,1,true,new int[]{});
		checkAdd(cluster,0,3,1,false,new int[]{1});

		//  X
		// X X
		cluster = createCluster(true,1,2);
		checkAdd(cluster,0,2,-1,true,new int[]{});
		checkAdd(cluster,0,2,-1,false,new int[]{2});
		checkAdd(cluster,0,3,1,true,new int[]{});
		checkAdd(cluster,0,3,1,false,new int[]{1});

		//  X X
		// X X
		cluster = createCluster(true,2,2);
		checkAdd(cluster,0,2,-1,true,new int[]{1});
		checkAdd(cluster,0,2,-1,false,new int[]{3});
		checkAdd(cluster,0,3,1,true,new int[]{});
		checkAdd(cluster,0,3,1,false,new int[]{2});
	}

	private void checkAdd( List<SquareNode> cluster,
						   int seed , int seedCorner , int sign , boolean skip ,
						   int expected[] )
	{
		List<SquareNode> row = new ArrayList<>();

		SquareCrossClustersIntoGrids alg = new SquareCrossClustersIntoGrids();
		alg.addToRow(cluster.get(seed),seedCorner,sign,skip,row);

		assertEquals(expected.length,row.size());
		for (int i = 0; i < row.size(); i++) {
			SquareNode e = cluster.get(expected[i]);
			SquareNode found = row.get(i);
			assertTrue(SquareNode.RESET_GRAPH != found.graph);
			assertTrue(e==found);
		}
	}

	@Test
	public void numberOfOpenEdges() {
		SquareNode a = new SquareNode();
		a.corners = new Polygon2D_F64(4);

		assertEquals(0, SquareCrossClustersIntoGrids.numberOfOpenEdges(a));
		connect(a,1,new SquareNode(),0);
		assertEquals(0, SquareCrossClustersIntoGrids.numberOfOpenEdges(a));
		a.edges[1].b.graph = SquareNode.RESET_GRAPH;
		assertEquals(1, SquareCrossClustersIntoGrids.numberOfOpenEdges(a));
	}

	/**
	 * Creates a new two row graph.  Skip indicates if the first row skips the first column or not.  The
	 * other two parameters specify how many nodes in each row
	 */
	private List<SquareNode> createCluster(boolean skip, int ...levels ) {

		int total = 0;
		for (int i = 0; i < levels.length; i++) {
			total += levels[i];
		}

		List<SquareNode> out = new ArrayList<>();
		for (int i = 0; i < total; i++) {
			out.add( new SquareNode());
			out.get(i).graph = SquareNode.RESET_GRAPH;
			out.get(i).corners = new Polygon2D_F64(4);
		}

		int previous = 0;
		for (int i = 0; i < levels.length-1; i++) {
			int current = previous + levels[i];
			int next = current + levels[i + 1];
			for (int a = 0; a < levels[i]; a++) {
				SquareNode n = out.get(previous + a);

				int right = skip ? current + a + 1 : current + a;
				int left = right - 1;

				if (right < next)
					connect(n, 2, out.get(right), 0);
				if (left >= current) {
					connect(n, 3, out.get(left), 1);
				}

			}
			previous = current;
			skip = !skip;
		}

		return out;
	}

	public static void connect( SquareNode a , int cornerA , SquareNode b , int cornerB ) {
		SquareEdge edge = new SquareEdge();
		edge.a = a;
		edge.sideA = cornerA;
		edge.b = b;
		edge.sideB = cornerB;

		a.edges[cornerA] = edge;
		b.edges[cornerB] = edge;
	}

	@Test
	public void findSeedNode() {
		List<SquareNode> cluster = new ArrayList<>();
		cluster.add( new SquareNode());
		cluster.add( new SquareNode());
		cluster.add( new SquareNode());
		for( SquareNode n : cluster ) {
			n.corners = new Polygon2D_F64(4);
		}

		cluster.get(1).edges[2] = new SquareEdge();
		cluster.get(2).edges[0] = new SquareEdge();
		cluster.get(2).edges[1] = new SquareEdge();

		assertTrue(cluster.get(1)== SquareCrossClustersIntoGrids.findSeedNode(cluster));

		cluster.get(1).edges[3] = new SquareEdge();
		assertTrue(cluster.get(1)== SquareCrossClustersIntoGrids.findSeedNode(cluster));

		cluster.get(1).edges[0] = new SquareEdge();
		assertTrue(cluster.get(2)== SquareCrossClustersIntoGrids.findSeedNode(cluster));
	}
}
