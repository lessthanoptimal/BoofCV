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
import org.ddogleg.struct.GrowQueue_B;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

// TODO disconnect edges with very different lengths from others
// TODO control for orientation tol
// TODO control to set grid shape
// TODO Get working rotated image01
// TODO then work on fisheye
/**
 * Clusters detected chessboard corners together into a grids that are 2x2 or larger.
 *
 * TODO describe steps
 *
 * @author Peter Abeles
 */
public class ChessboardCornerClusterFinder {

	// Number of nearest neighbors it will search. It's assumed that the feature detector does a very
	// good job removing false positives, meaning that tons of features do not need to be considered
	int maxNeighbors=16; // 8 is minimum number given perfect data.

	// how close to the expected orientation a corner needs to be. radians
	double orientationTol = 0.5;
	// angle tolerance for edge direction. radians
	double directionTol = 0.4;
	// maximum fractional error allowed between two distances
	double distanceTol = 0.2;

	// reference to input corners
	List<ChessboardCorner> corners;

	// Description of local graphs
	FastQueue<LNode> nodes = new FastQueue<>(LNode.class, LNode::new);

	// Output. Contains a graph of connected corners
	FastQueue<ChessboardCornerGraph> clusters = new FastQueue<>(ChessboardCornerGraph.class,true);

