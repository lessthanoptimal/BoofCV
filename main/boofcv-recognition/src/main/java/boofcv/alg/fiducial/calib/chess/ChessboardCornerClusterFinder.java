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
import boofcv.struct.image.ImageGray;
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
 *     <li>Prune non mutual connections and save a list of modified vertexes</li>
 *     <li>Use known graph to find better edges for modified vertexes</li>
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
@SuppressWarnings({"WeakerAccess", "ForLoopReplaceableByForEach"})
public class ChessboardCornerClusterFinder<T extends ImageGray<T>> {

	// TODO prune fake cluster borders by seeing of the edge continues a little bit past

	// Tolerance for deciding if two directions are the same. 0 to 1. Higher is more tolerant
	private double directionTol = 0.8;
	// Tolerance for deciding of two corner orientations are the same. Radians
	private double orientationTol = 0.50;
	// Tolerance for how close two corners need to be to be considered ambiguous. Relative
	private double ambiguousTol = 0.25;

	// Number of nearest neighbors it will search. It's assumed that the feature detector does a very
	// good job removing false positives, meaning that tons of features do not need to be considered
	private int maxNeighbors=20; // 8 is minimum number given perfect data.
	private double maxNeighborDistance=Double.MAX_VALUE; // maximum distance away (pixels Euclidean squared) a neighbor can be

	// Computes the intensity of the line which connects two corners
	private ChessboardCornerEdgeIntensity<T> computeConnInten;
	// Threshold relative to corner intensity used to prune. If <= 0 then this test is disabled
	private double thresholdEdgeIntensity = 0.05;

	// Data structures for the crude graph
	private FastQueue<Vertex> vertexes = new FastQueue<>(Vertex.class,true);
	private FastQueue<Edge> edges = new FastQueue<>(Edge.class,true);
	private FastQueue<LineInfo> lines = new FastQueue<>(LineInfo.class,true);

	// data structures for nearest neighbor search
	private NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	private NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	private FastQueue<NnData<ChessboardCorner>> nnResults = new FastQueue(NnData.class,true);

	// Output. Contains a graph of connected corners
	private FastQueue<ChessboardCornerGraph> clusters = new FastQueue<>(ChessboardCornerGraph.class,true);

	// predeclared storage for results
	private SearchResults results = new SearchResults();
	private TupleI32 tuple3 = new TupleI32();

	// Used to convert the internal graph into the output clusters
	private GrowQueue_I32 c2n = new GrowQueue_I32(); // corner index to output node index
	private GrowQueue_I32 n2c = new GrowQueue_I32(); // output node index to corner index
	private GrowQueue_I32 open = new GrowQueue_I32(); // list of corner index's which still need ot be processed

	// Work space to store distances from NN searched to find median distance
	private GrowQueue_F64 distanceTmp = new GrowQueue_F64();
	private QuickSort_F64 sorter = new QuickSort_F64(); // use this instead of build in to minimize memory allocation

	// reference to input corners for debugging
	private List<ChessboardCorner> corners;

	// Workspace for pruning invalid
	private List<Vertex> openVertexes = new ArrayList<>();
	private List<Vertex> dirtyVertexes = new ArrayList<>();

	// Workspace for connecting vertices
	private List<Edge> bestSolution = new ArrayList<>();
	private List<Edge> solution = new ArrayList<>();

	public ChessboardCornerClusterFinder( Class<T> imageType ) {
		this(new ChessboardCornerEdgeIntensity<>(imageType));
	}

	public ChessboardCornerClusterFinder(ChessboardCornerEdgeIntensity<T> computeConnInten) {
		this.computeConnInten = computeConnInten;
		setDirectionTol(directionTol);
	}

