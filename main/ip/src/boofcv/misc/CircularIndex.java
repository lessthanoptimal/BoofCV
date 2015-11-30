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

package boofcv.misc;

/**
 * Function for use when referencing the index in a circular list
 *
 * @author Peter Abeles
 */
public class CircularIndex {


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
	 * Returns the smallest distance you would need to travel (positive or negative) to go from index0 to index1.
	 *
	 * @param index0 element in circular buffer
	 * @param index1 element in circular buffer
	 * @param size size of the circular buffer
	 * @return smallest distance
	 */
	public static int distance(int index0, int index1, int size) {
		if( index0 > index1 ) {
			int tmp = index0;
			index0 = index1;
			index1 = tmp;
		}
		int distance0 = index1-index0;
		int distance1 = index0 + size-index1;
		if( distance0 < distance1)
			return distance0;
		else
			return distance1;
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

