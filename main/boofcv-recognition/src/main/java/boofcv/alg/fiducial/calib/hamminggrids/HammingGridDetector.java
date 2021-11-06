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

package boofcv.alg.fiducial.calib.hamminggrids;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.fiducial.square.DetectFiducialSquareHamming;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.nn.alg.KdTreeDistance;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.FastArray;
import org.ejml.data.DMatrixRMaj;

import java.util.Arrays;

/**
 * Detector for a grid of {@link boofcv.alg.fiducial.square.DetectFiducialSquareHamming Square-Hamming} markers.
 * Commonly used for camera calibration and enable partial views to be used due to the self identifying patterns.
 * Despite error correction. miss identifications of patterns are possible and increasingly frequent as distance
 * increases. To help compensate for this, a majority vote is taken for the largest consistent grid of connected
 * (adjacent) squares and that is what is returned. If a square is found to be inconsistent its corners are not
 * returned.
 *
 * The ID's of each marker in the grid incrementally increase in a row-major order. The first marker can be configured
 * and doesn't always need to be zero. Markers with an ID out of bounds are simply discarded.
 *
 * @author Peter Abeles
 */
public class HammingGridDetector<T extends ImageGray<T>> {
	/** Number distance between the side of two adjacent squares. Units of side length */
	@Getter @Setter public double distanceBetweenSquares = 0.5;

	/** Shape of the grid composed of square markers. */
	@Getter @Setter public int rows, cols;

	/** ID of first grid in top left corner */
	@Getter @Setter public int offsetID = 0;

	DetectFiducialSquareHamming<T> markerDetector;

	DogArray<Node> nodes = new DogArray<>(Node::new, Node::reset);

	// Nearest Neighbor Search related variables
	private NearestNeighbor<NodeCorner> nn = FactoryNearestNeighbor.kdtree(new NodeCornerDistance());
	private NearestNeighbor.Search<NodeCorner> search = nn.createSearch();
	private DogArray<NnData<NodeCorner>> searchResults = new DogArray(NnData::new);
	// Storage for all the corners on each square. Used to initialize NN search
	private FastArray<NodeCorner> allCorners = new FastArray<>(NodeCorner.class);

	//------------ Homography Estimation
	Estimate1ofEpipolar estimateHomography = FactoryMultiView.homographyTLS();
	DogArray<AssociatedPair> pairs = new DogArray<>(AssociatedPair::new);
	DMatrixRMaj H = new DMatrixRMaj(3, 3); // storage for homography

	public HammingGridDetector() {
		// specify corner locations in marker coordinate. This enver changes
		pairs.resetResize(4);
		pairs.get(0).p1.setTo(0, 0);
		pairs.get(1).p1.setTo(1, 0);
		pairs.get(2).p1.setTo(1, 1);
		pairs.get(3).p1.setTo(0, 1);
	}

	public void process( T image ) {
		// Detect markers inside the image
		markerDetector.process(image);

		addDetectedMarkersToGraph();

		// Initialize nearest neighbor search for connecting squares into a grid
		nn.setPoints(allCorners.toList(), false);

		connectNodes();
		disconnectIfNotMutual();
		findConnectedClusters();
		clustersToGrids();
		pickBestGrid();
		// TODO find neighbors of each square
		// TODO vote against a square if it's not consistent as a neighbor
		// TODO if a unique ID is duplicated then remove the duplicate with most down votes until there is just one
	}

	/**
	 * Add detected markers to the list of graph nodes
	 */
	private void addDetectedMarkersToGraph() {
		// Range of expected IDs
		int markerId0 = offsetID;
		int markerId1 = offsetID + rows*cols;

		// Sound found detections and create nodes in the graph
		DogArray<FoundFiducial> found = markerDetector.getFound();
		nodes.resetResize(found.size);
		nodes.reset();
		allCorners.reset();
		allCorners.reserve(found.size*4);
		for (int i = 0; i < found.size; i++) {
			// See if the ID is inside the valid range
			long markerID = found.get(i).id;
			if (markerID < markerId0 || markerID >= markerId1)
				continue;

			// Add the marker into the graph as a node
			Node n = nodes.grow().setFiducial(i, found.get(i));
			n.addCornersToList(allCorners);
		}
	}

	/**
	 * Finds connected adjacent squares by predicting where corners should be then seeing if a single marker
	 * matches the prediction
	 */
	private void connectNodes() {
		for (int nodeIdx = 0; nodeIdx < nodes.size; nodeIdx++) {
			Node target = nodes.get(nodeIdx);

			// The corner order should be consistent because of the self identifying pattern
			pairs.get(0).p2.setTo(target.det.distortedPixels.a);
			pairs.get(1).p2.setTo(target.det.distortedPixels.b);
			pairs.get(2).p2.setTo(target.det.distortedPixels.c);
			pairs.get(3).p2.setTo(target.det.distortedPixels.d);

			// compute homography relating marker to image coordinates
			if (!estimateHomography.process(pairs.toList(), H)) {
				continue;
			}

			// TODO for each side, predict location of the two corners.
			// TODO find the best match corner within tolerance
			// TODO accept if both matches point to the same node
		}
	}

	/**
	 * Removes a connection if both nodes are not connected to each other
	 */
	private void disconnectIfNotMutual() {

	}

	/**
	 * Finds sets connected nodes to form clusters
	 */
	private void findConnectedClusters() {

	}

	/**
	 * From the clusters find the largest consistent grid
	 */
	private void clustersToGrids() {

	}

	/**
	 * From the remaining grids pick the best one to use as output
	 */
	private void pickBestGrid() {

	}

	/**
	 * Observed corner on a detected marker. Used in nearest neighbor search.
	 */
	private static class NodeCorner extends Point2D_F64 {
		public Node node;

		public NodeCorner( Node node ) {this.node = node;}
	}

	/** Distance function used by KD-Tree */
	private static class NodeCornerDistance implements KdTreeDistance<NodeCorner> {
		@Override public double distance( NodeCorner a, NodeCorner b ) {return a.distance2(b);}

		@Override public double valueAt( NodeCorner point, int index ) {return index == 0 ? point.x : point.y;}

		@Override public int length() {return 2;}
	}

	private static class Node {
		// index in the array
		public int index;
		// connections. 0=up, 1=right, 2=down, 3=left
		public final Node[] neighbors = new Node[4];
		// Detected fiducial
		public final FoundFiducial det = new FoundFiducial();

		public final NodeCorner[] corners = new NodeCorner[4];

		public Node() {
			for (int i = 0; i < corners.length; i++) {
				corners[i] = new NodeCorner(this);
			}
		}

		public Node setFiducial( int index, FoundFiducial det ) {
			this.index = index;
			this.det.setTo(det);
			corners[0].setTo(det.distortedPixels.a);
			corners[1].setTo(det.distortedPixels.b);
			corners[2].setTo(det.distortedPixels.c);
			corners[3].setTo(det.distortedPixels.d);
			return this;
		}

		public void reset() {
			index = -1;
			Arrays.fill(neighbors, null);
			det.reset();
			for (int i = 0; i < corners.length; i++) {
				corners[i].setTo(0, 0);
			}
		}

		public void addCornersToList( FastArray<NodeCorner> list ) {
			for (int i = 0; i < corners.length; i++) {
				list.add(corners[i]);
			}
		}
	}
}
