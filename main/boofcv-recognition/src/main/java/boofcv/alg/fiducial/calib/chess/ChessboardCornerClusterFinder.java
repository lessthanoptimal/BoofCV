/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

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
@SuppressWarnings({"WeakerAccess", "ForLoopReplaceableByForEach", "NullAway.Init"})
public class ChessboardCornerClusterFinder<T extends ImageGray<T>> implements VerbosePrint {

	/** Tolerance for deciding if two directions are the same. 0 to 1. Higher is more tolerant */
	private @Getter @Setter double directionTol = 0.8;
	/** Tolerance for deciding of two corner orientations are the same. Radians */
	private @Getter @Setter double orientationTol = 0.50;
	/** Tolerance for how close two corners need to be to be considered ambiguous. Relative */
	private @Getter @Setter double ambiguousTol = 0.25;

	// Number of nearest neighbors it will search. It's assumed that the feature detector does a very
	// good job removing false positives, meaning that tons of features do not need to be considered
	private @Getter @Setter int maxNeighbors = 14; // 8 is minimum number given perfect data.
	private @Getter @Setter
	double maxNeighborDistance = Double.MAX_VALUE; // maximum distance away (pixels Euclidean squared) a neighbor can be

	/** Computes the intensity of the line which connects two corners */
	private @Getter final ChessboardCornerEdgeIntensity<T> computeConnInten;
	/** Threshold relative to corner intensity used to prune. If &le; 0 then this test is disabled */
	private @Getter @Setter double thresholdEdgeIntensity = 0.05;

	// Data structures for the crude graph
	private @Getter final DogArray<Vertex> vertexes = new DogArray<>(Vertex::new);
	private @Getter final DogArray<Edge> edges = new DogArray<>(Edge::new);
	private @Getter final DogArray<LineInfo> lines = new DogArray<>(LineInfo::new);

	// data structures for nearest neighbor search
	private final NearestNeighbor<ChessboardCorner> nn = FactoryNearestNeighbor.kdtree(new ChessboardCornerDistance());
	private final NearestNeighbor.Search<ChessboardCorner> nnSearch = nn.createSearch();
	private final DogArray<NnData<ChessboardCorner>> nnResults = new DogArray<>(NnData::new);

	/** Output. Contains a graph of connected corners */
	private @Getter final DogArray<ChessboardCornerGraph> outputClusters = new DogArray<>(ChessboardCornerGraph::new);

	// predeclared storage for results
//	private SearchResults results = new SearchResults();
//	private TupleI32 tuple3 = new TupleI32();

	// Used to convert the internal graph into the output clusters
	private final DogArray_I32 c2n = new DogArray_I32(); // corner index to output node index
	private final DogArray_I32 n2c = new DogArray_I32(); // output node index to corner index
	private final DogArray_I32 open = new DogArray_I32(); // list of corner index's which still need ot be processed

	// Work space to store distances from NN searched to find median distance
//	private DogArray_F64 distanceTmp = new DogArray_F64();
//	private QuickSort_F64 sorter = new QuickSort_F64(); // use this instead of build in to minimize memory allocation

	// reference to input corners for debugging
	private List<ChessboardCorner> corners;

	// Workspace for pruning invalid
	private final List<Vertex> openVertexes = new ArrayList<>();
	private final List<Vertex> dirtyVertexes = new ArrayList<>();

	// Workspace for connecting vertices
	private final List<Edge> bestSolution = new ArrayList<>();
	private final List<Edge> solution = new ArrayList<>();
	private final DogArray<PairIdx> pairs = new DogArray<>(PairIdx.class, PairIdx::new);
	private final DogArray_B matched = new DogArray_B();

	@Nullable PrintStream verbose = null;

	public ChessboardCornerClusterFinder( Class<T> imageType ) {
		this(new ChessboardCornerEdgeIntensity<>(imageType));
	}

	public ChessboardCornerClusterFinder( ChessboardCornerEdgeIntensity<T> computeConnInten ) {
		this.computeConnInten = computeConnInten;
		setDirectionTol(directionTol);
	}

