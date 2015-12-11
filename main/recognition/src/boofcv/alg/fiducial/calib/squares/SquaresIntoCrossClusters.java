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

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes the detected squares in the image and connects them into clusters in which the corners of each square
 * almost touches the corner of a neighbor.
 *
 * @author Peter Abeles
 */
public class SquaresIntoCrossClusters extends SquaresIntoClusters {

	// maximum neighbors on nearest-neighbor search
	public int maxNeighbors;

	// tolerance for maximum distance away two corners can be to be considered neighbors
	double maxCornerDistance = 5;

	// maximum distance two squares can be from each other relative to the size of a square
	double maxNeighborLengthChange;

	// used to search for neighbors that which are candidates for connecting
	private NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private List<SquareNode> searchSquareList = new ArrayList<SquareNode>();
	private FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);

	/**
	 * Declares data structures and configures algorithm
	 * @param maxCornerDistance Ratio of space between squares to square lengths
	 * @param maxNeighborLengthChange The maximum number of neighbors it will look at when connecting a node
	 * @param maxNeighbors Max number of neighbors it will consider.  Try 4 or -1
	 */
	public SquaresIntoCrossClusters(double maxCornerDistance, double maxNeighborLengthChange, int maxNeighbors) {
		this.maxCornerDistance = maxCornerDistance;
		this.maxNeighborLengthChange = maxNeighborLengthChange;
		this.maxNeighbors = maxNeighbors > 0 ? maxNeighbors : Integer.MAX_VALUE;

		//  avoid a roll over later on in the code
		if( this.maxNeighbors == Integer.MAX_VALUE ) {
			this.maxNeighbors = Integer.MAX_VALUE-1;
		}
		searchPoints = new FastQueue<double[]>(double[].class,true) {
			@Override
			protected double[] createInstance() {
				return new double[2];
			}
		};

		search.init(2);
	}

	/**
	 * Processes the unordered set of squares and creates a graph out of them using prior knowledge and geometric
	 * constraints.
	 * @param squares Set of squares
	 * @return List of graphs.  All data structures are recycled on the next call to process().
	 */
	public List<List<SquareNode>> process( List<Polygon2D_F64> squares ) {
		recycleData();

		// set up nodes
		computeNodeInfo(squares);

		// Connect nodes to each other
		connectNodes();

		// Find all valid graphs
		findClusters();
		return clusters.toList();
	}

	void computeNodeInfo( List<Polygon2D_F64> squares ) {

		for (int i = 0; i < squares.size(); i++) {
			SquareNode n = nodes.grow();
			n.reset();
			n.corners = squares.get(i);

			for (int j = 0; j < 4; j++) {
				int k = (j+1)%4;
				double l = n.corners.get(j).distance(n.corners.get(k));
				n.sideLengths[j] = l;
				n.largestSide = Math.max(n.largestSide,l);
			}
		}
	}

	/**
	 * Goes through each node and uses a nearest-neighbor search to find the closest nodes in its local neighborhood.
	 * It then checks those to see if it should connect
	 */
	void connectNodes() {
		setupSearch();

		int indexCornerList = 0;
		for (int indexNode = 0; indexNode < nodes.size(); indexNode++) {
			// search all the corners of this node for their neighbors
			SquareNode n = nodes.get(indexNode);

			for (int indexLocal = 0; indexLocal < 4; indexLocal++) {
				double[] point = searchPoints.get(indexCornerList++);
				// find it's neighbors
				searchResults.reset();
				search.findNearest(point, maxCornerDistance*maxCornerDistance, maxNeighbors + 1, searchResults);

				for (int indexResults = 0; indexResults < searchResults.size(); indexResults++) {
					NnData<SquareNode> neighborData = searchResults.get(indexResults);
					SquareNode neighborNode = neighborData.data;

					// if the neighbor corner is from the same node skip it
					if( neighborNode == n )
						continue;

					int neighborCornerIndex = getCornerIndex(neighborNode,neighborData.point[0],neighborData.point[1]);
					considerConnect(n, indexLocal, neighborNode, neighborCornerIndex, neighborData.distance);
				}
			}
		}
	}

	/**
	 * Returns the corner index of the specified coordinate
	 */
	int getCornerIndex( SquareNode node , double x , double y ) {
		for (int i = 0; i < 4; i++) {
			Point2D_F64 c = node.corners.get(i);
			if( c.x == x && c.y == y )
				return i;
		}

		throw new RuntimeException("BUG!");
	}

	/**
	 * Sets up data structures for nearest-neighbor search used in {@link #connectNodes()}
	 */
	private void setupSearch() {
		searchPoints.reset();
		searchSquareList.clear();

		for (int i = 0; i < nodes.size(); i++) {
			SquareNode n = nodes.get(i);

			for (int j = 0; j < 4; j++) {
				Point2D_F64 c = n.corners.get(j);
				double[] point = searchPoints.grow();
				point[0] = c.x;
				point[1] = c.y;
				// setup a list of squares for quick lookup
				searchSquareList.add(n);
			}
		}
		search.setPoints(searchPoints.toList(),searchSquareList);
	}

	/**
	 * Connects the 'candidate' node to node 'n' if they meet several criteria.  See code for details.
	 */
	void considerConnect(SquareNode node0,  int corner0 , SquareNode node1 , int corner1 , double distance ) {

		// TODO check max size change

		if( node0.edges[corner0] != null && node0.edges[corner0].distance > distance ) {
			detachEdge(node0.edges[corner0]);
		}

		if( node1.edges[corner1] != null && node1.edges[corner1].distance > distance ) {
			detachEdge(node1.edges[corner1]);
		}

		if( node0.edges[corner0] == null && node1.edges[corner1] == null) {
			connect(node0,corner0,node1,corner1,distance);
		}
	}
}