	// data structures for nearest neighbor search
	NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);

	FastQueue<LConnections> connectionsStorage = new FastQueue<>(LConnections.class,LConnections::new);

	// Keeps a list of edges with multiple solutions
	List<LConnections> ambiguousEdges = new ArrayList<>();
	GrowQueue_B connected = new GrowQueue_B();

	// Used to convert the internal graph into the output clusters
	GrowQueue_I32 c2n = new GrowQueue_I32(); // corner index to output node index
	GrowQueue_I32 n2c = new GrowQueue_I32(); // output node index to corner index
	GrowQueue_I32 open = new GrowQueue_I32(); // list of corner index's which still need ot be processed

	int HACK_interesting=-1;

	/**
	 * Given the unordered set of corners, for a set of clusters which represent sets of chessboard corners/
	 * @param corners Found corners inside of image
	 */
	public void process(List<ChessboardCorner> corners ) {
		HACK_interesting = -1;
		this.corners = corners;

		// reset internal data structures
		nodes.reset();
		connectionsStorage.reset();
		clusters.reset();
		ambiguousEdges.clear();

		// Initialize nearest-neighbor search.
		nn.setPoints(corners,true);

		// Find local graphs for each corner independently
		for (int i = 0; i < corners.size(); i++) {
			findLocalGraphForNode(i,corners);
		}

//		printInternalGraph();

		// remove connections which are not mutual and ensure graph assumptions are still meet
		pruneNonMutualConnections();
		disconnectInvalidNodes();

		if(HACK_interesting!=-1 ) {
			System.out.println("Prune Non: Interesting: edges = "+nodes.get(HACK_interesting).countEdges());
		}

		// Clean up the graph and remove uncertainty
		findAndResolveAllAmbiguity();
		disconnectInvalidNodes();

		if(HACK_interesting!=-1 ) {
			System.out.println("Resolve: Interesting: edges = "+nodes.get(HACK_interesting).countEdges());
		}


		// Disconnect edges which are much shorter or longer than all the others.
		// TODO do it

		// create the output
		convertToOutput(corners);
	}

	/**
	 * Prints the graph. Used for debugging the code.
	 */
	public void printInternalGraph() {
		for( LNode n : nodes.toList() ) {
			ChessboardCorner c = corners.get(n.index);
			System.out.printf("[%3d] {%3.0f, %3.0f) -> ",n.index,c.x,c.y);
			for (int i = 0; i < 4; i++) {
				if( n.edges[i] == null ) {
					System.out.print("[      ] ");
				} else {
					LConnections conn = n.edges[i];
					System.out.print("[ ");
					for (int j = 0; j < conn.dst.size; j++) {
						System.out.printf("%3d ",n.neighbors.get(conn.dst.get(j)).index);
					}
					System.out.print(" ] ");
				}
			}
			System.out.println();
		}
	}

	/**
	 * Converts the internal graphs into unordered chessboard grids.
	 */
	void convertToOutput(List<ChessboardCorner> corners) {

		c2n.resize(corners.size());
		n2c.resize(corners.size());
		open.reset();
		n2c.fill(-1);
		c2n.fill(-1);

		for (int seedIdx = 0; seedIdx < nodes.size; seedIdx++) {
			LNode seedN = nodes.get(seedIdx);
			if( seedN.insideCluster )
				continue;
			ChessboardCornerGraph graph = clusters.grow();
			graph.reset();

			// traverse the graph and add all the nodes in this cluster
			growCluster(corners, seedIdx, graph);

			// Connect the nodes together in the output graph
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				LNode n = nodes.get( n2c.get(i) );

				for (int j = 0; j < 4; j++) {
					if( n.edges[j] == null )
						continue;
					int outputIdx = c2n.get( n.getInfoForEdge(j).index );
					if( outputIdx == -1 ) {
						throw new IllegalArgumentException("Edge to node not in the graph");
					}
					gn.edges[j] = graph.corners.get(outputIdx);
				}
			}

			// ensure arrays are all -1 again for sanity checks
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				int indexCorner = n2c.get(gn.index);
				c2n.data[indexCorner] = -1;
				n2c.data[gn.index] = -1;
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
			if( nodes.get(target).insideCluster)
				continue;
			nodes.get(target).insideCluster = true;

			// Create the node in the output cluster for this corner
			ChessboardCornerGraph.Node gn = graph.growCorner();
			c2n.data[target] = gn.index;
			n2c.data[gn.index] = target;
			gn.set(corners.get(target));

			// Add to the open list all the edges which haven't been processed yet
			LNode n = nodes.get(target);
			for (int i = 0; i < 4; i++) {
				if( n.edges[i] == null )
					continue;
				LocalInfo info = n.getInfoForEdge(i);
				if( nodes.get(info.index).insideCluster )
					continue;
				open.add( info.index );
			}
		}
	}

	private void findLocalGraphForNode( int target , List<ChessboardCorner> corners ) {
		LNode ntarget = nodes.grow();
		ntarget.reset();
		ntarget.index = target;
		nnSearch.findNearest(corners.get(target),100*100,maxNeighbors,nnResults);

		// remove neighbors which are at the wrong angle
		if ( !filterNeighbors(corners.get(target),ntarget))
			return;

		// use these flags to see which neighbor has already been connected to and avoid having multiple edges
		// connect to the same neighbor
		connected.resize(ntarget.neighbors.size);
		connected.fill(false);

		// find the initial seed for the local connections
		if( !findBestPair(corners.get(target),ntarget)) {
			return;
		}

		// find the other two, if possible
		findClosestToParallel(ntarget,0);
		findClosestToParallel(ntarget,1);

		// Edges can have multiple solutions if its ambiguous. Add alternatives to the edges here
//		addSimilarNodesToConnections(ntarget);

		if( corners.get(target).distance(2432.78,1487.83) < 3 ) {
			HACK_interesting = target;
			System.out.println("Interesting: connections = "+ntarget.countEdges());
		}
	}

	/**
	 * Finds the two neighbors which have an acute angle which is closest to 90 degrees and their orientation
	 * is close enough to a 90 degree offset. Their distance from the target also needs to be about the same.
	 * Prefer pairs which are closer to the target.
	 * @return true if a pair was found
	 */
	boolean findBestPair( ChessboardCorner target , LNode tnode ) {
		double bestScore = Double.MAX_VALUE;

		// index in neighborhood
		int seedA = -1;
		int seedB = -1;

		for (int idxa = 0; idxa < tnode.neighbors.size(); idxa++) {
			LocalInfo a = tnode.neighbors.get(idxa);

			double dx = a.x - target.x;
			double dy = a.y - target.y;

			double angleA = Math.atan2(dy,dx);
			double distanceA = Math.sqrt(dx*dx + dy*dy);

			for (int idxb = idxa+1; idxb < tnode.neighbors.size(); idxb++) {
				LocalInfo b = tnode.neighbors.get(idxb);

				dx = b.x - target.x;
				dy = b.y - target.y;

				double angleB = Math.atan2(dy,dx);
				double distanceB = Math.sqrt(dx*dx + dy*dy);

				// Angle should be 90 degrees apart
				double angleDistance = UtilAngle.dist(angleA,angleB);

				double angleError = UtilAngle.dist(angleDistance,Math.PI/2);

				if( angleError <= directionTol) {
					// score is a weighted average of angle error and fractional difference in distance
					double aveDist = (distanceA+distanceB)/2.0;

					double score = 0.1*angleError/Math.PI; // max value of 0.1
					score += Math.abs(distanceA-distanceB)/Math.max(distanceA,distanceB); // max value of 1
					// score now is an error metric with 0 being perfect
					score = (1.0+score)*aveDist;

					if( score < bestScore ) {
						bestScore = score;
						seedA = idxa;
						seedB = idxb;
					}
				}
			}
		}

		if( seedA != -1 ) {
			// make them as used already
			connected.data[seedA] = true;
			connected.data[seedB] = true;
			tnode.edges[0] = connectionsStorage.grow();
			tnode.edges[0].reset();
			tnode.edges[0].src = tnode;
			tnode.edges[0].dst.add(seedA);
			tnode.edges[1] = connectionsStorage.grow();
			tnode.edges[1].reset();
			tnode.edges[1].src = tnode;
			tnode.edges[1].dst.add(seedB);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * filter list to only results that have the expected orientation. The expected orientation
	 * should be perpendicular to the target's orientation.
	 */
	private boolean filterNeighbors(ChessboardCorner target, LNode node ) {

		double expectedOri = UtilAngle.boundHalf(target.orientation+Math.PI/2.0);

		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> r = nnResults.get(i);
			double angle = UtilAngle.distHalf(r.point.orientation,expectedOri);
			if( angle <= orientationTol ) {
				ChessboardCorner c_i = r.point;
				LocalInfo info = node.neighbors.grow();
				info.reset();
				info.index = r.index;
				info.direction = Math.atan2(c_i.y-target.y,c_i.x-target.x);
				info.distance = Math.sqrt(r.distance); // NN distance function returns Euclidean distance squared
				info.set(r.point.x,r.point.y);
			}
		}

		return node.neighbors.size() >= 2;
	}

	/**
	 * searches for the node which is closest to being parallel, in the opposite direction, and about the same
	 * distance.
	 *
	 * @param edgeIdx Which edge is it looking at a match for
	 */
	private void findClosestToParallel( LNode ntarget, int edgeIdx ) {
		// At this point in time there's only one connection per edge
		LConnections connections = ntarget.edges[edgeIdx];
		LocalInfo neighbor = ntarget.neighbors.get(connections.dst.get(0));
		double distance = neighbor.distance;
		// we are only concerned with nodes pointed in the opposite direction
		double direction = UtilAngle.bound(neighbor.direction+Math.PI);

		double bestScore = Double.MAX_VALUE;
		int bestIndexNhb = -1;

		for (int idxNhb = 0; idxNhb < ntarget.neighbors.size(); idxNhb++) {
			if( connected.get(idxNhb) )
				continue;

			LocalInfo n = ntarget.neighbors.get(idxNhb);

			// see if the orientation error is within tolerance
			double errorDir = UtilAngle.dist(direction,n.direction);
			if( errorDir > directionTol)
				continue;

			// distance error is a fractional error but dampened for close up points where a pixel can result
			// in a large error
			double errorDist = Math.abs(distance-n.distance)/(Math.max(distance,n.distance)+2.0);

			double score = (errorDir + 0.1)*(errorDist+0.1);

			if( score < bestScore ) {
				bestScore = score;
				bestIndexNhb = idxNhb;
			}
		}

		// If it found a match add a new edge for it
		if( bestIndexNhb != -1 ) {
			connected.set(bestIndexNhb,true);
			LConnections c = connectionsStorage.grow();
			c.reset();
			c.src = ntarget;
			c.dst.add( bestIndexNhb );

			int edgeIdx1 = (edgeIdx+2)%4;
			ntarget.edges[edgeIdx1] = c;
		}
	}

	/**
	 * For each edge in the node see if there's another node in the filtered list which would be within tolerance
	 * and a possible alternative to the one which was added.
	 */
	private void addSimilarNodesToConnections(LNode node) {
		for (int edgeIdx = 0; edgeIdx < 4; edgeIdx++) {
			LConnections connections = node.edges[edgeIdx];
			if( connections == null )
				continue;

			if( connections.dst.size != 1 )
				throw new IllegalArgumentException("BUG!");
			LocalInfo target = node.neighbors.get( connections.dst.get(0) );

			boolean ambiguous = false;
			for (int idxNhbA = 0; idxNhbA < node.neighbors.size; idxNhbA++) {
				if( connected.get(idxNhbA))
					continue;

				LocalInfo a = node.neighbors.get(idxNhbA);
				if( a == target )
					continue;
				double angleDiff = UtilAngle.dist(target.direction,a.direction);
				if( angleDiff > orientationTol ) {
					continue;
				}

				double distErr = Math.abs(target.distance-a.distance)/target.distance;
				if( distErr <= distanceTol ) {
					ambiguous = true;
					connections.dst.add( idxNhbA );
					connected.set(idxNhbA,true);
				}
			}

			// Note which edges are ambiguous so that this ambiguity can be resolved later
			if( ambiguous ) {
				ambiguousEdges.add( connections );
			}
		}
	}

	/**
	 * If a connection between two nodes is only one direction remove it.
	 */
	private void pruneNonMutualConnections() {
		for (int idxSrc = 0; idxSrc < nodes.size; idxSrc++) {
			LNode src = nodes.get(idxSrc);

			for (int j = 0; j < 4; j++) {
				LConnections connections = src.edges[j];
				if( connections == null )
					continue;

				// iterate in reverse so items and be removed and not screw up the index
				for (int idxSrcConn = connections.dst.size - 1; idxSrcConn >= 0; idxSrcConn--) {
					LocalInfo dstInfo = src.neighbors.get( connections.dst.get(idxSrcConn));
					LNode dst = nodes.get( dstInfo.index );

					// If dst has no connection to 'src' then remove the connection in src to dst
					if( dst.findEdgeIdx(src.index) == -1 ) {
						connections.dst.remove(idxSrcConn);
					}
				}

				// No more connections left so remove the edge.
				if( connections.dst.size == 0 ) {
					src.edges[j] = null;
				}
			}
		}
	}

	/**
	 * Disconnect nodes which are not valid. This includes nodes with only one connection.
	 */
	private void disconnectInvalidNodes() {
		// Iterate until there are no more changes.
		boolean changed = true;
		while( changed ) {
			changed = false;
			for (int idxNode = 0; idxNode < nodes.size; idxNode++) {
				LNode n = nodes.get(idxNode);

				// to be part of a grid it needs to have two edges 90 degrees apart
				if( (n.edges[0] == null && n.edges[1] == null) ||
						(n.edges[1] == null && n.edges[3] == null) )
				{
					// remove all of this nodes connections. Effectively removing it from the graph

					for (int j = 0; j < 4; j++) {
						LConnections connections = n.edges[j];
						if (connections == null)
							continue;
						for (int idxC = 0; idxC < connections.dst.size; idxC++) {
							LNode dst = nodes.get(n.neighbors.get(connections.dst.get(idxC)).index);
							if (!dst.removeConnection(idxNode)) {
								throw new RuntimeException("disconnect invalid: "+dst.index+" not connected to "+n.index);
							}
						}
						n.edges[j] = null;
						changed = true;
					}
				}

			}
		}
	}

	/**
	 * It is possible from two corners to represent the same element in a local cluster. This situation is detected
	 * by seeing if more than one node is within X distance of each other. If that is detected then one of them
	 * is removed based on an energy minimization function.
	 */
	private void findAndResolveAllAmbiguity() {
		List<LNode> candidates = new ArrayList<>();

		for (int idxEdge = 0; idxEdge < ambiguousEdges.size(); idxEdge++) {
			LConnections conn = ambiguousEdges.get(idxEdge);
			// see if the ambiguity was already resolved
			if( conn.dst.size <= 1 )
				continue;

			// Create a list of the nodes which are being considered
			candidates.clear();
			for (int idxDst = 0; idxDst < conn.dst.size; idxDst++) {
				LocalInfo info = conn.src.neighbors.get( conn.dst.get(idxDst) );
				candidates.add( nodes.get(info.index) );
			}

			resolveAmbiguity(candidates);
		}
	}

	/**
	 * Disconnect all but one of the nodes in the list. Keep the node with the most edges and if there is a tie
	 * pick the one with the best score.
	 *
	 */
	private void resolveAmbiguity( List<LNode> candidates) {
		// out of all the choices, select just one to keep
		int keepIndex = selectNodeToKeep(candidates);
		if( keepIndex == -1 )
			throw new RuntimeException("BUG");

		// Disconnect the others from the graph
		for (int i = 0; i < candidates.size(); i++) {
			if( keepIndex == i )
				continue;
			disconnectNode(candidates.get(i));
		}
	}

	private int selectNodeToKeep( List<LNode> candidates ) {
		int bestIndex = -1;
		int bestEdges = 0;
		double bestError = Double.MAX_VALUE;

		for (int i = 0; i < candidates.size(); i++) {
			LNode n = candidates.get(i);

			int numEdges = n.countEdges();

			if( numEdges > bestEdges ) {
				bestEdges = numEdges;
				bestError = ambiguityError(n);
				bestIndex = i;
			} else if( numEdges == bestEdges ) {
				double error = ambiguityError(n);
				if( error < bestError ) {
					bestError = error;
					bestIndex = i;
				}
			}
		}
		return bestIndex;
	}

	/**
	 * Computes an error for edges pointing towards this node. The farther they are from ideal the larger
	 * the error will be.
	 */
	private double ambiguityError(LNode n ) {
		double error = 0;

		// NOTE: It might be better here to compute the error relative to the best fit corner at each edge
		//       if there are multiple options

		// compute errors for edges which should be 90 degrees off
		for (int i = 0,j=3; i < 4; j=i,i++) {
			if( n.edges[i] == null || n.edges[j] == null )
				continue;
			double dirI = n.getInfoForEdge(i).direction;
			double dirJ = n.getInfoForEdge(j).direction;

			double d = UtilAngle.dist(dirI,dirJ);
			error += Math.abs(d-Math.PI/2.0);
		}

		// These should be 180 degrees off
		for (int i = 0,j=2; i < 2; i++,j++) {
			if( n.edges[i] == null || n.edges[j] == null )
				continue;
			double dirI = n.getInfoForEdge(i).direction;
			double dirJ = n.getInfoForEdge(j).direction;

			double d = UtilAngle.dist(dirI,dirJ);
			error += Math.abs(d-Math.PI)*3.0; // this is more important. straight line should be straight
		}

		return error;
	}

	/**
	 * Removes all connection to the targeted node
	 */
	private void disconnectNode( LNode target ) {
		for (int j = 0; j < 4; j++) {
			LConnections connections = target.edges[j];
			if( connections == null )
				continue;
			for (int idxC = 0; idxC < connections.dst.size; idxC++) {
				LNode dst = nodes.get(target.neighbors.get(connections.dst.get(idxC)).index);
				if (!dst.removeConnection(target.index)) {
					throw new RuntimeException("disconnect node: "+dst.index+" not connected to "+target.index+". " +
							"Probably more than one edge connected to target");
				}
			}
			target.edges[j] = null;
		}
	}

	/**
	 * Found clusters of chessboard patterns
	 */
	public FastQueue<ChessboardCornerGraph> getOutputClusters() {
		return clusters;
	}

	public double getOrientationTol() {
		return orientationTol;
	}

	public void setOrientationTol(double orientationTol) {
		this.orientationTol = orientationTol;
	}

	public double getDirectionTol() {
		return directionTol;
	}

	public void setDirectionTol(double directionTol) {
		this.directionTol = directionTol;
	}

	public double getDistanceTol() {
		return distanceTol;
	}

	public void setDistanceTol(double distanceTol) {
		this.distanceTol = distanceTol;
	}

	// TODO remove (x,y) COORDINATES FROM LOCAL INFO?
	public static class LocalInfo extends Point2D_F64 {
		/**
		 * Index of this corner the node list
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

		public void reset() {
			index = -1;
			direction = Double.NaN;
			distance = Double.NaN;
		}
	}

	private static class LNode {
		/**
		 * Index of the corner that this node represents
		 */
		public int index;

		/**
		 * List of connections to other nodes. There will be 2,3 or 4 edges.
		 */
		public LConnections[] edges = new LConnections[4];

		/**
		 * Information on its local neighborhood
		 */
		public FastQueue<LocalInfo> neighbors = new FastQueue<>(4,LocalInfo.class, LocalInfo::new);

		/**
		 * Indicates if the node has been added to a cluster or not
		 */
		public boolean insideCluster = false;

		public void reset() {
			neighbors.reset();
			index = -1;
			insideCluster = false;
			for (int i = 0; i < 4; i++) {
				edges[i] = null;
			}
		}

		/**
		 * Returns the index of the edge with a connection to the corner index
		 */
		public int findEdgeIdx(int index ) {
			for (int i = 0; i < 4; i++) {
				LConnections c = edges[i];
				if( c == null )
					continue;
				for (int j = 0; j < c.dst.size; j++) {
					if( neighbors.get(c.dst.data[j]).index == index )
						return i;
				}
			}

			return -1;
		}

		/**
		 * Removes all edges which connect
		 * @return true if a connection was removed and false if it.
		 */
		public boolean removeConnection( int indexNode ) {
			boolean found = false;
			for (int i = 3; i >= 0 ; i--) {
				LConnections c = edges[i];
				if( c == null )
					continue;
				for (int j = c.dst.size-1; j >= 0; j--) {
					if( neighbors.get(c.dst.data[j]).index == indexNode ) {
						if( found )
							throw new RuntimeException("BUG!");
						found = true;
						c.dst.remove(j);
					}
				}
				// nothing is connected to this edge any more so remove it
				if( c.dst.size==0) {
					edges[i] = null;
				}
			}

			return found;
		}

		public int countEdges() {
			int total = 0;
			for (int i = 0; i < 4; i++) {
				if( edges[i] != null )
					total++;
			}
			return total;
		}

		public LocalInfo getInfoForEdge( int which ) {
			return neighbors.get( edges[which].dst.get(0));
		}

		@Override
		public int hashCode() {
			return index;
		}
	}

	private static class LConnections {
		LNode src;

		/**
		 * The neighbor's index
		 */
		public GrowQueue_I32 dst = new GrowQueue_I32();

		public void reset() {
			src = null;
			dst.reset();
		}
	}
}