	/**
	 * Processes corners and finds clusters of chessboard patterns
	 *
	 * @param corners Detected chessboard patterns. Not modified.
	 */
	public void process( T image, List<ChessboardCorner> corners, int numLevels ) {
		this.corners = corners;

		List<DogArray_I32> cornersInLevel = new ArrayList<>();

		initalizeStructures(image, corners, numLevels, cornersInLevel);


		// Find neighbor corners starting at low resolution layers going to high resolution
		List<ChessboardCorner> cornersUpToLevel = new ArrayList<>();
		DogArray_I32 indexesUpToLevel = new DogArray_I32();
		pyramidalFindNeighbors(corners, numLevels, cornersInLevel, cornersUpToLevel, indexesUpToLevel);

		BoofMiscOps.checkTrue(indexesUpToLevel.size == corners.size());

		if (verbose != null) {
			verbose.println("corners.size=" + corners.size() + " vertexes.size=" + vertexes.size);
			printDualGraph();
		}

		// use edge intensity to prune connections
		if (thresholdEdgeIntensity > 0) {
			pruneConnectionsByIntensity(corners);
		}
		pruneSingleConnections();

		if (verbose != null) printDualGraph();

		// If more than one vertex's are near each other, pick one and remove the others
		handleAmbiguousVertexes(corners);

		if (verbose != null) verbose.println("after ambiguous vertexes.size=" + vertexes.size);

		// TODO Change pruning of ambiguous vertexes here
		//      use incoming perpendicular edges to set distance threshold

		// Select the final 2 to 4 connections from perpendicular set
		// each pair of adjacent perpendicular edge needs to have a matching parallel edge between them
		// Use each perpendicular edge as a seed and select the best one
		for (int idx = 0; idx < vertexes.size(); idx++) {
			selectConnections(vertexes.get(idx));
		}
		if (verbose != null) printDualGraph();

		// Connects must be mutual to be accepted. Keep track of vertexes which were modified
		dirtyVertexes.clear();
		for (int i = 0; i < vertexes.size; i++) {
			Vertex v = vertexes.get(i);
			int before = v.connections.size();
			v.pruneNonMutal(EdgeType.CONNECTION);
			if (before != v.connections.size()) {
				dirtyVertexes.add(v);
				v.marked = true;
			}
		}

		if (verbose != null) printDualGraph();

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

	/**
	 * Prune vertexes with just a single connection
	 */
	private void pruneSingleConnections() {
		for (int i = 0; i < vertexes.size; i++) {
			Vertex va = vertexes.get(i);
			if (va.perpendicular.size() == 1) {
				removeReferences(va, EdgeType.PERPENDICULAR, true);
			}
		}
	}

	private void pyramidalFindNeighbors( List<ChessboardCorner> corners, int numLevels,
										 List<DogArray_I32> cornersInLevel, List<ChessboardCorner> cornersUpToLevel,
										 DogArray_I32 indexesUpToLevel ) {
		// start from top of the pyramid, which is the lowest resolution
		for (int level = numLevels - 1; level >= 0; level--) {
			DogArray_I32 levelCornerIdx = cornersInLevel.get(level);
			for (int i = 0; i < levelCornerIdx.size; i++) {
				cornersUpToLevel.add(corners.get(levelCornerIdx.get(i)));
			}
			indexesUpToLevel.addAll(levelCornerIdx);
//			System.out.println("level = "+level+" count "+levelCornerIdx.size+" total "+indexesUpToLevel.size);

			// Initialize nearest-neighbor search.
			nn.setPoints(cornersUpToLevel, true);

			// Connect corners to each other based on relative distance on orientation
			for (int i = 0; i < levelCornerIdx.size(); i++) {
				Vertex v = vertexes.get(levelCornerIdx.get(i));
				findVertexNeighbors(v, indexesUpToLevel, corners);
				// Order edges by angle to simplify later processing
				v.perpendicular.sortByAngle();
			}
		}
	}

	private void initalizeStructures( T image, List<ChessboardCorner> corners, int numLevels, List<DogArray_I32> cornersInLevel ) {
		// reset internal data structures
		vertexes.reset();
		edges.reset();
		lines.reset();
		outputClusters.reset();
		computeConnInten.setImage(image);
		cornersInLevel.clear();

		// Create a vertex for each corner
		for (int idx = 0; idx < corners.size(); idx++) {
			Vertex v = vertexes.grow();
			v.reset();
			v.index = idx;
		}

		// declare queues to store the corner index that appears in each level
		for (int level = 0; level < numLevels; level++) {
			cornersInLevel.add(new DogArray_I32());
		}

		// Add corners to the pyramid at the lowest resolution level they appear at
		for (int i = 0; i < corners.size(); i++) {
			ChessboardCorner c = corners.get(i);
			cornersInLevel.get(c.level2).add(i);
		}
	}

	/**
	 * Computes edge intensity and prunes connections if it's too low relative
	 */
	protected void pruneConnectionsByIntensity( List<ChessboardCorner> corners ) {

		for (int i = 0; i < lines.size; i++) {
			LineInfo line = lines.get(i);

			if (line.isDisconnected() || line.parallel)
				continue;

			Vertex va = Objects.requireNonNull(line.endA).dst;
			Vertex vb = Objects.requireNonNull(line.endB).dst;

			ChessboardCorner ca = corners.get(va.index);
			ChessboardCorner cb = corners.get(vb.index);

			double contrast = (ca.contrast + cb.contrast)/2;

			line.intensityRaw = computeConnInten.process(ca, cb, line.endA.direction);
			line.intensity = line.intensityRaw/contrast;

			if (line.intensity < thresholdEdgeIntensity) {
				if (!va.perpendicular.remove(line))
					throw new RuntimeException("BUG");
				if (!vb.perpendicular.remove(line))
					throw new RuntimeException("BUG");
				line.disconnect();
			}
		}
	}

	/**
	 * Prints the graph. Used for debugging the code.
	 */
	public void printDualGraph() {
		Objects.requireNonNull(verbose);
		verbose.println("============= Dual");
		int l = BoofMiscOps.numDigits(vertexes.size);
		String format = "%" + l + "d";

		for (Vertex n : vertexes.toList()) { // lint:forbidden ignore_line
			ChessboardCorner c = corners.get(n.index);
			verbose.printf("[" + format + "] px(%3.0f, %3.0f) ->  90[ ", n.index, c.x, c.y);
			for (int i = 0; i < n.perpendicular.size(); i++) {
				Edge e = n.perpendicular.get(i);
				verbose.printf(format + " ", e.dst.index);
			}
			verbose.println("]");
		}
	}

	public void printConnectionGraph() {
		System.out.println("============= Connection");
		int l = BoofMiscOps.numDigits(vertexes.size);
		String format = "%" + l + "d";

		for (Vertex n : vertexes.toList()) { // lint:forbidden ignore_line
			ChessboardCorner c = corners.get(n.index);
			System.out.printf("[" + format + "] px(%3.0f, %3.0f) -> [ ", n.index, c.x, c.y);
			for (int i = 0; i < n.connections.size(); i++) {
				Edge e = n.connections.get(i);
				System.out.printf(format + " ", e.dst.index);
			}
			System.out.println("]");
		}
	}

	/**
	 * Use nearest neighbor search to find closest corners. Split those into two groups, parallel and
	 * perpendicular.
	 */
	void findVertexNeighbors( Vertex va, DogArray_I32 indexesUpToLevel, List<ChessboardCorner> corners ) {
		ChessboardCorner targetCorner = corners.get(va.index);
		// distance is Euclidean squared
		double maxDist = Double.MAX_VALUE == maxNeighborDistance ? maxNeighborDistance : maxNeighborDistance*maxNeighborDistance;
		nnSearch.findNearest(corners.get(va.index), maxDist, maxNeighbors, nnResults);

		for (int i = 0; i < nnResults.size; i++) {
			NnData<ChessboardCorner> rb = nnResults.get(i);
			int cindex = indexesUpToLevel.get(rb.index);
			if (cindex == va.index) continue;

			Vertex vb = vertexes.get(cindex);

			double oriDiff = UtilAngle.distHalf(targetCorner.orientation, rb.point.orientation);
			boolean parallel = oriDiff <= Math.PI/4.0;
			double orientationError;
			if (parallel) {
				continue;
			} else {
				orientationError = Math.abs(oriDiff - Math.PI/2.0);
				if (vb.perpendicular.find(va) != -1)
					continue;
			}

			// if it's off from the ideal by too much then it's neither parallel or perpendicular
			if (orientationError > orientationTol) {
				continue;
			}

			// Use the relative angles of orientation and direction to prune more obviously bad matches
			double dx = rb.point.x - targetCorner.x;
			double dy = rb.point.y - targetCorner.y;

			LineInfo line = lines.grow();
			line.reset();
			line.distance = Math.sqrt(rb.distance);
			line.parallel = parallel;

			Edge ea = edges.grow(); // from a to b
			Edge eb = edges.grow(); // from b to a

			ea.reset();
			ea.dst = vb;
			ea.direction = Math.atan2(dy, dx);
			ea.line = line;

			eb.reset();
			eb.dst = va;
			eb.direction = Math.atan2(-dy, -dx);
			eb.line = line;

			// need to save a reference back the line's end points
			line.endA = ea;
			line.endB = eb;

			va.perpendicular.add(ea);
			vb.perpendicular.add(eb);
		}
	}

	/**
	 * Identify ambiguous vertexes which are too close to each other. Select the corner with the highest intensity
	 * score and remove the rest
	 */
	void handleAmbiguousVertexes( List<ChessboardCorner> corners ) {
		List<Vertex> candidates = new ArrayList<>();
		for (int idx = 0; idx < vertexes.size(); idx++) {
			Vertex target = vertexes.get(idx);
			if (target.perpendicular.size() == 0)
				continue;

			ChessboardCorner tc = corners.get(target.index);
//			if( c.distance(298.8,367.3) < 2 ) {
//				System.out.println("Inspecting")
//			}

			// Find candidates by looking at the neighbors of neighbors
			// these are the vertexes which might be confused by two near by
			candidates.clear();
			for (int i = 0; i < target.perpendicular.size(); i++) {
				Vertex a = target.perpendicular.get(i).dst;
				int targetIdx = a.perpendicular.find(target);
				double targetDir = a.perpendicular.get(targetIdx).direction;
				for (int j = 0; j < a.perpendicular.size(); j++) {
					Edge e = a.perpendicular.get(j);
					Vertex b = e.dst;
					ChessboardCorner tb = corners.get(b.index);
					if (b == target)
						continue;

					// We define too close and possibly confusing as pointing in a similar direction
					// and being at a distance not far relative to the line's length
					double acute = UtilAngle.dist(e.direction, targetDir);
					double foo = tc.distance(tb)/e.line.distance;
					if (acute < Math.PI/3 && foo < ambiguousTol && !candidates.contains(b)) {
						candidates.add(b);
					}
				}
			}
			if (candidates.size() == 0)
				continue;
			candidates.add(target);

			int bestIndex = -1;
			double bestError = 0;

//				System.out.println("==== Resolving ambiguity. src="+target.index);
			for (int i = 0; i < candidates.size(); i++) {
				Vertex v = candidates.get(i);
				ChessboardCorner a = corners.get(v.index);
				double angleError = 0;
				for (int j = 0; j < v.perpendicular.size(); j++) {
					Edge e = v.perpendicular.get(j);
					LineInfo line = v.perpendicular.get(j).line;
					ChessboardCorner b = corners.get(e.dst.index);
					double errorA = UtilAngle.distHalf(a.orientation, e.direction);
					double errorB = UtilAngle.distHalf(b.orientation, e.direction);

					angleError += line.intensityRaw/(1 + Math.abs(errorA + errorB - Math.PI/2.0));
				}
//				angleError /= v.perpendicular.size();
				// TODO take in account line angles. prefer stuff that involve parallel lines and close to 90
				//      see large shadow

//					System.out.println("   candidate = "+v.index);
				if (angleError > bestError) {
					bestError = angleError;
					bestIndex = i;
				}
			}
//				System.out.println("==== Resolved ambiguity. Selected "+candidates.get(bestIndex).index);

			for (int i = 0; i < candidates.size(); i++) {
				if (i == bestIndex)
					continue;
				removeReferences(candidates.get(i), EdgeType.PERPENDICULAR, true);
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
			if (n.connections.size() == 1 || n.connections.size() == 2) {
				openVertexes.add(n);
			}
		}

		// continue until there are no changes
		while (!openVertexes.isEmpty()) {
			dirtyVertexes.clear();
			for (int idxVert = 0; idxVert < openVertexes.size(); idxVert++) {
				boolean remove = false;
				Vertex n = openVertexes.get(idxVert);
				if (n.connections.size() == 1) {
					remove = true;
				} else if (n.connections.size() == 2) {
					Edge ea = n.connections.get(0);
					Edge eb = n.connections.get(1);

					// Look for a common vertex that isn't 'n'
					remove = true;
					for (int i = 0; i < ea.dst.connections.size(); i++) {
						Vertex va = ea.dst.connections.get(i).dst;
						if (va == n)
							continue;
						for (int j = 0; j < eb.dst.connections.size(); j++) {
							Vertex vb = ea.dst.connections.get(j).dst;
							if (va == vb) {
								remove = false;
								break;
							}
						}
					}
				}

				if (remove) {
					// only go through the subset referenced the disconnected. Yes there could be duplicates
					// not worth the time to fix that
					for (int i = 0; i < n.connections.size(); i++) {
						dirtyVertexes.add(n.connections.get(i).dst);
					}
					removeReferences(n, EdgeType.CONNECTION, false);
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
	void removeReferences( Vertex remove, EdgeType type, boolean disconnectLines ) {
		EdgeSet removeSet = remove.getEdgeSet(type);
		for (int i = removeSet.size() - 1; i >= 0; i--) {
			Vertex v = removeSet.get(i).dst;

			if (disconnectLines)
				removeSet.get(i).line.disconnect();
			EdgeSet setV = v.getEdgeSet(type);
			// remove the connection from v to 'remove'.
			int ridx = setV.find(remove);
			if (ridx != -1)
				setV.edges.remove(ridx);
			else
				throw new RuntimeException("EGads");
		}
		removeSet.reset();
	}

	// TODO Comment
	// TODO check if lines are parallel if > 2 connections
	void selectConnections( Vertex target ) {
		// TODO prefer sets of lines with similar length
		// TODO of on other side prefer that the lines be parallel

		// There needs to be at least two corners
		if (target.perpendicular.size() <= 1)
			return;

		// NOTE: What this should do is select one perpendicular, then find all the remaining. Score then as a group

		// Find max intensity of all the edges
		// It's extremely unlikely that a false positive would have a high edge score
		double maxIntensity = 0.0;
		for (int i = 0; i < target.perpendicular.size(); i++) {
			maxIntensity = Math.max(maxIntensity, target.perpendicular.get(i).line.intensity);
		}

		// go through each connection and select the one which should come after it
		pairs.reset();
		for (int i = 0; i < target.perpendicular.size(); i++) {
			// NOTE: Return value intentionally ignored since pairs needs to match up with pair indexes
			selectNext(target, maxIntensity, i, target.perpendicular, pairs.grow());
		}

		// Select the sequence which the largest score
		int bestStart = -1;
		double bestScore = 0;

		matched.resize(pairs.size);
		for (int i = 0; i < pairs.size(); i++) {
			matched.fill(false);
			double score = 0;
			int count = 1;
			PairIdx a = pairs.get(i);
			if (a.score <= 0)
				continue;
			do {
				count++;
				matched.set(a.idx0, true);
				score += a.score;
				a = pairs.get(a.idx1);
			} while (a.idx1 >= 0 && !matched.get(a.idx1) && count < 4);

			if (score > bestScore) {
				bestScore = score;
				bestStart = i;
			}
		}

		// Set the connections at this vertex to be the selected connections
		if (bestStart >= 0) {
			matched.fill(false);
			PairIdx a = pairs.get(bestStart);
			target.connections.add(target.perpendicular.edges.get(a.idx0));
			matched.set(a.idx0, true);
			do {
				matched.set(a.idx1, true);
				target.connections.add(target.perpendicular.edges.get(a.idx1));
				a = pairs.get(a.idx1);
				// iterate until there is no next or it loops or it has the max number of connections
			} while (a.idx1 >= 0 && !matched.get(a.idx1) && target.connections.size() < 4);
		}
	}

	boolean selectNext( Vertex target, double maxIntensity, int idx0, EdgeSet connections, PairIdx output ) {

		output.idx0 = idx0;
		output.idx1 = -1;
		output.score = -Double.MAX_VALUE;

		Edge edge0 = connections.get(idx0);
		for (int i = 1; i < connections.size(); i++) {
			int idx1 = (idx0 + i)%connections.size();
			Edge edge1 = connections.get(idx1);

			// Find all the common nodes between these two edges
			for (int idxA = 0; idxA < edge0.dst.perpendicular.size(); idxA++) {
				Vertex da = edge0.dst.perpendicular.get(idxA).dst;
				if (da == target)
					continue;
				for (int idxB = 0; idxB < edge1.dst.perpendicular.size(); idxB++) {
					Vertex db = edge1.dst.perpendicular.get(idxB).dst;

					// found a common, compute the score and see if it's better than the previous best
					if (da == db) {
						double score = score(target, idx0, idx1, da);
						if (score > output.score) {
							output.score = score;
							output.idx1 = idx1;
						}
					}
				}
			}
		}

		output.score *= 0.1 + edge0.line.intensity/maxIntensity;

		return output.score > 0;
	}

	private double score( Vertex tgt, int idxA, int idxB, Vertex common ) {
		// TODO comment

		double angleA = tgt.perpendicular.get(idxA).direction;
		double distA = tgt.perpendicular.get(idxA).line.distance;
		double angleB = tgt.perpendicular.get(idxB).direction;
		double distB = tgt.perpendicular.get(idxB).line.distance;

		// TODO break ties with intensity
//		double intensityA = tgt.perpendicular.get(idxA).line.intensity;
//		double intensityB = tgt.perpendicular.get(idxA).line.intensity;

		ChessboardCorner targetCorner = corners.get(tgt.index);
		ChessboardCorner commonCorner = corners.get(common.index);

		double angleC = Math.atan2(commonCorner.y - targetCorner.y, commonCorner.x - targetCorner.x);
		double distC = targetCorner.distance(commonCorner);

		// Perpendicular edges are ordered by increasing direction
		double angDistAB = UtilAngle.distanceCCW(angleB, angleA);
		double angDistAC = UtilAngle.distanceCCW(angleC, angleA);

		// See if C comes after B. If it's "before" the circle wraps around and it will appear to be after
		if (angDistAC > angDistAB)
			return -Double.MAX_VALUE;

		// it should be about perpendicular
		if (angDistAB > Math.PI*0.95)
			return -Double.MAX_VALUE;

		// Prefer a wider angle
		double angleScore = Math.min(angDistAC, angDistAB - angDistAC);

		// distA and distB should be the same length
		// prefer a distC which is closer
		double distScore = 0.1 + (Math.abs(distA - distB) + distC)/(distA + distB);
//		double distScore = Math.abs(1.0-(distB+distC*0.707)/(2*distA));

		return angleScore/distScore;
	}

	/**
	 * Given the current graph, attempt to replace edges from vertexes which lost them in an apparent bad decision.
	 * This is a very simple algorithm and gives up if the graph around the current node is less than perfect.
	 *
	 * 1) Can only connect to vertexes which lost edges
	 * 2) Can't modify an existing edge
	 * 3) Parallel lines must be parallel
	 * 4) If current graph can't predict direction no decision is made
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
				if (!(e.dst.marked || -1 != v.connections.find(e.dst))) {
					continue;
				}

//				System.out.println("  e[0].dst = "+e.dst.index);

				solution.clear();
				solution.add(e);

				// search for connections until there's no more to be found
				for (int count = 0; count < 3; count++) {
					// Find the an edge which is about to 90 degrees CCW of the previous edge 'v'
					Vertex va = null;
					double dir90 = UtilAngle.bound(e.direction + Math.PI/2.0);
					for (int i = 0; i < e.dst.connections.size(); i++) {
						Edge ei = e.dst.connections.get(i);
						if (UtilAngle.dist(ei.direction, dir90) < Math.PI/3) {
							va = ei.dst;
							break;
						}
					}
					Objects.requireNonNull(va);

					// Search for an edge in v which has a connection to 'va' and is ccw of 'e'
					boolean matched = false;
					for (int i = 0; i < v.perpendicular.size(); i++) {
						Edge ei = v.perpendicular.get(i);

						if (e == ei)
							continue;

						// angle test
						double ccw = UtilAngle.distanceCCW(e.direction, ei.direction);
						if (ccw > Math.PI*0.9)
							continue;

						if (!(ei.dst.marked || -1 != v.connections.find(ei.dst))) {
							continue;
						}

						// connection test
						if (ei.dst.connections.find(va) != -1) {
//							System.out.println("   e[i].dst = "+ei.dst.index+" va="+va.index);
							e = ei;
							solution.add(ei);
							matched = true;
							break;
						}
					}

					if (!matched)
						break;
				}

				if (solution.size() > bestSolution.size()) {
					bestSolution.clear();
					bestSolution.addAll(solution);
				}
			}

			if (bestSolution.size() > 1) {
				// See if any connection that was there before is now gone. If that's the case the destination
				// will need to be checked for mutual matches
				for (int i = 0; i < v.connections.edges.size(); i++) {
					if (!bestSolution.contains(v.connections.edges.get(i))) {
						Vertex ve = v.connections.edges.get(i).dst;
						if (!ve.marked) {
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
	private void convertToOutput( List<ChessboardCorner> corners ) {

		c2n.resize(corners.size());
		n2c.resize(vertexes.size());
		open.reset();
		n2c.fill(-1);
		c2n.fill(-1);

		for (int seedIdx = 0; seedIdx < vertexes.size; seedIdx++) {
			Vertex seedN = vertexes.get(seedIdx);
			if (seedN.marked)
				continue;
			ChessboardCornerGraph graph = outputClusters.grow();
			graph.reset();

			// traverse the graph and add all the nodes in this cluster
			growCluster(corners, seedN.index, graph);

			// Connect the nodes together in the output graph
			for (int i = 0; i < graph.corners.size; i++) {
				ChessboardCornerGraph.Node gn = graph.corners.get(i);
				Vertex n = vertexes.get(n2c.get(i));

				for (int j = 0; j < n.connections.size(); j++) {
					int edgeCornerIdx = n.connections.get(j).dst.index;
					int outputIdx = c2n.get(edgeCornerIdx);
					if (outputIdx == -1) {
						throw new IllegalArgumentException("Edge to node not in the graph. n.idx=" + n.index + " conn.idx=" + edgeCornerIdx);
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

			if (graph.corners.size <= 1)
				outputClusters.removeTail();
		}
	}

	/**
	 * Given the initial seed, add all connected nodes to the output cluster while keeping track of how to
	 * convert one node index into another one, between the two graphs
	 */
	private void growCluster( List<ChessboardCorner> corners, int seedIdx, ChessboardCornerGraph graph ) {
		// open contains corner list indexes
		open.add(seedIdx);
		vertexes.get(seedIdx).marked = true;
		while (open.size > 0) {
			int cornerIdx = open.pop();
			Vertex v = vertexes.get(cornerIdx);

			// Create the node in the output cluster for this corner
			ChessboardCornerGraph.Node gn = graph.growCorner();
			c2n.data[cornerIdx] = gn.index;
			n2c.data[gn.index] = cornerIdx;
			gn.corner = corners.get(cornerIdx);

			// Add to the open list all the edges which haven't been processed yet;
			for (int i = 0; i < v.connections.size(); i++) {
				Vertex dst = v.connections.get(i).dst;
				if (dst.marked)
					continue;
				dst.marked = true;
				open.add(dst.index);
			}
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	public static class SearchResults {
		public int index;
		public double error;
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
		 * Nodes which are close and have an orientation off by about 90 degrees
		 */
		public EdgeSet perpendicular = new EdgeSet();

		/**
		 * Final set of edges which it was decided that this vertex is connected to. Will have 2 to 4 elements.
		 */
		public EdgeSet connections = new EdgeSet();

		/**
		 * Used when computing output. Indicates that the vertex has already been processed.
		 */
		public boolean marked;

		public void reset() {
			index = -1;
			perpendicular.reset();
			connections.reset();
			marked = false;
		}

		public void pruneNonMutal( EdgeType which ) {
			EdgeSet set = getEdgeSet(which);

			for (int i = set.edges.size() - 1; i >= 0; i--) {
				Vertex dst = set.edges.get(i).dst;
				EdgeSet dstSet = dst.getEdgeSet(which);

				if (-1 == dstSet.find(this)) {
					set.edges.remove(i);
				}
			}
		}

		public EdgeSet getEdgeSet( EdgeType which ) {
			return switch (which) {
				case PERPENDICULAR -> perpendicular;
				case CONNECTION -> connections;
			};
		}

		@Override
		public String toString() {
			return "{index=" + index + " perp.size=" + perpendicular.size() + "}";
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

		public void set( int i, Edge e ) {
			edges.set(i, e);
		}

		public int size() {
			return edges.size();
		}

		public int find( Vertex v ) {
			for (int i = 0; i < edges.size(); i++) {
				if (edges.get(i).dst == v)
					return i;
			}
			return -1;
		}

		public int find( LineInfo line ) {
			Vertex va = Objects.requireNonNull(line.endA).dst;
			Vertex vb = Objects.requireNonNull(line.endB).dst;

			for (int i = 0; i < edges.size(); i++) {
				Edge e = edges.get(i);
				if (e.dst == va || e.dst == vb) {
					return i;
				}
			}

			return -1;
		}

		public boolean remove( LineInfo line ) {
			int idx = find(line);
			if (idx == -1)
				return false;
			edges.remove(idx);
			return true;
		}

		public void sortByAngle() {
			// Use Collections and not edges.sort() for compatibility with Android 23 or earlier
			Collections.sort(edges, ( o1, o2 ) -> Double.compare(o1.direction, o2.direction));
		}
	}

	@SuppressWarnings({"NullAway.Init"})
	public static class Edge {

		// Descriptor of the line
		public LineInfo line;

		// pointing direction (-pi to pi) from src (x,y) to dst (x,y)
		public double direction;

		// Vertex this is connected to
		public Vertex dst;

		@SuppressWarnings({"NullAway"})
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
		// Image edge intensity normalized by corner intensity
		public double intensity;
		// Edge intensity without normalization
		public double intensityRaw;
		// Euclidean distance between the two vertexes
		public double distance;

		// if true it connects two corners which are parallel to each other
		public boolean parallel;

		public boolean xcorner;

		public @Nullable Edge endA;
		public @Nullable Edge endB;

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
			intensityRaw = -Double.MAX_VALUE;
		}

		public void disconnect() {
			endA = endB = null;
		}

		public boolean isDisconnected() {
			return endA == null;
		}
	}

	private enum EdgeType {
		PERPENDICULAR, CONNECTION
	}

	private static class PairIdx {
		public int idx0;
		public int idx1;
		public double score;
	}
}
