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

	double orientationTol = 0.1;
	double acuteTol = 0.1;
	double distanceTol = 0.2;

	public void process(List<ChessboardCorner> corners ) {
		// TODO for each corner find either the two or for adjacent corners which are about the same distance
		//      from this corner and 90 degrees offset

		// TODO Connect these local graphs into a proper grid
	}

	private void foo( int target , List<ChessboardCorner> corners ) {
		// TODO find the
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