	/**
	 * Processes corners and finds clusters of chessboard patterns
	 *
	 * @param corners Detected chessboard patterns. Not modified.
	 */
	public void process( T image , List<ChessboardCorner> corners ) {
		this.corners = corners;

		// reset internal data structures
		vertexes.reset();
		edges.reset();
		lines.reset();
		clusters.reset();
		computeConnInten.setImage(image);

		// Create a vertex for each corner
		for (int idx = 0; idx < corners.size(); idx++) {
			Vertex v = vertexes.grow();
			v.reset();
			v.index = idx;
		}

		// Initialize nearest-neighbor search.
		nn.setPoints(corners,true);

		// TODO Should edges always be mutual now?

		// Connect corners to each other based on relative distance on orientation
		for (int i = 0; i < corners.size(); i++) {
			findVertexNeighbors(vertexes.get(i),corners);
		}

		// If more than one vertex's are near each other, pick one and remove the others
		handleAmbiguousVertexes(corners);
//		printDualGraph();

		// use edge intensity to prune connections
		if( thresholdEdgeIntensity > 0 ) {
			pruneConnectionsByIntensity(corners);
		}
//		printDualGraph();

		// Select the final 2 to 4 connections from perpendicular set
		// each pair of adjacent perpendicular edge needs to have a matching parallel edge between them
		// Use each perpendicular edge as a seed and select the best one
		for (int idx = 0; idx < vertexes.size(); idx++) {
			selectConnections(vertexes.get(idx));
		}
//		printConnectionGraph();

		// Connects must be mutual to be accepted. Keep track of vertexes which were modified
		dirtyVertexes.clear();
		for (int i = 0; i < vertexes.size; i++) {
			Vertex v = vertexes.get(i);
			int before = v.connections.size();
			v.pruneNonMutal(EdgeType.CONNECTION);
			if( before != v.connections.size() ) {
				dirtyVertexes.add(v);
				v.marked = true;
			}
		}

//		printConnectionGraph();
		// attempt to recover from poorly made decisions in the past from the greedy algorithm
		repairVertexes();

		// Prune non-mutual edges again. Only need to consider dirty edges since it makes sure that the new
		// set of connections is a super set of the old
		for (int i = 0; i < dirtyVertexes.size(); i++) {
			dirtyVertexes.get(i).pruneNonMutal(EdgeType.CONNECTION);
			dirtyVertexes.get(i).marked = false;
		}

		// Final clean up to return just valid grids
		disconnectInvalidVertices();

		// Name says it all
		convertToOutput(corners);
	}

//	public void check() {
//		System.out.println("Running check");
//		int total = 0;
//		for( LineInfo line : lines.toList() ) {
//			if( line.isDisconnected() || line.parallel )
//				continue;
//
//			total++;
//			Vertex va = line.endA.dst;
//			Vertex vb = line.endB.dst;
//
//			ChessboardCorner ca = corners.get(va.index);
//			ChessboardCorner cb = corners.get(vb.index);
//
//			if( ca.distance(2524.6,2431.1) < 1.5 && cb.distance(3184.9,2365.3) < 1.5 ) {
//				System.out.println("Survived "+line.intensity+" "+line.parallel);
//			} else if( cb.distance(2524.6,2431.1) < 1.5 && ca.distance(3184.9,2365.3) < 1.5 ) {
//				System.out.println("Survived "+line.intensity+" "+line.parallel);
//			}
//		}
//		System.out.println("total perp "+total);
//	}

