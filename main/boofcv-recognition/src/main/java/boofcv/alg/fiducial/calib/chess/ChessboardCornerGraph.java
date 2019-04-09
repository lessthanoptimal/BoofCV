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
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * A graph describing the inner corners in a chessboard patterns.
 *
 * TODO describe orientation
 *
 * @author Peter Abeles
 */
public class ChessboardCornerGraph {

	public FastQueue<Node> corners = new FastQueue<>(Node.class,true);

	public Node growCorner() {
		Node n = corners.grow();
		n.reset();
		n.index = corners.size-1;
		return n;
	}

	public Node findClosest( double x , double y ) {
		double distance = Double.MAX_VALUE;
		Node closest = null;
		for (int i = 0; i < corners.size; i++) {
			Node n = corners.get(i);
			double d = n.distance2(x,y);
			if( d < distance ) {
				distance = d;
				closest = n;
			}
		}

		return closest;
	}

	public void reset() {
		corners.reset();
	}

	public static class Node extends Point2D_F64 {
		/**
		 * Index in the node list
		 */
		public int index;
		/**
		 * Orientation of the corner feature as defined by the corner detector
		 */
		public double orientation;
		/**
		 * References to other corners. Can only be connected to 4 corners in directions approximated 90 degrees apart.
		 */
		public final Node[] edges = new Node[4];

		public void set(ChessboardCorner c) {
			super.set(c);
			this.orientation = c.orientation;
		}

		/**
		 * Rotate edges down in the array.
		 */
		public void rotateEdgesDown() {
			Node tmp = edges[0];
			for (int i = 1; i < 4; i++) {
				edges[i-1] = edges[i];
			}
			edges[3] = tmp;
		}

		public void reset() {
			orientation = Double.NaN;
			for (int i = 0; i < 4; i++) {
				edges[i] = null;
			}
		}

		public int countEdges() {
			int total = 0;

			for (int i = 0; i < 4; i++) {
				if( edges[i] != null )
					total++;
			}

			return total;
		}
	}
}
