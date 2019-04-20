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
import boofcv.misc.BoofMiscOps;
import georegression.metric.UtilAngle;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;

/**
 * From a set of {@link ChessboardCorner ChessboardCorners} find all the chessboard grids in view. Assumptions
 * about the grids size are not made at this stage.
 *
 * Algorithmic Steps:
 * <ol>
 *     <li>Nearest neighbor search for each corners. For each corner, matches are put into parallel and perpendicular sets</li>
 *     <li>Identify ambiguous corners that are close to each other and pick be most intense corner</li>
 *     <li>Prune corners from each nodes parallel and perpendicular sets if they are not mutual</li>
 *     <li>Select 2 to 4 connections for each corner using projective invariant properties</li>
 *     <li>Find connected sets of vertexes and convert into output format</li>
 * </ol>
 *
 * At a high level, a graph is formed where each vertex (a corner) can have at most 4 edges (connections) that
 * describe the relationship between two vertexes. If there is no distortion then each edge will be 90 degrees
 * apart radially. "Error" functions are use through out the code to decide which edge or vertex is best to select.
 * Error functions are not always true errors and just means they are functions where the best value is a minimum.
 * Whenever possible, errors are computed using projective invariant properties, which is how it can handle large
 * amounts of distortion. Hard thresholds are used to reduce computational complexity and weird edge cases, thus
 * are typically very generous. Lens distortion is not explicitly modeled. Instead fuzzy thresholds and error functions
 * are used to account for it.
 *
 * Chessboard and geometric properties used:
 * <ul>
 *     <li>Orientation of corners alternates by 90 degrees</li>
 *     <li>A corner can only be connected to a corner with an orientation perpendicular to it</li>
 *     <lI>Between any two adjacent connections there must lie a corner with an orientation that is parallel.</lI>
 *     <li>Without perspective distortion, corners are laid out in a grid pattern</li>
 *     <li>Assume projective transform, with fuzzy parameters to account for lens distortion</li>
 *     <li>Straight lines are straight</li>
 *     <li>Order of vector angles from a point to another point does not change</li>
 *     <li>Order of intersections along a line does not change</li>
 * </ul>
 *
 * @author Peter Abeles
 */
public class ChessboardCornerClusterFinder2 {

	// Tolerance for deciding if two directions are the same.
	double directionTol =0.60;
	// Tolerance for deciding of two corner orientations are the same.
	double orientationTol =0.45;
	// Tolerance for how close two corners need to be to be considered ambiguous. Relative
	double ambiguousTol = 0.25;

	// automatically computed
	double parallelTol; // angle tolerance for parallel lines. smaller than direction

	// Number of nearest neighbors it will search. It's assumed that the feature detector does a very
	// good job removing false positives, meaning that tons of features do not need to be considered
	int maxNeighbors=20; // 8 is minimum number given perfect data.

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
	TupleI32 tuple3 = new TupleI32();

	// Used to convert the internal graph into the output clusters
	GrowQueue_I32 c2n = new GrowQueue_I32(); // corner index to output node index
	GrowQueue_I32 n2c = new GrowQueue_I32(); // output node index to corner index
	GrowQueue_I32 open = new GrowQueue_I32(); // list of corner index's which still need ot be processed

	// Work space to store distances from NN searched to find median distance
	GrowQueue_F64 distanceTmp = new GrowQueue_F64();
	QuickSort_F64 sorter = new QuickSort_F64(); // use this instead of build in to minimize memory allocation

	List<ChessboardCorner> corners;

	public ChessboardCornerClusterFinder2() {
		setDirectionTol(directionTol);
	}

	public void process(List<ChessboardCorner> corners ) {
		this.corners = corners;

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
			findVertexNeighbors(vertexes.get(i),corners);
		}

		// If more than one vertex's are near each other, remove
		handleAmbiguousVertexes(corners);
		printDualGraph();

		// Prune connections which are not mutual
		for (int i = 0; i < vertexes.size; i++) {
			Vertex v = vertexes.get(i);
			v.pruneNonMutal(EdgeType.PARALLEL);
			v.pruneNonMutal(EdgeType.PERPENDICULAR);
		}
//		printDualGraph();

