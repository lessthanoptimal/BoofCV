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
 * @author Peter Abeles
 */
public class ChessboardCornerClusterFinder2 {

	double angleTol;
	double distanceTol=0.2;

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

	// predeclared storage for results
	SearchResults results = new SearchResults();

	// Used to convert the internal graph into the output clusters
	GrowQueue_I32 c2v = new GrowQueue_I32(); // corner index to output node index
	GrowQueue_I32 v2c = new GrowQueue_I32(); // output node index to corner index
	GrowQueue_I32 open = new GrowQueue_I32(); // list of corner index's which still need ot be processed


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

		// Select the final 2 to 4 connections from perpendicular set
		// each pair of adjacent perpendicular edge needs to have a matching parallel edge between them
		// Use each perpendicular edge as a seed and select the best one
		for (int idx = 0; idx < vertexes.size(); idx++) {
			selectConnections(vertexes.get(idx));
		}

		// Identify situations where two vertexes are essentially identical and pick one and remove others
		pruneDuplicates();

		// TODO compute outputs
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

	/**
	 * Go through each node, and in their set of selected nodes identify ones which appear identicial to a selected one.
	 * Out of those candidates, select the one which the highest selectedCount. Remove and change all references
	 * to the one just removed.
	 */
	void pruneDuplicates() {
		for (int idx = 0; idx < vertexes.size(); idx++) {
			Vertex v = vertexes.get(idx);

			for (int i = 0; i < v.connections.size(); i++) {
				Edge c = v.connections.get(i);

			}
		}
	}

	void pruneDuplicates( Vertex src , Edge target ) {
		List<Vertex> candidates = new ArrayList<>();

		for (int i = 0; i < src.perpendicular.size(); i++) {
			Edge e = src.perpendicular.get(i);
			if( e == target )
				continue;

			double diffDirection = UtilAngle.dist(target.direction,e.direction);

			if( diffDirection > angleTol )
				continue;

			double diffDistance = Math.abs(target.distance-e.distance)/target.distance;
			if( diffDistance > distanceTol )
				continue;

			candidates.add(e.dst);
		}

		if( candidates.size() > 0 ) {
			candidates.add(target.dst);

			int bestIndex = -1;
			double bestScore = 0;

			for (int i = 0; i < candidates.size(); i++) {
				Vertex v = candidates.get(i);
				if( v.selectedCount > bestScore ) {
					bestScore = v.selectedCount;
					bestIndex = i;
				}
			}

			for (int i = 0; i < candidates.size(); i++) {
				if( i == bestIndex )
					continue;
				removeAndReplace(candidates.get(i),candidates.get(bestIndex));
			}
		}
	}

	void removeAndReplace( Vertex remove , Vertex replaceWith ) {
		for (int i = 0; i < remove.perpendicular.size(); i++) {
			Vertex v = remove.perpendicular.get(i).dst;

			// get the index of the vertex that is to replace 'remove'
			int ridx = v.perpendicular.find(replaceWith);
			if( ridx == -1 ) // its not attached to this vertex, just move on
				continue;

			// remove reference to this vertex to avoid future confusion
			v.perpendicular.edges.remove(v.perpendicular.find(remove));

			// change the connection
			for (int j = 0; j < v.connections.size(); j++) {
				if( v.connections.get(i).dst == remove ) {
					v.connections.set(i,v.perpendicular.get(ridx));
				}
			}
		}
	}

	/**
	 * Select the best 2,3, or 4 perpendicular vertexes to connect to.
	 */
	void selectConnections( Vertex target ) {
		// There needs to be at least two corners
		if( target.perpendicular.size() <= 1 )
			return;

		double bestError = Double.MAX_VALUE;
		List<Edge> bestSolution = target.connections;
		List<Edge> solution = new ArrayList<>();

		for (int i = 0; i < target.perpendicular.size(); i++) {
			Edge e = target.perpendicular.get(i);
			solution.clear();
			solution.add(e);

			double error = 0;

			// see if matches can be found in 90 degree increments
			if( target.perpendicular.findBestMatch(e.direction,Math.PI/2.0,e.distance,
					results,angleTol,distanceTol))  {
				error += results.error;
				solution.add(target.perpendicular.edges.get(results.index));

				if( target.perpendicular.findBestMatch(e.direction,Math.PI,e.distance,results,angleTol,distanceTol)) {
					error += results.error;
					solution.add(target.perpendicular.edges.get(results.index));

					// TODO require a parallel nodes to lie between them?

					if( target.perpendicular.findBestMatch(e.direction,3*Math.PI/2.0,e.distance,
							results,angleTol,distanceTol)) {
						error += results.error;
						solution.add(target.perpendicular.edges.get(results.index));
					}
				}
			} else {
				continue;
			}

			// favor more corners being added, but not too much
			error = (error + 1)/(2+2*solution.size());

			// favor a closer solution
			error *= Math.sqrt(e.distance);

			if( error < bestError ) {
				bestSolution.clear();
				bestSolution.addAll(solution);
			}
		}

		// increment the number of times it has been selected
		for (int i = 0; i < bestSolution.size(); i++) {
			bestSolution.get(i).dst.selectedCount++;
		}
	}

