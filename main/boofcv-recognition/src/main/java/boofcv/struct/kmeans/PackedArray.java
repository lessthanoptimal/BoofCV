/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.kmeans;

import org.ddogleg.struct.LArrayAccessor;

/**
 * Interface for objects which are stored in a dense array instead as individual elements. This is typically
 * implemented internally as a structure of array format.
 *
 * @author Peter Abeles
 */
public interface PackedArray<T> extends LArrayAccessor<T> {
	/**
	 * Resets the array's size to be zero
	 */
	void reset();

	/**
	 * Ensure there is enough space to store 'numElements' before the internal array
	 * needs to grow. Does not change the size
	 *
	 * @param numElements Minimum number of elements allocated to the array
	 */
	void reserve( int numElements );

	/**
	 * Adds a copy of element to the array
	 * @param element (Input) The element which is copied then added
	 */
	void addCopy( T element );
}
