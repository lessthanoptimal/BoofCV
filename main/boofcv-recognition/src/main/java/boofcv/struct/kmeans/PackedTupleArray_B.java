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

import boofcv.struct.feature.TupleDesc_B;
import org.ddogleg.struct.DogArray_I32;

/**
 * Stores a set of tuples in a single continuous array. This is intended to make storage of a large number of tuples
 * more memory efficient by removing all the packaging that Java adds to a class. The memory is also continuous,
 * opening the possibility of further optimizations.
 *
 * @author Peter Abeles
 */
public class PackedTupleArray_B implements PackedArray<TupleDesc_B> {
	// degree-of-freedom, number of elements in the tuple
	public final int dof;
	// Stores tuple in a single continuous array
	public final DogArray_I32 array;
	// tuple that the result is temporarily written to
	public final TupleDesc_B temp;

	// Number of tuples stored in the array
	protected int numElements;

	// Number of integers required to store the descriptor
	protected final int numInts;

	public PackedTupleArray_B( int dof ) {
		this.dof = dof;
		this.temp = new TupleDesc_B(dof);
		this.numInts = temp.value.length;
		array = new DogArray_I32(dof);
		array.resize(0);
	}

	@Override public void reset() {
		numElements = 0;
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*numInts);
	}

	@Override public void addCopy( TupleDesc_B element ) {
		array.addAll(element.value, 0, numInts);
		numElements++;
	}

	@Override public TupleDesc_B getTemp( int index ) {
		System.arraycopy(array.data, index*numInts, temp.value, 0, numInts);
		return temp;
	}

	@Override public void getCopy( int index, TupleDesc_B dst ) {
		System.arraycopy(array.data, index*numInts, dst.value, 0, numInts);
	}

	@Override public void copy( TupleDesc_B src, TupleDesc_B dst ) {
		System.arraycopy(src.value, 0, dst.value, 0, numInts);
	}

	@Override public int size() {
		return numElements;
	}

	@Override public Class<TupleDesc_B> getElementType() {
		return TupleDesc_B.class;
	}
}