	/**
	 * Computes edge intensity and prunes connections if it's too low relative
	 *
	 */
	protected void pruneConnectionsByIntensity(List<ChessboardCorner> corners) {

		for (int i = 0; i < lines.size; i++) {
			LineInfo line = lines.get(i);

			// TODO prune parallel lines
			if( line.isDisconnected() )
				continue;

			Vertex va = line.endA.dst;
			Vertex vb = line.endB.dst;

			ChessboardCorner ca = corners.get(va.index);
			ChessboardCorner cb = corners.get(vb.index);

			double contrast = (ca.constrast + cb.constrast)/2;

			if( line.parallel ) {
				// TODO explain why edge intensity for parallel lines is a bad idea. blurred images, extreme distortion, ..etc
				line.intensity = 1.0 - computeConnInten.longitudinalEdge(ca, cb, line.endA.direction) / contrast;
			} else {
				line.intensity = computeConnInten.process(ca, cb, line.endA.direction) / contrast;
			}

			if( ca.distance(2524.6,2431.1) < 1.5 && cb.distance(3184.9,2365.3) < 1.5 ) {
				System.out.println("So intenense "+line.intensity+" "+line.parallel);
			} else if( cb.distance(2524.6,2431.1) < 1.5 && ca.distance(3184.9,2365.3) < 1.5 ) {
				System.out.println("So intenense "+line.intensity+" "+line.parallel);
			}

			if( line.intensity < thresholdEdgeIntensity ) {
				if( line.parallel ) {
//					if( !va.parallel.remove(line) )
//						throw new RuntimeException("BUG");
//					if( !vb.parallel.remove(line) )
//						throw new RuntimeException("BUG");
//					line.disconnect();
				} else {
					if( !va.perpendicular.remove(line) )
						throw new RuntimeException("BUG");
					if( !vb.perpendicular.remove(line) )
						throw new RuntimeException("BUG");
					line.disconnect();
				}
			}
		}
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

	/**
	 * Use nearest neighbor search to find closest corners. Split those into two groups, parallel and
	 * perpendicular.
	 */
	void findVertexNeighbors(Vertex va  , List<ChessboardCorner> corners ) {
		ChessboardCorner targetCorner = corners.get(va.index);
		// distance is Euclidean squared
		double maxDist = Double.MAX_VALUE==maxNeighborDistance?maxNeighborDistance:maxNeighborDistance*maxNeighborDistance;
		nnSearch.findNearest(corners.get(va.index),maxDist,maxNeighbors,nnResults);

		// storage distances here to find median distance of closest neighbors
		distanceTmp.reset();

//		if( targetCorner.distance(3003,1887) < 2 )
//			System.out.println("Egads");

		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> r = nnResults.get(i);
			if( r.index == va.index) continue;

			Vertex vb = vertexes.get(r.index );

			if( targetCorner.distance(2524.6,2431.1) < 1.5 && r.point.distance(3184.9,2365.3) < 1.5 ) {
				System.out.println("There! A");
//			} else if( r.point.distance(604.89,709.55) < 1.5 && targetCorner.distance(772.5,861.26) < 1.5 ) {
//				System.out.println("There! B");
			}

			distanceTmp.add( r.distance );

			double oriDiff = UtilAngle.distHalf( targetCorner.orientation , r.point.orientation );
			boolean parallel = oriDiff <= Math.PI/4.0;
			double orientationError;
			if( parallel ) {
				// error is distance from zero
				orientationError = oriDiff;
				// make sure it wasn't already connected
				if( vb.parallel.find(va) != -1 )
					continue;
			} else {
				orientationError = Math.abs(oriDiff-Math.PI/2.0);
				if( vb.perpendicular.find(va) != -1 )
					continue;
			}

			// if it's off from the ideal by too much then it's neither parallel or perpendicular
			if( orientationError > orientationTol ) {
				continue;
			}

			// Use the relative angles of orientation and direction to prune more obviously bad matches
			double dx = r.point.x - targetCorner.x;
			double dy = r.point.y - targetCorner.y;

			LineInfo line = lines.grow();
			line.reset();
			line.distance = Math.sqrt(r.distance);
			line.parallel = parallel;

			Edge ea = edges.grow(); // from a to b
			Edge eb = edges.grow(); // from b to a

			ea.reset();
			ea.dst = vb;
			ea.direction = Math.atan2(dy,dx);
			ea.line = line;

			eb.reset();
			eb.dst = va;
			eb.direction = Math.atan2(-dy,-dx);
			eb.line = line;

			// need to save a reference back the line's end points
			line.endA = ea;
			line.endB = eb;

			if( parallel ) {
				// test to see if direction and orientation are aligned or off by 90 degrees
//				double errorA = Math.min(directionDiff_A,Math.abs(directionDiff_A-Math.PI/2.0));
//				double errorB = Math.min(directionDiff_B,Math.abs(directionDiff_B-Math.PI/2.0));
//				remove = errorA > directionTol*Math.PI/4;
//				remove |= errorB > directionTol*Math.PI/4;

				va.parallel.add(ea);
				vb.parallel.add(eb);
			} else {
				// should be at 45 degree angle or 135 degrees
//				double errorA = Math.min(Math.abs(directionDiff_A-Math.PI/4.0), Math.abs(directionDiff_A-3*Math.PI/4.0));
//				double errorB = Math.min(Math.abs(directionDiff_B-Math.PI/4.0), Math.abs(directionDiff_B-3*Math.PI/4.0));

//				remove = errorA > directionTol*Math.PI/4;
//				remove |= errorB > directionTol*Math.PI/4;

				va.perpendicular.add(ea);
				vb.perpendicular.add(eb);
			}
		}

		// Compute the distance of the closest neighbors. This is used later on to identify ambiguous corners.
		// If it's a graph corner there should be at least 3 right next to the node.
		if( distanceTmp.size == 0 ) {
			va.neighborDistance = 0;
		} else {
			sorter.sort(distanceTmp.data, distanceTmp.size);
			int idx = Math.min(3,distanceTmp.size-1);
			va.neighborDistance = Math.sqrt(distanceTmp.data[idx]); // NN distance is Euclidean squared
		}
	}

