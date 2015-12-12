/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.calib.squares;

import boofcv.misc.CircularIndex;

import java.util.List;

/**
 * Takes as input a set of unordered cross connected clusters and converts them into ordered grids with known numbers
 * of rows and columns.  The output will be valid "chessboard" pattern. When rows and columns are discussed in the
 * code below it refers to both white and black squares in the chessboard. A row that starts with a white square
 * is referred to as white and one which starts with a black square as black.
 *
 * @author Peter Abeles
 */
public class CrossClustersIntoGrids {

	// verbose debug output
	private boolean verbose = false;

	// minimum number of squares in a grid
	private int minimumElements;

	/**
	 * Configures class
	 * @param minimumElements The minimum number of elements which must be in a cluster for it to be accepted
	 */
	public CrossClustersIntoGrids(int minimumElements) {
		this.minimumElements = minimumElements;
	}

	public void process( List<List<SquareNode>> clusters ) {

	}

	protected void processCluster( List<SquareNode> cluster ) {
		SquareNode seed = null;

		for (int i = 0; i < cluster.size(); i++) {

		}
	}

	/**
	 * Trace along the edge until it can't move any more in that direction.  Returns the number of nodes
	 * it traverses, excluding the seed.
	 */
	private int findLength( SquareNode n , int corner , int sign ) {
		int length = 0;

		SquareEdge e;
		while( (e = n.edges[corner]) != null ) {
			if( e.a == n ) {
				n = e.b;
				corner = e.sideB;
			} else {
				n = e.a;
				corner = e.sideA;
			}
			sign *= -1;
			corner = CircularIndex.addOffset(corner,sign,4);
		}

		return length;
	}

}
