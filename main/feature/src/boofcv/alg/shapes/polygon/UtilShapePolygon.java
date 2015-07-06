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

	/**
	 * Adds a positive offset to index in a circular buffer.
	 * @param index element in circular buffer
	 * @param offset integer which is positive and less than size
	 * @param size size of the circular buffer
	 * @return new index
	 */
	public static int plusPOffset(int index, int offset, int size) {
		return (index+offset)%size;
	}

	/**
	 * Subtracts a positive offset to index in a circular buffer.
	 * @param index element in circular buffer
	 * @param offset integer which is positive and less than size
	 * @param size size of the circular buffer
	 * @return new index
	 */
	public static int minusPOffset(int index, int offset, int size) {
		index -= offset;
		if( index < 0 ) {
			return size + index;
		} else {
			return index;
		}
	}
	/**
	 * Adds offset (positive or negative) to index in a circular buffer.
	 * @param index element in circular buffer
	 * @param offset offset.  |offset| < size
	 * @param size size of the circular buffer
	 * @return new index
	 */

	public static int addOffset(int index, int offset, int size) {
		index += offset;
		if( index < 0 ) {
			return size + index;
		} else {
			return index%size;
		}
	}


	/**
	 * Returns how many elements away in the positive direction you need to travel to get from
	 * index0 to index1.
	 *
	 * @param index0 element in circular buffer
	 * @param index1 element in circular buffer
	 * @param size size of the circular buffer
	 * @return positive distance
	 */
	public static int distanceP(int index0, int index1, int size) {
		int difference = index1-index0;
		if( difference < 0 ) {
			difference = size+difference;
		}
		return difference;
	}

	/**
	 * Subtracts index1 from index0. positive number if its closer in the positive
	 * direction or negative if closer in the negative direction.  if equal distance then
	 * it will return a negative number.
	 *
	 * @param index0 element in circular buffer
	 * @param index1 element in circular buffer
	 * @param size size of the circular buffer
	 * @return new index
	 */
	public static int subtract(int index0, int index1, int size) {
		int distance = distanceP(index0, index1, size);
		if( distance >= size/2+size%2 ) {
			return distance-size;
		} else {
			return distance;
		}
	}
}