	/**
	 * Identify ambiguous vertexes which are too close to each other. Select the corner with the highest intensity
	 * score and remove the rest
	 */
	void handleAmbiguousVertexes(List<ChessboardCorner> corners) {
		List<Vertex> candidates = new ArrayList<>();
		for (int idx = 0; idx < vertexes.size(); idx++) {
			Vertex target = vertexes.get(idx);

			// median distance was previously found based on the closer neighbors. In an actual chessboard
			// there are solid white and black squares with no features, in theory
			double threshold = target.neighborDistance *ambiguousTol;

			candidates.clear();
			// only need to search parallel since perpendicular nodes won't be confused for the target
			for (int i = 0; i < target.parallel.size(); i++) {
				Edge c = target.parallel.get(i);
				if( c.line.distance <= threshold ) {
					candidates.add(c.dst);
				}
			}

			if( candidates.size() > 0 ) {
				candidates.add(target);

				int bestIndex = -1;
				double bestScore = 0;

//				System.out.println("==== Resolving ambiguity. src="+target.index);
				for (int i = 0; i < candidates.size(); i++) {
					Vertex v = candidates.get(i);
//					System.out.println("   candidate = "+v.index);
					double intensity = corners.get(v.index).intensity;
					if( intensity > bestScore ) {
						bestScore = intensity;
						bestIndex = i;
					}
				}
//				System.out.println("==== Resolved ambiguity. Selected "+candidates.get(bestIndex).index);

				for (int i = 0; i < candidates.size(); i++) {
					if( i == bestIndex )
						continue;
					removeReferences(candidates.get(i),EdgeType.PARALLEL,true);
					removeReferences(candidates.get(i),EdgeType.PERPENDICULAR,true);
				}
			}
		}
	}

