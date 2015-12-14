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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestCrossClustersIntoGrids {
	@Test
	public void stuff() {
		fail("implement");
	}

	@Test
	public void addNextRow() {
		fail("implement");
	}

	@Test
	public void lowerEdgeIndex() {
		fail("implement");
	}

	@Test
	public void isOpenEdge() {
		fail("implement");
	}

	@Test
	public void addToRow() {
		List<SquareNode> cluster;

		// X
		//  X
		cluster = createGraph(false,1,1);
		checkAdd(cluster,0,2,-1,true,new int[]{});
		checkAdd(cluster,0,2,-1,false,new int[]{1});

		// X X
		//  X
		cluster = createGraph(false,2,1);
		checkAdd(cluster,0,2,-1,true,new int[]{1});
		checkAdd(cluster,0,2,-1,false,new int[]{2});

		// X X
		//  X X
		cluster = createGraph(false,2,2);
		checkAdd(cluster,0,2,-1,true,new int[]{1});
		checkAdd(cluster,0,2,-1,false,new int[]{2,3});

		//  X
		// X
		cluster = createGraph(true,1,1);
		checkAdd(cluster,0,3,1,true,new int[]{});
		checkAdd(cluster,0,3,1,false,new int[]{1});

		//  X
		// X X
		cluster = createGraph(true,1,2);
		checkAdd(cluster,0,2,-1,true,new int[]{});
		checkAdd(cluster,0,2,-1,false,new int[]{2});
		checkAdd(cluster,0,3,1,true,new int[]{});
		checkAdd(cluster,0,3,1,false,new int[]{1});

		//  X X
		// X X
		cluster = createGraph(true,2,2);
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
	}

	/**
	 * Creates a new two row graph.  Skip indicates if the first row skips the first column or not.  The
	 * other two parameters specify how many nodes in each row
	 */
	private List<SquareNode> createGraph( boolean skip, int top , int bottom ) {
		List<SquareNode> out = new ArrayList<SquareNode>();
		for (int i = 0; i < top + bottom; i++) {
			out.add( new SquareNode());
		}

		for (int i = 0; i < top; i++) {

			SquareNode n = out.get(i);

			int right = skip ? top + i + 1: top + i;
			int left = right - 1;

			if( right < top+bottom)
				connect(n,2,out.get(right),0);
			if( left >= top ) {
				connect(n,3,out.get(left),1);
			}
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
