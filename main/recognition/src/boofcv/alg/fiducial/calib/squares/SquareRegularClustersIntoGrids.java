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

import georegression.metric.Distance2D_F64;
import georegression.struct.line.LineParametric2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes as input a set of unordered regular connected clusters and converts them into ordered grids with known numbers
 * of rows and columns.
 *
 * @author Peter Abeles
 */
public class SquareRegularClustersIntoGrids {

	// Value of a node which has been searched already
	static final int SEARCHED = 1;
	// verbose debug output
	private boolean verbose = false;

	// minimum number of squares in a grid
	private int minimumElements;

	// All valid graphics
	FastQueue<SquareGrid> valid = new FastQueue<>(SquareGrid.class, true);

	/**
	 * Configures class
	 * @param minimumElements The minimum number of elements which must be in a cluster for it to be accepted
	 */
	public SquareRegularClustersIntoGrids(int minimumElements) {
		this.minimumElements = minimumElements;
	}

	/**
	 * Converts the set of provided clusters into ordered grids.
	 *
	 * @param clusters List of clustered nodes
	 */
	public void process( List<List<SquareNode>> clusters ) {

		valid.reset();
		for( int i = 0; i < clusters.size(); i++ ) {
			List<SquareNode> graph = clusters.get(i);

			if( graph.size() < minimumElements )
				continue;

			switch( checkNumberOfConnections(graph) ) {
				case 1:orderIntoLine(graph); break;
				case 2:orderIntoGrid(graph); break;
//				default: System.out.println("Failed number of connections. size = "+graph.size());
			}
		}
	}

	/**
	 * Does a weak check on the number of edges in the graph.  Since the structure isn't known it can't make
	 * harder checks
	 *
	 * @return 0 = not a grid.  1 = line, 2 = grud
	 */
	int checkNumberOfConnections( List<SquareNode> graph ) {
		int histogram[] = new int[5];

		for (int i = 0; i < graph.size(); i++) {
			histogram[ graph.get(i).getNumberOfConnections() ]++;
		}

		if( graph.size() == 1 ) {
			if( histogram[0] != 1 )
				return 0;

			return 1;
		} else if( histogram[1] == 2 ) {
			// line
			if( histogram[0] != 0 )
				return 0;
			if( histogram[2] != graph.size()-2 )
				return 0;
			if( histogram[3] != 0 )
				return 0;
			if( histogram[4] != 0 )
				return 0;

			return 1;
		} else {
			// grid
			if (histogram[0] != 0)
				return 0;
			if (histogram[1] != 0)
				return 0;
			if (histogram[2] != 4)
				return 0;
			return 2;
		}
	}

	/**
	 * Puts the un-ordered graph into a ordered grid which is a line.
	 */
	List<SquareNode> nodesLine = new ArrayList<>();
	void orderIntoLine(List<SquareNode> graph) {

		// discard previous label information since its now being used to avoid cycles
		for (int i = 0; i < graph.size(); i++) {
			graph.get(i).graph = -1;
		}

		nodesLine.clear();

		if( graph.size() > 1 ) {
			escape:
			for (int i = 0; i < graph.size(); i++) {
				// Find a side with 2 connections and use that as the seed
				SquareNode seed = graph.get(i);
				if (seed.getNumberOfConnections() != 1)
					continue;

				seed.graph = SEARCHED;
				nodesLine.add(seed);

				// Find the one connecting node
				for (int edge = 0; edge < 4; edge++) {
					if (seed.edges[edge] == null)
						continue;

					SquareNode b = seed.edges[edge].destination(seed);
					b.graph = 1;

					nodesLine.add(b);
					addLineToGrid(seed, b, nodesLine);
					break escape;
				}
			}
		} else {
			nodesLine.add(graph.get(0));
		}

		SquareGrid grid = valid.grow();
		grid.nodes.clear();
		grid.nodes.addAll(nodesLine);
		grid.columns = nodesLine.size();
		grid.rows = 1;
	}

	/**
	 * Given an unordered set of nodes, it will order them into a grid with row-major indexes.  This assumes
	 * the grid is 2 by 2 or larger.
	 * @param graph unordered nodes in a connected graph
	 */
	List<SquareNode> column = new ArrayList<>();
	List<SquareNode> ordered = new ArrayList<>();
	void orderIntoGrid(List<SquareNode> graph) {

		// discard previous label information since its now being used to avoid cycles
		for (int i = 0; i < graph.size(); i++) {
			graph.get(i).graph = -1;
		}

		column.clear();
		ordered.clear();

		for (int i = 0; i < graph.size(); i++) {
			// Find a side with 2 connections and use that as the seed
			SquareNode seed = graph.get(i);
			if( seed.getNumberOfConnections() != 2 )
				continue;

			seed.graph = SEARCHED;
			column.add(seed);

			// find all the nodes along one side, just pick an edge arbitrarily.  This will be the first column
			for (int edge = 0; edge < 4; edge++) {
				if( seed.edges[edge] == null )
					continue;

				SquareNode b = seed.edges[edge].destination(seed);
				b.graph = SEARCHED;

				column.add(b);
				addLineToGrid(seed, b, column);
				break;
			}

			if (addRowsToGrid(column, ordered))
				return;

			break;
		}

		SquareGrid grid = valid.grow();
		grid.nodes.clear();
		grid.nodes.addAll(ordered);
		grid.columns = ordered.size() / column.size();
		grid.rows = column.size();
	}

	/**
	 * Competes the graph by traversing down the first column and adding the rows one at a time
	 */
	boolean addRowsToGrid(List<SquareNode> column, List<SquareNode> ordered) {
		for (int i = 0; i < column.size(); i++) {
			column.get(i).graph = 0;
		}

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
			if( e == null )
				continue;
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
			if( e == null ) continue;
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
		return valid.toList();
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
}
