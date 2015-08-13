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

package boofcv.alg.feature.detect.squares;

import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Detector for square grid calibration pattern.
 *
 * @author Peter Abeles
 */
// TODO support partial observations of grid
	// TODO tell the polygon detector that there should be no inner contour
public class ClustersIntoGrids {

	static final int SEARCHED = 1;
	boolean verbose = false;

	// minimum number of squares in a grid
	int minimumElements;

	// All valid graphics
	List<SquareGrid> valid = new ArrayList<SquareGrid>();

	public ClustersIntoGrids( int minimumElements) {
		this.minimumElements = minimumElements;
	}

	public void process( List<List<SquareNode>> clusters ) {

		for( int i = 0; i < clusters.size(); i++ ) {
			List<SquareNode> graph = clusters.get(i);

			if( graph.size() < minimumElements )
				continue;

			if( !checkNumberOfConnections(graph))
				continue;

			SquareGrid grid = orderIntoGrid(graph);
			if( grid != null )
				valid.add( grid );
		}
	}

	/**
	 * Does a weak check on the number of edges in the graph.  Since the structure isn't known it can't make
	 * harder checks
	 */
	private boolean checkNumberOfConnections( List<SquareNode> graph ) {
		int histogram[] = new int[5];

		for (int i = 0; i < graph.size(); i++) {
			histogram[ graph.get(i).getNumberOfConnections() ]++;
		}

		if( histogram[0] != 0 )
			return false;
		if( histogram[1] != 0 )
			return false;
		if( histogram[2] != 4 )
			return false;
		return true;
	}

	/**
	 * Given an unordered set of nodes, it will order them into a grid with row-major indexes.
	 * @param graph unordered nodes in a connected graph
	 * @return grid
	 */
	SquareGrid orderIntoGrid(List<SquareNode> graph) {

		// discard previous label information since its now being used to avoid cycles
		for (int i = 0; i < graph.size(); i++) {
			graph.get(i).graph = -1;
		}

		List<SquareNode> column = new ArrayList<SquareNode>();
		List<SquareNode> ordered = new ArrayList<SquareNode>();

		for (int i = 0; i < graph.size(); i++) {
			// Find a side with 2 connections and use that as the seed
			SquareNode seed = graph.get(i);
			if( seed.getNumberOfConnections() != 2 )
				continue;

			seed.graph = 1;
			column.add(seed);

			// find all the nodes along one side, just pick an edge arbitrarily.  This will be the first column
			for (int j = 0; j < 4; j++) {
				if( seed.edges[i] == null )
					continue;

				SquareNode b = seed.edges[i].destination(seed);
				b.graph = 1;

				column.add(b);
				addLineToGrid(seed, b, column);
				break;
			}

			// handle special case where there is only one element
			if( column.size() <= 1 ) {
				ordered.addAll(column);
				break;
			}
			if (addRowsToGrid(column, ordered)) return null;

			break;
		}

		SquareGrid grid = new SquareGrid();
		grid.nodes = ordered;
		grid.columns = column.size();
		grid.rows = ordered.size() / column.size();

		return grid;
	}

	/**
	 * Competes the graph by traversing down the first column and adding the rows one at a time
	 */
	boolean addRowsToGrid(List<SquareNode> column, List<SquareNode> ordered) {
		// now add the rows by traversing down the column
		int numFirsRow = 0;
		for (int j = 0; j < column.size(); j++) {
			SquareNode n = column.get(j);

			n.graph = SEARCHED;
			ordered.add(n);

			SquareNode nextRow;
			if( j == 0 ) {
				if( n.getNumberOfConnections() != 2 ) {
					if( verbose ) System.err.println(
							"Unexpected number of connections. want 2 found "+n.getNumberOfConnections());
					return true;
				}

				nextRow = pickNot(n, column.get(j + 1));

			} else if( j == column.size()-1 ) {
				if( n.getNumberOfConnections() != 2 ) {
					if (verbose) System.err.println(
							"Unexpected number of connections. want 2 found " + n.getNumberOfConnections());
					return true;
				}
				nextRow = pickNot(n,column.get(j-1));
			} else {
				if( n.getNumberOfConnections() != 3 ) {
					if (verbose) System.err.println(
							"Unexpected number of connections. want 2 found " + n.getNumberOfConnections());
					return true;
				}
				nextRow = pickNot(n, column.get(j-1),column.get(j+1));
			}

			nextRow.graph = SEARCHED;
			ordered.add(nextRow);
			int numberLine = addLineToGrid(n, nextRow, ordered);

			if( j == 0 ) {
				numFirsRow = numberLine;
			} else if(numberLine != numFirsRow ) {
				if( verbose ) System.err.println("Number of elements in rows do not match.");
				return true;
			}
		}
		return false;
	}

	/**
	 * Add all the nodes into the list which lie along the line defined by a and b.  a is assumed to be
	 * an end point.  Care is taken to not cycle.
	 */
	LineParametric2D_F64 line = new LineParametric2D_F64();
	int addLineToGrid(SquareNode a, SquareNode b, List<SquareNode> list) {

		int total = 2;

		while( true ) {
			// maximum distance off of line
			double bestDistance = b.largestSide / 4.0;
			bestDistance *= bestDistance;

			SquareNode best = null;

			line.setP(a.center);
			line.setSlope(b.center.x - a.center.x, b.center.y - a.center.y);

			// pick the child of b which is closest to the line going through the centers and not outside of tolerance
			for (int i = 0; i < 4; i++) {
				if( b.edges[i] == null )
					continue;

				SquareNode c = b.edges[i].destination(b);

				if (c.graph == SEARCHED )
					continue;

				double distance = Distance2D_F64.distanceSq(line, c.center);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = c;
				}
			}

			if( best == null )
				return total;
			else {
				total++;
				best.graph = SEARCHED;
				list.add(best);
				a = b;
				b = best;
			}
		}
	}

	/**
	 * There are only two edges on target.  Pick the edge which does not go to the provided child
	 */
	static SquareNode pickNot( SquareNode target , SquareNode child ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge e = target.edges[i];
			SquareNode c = e.destination(target);
			if( c != child )
				return c;
		}
		throw new RuntimeException("There was no odd one out some how");
	}

	/**
	 * There are only three edges on target and two of them are known.  Pick the one which isn't an inptu child
	 */
	static SquareNode pickNot( SquareNode target , SquareNode child0 , SquareNode child1 ) {
		for (int i = 0; i < 4; i++) {
			SquareEdge e = target.edges[i];
			SquareNode c = e.destination(target);
			if( c != child0 && c != child1 )
				return c;
		}
		throw new RuntimeException("There was no odd one out some how");
	}

	/**
	 * Returns a list of all the square grids it found
	 */
	public List<SquareGrid> getGrids() {
		return valid;
	}
}
