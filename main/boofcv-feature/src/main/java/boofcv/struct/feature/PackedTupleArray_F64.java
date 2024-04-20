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

package boofcv.struct.feature;

import boofcv.misc.BoofLambdas;
import org.ddogleg.struct.DogArray_F64;

/**
 * Stores a set of tuples in a single continuous array. This is intended to make storage of a large number of tuples
 * more memory efficient by removing all the packaging that Java adds to a class. The memory is also continuous,
 * opening the possibility of further optimizations.
 *
 * @author Peter Abeles
 */
public class PackedTupleArray_F64 implements PackedTupleArray<TupleDesc_F64> {
	// degree-of-freedom, number of elements in the tuple
	public final int dof;
	// Stores tuple in a single continuous array
	public final DogArray_F64 array;
	// tuple that the result is temporarily written to
	public final TupleDesc_F64 temp;

	// Number of tuples stored in the array
	protected int numElements;

	public PackedTupleArray_F64( int dof ) {
		this.dof = dof;
		this.temp = new TupleDesc_F64(dof);
		array = new DogArray_F64();
		array.resize(0);
	}

	@Override public void reset() {
		numElements = 0;
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*dof);
	}

	@Override public void append( TupleDesc_F64 element ) {
		array.addAll(element.data, 0, dof);
		numElements++;
	}

	@Override public TupleDesc_F64 getTemp( int index ) {
		System.arraycopy(array.data, index*dof, temp.data, 0, dof);
		return temp;
	}

	@Override public void getCopy( int index, TupleDesc_F64 dst ) {
		System.arraycopy(array.data, index*dof, dst.data, 0, dof);
	}

	@Override public void copy( TupleDesc_F64 src, TupleDesc_F64 dst ) {
		System.arraycopy(src.data, 0, dst.data, 0, dof);
	}

	@Override public int size() {
		return numElements;
	}

	@Override public Class<TupleDesc_F64> getElementType() {
		return TupleDesc_F64.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<TupleDesc_F64> op ) {
		int pointIndex = idx0;
		idx0 *= dof;
		idx1 *= dof;
		for (int i = idx0; i < idx1; i += dof) {
			System.arraycopy(array.data, i, temp.data, 0, dof);
			op.process(pointIndex++, temp);
		}
	}

	@Override public int getDOF() {
		return dof;
	}
}
