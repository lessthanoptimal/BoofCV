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
public class ChessboardCornerCluster {

	int maxNeighbors=10;

	double orientationTol = 0.1;
	double acuteTol = 0.1;
	double distanceTol = 0.2;

	// the two connected corners which are the best fit for being a pair
	int seedA,seedB;

	// data structures for nearest neighbor search
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);
	List<NnData<ChessboardCorner>> filtered = new ArrayList<>();


	public void process(List<ChessboardCorner> corners ) {
		nn.setPoints(corners,true);

		// Find local graphs for each corner independently
		for (int i = 0; i < corners.size(); i++) {
			findLocalGraphForNode(i,corners);
		}

		// Connect these local graphs into a proper grid(s)
		connectLocalGraph();
	}

	private void findLocalGraphForNode( int target , List<ChessboardCorner> corners ) {
		nnSearch.findNearest(corners.get(target),Double.MAX_VALUE,maxNeighbors,nnResults);

		// find the initial seed for the local connections
		if( !findBestPair(corners.get(target))) {
			return;
		}

		// TODO find the other two, if possible
		addConnections();


		// TODO find nodes which are similar to any of the selected nodes for future consideration
		addSimilarNodesToConnections();
	}

	/**
	 * Finds the two neighbors which have an acute angle which is closest to 90 degrees and their orientation
	 * is close enough to a 90 degree offset. Their distance from the target also needs to be about the same.
	 * Prefer pairs which are closer to the target.
	 * @return true if a pair was found
	 */
	boolean findBestPair( ChessboardCorner target ) {
		// filter list to only results that have the expected orientation
		filtered.clear();
		for (int i = 0; i < nnResults.size; i++) {
			double angle = UtilAngle.distHalf(nnResults.get(i).point.orientation,target.orientation);
			if( angle <= orientationTol ) {
				filtered.add(nnResults.get(i));
			}
		}

		if( filtered.size() < 2 )
			return false;

		double bestScore = Double.MAX_VALUE;
		seedA = -1;
		seedB = -1;

		for (int i = 0; i < filtered.size(); i++) {
			NnData<ChessboardCorner> a = filtered.get(i);

			double dx = a.point.x - target.x;
			double dy = a.point.y - target.y;

			double angleA = Math.atan2(dy,dx);
			double distanceA = Math.sqrt(dx*dx + dy*dy);

			for (int j = i+1; j < filtered.size(); j++) {
				NnData<ChessboardCorner> b = filtered.get(j);

				dx = b.point.x - target.x;
				dy = b.point.y - target.y;

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

		return seedA != -1;
	}

	private void addConnections() {

	}

	private void addSimilarNodesToConnections() {

	}


	private void connectLocalGraph() {

	}

	public static class Node {
		public int index;

		/**
		 * List of connections to other nodes. There will be 2,3 or 4 edges.
		 */
		public List<Edge> edges = new ArrayList<>();
	}

	public static class Edge {
		/**
		 * Index's of nodes it could be connected to
		 */
		public GrowQueue_I32 dst = new GrowQueue_I32();
	}
}
