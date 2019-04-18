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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ChessboardCornerClusterFinder2 {

	double angleTol;

	// Number of nearest neighbors it will search. It's assumed that the feature detector does a very
	// good job removing false positives, meaning that tons of features do not need to be considered
	int maxNeighbors=16; // 8 is minimum number given perfect data.

	// Data structures for the crude graph
	FastQueue<Vertex> vertexes = new FastQueue<>(Vertex.class,true);
	FastQueue<Edge> edges = new FastQueue<>(Edge.class,true);

	// data structures for nearest neighbor search
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);

	// Output. Contains a graph of connected corners
	FastQueue<ChessboardCornerGraph> clusters = new FastQueue<>(ChessboardCornerGraph.class,true);

	public void process(List<ChessboardCorner> corners ) {
		// reset internal data structures
		vertexes.reset();
		edges.reset();
		clusters.reset();

		// Create a vertex for each corner
		for (int idx = 0; idx < corners.size(); idx++) {
			Vertex v = vertexes.grow();
			v.reset();
			v.index = idx;
		}

		// Initialize nearest-neighbor search.
		nn.setPoints(corners,true);

		// Connect corners to each other based on relative distance on orientation
		for (int i = 0; i < corners.size(); i++) {
			findVertexConnections(vertexes.get(i),corners);
		}

		// Prune connections which are not mutual
		for (int i = 0; i < vertexes.size; i++) {
			Vertex v = vertexes.get(i);
			v.pruneNonMutal(true);
			v.pruneNonMutal(false);
		}

		// Identify situations where two vertexes are essentially identical and pick one and remove others


		// Select the final 2 to 4 connections from perpendicular set
		// each pair of adjacent perpendicular edge needs to have a matching parallel edge between them
		// Use each perpendicular edge as a seed and select the best one
	}

	void findVertexConnections( Vertex target  , List<ChessboardCorner> corners ) {
		ChessboardCorner targetCorner = corners.get(target.index);
		nnSearch.findNearest(corners.get(target.index),Double.MAX_VALUE,maxNeighbors,nnResults);

		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> r = nnResults.get(i);
			if( r.index == target.index) continue;

			double oriDiff = UtilAngle.distHalf( targetCorner.orientation , r.point.orientation );

			Edge edge = edges.grow();
			boolean parallel;
			if( oriDiff <= angleTol ) { // see if it's parallel
				parallel = true;
			} else if( Math.abs(oriDiff-Math.PI/2.0) <= angleTol ) { // see if it's perpendicular
				parallel = false;
			} else {
				edges.removeTail();
				continue;
			}

			// Use the relative angles of orientation and direction to prune more obviously bad matches
			double dx = r.point.x - targetCorner.x;
			double dy = r.point.y - targetCorner.y;

			edge.distance = Math.sqrt(r.distance);
			edge.dst = vertexes.get(r.index);
			edge.direction = Math.atan2(dy,dx);

			double direction180 = UtilAngle.boundHalf(edge.direction);
			double directionDiff = UtilAngle.distHalf(direction180,r.point.orientation);
			boolean remove = false;
			if( parallel ) {
				// test to see if direction and orientation are aligned or off by 90 degrees
				remove = directionDiff > angleTol && Math.abs(directionDiff-Math.PI/2.0) > angleTol;
			} else {
				// should be at 45 degree angle
				remove = Math.abs(directionDiff-Math.PI/4.0) > angleTol;
			}

			if( remove ) {
				edges.removeTail();
			} else if( parallel ) {
				target.parallel.add(edge);
			} else {
				target.perpendicular.add(edge);
			}
		}
	}

	public static class Vertex {
		/**
		 * Index of the corner that this node represents
		 */
		public int index;

		/**
		 * Nodes which are close and have the same orientation
		 */
		public EdgeSet parallel = new EdgeSet();
		/**
		 * Nodes which are close and have an orientation off by about 90 degrees
		 */
		public EdgeSet perpendicular = new EdgeSet();

		public void reset() {
			index = -1;
			parallel.reset();
			perpendicular.reset();
		}

		public void pruneNonMutal( boolean isParallel ) {
			EdgeSet set = isParallel ? this.parallel : this.perpendicular;

			for (int j = set.edges.size()-1; j >= 0; j-- ) {
				Vertex dst = set.edges.get(j).dst;
				EdgeSet dstSet = isParallel ? dst.parallel : dst.perpendicular;

				if( !dstSet.contains(this) ) {
					set.edges.remove(j);
				}
			}
		}
	}

	private static class EdgeSet {
		public List<Edge> edges = new ArrayList<>();

		public void reset() {
			edges.clear();
		}

		public void add( Edge e ) {
			edges.add(e);
		}

		public Edge get( int i ) {
			return edges.get(i);
		}

		public int size() {
			return edges.size();
		}

		public boolean contains( Vertex v ) {
			for (int i = 0; i < edges.size(); i++) {
				if( edges.get(i).dst == v )
					return true;
			}
			return false;
		}
	}

	public static class Edge {
		// Euclidean distance between the two
		public double distance;
		// pointing direction (-pi to pi) from src (x,y) to dst (x,y)
		public double direction;
		// Destination vertex
		public Vertex dst;
	}
}
