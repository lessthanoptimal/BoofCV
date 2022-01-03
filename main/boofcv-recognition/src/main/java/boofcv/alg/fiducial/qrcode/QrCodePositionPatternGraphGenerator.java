/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.fiducial.calib.squares.SquareGraph;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * Collects position patterns together based on their relative orientation
 *
 * @author Peter Abeles
 */
public class QrCodePositionPatternGraphGenerator implements VerbosePrint {

	/** maximum QR code version that it can detect */
	@Getter @Setter int maxVersionQR = QrCode.MAX_VERSION;

	SquareGraph graph = new SquareGraph();

	// Nearest Neighbor Search related variables
	private final NearestNeighbor<SquareNode> nn = FactoryNearestNeighbor.kdtree(new SquareNode.KdTreeSquareNode());
	private final NearestNeighbor.Search<SquareNode> search = nn.createSearch();
	private final DogArray<NnData<SquareNode>> searchResults = new DogArray(NnData::new);

	// Workspace for checking to see if two squares should be connected
	protected LineSegment2D_F64 lineA = new LineSegment2D_F64();
	protected LineSegment2D_F64 lineB = new LineSegment2D_F64();
	protected LineSegment2D_F64 connectLine = new LineSegment2D_F64();
	protected Point2D_F64 intersection = new Point2D_F64();

	public QrCodePositionPatternGraphGenerator() {}

	public QrCodePositionPatternGraphGenerator( int maxVersionQR ) {
		this.maxVersionQR = maxVersionQR;
	}

	/**
	 * Connects the found position patterns into a graph
	 *
	 * @param positionPatterns Found position patterns.
	 */
	public void process( List<PositionPatternNode> positionPatterns ) {
		// Reset the graph and compute node information
		graph.reset();

		// Add items to NN search
		nn.setPoints((List)positionPatterns, false);

		for (int i = 0; i < positionPatterns.size(); i++) {
			PositionPatternNode f = positionPatterns.get(i);

			// The QR code version specifies the number of "modules"/blocks across the marker is
			// A position pattern is 7 blocks. A version 1 qr code is 21 blocks. Each version past one increments
			// by 4 blocks. The search is relative to the center of each position pattern, hence the - 7
			double maximumQrCodeWidth = f.largestSide*(17 + 4*maxVersionQR - 7.0)/7.0;
			double searchRadius = 1.2*maximumQrCodeWidth; // search 1/2 the width + some fudge factor
			searchRadius *= searchRadius;

			// Connect all the finder patterns which are near by each other together in a graph
			search.findNearest(f, searchRadius, Integer.MAX_VALUE, searchResults);

			if (searchResults.size > 1) {
				for (int j = 0; j < searchResults.size; j++) {
					NnData<SquareNode> r = searchResults.get(j);

					if (r.point == f) continue; // skip over if it's the square that initiated the search

					considerConnect(f, r.point);
				}
			}
		}
	}

	/**
	 * Connects the 'candidate' node to node 'n' if they meet several criteria. See code for details.
	 */
	void considerConnect( SquareNode node0, SquareNode node1 ) {
		// Find the side on each line which intersects the line connecting the two centers
		lineA.a = node0.center;
		lineA.b = node1.center;

		int intersection0 = graph.findSideIntersect(node0, lineA, intersection, lineB);
		connectLine.a.setTo(intersection);
		int intersection1 = graph.findSideIntersect(node1, lineA, intersection, lineB);
		connectLine.b.setTo(intersection);

		if (intersection1 < 0 || intersection0 < 0) {
			return;
		}

		double side0 = node0.sideLengths[intersection0];
		double side1 = node1.sideLengths[intersection1];

		// it should intersect about in the middle of the line

		double sideLoc0 = connectLine.a.distance(node0.square.get(intersection0))/side0;
		double sideLoc1 = connectLine.b.distance(node1.square.get(intersection1))/side1;

		if (Math.abs(sideLoc0 - 0.5) > 0.35 || Math.abs(sideLoc1 - 0.5) > 0.35)
			return;

		// see if connecting sides are of similar size
		if (Math.abs(side0 - side1)/Math.max(side0, side1) > 0.25) {
			return;
		}

		// Checks to see if the two sides selected above are closest to being parallel to each other.
		// Perspective distortion will make the lines not parallel, but will still have a smaller
		// acute angle than the adjacent sides
		if (!graph.almostParallel(node0, intersection0, node1, intersection1)) {
			return;
		}

		double ratio = Math.max(node0.smallestSide/node1.largestSide,
				node1.smallestSide/node0.largestSide);

//		System.out.println("ratio "+ratio);
		if (ratio > 1.3)
			return;

		double angle = graph.acuteAngle(node0, intersection0, node1, intersection1);
		double score = lineA.getLength()*(1.0 + angle/0.1);

		graph.checkConnect(node0, intersection0, node1, intersection1, score);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
	}
}
