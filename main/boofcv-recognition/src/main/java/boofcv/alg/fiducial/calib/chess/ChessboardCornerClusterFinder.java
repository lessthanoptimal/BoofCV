/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.chess;

import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.feature.detect.chess.ChessboardCornerDistance;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * Clusters detected chessboard corners together into a grids that are 2x2 or larger.
 *
 * TODO describe steps
 *
 * @author Peter Abeles
 */
public class ChessboardCornerClusterFinder {

	int maxNeighbors=10;

	double orientationTol = 0.1;
	double acuteTol = 0.1;
	double distanceTol = 0.2;

	FastQueue<Node> nodes = new FastQueue<>(Node.class,true);

	// data structures for nearest neighbor search
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);
	FastQueue<LocalInfo> filtered = new FastQueue<>(LocalInfo.class,true);
	List<LocalInfo> neighbors = new ArrayList<>();

	public void process(List<ChessboardCorner> corners ) {
		nn.setPoints(corners,true);

		// Find local graphs for each corner independently
		for (int i = 0; i < corners.size(); i++) {
			findLocalGraphForNode(i,corners);
		}

		// Connect these local graphs into a proper grid(s)
		connectLocalGraph();

		// Prune redundant nodes from clusters
		pruneNodesFromGraphs();
	}

	private void findLocalGraphForNode( int target , List<ChessboardCorner> corners ) {
		nnSearch.findNearest(corners.get(target),Double.MAX_VALUE,maxNeighbors,nnResults);

		// remove neighbors which are at the wrong angle
		if ( !filterNeighbors(corners.get(target)))
			return;

		// find the initial seed for the local connections
		if( !findBestPair(corners.get(target))) {
			return;
		}

		// find the other two, if possible
		LocalInfo neighborA = neighbors.get(0);
		LocalInfo neighborB = neighbors.get(1);
		findClosestToParallel(corners.get(target), neighborA.direction,neighborA.distance);
		findClosestToParallel(corners.get(target), neighborB.direction,neighborB.distance);

		// Create the local graph
		Node node = nodes.grow();
		node.reset();
		for (int i = 0; i < neighbors.size(); i++) {
			Edge e = node.edges.grow();
			e.reset();
			e.dst.add( neighbors.get(i).index );
		}

		// Edges can have multiple solutions if its ambiguous. Add alternatives to the edges here
		addSimilarNodesToConnections(node);
	}

	/**
	 * Finds the two neighbors which have an acute angle which is closest to 90 degrees and their orientation
	 * is close enough to a 90 degree offset. Their distance from the target also needs to be about the same.
	 * Prefer pairs which are closer to the target.
	 * @return true if a pair was found
	 */
	boolean findBestPair( ChessboardCorner target ) {
		double bestScore = Double.MAX_VALUE;
		int seedA = -1;
		int seedB = -1;

		for (int i = 0; i < filtered.size(); i++) {
			LocalInfo a = filtered.get(i);

			double dx = a.x - target.x;
			double dy = a.y - target.y;

			double angleA = Math.atan2(dy,dx);
			double distanceA = Math.sqrt(dx*dx + dy*dy);

			for (int j = i+1; j < filtered.size(); j++) {
				LocalInfo b = filtered.get(j);

				dx = b.x - target.x;
				dy = b.y - target.y;

				double angleB = Math.atan2(dy,dx);
				double distanceB = Math.sqrt(dx*dx + dy*dy);

				// Angle should be 90 degrees apart
				double angleDistance = UtilAngle.dist(angleA,angleB);

				double angleError = UtilAngle.dist(angleDistance,Math.PI/2);

				if( angleError <= acuteTol) {
					// score is a weighted average of angle error and fractional difference in distance
					double aveDist = (distanceA+distanceB)/2.0;

					double score = 0.1*angleError/Math.PI; // max value of 0.1
					score += Math.abs(distanceA-distanceB)/Math.max(distanceA,distanceB); // max value of 1
					// score now is an error metric with 0 being perfect
					score = (1.0+score)*aveDist;

					if( score < bestScore ) {
						bestScore = score;
						seedA = a.index;
						seedB = b.index;
					}
				}
			}
		}

		neighbors.clear();
		if( seedA != -1 ) {
			neighbors.add(filtered.get(seedA));
			neighbors.add(filtered.get(seedB));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * filter list to only results that have the expected orientation
	 */
	private boolean filterNeighbors(ChessboardCorner target) {
		filtered.reset();
		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> r = nnResults.get(i);
			double angle = UtilAngle.distHalf(r.point.orientation,target.orientation);
			if( angle <= orientationTol ) {
				ChessboardCorner c_i = r.point;
				LocalInfo info = filtered.grow();
				info.index = r.index;
				info.direction = Math.atan2(c_i.y-target.y,c_i.x-target.x);
				info.distance = r.distance;
				info.set(r.point.x,r.point.y);
			}
		}

		return filtered.size() >= 2;
	}

	/**
	 * searches for the node which is closest to being parallel, in the opposite direction, and about the same
	 * distance.
	 *
	 * @param direction angle (-pi to pi) of corner
	 * @param distance distance of corner
	 */
	private void findClosestToParallel( ChessboardCorner target, double direction, double distance ) {
		double bestScore = Double.MAX_VALUE;
		int bestIndex = -1;

		// we are only concerned with nodes pointed in the opposite direction
		direction = UtilAngle.bound(direction+Math.PI);

		for (int i = 0; i < filtered.size(); i++) {
			LocalInfo n = filtered.get(i);

			// see if the orientation error is within tolerance
			double errorDir = UtilAngle.dist(direction,n.direction);
			if( errorDir <= acuteTol )
				continue;

			// distance error is a fractional error but dampened for close up points where a pixel can result
			// in a large error
			double errorDist = Math.abs(distance-n.distance)/(Math.max(distance,n.distance)+2.0);

			double score = (errorDir + 0.1)*(errorDist+0.1);

			if( score < bestScore ) {
				bestScore = score;
				bestIndex = n.index;
			}
		}

		if( bestIndex != -1 ) {
			neighbors.add(filtered.get(bestIndex));
		}
	}

	/**
	 * For each edge in the node see if there's another node in the filtered list which would be within tolerance
	 * and a possible alternative to the one which was added.
	 */
	private void addSimilarNodesToConnections(Node node) {
		// TODO implement
	}

	/**
	 * Forms clusters of nodes by combining the local graphs. Two corners are connected to each other if they
	 * are mutually an edge
	 */
	private void connectLocalGraph() {

	}

	/**
	 * It is possible from two corners to represent the same element in a local cluster. This situation is detected
	 * by seeing if more than one node is within X distance of each other. If that is detected then one of them
	 * is removed based on an energy minimization function.
	 */
	private void pruneNodesFromGraphs() {

	}

	public static class LocalInfo extends Point2D_F64 {
		/**
		 * Index of this corner in the list of detected corners
		 */
		public int index;
		/**
		 * Relative direction of corner from target. -pi to pi
		 */
		public double direction;
		/**
		 * Euclidean distance from target corner
		 */
		public double distance;
	}

	public static class Node {
		/**
		 * Index of the corner that this node represents
		 */
		public int index;

		/**
		 * List of connections to other nodes. There will be 2,3 or 4 edges.
		 */
		public FastQueue<Edge> edges = new FastQueue<>(Edge.class,true);

		public void reset() {
			index = -1;
			edges.reset();
		}
	}

	public static class Edge {
		/**
		 * Index's of nodes it could be connected to
		 */
		public GrowQueue_I32 dst = new GrowQueue_I32();

		public void reset() {
			dst.reset();
		}
	}
}