	/**
	 * Converts the internal graphs into unordered chessboard grids.
	 */
	void convertToOutput(List<ChessboardCorner> corners) {

		c2v.resize(corners.size());
		v2c.resize(vertexes.size());
		open.reset();
		v2c.fill(-1);
		c2v.fill(-1);

		for (int seedIdx = 0; seedIdx < vertexes.size; seedIdx++) {
			Vertex seedN = vertexes.get(seedIdx);
			if( v2c.get(seedN.index) != -1 )
				continue;
			ChessboardCornerGraph graph = clusters.grow();
			graph.reset();

			// traverse the graph and add all the nodes in this cluster
			growCluster(corners, seedIdx, graph);

			// Connect the nodes together in the output graph
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				Vertex n = vertexes.get( v2c.get(i) );

				for (int j = 0; j < n.connections.size(); j++) {
					int outputIdx = c2v.get( n.connections.get(j).dst.index );
					if( outputIdx == -1 ) {
						throw new IllegalArgumentException("Edge to node not in the graph");
					}
					gn.edges[j] = graph.corners.get(outputIdx);
				}
			}

			// ensure arrays are all -1 again for sanity checks
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				int indexCorner = v2c.get(gn.index);
				c2v.data[indexCorner] = -1;
				v2c.data[gn.index] = -1;
			}

			if( graph.corners.size <= 1 )
				clusters.removeTail();
		}
	}

	/**
	 * Given the initial seed, add all connected nodes to the output cluster while keeping track of how to
	 * convert one node index into another one, between the two graphs
	 */
	private void growCluster(List<ChessboardCorner> corners, int seedIdx, ChessboardCornerGraph graph) {
		// open contains corner list indexes
		open.add(seedIdx);
		while( open.size > 0 ) {
			int target = open.pop();

			// make sure it hasn't already been processed
			if( v2c.get(target) != -1 )
				continue;

			// Create the node in the output cluster for this corner
			ChessboardCornerGraph.Node gn = graph.growCorner();
			c2v.data[target] = gn.index;
			v2c.data[gn.index] = target;
			gn.set(corners.get(target));

			// Add to the open list all the edges which haven't been processed yet
			Vertex v = vertexes.get(target);
			for (int i = 0; i < v.connections.size(); i++) {
				Vertex dst = v.connections.get(i).dst;
				if( v2c.get(dst.index) != -1 )
					continue;
				open.add( dst.index );
			}
		}
	}

	public FastQueue<ChessboardCornerGraph> getOutputClusters() {
		return clusters;
	}

	public static class SearchResults {
		public int index;
		public double error;
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

		/**
		 * Final set of edfes which it was decided that this vertex is connected to. Will have 2 to 4 elements.
		 */
		public List<Edge> connections = new ArrayList<>();

		/**
		 * Number of times it has been selected by another node to connect with it
		 */
		public int selectedCount;

		public void reset() {
			index = -1;
			parallel.reset();
			perpendicular.reset();
			connections.clear();
			selectedCount = 0;
		}

		public void pruneNonMutal( boolean isParallel ) {
			EdgeSet set = isParallel ? this.parallel : this.perpendicular;

			for (int j = set.edges.size()-1; j >= 0; j-- ) {
				Vertex dst = set.edges.get(j).dst;
				EdgeSet dstSet = isParallel ? dst.parallel : dst.perpendicular;

				if( -1 == dstSet.find(this) ) {
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

		public boolean findBestMatch(double reference, double targetCCW, double targetDist,
									 SearchResults result , double maxErrorCCW, double maxErrorDist ) {
			result.error = Double.MAX_VALUE;
			result.index = -1;

			for (int i = 0; i < edges.size(); i++) {
				Edge e = edges.get(i);
				double errorCCW = UtilAngle.dist(targetCCW,UtilAngle.distanceCCW(reference,e.direction));
				double errorDist = Math.abs(targetDist-e.distance)/targetDist;

				if( errorCCW > maxErrorCCW || errorDist > maxErrorDist )
					continue;

				// angle error should be minimized more than distance error
				result.error = 2*errorCCW/maxErrorCCW + errorDist/maxErrorDist;
				result.index = i;
			}

			return result.index != -1;
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

		public int find( Vertex v ) {
			for (int i = 0; i < edges.size(); i++) {
				if( edges.get(i).dst == v )
					return i;
			}
			return -1;
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
