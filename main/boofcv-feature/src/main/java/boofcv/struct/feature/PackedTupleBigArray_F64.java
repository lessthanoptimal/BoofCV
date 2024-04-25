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
import org.ddogleg.struct.BigDogArray_F64;
import org.ddogleg.struct.BigDogGrowth;

/**
 * Stores a set of tuples in a single continuous array. This is intended to make storage of a large number of tuples
 * more memory efficient by removing all the packaging that Java adds to a class. The memory is also continuous,
 * opening the possibility of further optimizations.
 *
 * @author Peter Abeles
 */
public class PackedTupleBigArray_F64 implements PackedTupleArray<TupleDesc_F64> {
	// degree-of-freedom, number of elements in the tuple
	public final int dof;
	// Stores tuple in a single continuous array
	public final BigDogArray_F64 array;
	// tuple that the result is temporarily written to
	public final TupleDesc_F64 temp;

	// Number of tuples stored in the array
	protected int numElements;

	public PackedTupleBigArray_F64( int dof ) {
		this.dof = dof;
		this.temp = new TupleDesc_F64(dof);
		array = new BigDogArray_F64(dof, dof*65536, BigDogGrowth.GROW_FIRST);
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
		array.append(element.data, 0, dof);
		numElements++;
	}

	@Override public void set( int index, TupleDesc_F64 element ) {
		array.setArray((long)index*dof, element.data, 0, dof);
	}

	@Override public TupleDesc_F64 getTemp( int index ) {
		array.getArray(index*dof, temp.data, 0, dof);
		return temp;
	}

	@Override public void getCopy( int index, TupleDesc_F64 dst ) {
		array.getArray(index*dof, dst.data, 0, dof);
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
		array.processByBlock(idx0*dof, idx1*dof, ( array, arrayIdx0, arrayIdx1, offset ) -> {
			int pointIndex = idx0 + offset/dof;
			for (int i = arrayIdx0; i < arrayIdx1; i += dof) {
				System.arraycopy(array, i, temp.data, 0, dof);
				op.process(pointIndex++, temp);
			}
		});
	}

	@Override public int getDOF() {
		return dof;
	}
}