	/**
	 * Disconnect the edges from invalid vertices. Invalid ones have only one edge or two edges which do
	 * not connect to a common vertex, i.e. they are point 180 degrees away from each other.
	 */
	void disconnectInvalidVertices() {
		// add elements with 1 or 2 edges
		openVertexes.clear();
		for (int idxVert = 0; idxVert < vertexes.size; idxVert++) {
			Vertex n = vertexes.get(idxVert);
			if( n.connections.size() == 1 || n.connections.size()==2) {
				openVertexes.add(n);
			}
		}

		// continue until there are no changes
		while( !openVertexes.isEmpty() ) {
			dirtyVertexes.clear();
			for (int idxVert = 0; idxVert < openVertexes.size(); idxVert++) {
				boolean remove = false;
				Vertex n = openVertexes.get(idxVert);
				if( n.connections.size() == 1 ) {
					remove = true;
				} else if( n.connections.size() == 2 ) {
					Edge ea = n.connections.get(0);
					Edge eb = n.connections.get(1);

					// Look for a common vertex that isn't 'n'
					remove = true;
					for (int i = 0; i < ea.dst.connections.size(); i++) {
						Vertex va = ea.dst.connections.get(i).dst;
						if( va == n )
							continue;
						for (int j = 0; j < eb.dst.connections.size(); j++) {
							Vertex vb = ea.dst.connections.get(j).dst;
							if( va == vb ) {
								remove = false;
								break;
							}
						}
					}
				}

				if( remove ) {
					// only go through the subset referenced the disconnected. Yes there could be duplicates
					// not worth the time to fix that
					for (int i = 0; i < n.connections.size(); i++) {
						dirtyVertexes.add( n.connections.get(i).dst );
					}
					removeReferences(n,EdgeType.CONNECTION,false);
				}
			}
			openVertexes.clear();
			openVertexes.addAll(dirtyVertexes);
		}

	}

	/**
	 * Go through all the vertexes that 'remove' is connected to and remove that link. if it is
	 * in the connected list swap it with 'replaceWith'.
	 */
	void removeReferences( Vertex remove , EdgeType type , boolean disconnectLines ) {
		EdgeSet removeSet = remove.getEdgeSet(type);
		for (int i = removeSet.size()-1; i >= 0; i--) {
			Vertex v = removeSet.get(i).dst;

			if( disconnectLines )
				removeSet.get(i).line.disconnect();
			EdgeSet setV = v.getEdgeSet(type);
			// remove the connection from v to 'remove'.
			int ridx = setV.find(remove);
			if( ridx != -1 )
				setV.edges.remove(ridx);
			else
				throw new RuntimeException("EGads");
		}
		removeSet.reset();
	}

	/**
	 * Select the best 2,3, or 4 perpendicular vertexes to connect to. These are the output grid connections.
	 */
	void selectConnections( Vertex target ) {
		// There needs to be at least two corners
		if( target.perpendicular.size() <= 1 )
			return;

//		if( corners.get(target.index).distance(548,135) < 1.5 ) {
//			System.out.println("Found it. idx="+target.index);
//		}

//		if( target.index == 16 ) {
//			System.out.println("ASDSAD");
//		}
//		System.out.println("======= Connecting "+target.index);

		double bestError = Double.MAX_VALUE;
		List<Edge> bestSolution = target.connections.edges;

		// Greedily select one vertex at a time to connect to. findNext() looks at all possible
		// vertexes it can connect too and minimizes an error function based on projectively invariant features
		for (int i = 0; i < target.perpendicular.size(); i++) {
			Edge e = target.perpendicular.get(i);
			solution.clear();
			solution.add(e);

			double sumDistance = solution.get(0).line.distance;
			double minDistance = sumDistance;

			if( !findNext(i,target.parallel,target.perpendicular,Double.NaN,results) ) {
				continue;
			}

			solution.add(target.perpendicular.get(results.index));
			sumDistance += solution.get(1).line.distance;
			minDistance = Math.min(minDistance,solution.get(1).line.distance);

			// Use knowledge that solution[0] and solution[2] form a line.
			// Lines are straight under projective distortion
			if( findNext(results.index,target.parallel,target.perpendicular,solution.get(0).direction,results) ) {
				solution.add(target.perpendicular.get(results.index));
				sumDistance += solution.get(2).line.distance;
				minDistance = Math.min(minDistance,solution.get(2).line.distance);

				// Use knowledge that solution[1] and solution[3] form a line.
				if( findNext(results.index,target.parallel,target.perpendicular,solution.get(1).direction,results) ) {
					solution.add(target.perpendicular.get(results.index));
					sumDistance += solution.get(3).line.distance;
					minDistance = Math.min(minDistance,solution.get(3).line.distance);
				}
			}

			// Prefer closer valid sets of edges and larger sets.
			// the extra + minDistance was needed to bias it against smaller sets which would be more likely
			// to have a smaller average error. Division by the square of set/solution size biases it towards larger sets
			double error = (sumDistance+minDistance)/(solution.size()*solution.size());

//			System.out.println("  first="+solution.get(0).dst.index+"  size="+solution.size()+" error="+error);

			if( error < bestError ) {
				bestError = error;
				bestSolution.clear();
				bestSolution.addAll(solution);
			}
		}
	}

