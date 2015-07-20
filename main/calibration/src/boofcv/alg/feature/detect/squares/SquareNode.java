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

package boofcv.alg.feature.detect.squares;

import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * @author Peter Abeles
 */
public class SquareNode {
	Polygon2D_F64 corners;

	// intersection of line 0 and 2  with 1 and 3.
	Point2D_F64 center = new Point2D_F64();
	// length of sides. side = i and i+1
	double sideLengths[] = new double[4];
	// the largest length
	double largestSide;

	int graph;

	SquareEdge edges[] = new SquareEdge[4];

	public void reset() {
		graph = -2;
		largestSide = 0;
		for (int i = 0; i < 4; i++) {
			edges[i] = null;
			sideLengths[i] = 0;
		}
	}

	public int getNumberOfConnections() {
		int ret = 0;
		for (int i = 0; i < 4; i++) {
			if( edges[i] != null )
				ret++;
		}
		return ret;
	}
}
