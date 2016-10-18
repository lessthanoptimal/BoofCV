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

import boofcv.alg.shapes.polygon.BinaryPolygonDetector;
import georegression.geometry.UtilPoint2D_F64;
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
// TODO If number of corners != 4 will considerConnect work correctly? Especially if more than 4
// TODO also update SquareRegularClustersIntoGrids for non-4-corner shapes
public class SquaresIntoCrossClusters extends SquaresIntoClusters {

	// maximum neighbors on nearest-neighbor search
	public int maxNeighbors;

	// tolerance for maximum distance away two corners can be to be considered neighbors
	double maxCornerDistance;

	// when connecting two nodes the connection's distance must be less than this fraction of the largest side's length
	double tooFarFraction = 0.3;

	// used to search for neighbors that which are candidates for connecting
	private NearestNeighbor<SquareNode> search = FactoryNearestNeighbor.kdtree();
	private FastQueue<double[]> searchPoints;
	private List<SquareNode> searchSquareList = new ArrayList<>();
	private FastQueue<NnData<SquareNode>> searchResults = new FastQueue(NnData.class,true);

	/**
	 * Declares data structures and configures algorithm
	 * @param maxCornerDistance Maximum distance two corners can be in pixels.
	 * @param maxNeighbors Max number of neighbors it will consider.  Try 4 or -1 for all
	 */
	public SquaresIntoCrossClusters(double maxCornerDistance, int maxNeighbors) {
		this.maxCornerDistance = maxCornerDistance;
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
	public List<List<SquareNode>> process(List<Polygon2D_F64> squares , List<BinaryPolygonDetector.Info> info ) {
		recycleData();

		// set up nodes
		computeNodeInfo(squares,info);

		// Connect nodes to each other
		connectNodes();

		// Find all valid graphs
		findClusters();
		return clusters.toList();
	}

	void computeNodeInfo( List<Polygon2D_F64> squares , List<BinaryPolygonDetector.Info> squaresInfo ) {
		for (int i = 0; i < squares.size(); i++) {
			SquareNode n = nodes.grow();
			n.reset();

			Polygon2D_F64 polygon = squares.get(i);
			BinaryPolygonDetector.Info info = squaresInfo.get(i);

			// see if every corner touches a border
			if( info.borderCorners.size() > 0 ) {
				boolean allBorder = true;
				for (int j = 0; j < info.borderCorners.size(); j++) {
					if (!info.borderCorners.get(j)) {
						allBorder = false;
						break;
					}
				}
				if (allBorder) {
					nodes.removeTail();
					continue;
				}
			}

			// The center is used when visualizing results
			UtilPoint2D_F64.mean(polygon.vertexes.data,0,polygon.size(),n.center);

			for (int j = 0,k = polygon.size()-1; j < polygon.size(); k=j,j++) {
				double l = polygon.get(j).distance(polygon.get(k));
				n.largestSide = Math.max(n.largestSide,l);
			}

			n.corners = polygon;
			n.touch = info.borderCorners;
			n.updateArrayLength();
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

			for (int indexLocal = 0; indexLocal < n.corners.size(); indexLocal++) {
				if( n.touch.size > 0 && n.touch.get(indexLocal) )
					continue;

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

					if( candidateIsMuchCloser(n, neighborNode, neighborData.distance))
						considerConnect(n, indexLocal, neighborNode, neighborCornerIndex, neighborData.distance);
				}
			}
		}
	}

	/**
	 * Returns the corner index of the specified coordinate
	 */
	int getCornerIndex( SquareNode node , double x , double y ) {
		for (int i = 0; i < node.corners.size(); i++) {
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

			for (int j = 0; j < n.corners.size(); j++) {
				if( n.touch.size > 0 && n.touch.get(j) )
					continue;

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
	 * Checks to see if the two corners which are to be connected are by far the two closest corners between the two
	 * squares
	 */
	boolean candidateIsMuchCloser( SquareNode node0 ,
								   SquareNode node1 ,
								   double distance2 )
	{
		double length = Math.max(node0.largestSide,node1.largestSide)*tooFarFraction;
		length *= length;

		if( distance2 > length)
			return false;
		return distance2 <= length;
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