	/**
	 * Greedily select the next edge that will belong to the final graph.
	 *
	 * @param firstIdx Index of the edge CW of the one being considered
	 * @param splitterSet Set of edges that the splitter can belong to
	 * @param candidateSet Set of edges that the next edge can belong to
	 * @param parallel if NaN this is the angle the edge should be parallel to
	 * @param results Output.
	 * @return true if successful.
	 */
	boolean findNext( int firstIdx , EdgeSet splitterSet , EdgeSet candidateSet ,
					  double parallel,
					  SearchResults results ) {

		// maximum allowed error between the corner's angle and the edge's direction
		final double tolCornerToEdge = Math.PI*0.4;

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
				if( distanceParallel > directionTol*Math.PI/4 ) {
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

			if( error0 > tolCornerToEdge )
				continue;

			double acute1 = UtilAngle.dist(
					candidateSet.get(i).direction,
					eI.dst.perpendicular.get(tuple3.c).direction);
			double error1 = UtilAngle.dist(acute1,Math.PI/2.0);

			if( error1 > tolCornerToEdge )
				continue;

			// Find the edge from corner 0 to corner i. The direction of this vector and the corner's
			// orientation has a known relationship described by 'phaseOri'
			int e0_to_eI = e0.dst.parallel.find(eI.dst);
			if( e0_to_eI < 0 )
				continue;

			// The quadrilateral with the smallest area is most often the best solution. Area is more expensive
			// so the perimeter is computed instead. one side is left off since all of them have that side
			double error = e0.dst.perpendicular.get(tuple3.b).line.distance;
			error += eI.dst.perpendicular.get(tuple3.c).line.distance;
			error += eI.line.distance;

			if( error < results.error ) {
				results.error = error;
				results.index = i;
			}
		}

		return results.index != -1;
	}

