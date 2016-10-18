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
public class SquareCrossClustersIntoGrids {

	// verbose debug output
	private boolean verbose = false;

	FastQueue<SquareGrid> grids = new FastQueue<>(SquareGrid.class, true);

	// indicates if a fatal error was found in the grid
	protected boolean invalid;

	/**
	 * Converts all the found clusters into grids, if they are valid.
	 *
	 * @param clusters List of clusters
	 */
	public void process( List<List<SquareNode>> clusters ) {
		grids.reset();
		for (int i = 0; i < clusters.size(); i++) {
			if( checkPreconditions(clusters.get(i)))
				processCluster(clusters.get(i));
		}
	}

	/**
	 * Checks basic preconditions.
	 * 1) No node may be linked two more than once
	 */
	protected boolean checkPreconditions(List<SquareNode> cluster) {
		for( int i = 0; i < cluster.size(); i++ ) {
			SquareNode n = cluster.get(i);
			for (int j = 0; j < n.corners.size(); j++) {
				SquareEdge e0 = n.edges[j];
				if( e0 == null)
					continue;
				for (int k = j+1; k < n.corners.size(); k++) {
					SquareEdge e1 = n.edges[k];
					if( e1 == null)
						continue;
					if( e0.destination(n) == e1.destination(n) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	/**
	 * Converts the cluster into a grid data structure.  If its not a grid then
	 * nothing happens
	 */
	protected void processCluster( List<SquareNode> cluster ) {
		invalid = false;
		// handle a special case
		if( cluster.size() == 1 ) {
			SquareNode n = cluster.get(0);
			if( n.getNumberOfConnections() == 0 ) {
				SquareGrid grid = grids.grow();
				grid.reset();
				grid.columns = grid.rows = 1;
				grid.nodes.add(n);
				return;
			}
		}

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
		if( invalid || firstRow == null )
			return;

		// Add the next rows to the list, one after another
		List<List<SquareNode>> listRows = new ArrayList<>();// TODO remove memory declaration here
		listRows.add(firstRow);
		while(true ) {
			List<SquareNode> previous = listRows.get(listRows.size()-1);
			if( !addNextRow(previous.get(0),listRows)) {
				break;
			}
		}

		if( invalid || listRows.size() < 2)
			return;

		// re-organize into a grid data structure
		SquareGrid grid = assembleGrid(listRows);

		// check the grids connectivity
		if( grid == null || !checkEdgeCount(grid) ) {
			grids.removeTail();
		}
	}

	/**
	 * Converts the list of rows into a grid.  Since it is a chessboard pattern some of the grid
	 * elements will be null.
	 */
	private SquareGrid assembleGrid( List<List<SquareNode>> listRows) {
		SquareGrid grid = grids.grow();
		grid.reset();

		List<SquareNode> row0 = listRows.get(0);
		List<SquareNode> row1 = listRows.get(1);

		int offset = row0.get(0).getNumberOfConnections() == 1 ? 0 : 1;

		grid.columns = row0.size() + row1.size();
		grid.rows = listRows.size();

		// initialize grid to null
		for (int i = 0; i < grid.columns * grid.rows; i++) {
			grid.nodes.add(null);
		}

		// fill in the grid
		for (int row = 0; row < listRows.size(); row++) {
			List<SquareNode> list = listRows.get(row);
			int startCol = offset - row%2 == 0 ? 0 : 1;
			// make sure there is the expected number of elements in the row
			int adjustedLength = grid.columns-startCol;
			if( (adjustedLength)-adjustedLength/2 != list.size() ) {
				return null;
			}
			int listIndex = 0;
			for (int col = startCol; col < grid.columns; col += 2) {
				grid.set(row,col,list.get(listIndex++));
			}
		}
		return grid;
	}

	/**
	 * Looks at the edge count in each node and sees if it has the expected number
	 */
	private boolean checkEdgeCount( SquareGrid grid ) {

		int left = 0, right = grid.columns-1;
		int top = 0, bottom = grid.rows-1;

		for (int row = 0; row < grid.rows; row++) {
			boolean skip = grid.get(row,0) == null;

			for (int col = 0; col < grid.columns; col++) {
				SquareNode n = grid.get(row,col);
				if( skip ) {
					if ( n != null )
						return false;
				} else {
					boolean horizontalEdge =  col == left || col == right;
					boolean verticalEdge =  row == top || row == bottom;

					boolean outer = horizontalEdge || verticalEdge;
					int connections = n.getNumberOfConnections();

					if( outer ) {
						if( horizontalEdge && verticalEdge ) {
							if( connections != 1 )
								return false;
						} else if( connections != 2 )
							return false;
					} else {
						if( connections != 4 )
							return false;
					}
				}
				skip = !skip;
			}
		}
		return true;
	}

	/**
	 * Adds the first row to the list of rows when the seed element has only one edge
	 */
	List<SquareNode> firstRow1( SquareNode seed ) {
		for (int i = 0; i < seed.corners.size(); i++) {
			if( isOpenEdge(seed,i) ) {
				List<SquareNode> list = new ArrayList<>();
				seed.graph = 0;

				// Doesn't know which direction it can traverse along.  See figure that out
				// by looking at the node its linked to
				int corner = seed.edges[i].destinationSide(seed);
				SquareNode dst = seed.edges[i].destination(seed);
				int l = addOffset(corner,-1,dst.corners.size());
				int u = addOffset(corner, 1,dst.corners.size());

				if( dst.edges[u] != null ) {
					list.add(seed);
					if( !addToRow(seed,i,-1,true,list) ) return null;
				} else if( dst.edges[l] != null ){
					List<SquareNode> tmp = new ArrayList<>();
					if( !addToRow(seed,i, 1,true,tmp) ) return null;
					flipAdd(tmp, list);
					list.add(seed);
				} else {
					// there is only a single node below it
					list.add(seed);
				}

				return list;
			}
		}
		throw new RuntimeException("BUG");
	}

	/**
	 * Adds the first row to the list of rows when the seed element has two edges
	 */
	List<SquareNode> firstRow2(SquareNode seed ) {
		int indexLower = lowerEdgeIndex(seed);
		int indexUpper = addOffset(indexLower,1,seed.corners.size());

		List<SquareNode> listDown = new ArrayList<>();
		List<SquareNode> list = new ArrayList<>();

		if( !addToRow(seed,indexUpper,1,true,listDown) ) return null;
		flipAdd(listDown, list);
		list.add(seed);
		seed.graph = 0;
		if( !addToRow(seed,indexLower,-1,true,list) ) return null;

		return list;
	}

	/**
	 * Given a node, add all the squares in the row directly below it.  They will be ordered from "left" to "right".  The
	 * seed node can be anywhere in the row, e.g. middle, start, end.
	 *
	 * @return true if a row was added to grid and false if not
	 */
	boolean addNextRow( SquareNode seed , List<List<SquareNode>> grid ) {
		List<SquareNode> row = new ArrayList<>();
		List<SquareNode> tmp = new ArrayList<>();

		int numConnections = numberOfOpenEdges(seed);
		if( numConnections == 0 ) {
			return false;
		} else if( numConnections == 1 ) {
			for (int i = 0; i < seed.corners.size(); i++) {
				SquareEdge edge = seed.edges[i];
				if( edge != null ) {
					// see if the edge is one of the open ones
					SquareNode dst = edge.destination(seed);
					if( dst.graph != SquareNode.RESET_GRAPH)
						continue;

					// determine which direction to traverse along
					int corner = edge.destinationSide(seed);
					int l = addOffset(corner,-1,dst.corners.size());
					int u = addOffset(corner, 1,dst.corners.size());

					// Nodes in the seed's row should all be marked, so any unmarked nodes
					// are ones you don't want to traverse down
					if( isClosedValidEdge(dst,l) ) {
						if( !addToRow(seed,i, 1,false,tmp) ) return false;
						flipAdd(tmp, row);
					} else if( isClosedValidEdge(dst,u) ){
						if( !addToRow(seed,i, -1,false,row) ) return false;
					} else {
						dst.graph = 0;
						row.add(dst);
					}
					break;
				}
			}
		} else if( numConnections == 2 ) {
			int indexLower = lowerEdgeIndex(seed);
			int indexUpper = addOffset(indexLower,1,seed.corners.size());

			if( !addToRow(seed,indexUpper, 1,false,tmp) ) return false;
			flipAdd(tmp, row);
			if( !addToRow(seed,indexLower,-1,false,row) ) return false;
		} else {
			return false;
		}
		grid.add(row);
		return true;
	}

	private void flipAdd(List<SquareNode> tmp, List<SquareNode> row) {
		for (int i = tmp.size()-1;i>=0; i--) {
			row.add( tmp.get(i));
		}
	}

	/**
	 * Returns the open corner index which is first.  Assuming that there are two adjacent corners.
	 */
	static int lowerEdgeIndex( SquareNode node ) {
		for (int i = 0; i < node.corners.size(); i++) {
			if( isOpenEdge(node,i) ) {
				int next = addOffset(i,1,node.corners.size());
				if( isOpenEdge(node,next)) {
					return i;
				}
				if( i == 0 ) {
					int previous = node.corners.size()-1;
					if( isOpenEdge(node,previous)) {
						return previous;
					}
				}
				return i;
			}
		}

		throw new RuntimeException("BUG!");
	}

	static int numberOfOpenEdges( SquareNode node ) {
		int total = 0;
		for (int i = 0; i < node.corners.size(); i++) {
			if( isOpenEdge(node,i) )
				total++;
		}
		return total;
	}

	/**
	 * Is the edge open and can be traversed to?  Can't be null and can't have
	 * the marker set to a none RESET_GRAPH value.
	 */
	static boolean isOpenEdge( SquareNode node , int index ) {
		if( node.edges[index] == null )
			return false;
		int marker = node.edges[index].destination(node).graph;
		return marker == SquareNode.RESET_GRAPH;
	}

	/**
	 * Does the edge point some place, but is closed
	 */
	static boolean isClosedValidEdge( SquareNode node , int index ) {
		if( node.edges[index] == null )
			return false;
		int marker = node.edges[index].destination(node).graph;
		return marker != SquareNode.RESET_GRAPH;
	}

	/**
	 * Given a node and the corner to the next node down the line, add to the list every other node until
	 * it hits the end of the row.
	 * @param n Initial node
	 * @param corner Which corner points to the next node
	 * @param sign Determines the direction it will traverse.  -1 or 1
	 * @param skip true = start adding nodes at second, false = start first.
	 * @param row List that the nodes are placed into
	 */
	boolean addToRow( SquareNode n , int corner , int sign , boolean skip ,
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
				if( n.graph != SquareNode.RESET_GRAPH) {
					// This should never happen in a valid grid.  It can happen if two nodes link to each other multiple
					// times.  Other situations as well
					invalid = true;
					return false;
				}
				n.graph = 0;
				row.add(n);
			}
			skip = !skip;
			sign *= -1;
			corner = addOffset(corner,sign,n.corners.size());
		}
		return true;
	}

	/**
	 * Finds a seed with 1 or 2 edges.
	 */
	static SquareNode findSeedNode(List<SquareNode> cluster) {
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

	public FastQueue<SquareGrid> getGrids() {
		return grids;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
