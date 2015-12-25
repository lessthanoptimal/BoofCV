/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCrossClustersIntoGrids {
	@Test
	public void processCluster_positive() {
		CrossClustersIntoGrids alg = new CrossClustersIntoGrids();

		for (int rows = 2; rows <= 4; rows++) {
			for (int cols = 2; cols <= 4; cols++) {
				System.out.println(rows+" "+cols);

				// todo add skip first column
				int[] levels = createLevels(rows, cols);
				List<SquareNode> nodes = createCluster(false,levels);

				alg.grids.reset();
				alg.processCluster(nodes);

				assertEquals(1,alg.grids.size());
				SquareGrid grid = alg.grids.get(0);
				assertEquals(rows,grid.rows);
				assertEquals(cols,grid.columns);
				for (int i = 0; i < rows; i++) {
					boolean expected = i%2 == 0;
					for (int j = 0; j < cols; j++) {
						assertEquals(expected , (grid.get(i,j) != null));
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
	public void processCluster_negative() {
		fail("implement");
	}

	@Test
	public void firstRow1() {
		fail("implement");
	}

	@Test
	public void firstRow2() {
		fail("implement");
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
		CrossClustersIntoGrids alg = new CrossClustersIntoGrids();
		List<List<SquareNode>> ordered = new ArrayList<List<SquareNode>>();
		for (int i = 0; i < top; i++) {
			assertTrue(alg.addNextRow(cluster.get(i),ordered));

			List<SquareNode> found = ordered.remove( ordered.size()-1);
			assertEquals(bottom,found.size());

			for (int j = 0; j < bottom; j++) {
				assertTrue(j+"",cluster.get(top+j)==found.get(j));
			}
		}

		fail("test honor explored");
		fail("make sure it doesn't add a row when at the bottom");
	}

	@Test
	public void lowerEdgeIndex() {
		for (int first = 0; first < 4; first++) {
			int second = (first+1)%4;

			SquareNode node = new SquareNode();
			connect(node,first,new SquareNode(),0);
			node.edges[first].b.graph = SquareNode.RESET_GRAPH;

			connect(node,second,new SquareNode(),0);
			node.edges[second].b.graph = SquareNode.RESET_GRAPH;

			assertEquals(first,CrossClustersIntoGrids.lowerEdgeIndex(node));
		}
	}

	@Test
	public void isOpenEdge() {
		SquareNode node = new SquareNode();

		for (int i = 0; i < 4; i++) {
			assertFalse(CrossClustersIntoGrids.isOpenEdge(node,i));
		}
		for (int i = 0; i < 4; i++) {
			connect(node,i,new SquareNode(),i);
		}
		for (int i = 0; i < 4; i++) {
			assertFalse(CrossClustersIntoGrids.isOpenEdge(node,i));
		}
		for (int i = 0; i < 4; i++) {
			node.edges[i].b.graph = SquareNode.RESET_GRAPH;
			assertTrue(CrossClustersIntoGrids.isOpenEdge(node,i));
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
		List<SquareNode> row = new ArrayList<SquareNode>();

		CrossClustersIntoGrids alg = new CrossClustersIntoGrids();
		alg.addToRow(cluster.get(seed),seedCorner,sign,skip,row);

		assertEquals(expected.length,row.size());
		for (int i = 0; i < row.size(); i++) {
			SquareNode e = cluster.get(expected[i]);
			SquareNode found = row.get(i);
			assertTrue(e==found);
		}

		fail("add test to see if its explored already");
	}

	@Test
	public void numberOfOpenEdges() {
		fail("implement");
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

		List<SquareNode> out = new ArrayList<SquareNode>();
		for (int i = 0; i < total; i++) {
			out.add( new SquareNode());
			out.get(i).graph = SquareNode.RESET_GRAPH;
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
		}

		return out;
	}

	private void connect( SquareNode a , int cornerA , SquareNode b , int cornerB ) {
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
		List<SquareNode> cluster = new ArrayList<SquareNode>();
		cluster.add( new SquareNode());
		cluster.add( new SquareNode());
		cluster.add( new SquareNode());

		cluster.get(1).edges[2] = new SquareEdge();
		cluster.get(2).edges[0] = new SquareEdge();
		cluster.get(2).edges[1] = new SquareEdge();

		assertTrue(cluster.get(1)==CrossClustersIntoGrids.findSeedNode(cluster));

		cluster.get(1).edges[3] = new SquareEdge();
		assertTrue(cluster.get(1)==CrossClustersIntoGrids.findSeedNode(cluster));

		cluster.get(1).edges[0] = new SquareEdge();
		assertTrue(cluster.get(2)==CrossClustersIntoGrids.findSeedNode(cluster));
	}
}