	/**
	 * Splitter is a vertex that has an angle between two edges being considered.
	 *
	 * @param ccw0 angle of edge 0
	 * @param ccw1 angle of edge 1
	 * @param master Set of edges that contains all potential spitters
	 * @param other1 set of perpendicular edges to vertex ccw0
	 * @param other2 set of perpendicular edges to vertex ccw1
	 * @param output index of indexes for edges in master, other1, other2
	 * @return true if successful
	 */
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
			double error = me.line.distance/(0.5+Math.min(thetaB,thetaC));

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
	 * Given the current graph, attempt to replace edges from vertexes which lost them in an apparent bad decision.
	 * This is a very simple algorithm and gives up if the graph around the current node is less than perfect.
	 *
	 * 1) Can only connect to vertexes which lost edges
	 * 2) Can't modify an existing edge
	 * 3) Parallel lines must be parallel
	 * 4) If current graph can't predict direction no decision is made
	 *
	 */
	private void repairVertexes() {
//		System.out.println("######## Repair");

		// the number of dirty can increase, but we don't want to check new additions
		int totalDirty = dirtyVertexes.size();
		for (int idxV = 0; idxV < totalDirty; idxV++) {
			final Vertex v = dirtyVertexes.get(idxV);

//			System.out.println(" dirty="+v.index);

			bestSolution.clear();

			for (int idxE = 0; idxE < v.perpendicular.size(); idxE++) {
				// Assume this edge is in the solutions
				Edge e = v.perpendicular.get(idxE);

				// only can connect new modified vertexes or ones already connected too
				if( !(e.dst.marked || -1 != v.connections.find(e.dst)) ) {
					continue;
				}

//				System.out.println("  e[0].dst = "+e.dst.index);

				solution.clear();
				solution.add(e);

				// search for connections until there's no more to be found
				for (int count = 0; count < 3; count++) {
					// Find the an edge which is about to 90 degrees CCW of the previous edge 'v'
					Vertex va=null;
					double dir90 = UtilAngle.bound(e.direction+Math.PI/2.0);
					for (int i = 0; i < e.dst.connections.size(); i++) {
						Edge ei = e.dst.connections.get(i);
						if( UtilAngle.dist(ei.direction,dir90) < Math.PI/3 ) {
							va = ei.dst;
							break;
						}
					}

					// Search for an edge in v which has a connection to 'va' and is ccw of 'e'
					boolean matched = false;
					for (int i = 0; i < v.perpendicular.size(); i++) {
						Edge ei = v.perpendicular.get(i);

						if( e == ei )
							continue;

						// angle test
						double ccw = UtilAngle.distanceCCW(e.direction,ei.direction);
						if( ccw > Math.PI*0.9 )
							continue;

						if( !(ei.dst.marked || -1 != v.connections.find(ei.dst)) ) {
							continue;
						}

						// connection test
						if( ei.dst.connections.find(va) != -1 ) {
//							System.out.println("   e[i].dst = "+ei.dst.index+" va="+va.index);
							e = ei;
							solution.add(ei);
							matched = true;
							break;
						}
					}

					if( !matched )
						break;
				}

				if( solution.size() > bestSolution.size() ) {
					bestSolution.clear();
					bestSolution.addAll(solution);
				}
			}

			if( bestSolution.size() > 1 ) {
				// See if any connection that was there before is now gone. If that's the case the destination
				// will need to be checked for mutual matches
				for (int i = 0; i < v.connections.edges.size(); i++) {
					if( !bestSolution.contains(v.connections.edges.get(i)) ){
						Vertex ve = v.connections.edges.get(i).dst;
						if( !ve.marked ) {
							ve.marked = true;
							dirtyVertexes.add(ve);
						}
					}
				}

				// Save the new connections
				v.connections.edges.clear();
				v.connections.edges.addAll(bestSolution);
			}
		}
	}

	/**
	 * Converts the internal graphs into unordered chessboard grids.
	 */
	private void convertToOutput(List<ChessboardCorner> corners) {

		c2n.resize(corners.size());
		n2c.resize(vertexes.size());
		open.reset();
		n2c.fill(-1);
		c2n.fill(-1);

		for (int seedIdx = 0; seedIdx < vertexes.size; seedIdx++) {
			Vertex seedN = vertexes.get(seedIdx);
			if( seedN.marked)
				continue;
			ChessboardCornerGraph graph = clusters.grow();
			graph.reset();

			// traverse the graph and add all the nodes in this cluster
			growCluster(corners, seedN.index, graph);

			// Connect the nodes together in the output graph
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				Vertex n = vertexes.get( n2c.get(i) );

				for (int j = 0; j < n.connections.size(); j++) {
					int edgeCornerIdx = n.connections.get(j).dst.index;
					int outputIdx = c2n.get( edgeCornerIdx );
					if( outputIdx == -1 ) {
						throw new IllegalArgumentException("Edge to node not in the graph. n.idx="+n.index+" conn.idx="+edgeCornerIdx);
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
		vertexes.get(seedIdx).marked = true;
		while( open.size > 0 ) {
			int cornerIdx = open.pop();
			Vertex v = vertexes.get(cornerIdx);

			// Create the node in the output cluster for this corner
			ChessboardCornerGraph.Node gn = graph.growCorner();
			c2n.data[cornerIdx] = gn.index;
			n2c.data[gn.index] = cornerIdx;
			gn.set(corners.get(cornerIdx));

			// Add to the open list all the edges which haven't been processed yet;
			for (int i = 0; i < v.connections.size(); i++) {
				Vertex dst = v.connections.get(i).dst;
				if( dst.marked )
					continue;
				dst.marked = true;
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

	public double getMaxNeighborDistance() {
		return maxNeighborDistance;
	}

	public void setMaxNeighborDistance(double maxNeighborDistance) {
		this.maxNeighborDistance = maxNeighborDistance;
	}

	public double getThresholdEdgeIntensity() {
		return thresholdEdgeIntensity;
	}

	public void setThresholdEdgeIntensity(double thresholdEdgeIntensity) {
		this.thresholdEdgeIntensity = thresholdEdgeIntensity;
	}

	public ChessboardCornerEdgeIntensity<T> getConnectionIntensity() {
		return computeConnInten;
	}

	public FastQueue<Vertex> getVertexes() {
		return vertexes;
	}

	public FastQueue<Edge> getEdges() {
		return edges;
	}

	public FastQueue<LineInfo> getLines() {
		return lines;
	}

	public static class SearchResults {
		public int index;
		public double error;
	}

	public static class TupleI32 {
		public int a,b,c;
	}

	/**
	 * Graph vertex for a corner.
	 */
	@SuppressWarnings("WeakerAccess")
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
		public boolean marked;

		/**
		 * Used to determine if a point is ambiguous with this one
		 */
		public double neighborDistance;

		public void reset() {
			index = -1;
			parallel.reset();
			perpendicular.reset();
			connections.reset();
			marked = false;
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

	/**
	 * Collection of edges that share the same relationship with the source vertex. See {@link EdgeType}.
	 */
	public static class EdgeSet {
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

		public int find( LineInfo line ) {
			Vertex va = line.endA.dst;
			Vertex vb = line.endB.dst;

			for (int i = 0; i < edges.size(); i++) {
				Edge e = edges.get(i);
				if( e.dst == va || e.dst == vb ) {
					return i;
				}
			}

			return -1;
		}

		public boolean remove( LineInfo line ) {
			int idx = find(line);
			if( idx == -1 )
				return false;
			edges.remove(idx);
			return true;
		}
	}

	public static class Edge {

		// Descriptor of the line
		public LineInfo line;

		// pointing direction (-pi to pi) from src (x,y) to dst (x,y)
		public double direction;

		// Vertex this is connected to
		public Vertex dst;

		public void reset() {
			line = null;
			direction = Double.NaN;
			dst = null;
		}
	}

	/**
	 * Describes the relationship between two vertexes in the graph. Features are from the
	 * perspective of the src vertex.
	 */
	public static class LineInfo {
		// Image edge intensity
		public double intensity;
		// Euclidean distance between the two vertexes
		public double distance;

		// if true it connects two corners which are parallel to each other
		public boolean parallel;

		public boolean xcorner;

		public Edge endA;
		public Edge endB;

		public void reset() {
			invalidateIntensity();
			distance = Double.NaN;
			parallel = false;
			xcorner = false;
			endA = null;
			endB = null;
		}

		public boolean isIntensityInvalid() {
			return intensity == -Double.MAX_VALUE;
		}
		public void invalidateIntensity() {
			intensity = -Double.MAX_VALUE;
		}

		public void disconnect() {
			endA = endB = null;
		}

		public boolean isDisconnected() {
			return endA == null;
		}
	}

	private enum EdgeType {
		PARALLEL,PERPENDICULAR,CONNECTION
	}
}
