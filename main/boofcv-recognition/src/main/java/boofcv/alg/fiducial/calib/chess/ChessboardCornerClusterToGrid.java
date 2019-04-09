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

import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph.Node;
import org.ddogleg.sorting.QuickSort_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Given a chessboard corner cluster find the grid which it matches. Orientation of the grid will be found
 * up to any symmetric ambiguities. If there is any ambiguities the top-left corner of the image will have
 * corners with a lower index.
 *
 * @author Peter Abeles
 */
public class ChessboardCornerClusterToGrid {
	QuickSort_F64 sorter = new QuickSort_F64();
	double[] directions = new double[4];
	int[] order = new int[4];
	Node[] tmpEdges = new Node[4];
	GrowQueue_B marked = new GrowQueue_B();
	Queue<Node> open = new LinkedList<>(); // FIFO queue

	/**
	 * Puts cluster nodes into grid order and computes the number of rows and columns. If the cluster is not
	 * a complete grid this function will fail and return false
	 * @param cluster (Input) cluster. This will be modified.
	 * @param info (Output) grid size
	 * @return true if successful or false if it failed
	 */
	public boolean convert( ChessboardCornerGraph cluster , GridInfo info ) {
		// default to an invalid value to ensure a failure doesn't go unnoticed.
		info.cols = info.rows = -1;
		orderEdges(cluster);

		return true;
	}

	/**
	 * Puts the edges in CCW order and aligns edge indexes into pairs.
	 */
	public void orderEdges( ChessboardCornerGraph cluster ) {
		sortEdgesCCW(cluster.corners);
		alignEdges(cluster.corners);
	}

	/**
	 * Enforces the rule that an edge in node A has an edge in node B that points back to A at index (i+2)%4.
	 */
	private void alignEdges(FastQueue<Node> corners) {
		open.clear();
		open.add( corners.get(0) );

		marked.resize(corners.size);
		marked.fill(false);

		marked.set(corners.get(0).index,true);

		while( !open.isEmpty() ) {
			Node na = open.remove();

			// examine each neighbor and see the neighbor is correctly aligned
			for (int i = 0; i < 4; i++) {
				if( na.edges[i] == null ) {
					continue;
				}
				// Compute which index should be an edge pointing back at 'na'
				int j = (i+2)%4;

				Node nb = na.edges[i];
				if( marked.get(nb.index) ) {
					if( nb.edges[j] != na )
						throw new RuntimeException("BUG! node has been processed and its edges do not align.");
					continue;
				}

				// Rotate edges
				boolean failed = true;
				for (int attempt = 0; attempt < 4; attempt++) {
					if( nb.edges[j] != na ) {
						nb.rotateEdgesDown();
					} else {
						failed = false;
						break;
					}
				}
				if( failed )
					throw new RuntimeException("BUG! Can't align edges");
				marked.set(nb.index,true);
			}
		}
	}

	private void sortEdgesCCW(FastQueue<Node> corners) {
		for (int nodeIdx = 0; nodeIdx < corners.size; nodeIdx++) {
			Node na = corners.get(nodeIdx);

			for (int i = 0; i < 4; i++) {
				order[i] = i;
				tmpEdges[i] = na.edges[i];
				if( na.edges[i] == null ) {
					directions[i] = Double.MAX_VALUE;
				} else {
					Node nb = na.edges[i];

					directions[i] = Math.atan2(nb.y-na.y,nb.x-nb.x);
				}
			}

			sorter.sort(directions,0,4,order);
			for (int i = 0; i < 4; i++) {
				na.edges[i] = tmpEdges[ order[i] ];
			}
		}
	}

	public static class GridInfo {
		public int rows,cols;
	}
}