		// Select the final 2 to 4 connections from perpendicular set
		// each pair of adjacent perpendicular edge needs to have a matching parallel edge between them
		// Use each perpendicular edge as a seed and select the best one
		for (int idx = 0; idx < vertexes.size(); idx++) {
			selectConnections(vertexes.get(idx));
		}
		printConnectionGraph();

		for (int i = 0; i < vertexes.size; i++) {
			Vertex v = vertexes.get(i);
			v.pruneNonMutal(EdgeType.CONNECTION);
		}

		convertToOutput(corners);
	}

	/**
	 * Prints the graph. Used for debugging the code.
	 */
	public void printDualGraph() {
		System.out.println("============= Dual");
		int l = BoofMiscOps.numDigits(vertexes.size);
		String format = "%"+l+"d";

		for( Vertex n : vertexes.toList() ) {
			ChessboardCorner c = corners.get(n.index);
			System.out.printf("["+format+"] {%3.0f, %3.0f} ->  90[ ",n.index,c.x,c.y);
			for (int i = 0; i < n.perpendicular.size(); i++) {
				Edge e = n.perpendicular.get(i);
				System.out.printf(format+" ",e.dst.index);
			}
			System.out.println("]");
			System.out.print("                -> 180[ ");
			for (int i = 0; i < n.parallel.size(); i++) {
				Edge e = n.parallel.get(i);
				System.out.printf(format+" ",e.dst.index);
			}
			System.out.println("]");
		}
	}

	public void printConnectionGraph() {
		System.out.println("============= Connection");
		int l = BoofMiscOps.numDigits(vertexes.size);
		String format = "%"+l+"d";

		for( Vertex n : vertexes.toList() ) {
			ChessboardCorner c = corners.get(n.index);
			System.out.printf("["+format+"] {%3.0f, %3.0f} -> [ ",n.index,c.x,c.y);
			for (int i = 0; i < n.connections.size(); i++) {
				Edge e = n.connections.get(i);
				System.out.printf(format+" ",e.dst.index);
			}
			System.out.println("]");
		}
	}

	void findVertexNeighbors(Vertex target  , List<ChessboardCorner> corners ) {
		if( target.index == 18 ) {
			System.out.println("Vertex Neighbors "+target.index);
		}

		ChessboardCorner targetCorner = corners.get(target.index);
		nnSearch.findNearest(corners.get(target.index),Double.MAX_VALUE,maxNeighbors,nnResults);

		// storage distances here to find median distance of closest neighbors
		distanceTmp.reset();

		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> r = nnResults.get(i);
			if( r.index == target.index) continue;

			distanceTmp.add( r.distance );

			double oriDiff = UtilAngle.distHalf( targetCorner.orientation , r.point.orientation );

			Edge edge = edges.grow();
			boolean parallel;
			if( oriDiff <= orientationTol) { // see if it's parallel
				parallel = true;
			} else if( Math.abs(oriDiff-Math.PI/2.0) <= orientationTol) { // see if it's perpendicular
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
			boolean remove;
			EdgeSet edgeSet;
			if( parallel ) {
				// test to see if direction and orientation are aligned or off by 90 degrees
				remove = directionDiff > 2* directionTol && Math.abs(directionDiff-Math.PI/2.0) > 2* directionTol;
				edgeSet = target.parallel;
			} else {
				// should be at 45 degree angle
				remove = Math.abs(directionDiff-Math.PI/4.0) > 2* directionTol;
				edgeSet = target.perpendicular;
			}

			if( remove ) {
				edges.removeTail();
				continue;
			}

			edgeSet.add(edge);
		}

		// only want local neighbors. If it's a corner there should be at least 3 right next to the node. Unless
		// there's a large cluster of noise this should work
		if( distanceTmp.size == 0 ) {
			target.medianDistance = 0;
		} else {
			sorter.sort(distanceTmp.data, distanceTmp.size);
			int idx = Math.min(3,distanceTmp.size-1);
			target.medianDistance = Math.sqrt(distanceTmp.data[idx]); // NN distance is Euclidean squared
		}
	}

	void handleAmbiguousVertexes(List<ChessboardCorner> corners) {
		List<Vertex> candidates = new ArrayList<>();
		for (int idx = 0; idx < vertexes.size(); idx++) {
			Vertex target = vertexes.get(idx);

			double threshold = target.medianDistance*ambiguousTol;

			candidates.clear();
			// only need to search parallel since perpendicular nodes won't be confused for the target
			for (int i = 0; i < target.parallel.size(); i++) {
				Edge c = target.parallel.get(i);
				if( c.distance <= threshold ) {
					candidates.add(c.dst);
				}
			}

			if( candidates.size() > 0 ) {
				candidates.add(target);

				int bestIndex = -1;
				double bestScore = 0;

				System.out.println("==== Resolving ambiguity. src="+target.index);
				for (int i = 0; i < candidates.size(); i++) {
					Vertex v = candidates.get(i);
					System.out.println("   candidate = "+v.index);
					double intensity = corners.get(v.index).intensity;
					if( intensity > bestScore ) {
						bestScore = intensity;
						bestIndex = i;
					}
				}
				System.out.println("==== Resolved ambiguity. Selected "+candidates.get(bestIndex).index);

				for (int i = 0; i < candidates.size(); i++) {
					if( i == bestIndex )
						continue;
					removeReferences(candidates.get(i),EdgeType.PARALLEL);
					removeReferences(candidates.get(i),EdgeType.PERPENDICULAR);
				}
			}
		}
	}

	/**
	 * Go through all the vertexes that 'remove' is connected to and remove that link. if it is
	 * in the connected list swap it with 'replaceWith'.
	 */
	void removeReferences( Vertex remove , EdgeType type ) {
		EdgeSet removeSet = remove.getEdgeSet(type);
		for (int i = removeSet.size()-1; i >= 0; i--) {
			Vertex v = removeSet.get(i).dst;
			EdgeSet setV = v.getEdgeSet(type);
			// remove the connection from v to 'remove'. Be careful since the connection isn't always mutual
			// at this point
			int ridx = setV.find(remove);
			if( ridx != -1 )
				setV.edges.remove(ridx);
		}
		removeSet.reset();
	}

	/**
	 * Select the best 2,3, or 4 perpendicular vertexes to connect to.
	 */
	void selectConnections( Vertex target ) {
		// There needs to be at least two corners
		if( target.perpendicular.size() <= 1 )
			return;

		if( target.index == 16 ) {
			System.out.println("ASDSAD");
		}
		System.out.println("======= Connecting "+target.index);

		double bestError = Double.MAX_VALUE;
		List<Edge> bestSolution = target.connections.edges;
		List<Edge> solution = new ArrayList<>();

		for (int i = 0; i < target.perpendicular.size(); i++) {
			Edge e = target.perpendicular.get(i);
			solution.clear();
			solution.add(e);

			double error = 0;
			double sumDistance = solution.get(0).distance;
			double minDistance = sumDistance;

			if( !findNext(i,target.parallel,target.perpendicular,Double.NaN,results) ) {
				continue;
			}

			error += results.error;
			solution.add(target.perpendicular.get(results.index));
			sumDistance += solution.get(1).distance;
			minDistance = Math.min(minDistance,solution.get(1).distance);

			// Use knowledge that solution[0] and solution[2] form a line.
			// Lines are straight under projective distortion
			if( findNext(results.index,target.parallel,target.perpendicular,solution.get(0).direction,results) ) {
				error += results.error;
				solution.add(target.perpendicular.get(results.index));
				sumDistance += solution.get(2).distance;
				minDistance = Math.min(minDistance,solution.get(2).distance);


				// Use knowledge that solution[1] and solution[3] form a line.
				if( findNext(results.index,target.parallel,target.perpendicular,solution.get(1).direction,results) ) {
					error += results.error;
					solution.add(target.perpendicular.get(results.index));
					sumDistance += solution.get(3).distance;
					minDistance = Math.min(minDistance,solution.get(3).distance);
				}
			}

			// TODO some how make prefer larger if all things basically equal
			// favor more corners being added, but not too much
//			error = (error + 3)/(1+2*solution.size());

			error = (sumDistance+minDistance)/(solution.size()*solution.size());

			System.out.println("  first="+solution.get(0).dst.index+"  size="+solution.size()+" error="+error);

			if( error < bestError ) {
				bestError = error;
				bestSolution.clear();
				bestSolution.addAll(solution);
			}
		}
	}

	boolean findNext( int firstIdx , EdgeSet splitterSet , EdgeSet candidateSet , double parallel,
					  SearchResults results ) {
		Edge e0 = candidateSet.get(firstIdx);

		results.index = -1;
		results.error = Double.MAX_VALUE;
		boolean checkParallel = !Double.isNaN(parallel);

		for (int i = 0; i < candidateSet.size(); i++) {
			if( i == firstIdx )
				continue;

			// stop considering edges when they are more than 180 degrees away
			Edge eI = candidateSet.get(i);
			double distanceCCW = UtilAngle.distanceCCW(e0.direction,eI.direction);
			if( distanceCCW >= Math.PI*0.9 ) // Multiplying by 0.9 helped remove a lot of bad matches
				continue;                    // It was pairing up opposite corners under heavy perspective distortion

			// It should be parallel to a previously found line
			if( checkParallel ) {
				double a = UtilAngle.boundHalf(eI.direction);
				double b = UtilAngle.boundHalf(parallel);
				double distanceParallel = UtilAngle.distHalf(a,b);
				if( distanceParallel > parallelTol ) {
					continue;
				}
			}

			// find the perpendicular corner which splits these two edges and also find the index of
			// the perpendicular sets which points towards the splitter.
			if( !findSplitter(e0.direction,eI.direction,splitterSet,e0.dst.perpendicular,eI.dst.perpendicular,tuple3) )
				continue;

			double acute0 = UtilAngle.dist(
					candidateSet.get(firstIdx).direction,
					e0.dst.perpendicular.get(tuple3.b).direction);
			double error0 = UtilAngle.dist(acute0,Math.PI/2.0);

			if( error0 > Math.PI*0.3 )
				continue;

			double acute1 = UtilAngle.dist(
					candidateSet.get(i).direction,
					eI.dst.perpendicular.get(tuple3.c).direction);
			double error1 = UtilAngle.dist(acute1,Math.PI/2.0);

			if( error1 > Math.PI*0.3 )
				continue;

			// The quadrilateral with the smallest area is most often the best solution. Area is more expensive
			// so the perimeter is computed instead. one side is left off since all of them have that side
			double error = e0.dst.perpendicular.get(tuple3.b).distance;
			error += eI.dst.perpendicular.get(tuple3.c).distance;
			error += eI.distance;

			if( error < results.error ) {
				results.error = error;
				results.index = i;
			}
		}

		return results.index != -1;
	}

	boolean findSplitter(double ccw0 , double ccw1 ,
						 EdgeSet master , EdgeSet other1 , EdgeSet other2 ,
						 TupleI32 output ) {

		double bestDistance = Double.MAX_VALUE;

		for (int i = 0; i < master.size(); i++) {
			// select the splitter
			Edge me = master.get(i);

			// TODO decide if this is helpful or not
			// check that it lies between these two angles
			if( UtilAngle.distanceCCW(ccw0 , me.direction) > Math.PI*0.9 ||
					UtilAngle.distanceCW(ccw1,me.direction) > Math.PI*0.9 )
				continue;

			// Find indexes which point towards the splitter corner
			int idxB = other1.find(me.dst);
			if( idxB == -1 )
				continue;

			double thetaB = UtilAngle.distanceCCW(ccw0 , other1.get(idxB).direction);
			if( thetaB < 0.05 )
				continue;

			int idxC = other2.find(me.dst);
			if( idxC == -1 )
				continue;

			double thetaC = UtilAngle.distanceCW(ccw1 , other2.get(idxC).direction);
			if( thetaC < 0.05 )
				continue;

			// want it to be closer and without parallel lines
			double error = me.distance/(0.5+Math.min(thetaB,thetaC));

			if( error < bestDistance ) {
				bestDistance = error;
				output.a = i;
				output.b = idxB;
				output.c = idxC;
			}
		}

		// Reject the best solution if it doesn't form a convex quadrilateral
		if( bestDistance < Double.MAX_VALUE ) {
			// 2 is CCW of 1, and since both of them are pointing towards the splitter we know
			// how to compute the angular distance
			double pointingB = other1.edges.get(output.b).direction;
			double pointingC = other2.edges.get(output.c).direction;
			return UtilAngle.distanceCW(pointingB, pointingC) < Math.PI;
		} else {
			return false;
		}
	}

	/**
	 * Converts the internal graphs into unordered chessboard grids.
	 */
	void convertToOutput(List<ChessboardCorner> corners) {

		c2n.resize(corners.size());
		n2c.resize(vertexes.size());
		open.reset();
		n2c.fill(-1);
		c2n.fill(-1);

		for (int seedIdx = 0; seedIdx < vertexes.size; seedIdx++) {
			Vertex seedN = vertexes.get(seedIdx);
			if( seedN.insideCluster )
				continue;
			ChessboardCornerGraph graph = clusters.grow();
			graph.reset();

			// traverse the graph and add all the nodes in this cluster
			growCluster(corners, seedIdx, graph);

			// Connect the nodes together in the output graph
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				Vertex n = vertexes.get( n2c.get(i) );

				for (int j = 0; j < n.connections.size(); j++) {
					int edgeCornerIdx = n.connections.get(j).dst.index;
					int outputIdx = c2n.get( edgeCornerIdx );
					if( outputIdx == -1 ) {
						throw new IllegalArgumentException("Edge to node not in the graph. c="+edgeCornerIdx);
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
			int cornerIdx = open.pop();
			Vertex v = vertexes.get(cornerIdx);

			// make sure it hasn't already been processed
			if( v.insideCluster )
				continue;
			v.insideCluster = true;

			// Create the node in the output cluster for this corner
			ChessboardCornerGraph.Node gn = graph.growCorner();
			c2n.data[cornerIdx] = gn.index;
			n2c.data[gn.index] = cornerIdx;
			gn.set(corners.get(cornerIdx));

			// Add to the open list all the edges which haven't been processed yet;
			for (int i = 0; i < v.connections.size(); i++) {
				Vertex dst = v.connections.get(i).dst;
				if( dst.insideCluster )
					continue;
				open.add( dst.index );
			}
		}
	}

	public FastQueue<ChessboardCornerGraph> getOutputClusters() {
		return clusters;
	}

	public double getDirectionTol() {
		return directionTol;
	}

	public void setDirectionTol(double directionTol) {
		this.directionTol = directionTol;
		this.parallelTol = directionTol /2;
	}

	public int getMaxNeighbors() {
		return maxNeighbors;
	}

	public void setMaxNeighbors(int maxNeighbors) {
		this.maxNeighbors = maxNeighbors;
	}

	public double getOrientationTol() {
		return orientationTol;
	}

	public void setOrientationTol(double orientationTol) {
		this.orientationTol = orientationTol;
	}

	public double getAmbiguousTol() {
		return ambiguousTol;
	}

	public void setAmbiguousTol(double ambiguousTol) {
		this.ambiguousTol = ambiguousTol;
	}

	public static class SearchResults {
		public int index;
		public double error;
	}

	public static class TupleI32 {
		public int a,b,c;
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
		public EdgeSet connections = new EdgeSet();

		/**
		 * Used when computing output. Indicates that the vertex has already been processed.
		 */
		public boolean insideCluster;

		/**
		 * Used to determine if a point is ambiguous with this one
		 */
		public double medianDistance;

		public void reset() {
			index = -1;
			parallel.reset();
			perpendicular.reset();
			connections.reset();
			insideCluster = false;
		}

		public void pruneNonMutal( EdgeType which ) {
			EdgeSet set = getEdgeSet(which);

			for (int i = set.edges.size()-1; i >= 0; i-- ) {
				Vertex dst = set.edges.get(i).dst;
				EdgeSet dstSet = dst.getEdgeSet(which);

				if( -1 == dstSet.find(this) ) {
					set.edges.remove(i);
				}
			}
		}

		public EdgeSet getEdgeSet( EdgeType which ) {
			switch( which ) {
				case PARALLEL: return parallel;
				case PERPENDICULAR: return perpendicular;
				case CONNECTION: return connections;
			}
			throw new RuntimeException("BUG!");
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
				double errorDist = Math.abs(targetDist-e.distance)/Math.max(targetDist,e.distance);

				if( errorCCW > maxErrorCCW || errorDist > maxErrorDist )
					continue;

				// angle error should be minimized more than distance error
				double error = 2*errorCCW/maxErrorCCW;
				if( error < result.error ) {
					result.error = error;
					result.index = i;
				}
			}

			return result.index != -1;
		}

		public void add( Edge e ) {
			edges.add(e);
		}

		public Edge get( int i ) {
			return edges.get(i);
		}

		public void set( int i , Edge e ) {
			edges.set(i,e);
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

	private enum EdgeType {
		PARALLEL,PERPENDICULAR,CONNECTION
	}
}
