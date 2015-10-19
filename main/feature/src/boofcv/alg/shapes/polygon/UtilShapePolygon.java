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

package boofcv.alg.shapes.polygon;

import georegression.metric.Intersection2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Utility functions used by classes which fit polygons to shapes in the image
 *
 * @author Peter Abeles
 */
public class UtilShapePolygon {

	/**
	 * Finds the intersections between the four lines and converts it into a quadrilateral
	 *
	 * @param lines Assumes lines are ordered
	 */
	public static boolean convert( LineGeneral2D_F64[] lines , Polygon2D_F64 poly ) {

		for (int i = 0; i < poly.size(); i++) {
			int j = (i + 1) % poly.size();
			if( null == Intersection2D_F64.intersection(lines[i], lines[j], poly.get(j)) )
				return false;
		}

		return true;
	}
}
