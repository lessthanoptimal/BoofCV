/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.GrowQueue_B;

/**
 * Graph representation of square blobs.  Each blob can be connected to at most 4 other shapes which are directly
 * adjacent of one of the sides.
 *
 * @author Peter Abeles
 */
public class SquareNode {
	public static final int RESET_GRAPH = -2;

	// polygon which this node represents.
	// cw or ccw ordering of edges doesn't matter
	public Polygon2D_F64 corners;
	// does a corner touch the border?
	public GrowQueue_B touch;

	// intersection of line 0 and 2  with 1 and 3.
	public Point2D_F64 center = new Point2D_F64();
	// length of sides. side = i and i+1
	public double sideLengths[] = new double[4];
	// the largest length
	public double largestSide;

	// marker used to indicate that this has been traversed by different algorithms
	public int graph;

	// edges in the graph.  One for each side in the shape
	public SquareEdge edges[] = new SquareEdge[4];

	/**
	 * Finds the Euclidean distance squared of the closest corner to point p
	 */
	public double distanceSqCorner( Point2D_F64 p ) {
		double best = Double.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			double d = corners.get(i).distance2(p);
			if( d < best ) {
				best = d;
			}
		}
		return best;
	}

	/**
	 * Discards previous information
	 */
	public void reset() {
		corners = null;
		touch = null;
		center.set(-1,-1);
		largestSide = 0;
		graph = RESET_GRAPH;
		for (int i = 0; i < edges.length; i++) {
			if ( edges[i] != null )
				throw new RuntimeException("BUG!");
			sideLengths[i] = 0;
		}
	}

	public void updateArrayLength() {
		if( edges.length < corners.size() ) {
			edges = new SquareEdge[corners.size()];
			sideLengths = new double[corners.size()];
		}
	}

	/**
	 * Computes the number of edges attached to this node
	 */
	public int getNumberOfConnections() {
		int ret = 0;
		for (int i = 0; i < corners.size(); i++) {
			if( edges[i] != null )
				ret++;
		}
		return ret;
	}

	public double smallestSideLength() {
		double smallest = Double.MAX_VALUE;
		for (int i = 0; i < corners.size(); i++) {
			double length = sideLengths[i];
			if( length < smallest ) {
				smallest = length;
			}
		}
		return smallest;
	}
}
