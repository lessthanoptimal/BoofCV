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

import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

import static boofcv.misc.CircularIndex.addOffset;

/**
 * Takes as input a set of unordered cross connected clusters and converts them into ordered grids with known numbers
 * of rows and columns.  The output will be valid "chessboard" pattern. When rows and columns are discussed in the
 * code below it refers to both white and black squares in the chessboard. A row that starts with a white square
 * is referred to as white and one which starts with a black square as black.
 *
 * @author Peter Abeles
 */
public class CrossClustersIntoGrids {

	// verbose debug output
	private boolean verbose = false;


	FastQueue<SquareGrid> grid = new FastQueue<SquareGrid>(SquareGrid.class,true);

	boolean failed;

	public void process( List<List<SquareNode>> clusters ) {

	}

	protected void processCluster( List<SquareNode> cluster ) {
		// handle a special case
		if( cluster.size() == 1 ) {
			SquareNode n = cluster.get(0);
			if( n.getNumberOfConnections() == 0 ) {
				// TODO create grid
			}
			return;
		}

		failed = false;
		for (int i = 0; i < cluster.size(); i++) {
			cluster.get(i).graph = SquareNode.RESET_GRAPH;
		}

		SquareNode seed = findSeedNode(cluster);

		if( seed == null )
			return;

		// find the first row
		List<SquareNode> firstRow;
		if( seed.getNumberOfConnections() == 1 ) {
			firstRow = firstRow1(seed);
		} else if( seed.getNumberOfConnections() == 2 ) {
			firstRow = firstRow2(seed);
		} else {
			throw new RuntimeException("BUG");
		}

		// now add each row below it
		List<List<SquareNode>> grid = new ArrayList<List<SquareNode>>();
		grid.add(firstRow);
		while(!failed) {
			List<SquareNode> previous = grid.get( grid.size()-1);
			if( !addNextRow(previous.get(0),grid))
				break;
		}

		// check to see if it's valid
		// TODO do that

		// todo convert into a grid

	}

	private List<SquareNode> firstRow1( SquareNode seed ) {
		for (int i = 0; i < 4; i++) {
			if( isOpenEdge(seed,i) ) {
				List<SquareNode> list = new ArrayList<SquareNode>();
				addToRow(seed,i,1,true,list);
				return list;
			}
		}
		throw new RuntimeException("BUG");
	}

	private List<SquareNode> firstRow2(SquareNode seed ) {
		int indexLower = lowerEdgeIndex(seed);
		int indexUpper = addOffset(indexLower,1,4);

		List<SquareNode> listDown = new ArrayList<SquareNode>();
		List<SquareNode> list = new ArrayList<SquareNode>();

		addToRow(seed,indexLower,-1,true,listDown);

		for (int i = listDown.size()-1; i >= 0; i--) {
			list.add(listDown.get(i));
		}
		list.add(seed);
		addToRow(seed,indexUpper,1,true,list);

		return list;
	}

	private boolean addNextRow( SquareNode seed , List<List<SquareNode>> grid ) {
		List<SquareNode> row = new ArrayList<SquareNode>();

		int numConnections = seed.getNumberOfConnections();
		if( numConnections == 0 ) {
			return true;
		} else if( numConnections == 1 ) {
			for (int i = 0; i < 4; i++) {
				if( isOpenEdge(seed,i)) {
					addToRow(seed,i,-1,false,row);
					break;
				}
			}
		} else if( numConnections == 2 ) {
			int indexLower = lowerEdgeIndex(seed);
			int indexUpper = addOffset(indexLower,1,4);

			row.add( seed.edges[indexLower].destination(seed) );
			addToRow(seed,indexUpper,-1,false,row);
		} else {
			failed = true;
			return false;
		}
		grid.add(row);
		return true;
	}

	/**
	 * Returns the index which comes first.  Assuming that there are two options
	 */
	private int lowerEdgeIndex( SquareNode node ) {
		if( isOpenEdge(node,0) ) {
			if( isOpenEdge(node,1)) {
				return 0;
			} else {
				return 3;
			}
		}

		// first find the index of the two corners and sanity check them
		for (int i = 1; i < 4; i++) {
			if( isOpenEdge(node,i) ) {
				return i;
			}
		}

		throw new RuntimeException("BUG!");
	}

	/**
	 * Is the edge open and can be traversed to?  Can't be null and can't have
	 * the marker modified.
	 */
	private boolean isOpenEdge( SquareNode node , int index ) {
		if( node.edges[index] == null )
			return false;
		int marker = node.edges[index].destination(node).graph;
		return marker == SquareNode.RESET_GRAPH;
	}

	/**
	 * Given a node and the corner to the next node, add to the list every other node until
	 * it hits the end of the row.
	 * @param n Initial node
	 * @param corner Which corner points to the next node
	 * @param sign Determines the direction it will traverse.  -1 or 1
	 * @param skip true = start adding nodes at second, false = start first.
	 * @param row List that the nodes are placed into
	 */
	private void addToRow( SquareNode n , int corner , int sign , boolean skip ,
						   List<SquareNode> row ) {
		SquareEdge e;
		while( (e = n.edges[corner]) != null ) {
			if( e.a == n ) {
				n = e.b;
				corner = e.sideB;
			} else {
				n = e.a;
				corner = e.sideA;
			}
			if( !skip ) {
				row.add(n);
			}
			skip = !skip;
			sign *= -1;
			corner = addOffset(corner,sign,4);
		}
	}

	/**
	 * Finds a seed with 1 or 2 edges.
	 */
	private SquareNode findSeedNode(List<SquareNode> cluster) {
		SquareNode seed = null;

		for (int i = 0; i < cluster.size(); i++) {
			SquareNode n = cluster.get(i);
			int numConnections = n.getNumberOfConnections();
			if( numConnections == 0 || numConnections > 2 )
				continue;
			seed = n;
			break;
		}
		return seed;
	}


}
