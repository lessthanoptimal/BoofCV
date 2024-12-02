/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.packed;

import boofcv.struct.PackedArray;
import lombok.Getter;
import org.ddogleg.struct.DogArray_F64;

public abstract class PackedArray_F64<T> implements PackedArray<T> {
	/** Degrees of freedom */
	protected final int DOF;

	/** Stores tuple in a single continuous array */
	@Getter protected final DogArray_F64 array;

	public PackedArray_F64( int DOF, int length ) {
		this.DOF = DOF;
		this.array = new DogArray_F64(length);
	}

	@Override public void reset() {
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*DOF);
	}

	@Override public int size() {
		return array.size/DOF;
	}

	@Override public boolean isEquals( PackedArray<T> b ) {return array.isEquals(((PackedArray_F64<T>)b).array);}

	/** True if the two arrays are equal to within the specified tolerance */
	public boolean isEquals( PackedArray<T> b, double tol ) {return array.isEquals(((PackedArray_F64<T>)b).array, tol);}
}
