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
import org.ddogleg.struct.BigDogArray_I32;
import org.ddogleg.struct.BigDogGrowth;

/**
 * Stores a set of tuples in a single continuous array. This is intended to make storage of a large number of tuples
 * more memory efficient by removing all the packaging that Java adds to a class. The memory is also continuous,
 * opening the possibility of further optimizations.
 *
 * @author Peter Abeles
 */
public class PackedTupleBigArray_B implements PackedTupleArray<TupleDesc_B> {
	// degree-of-freedom, number of elements in the tuple
	public final int dof;
	// Stores tuple in a single continuous array
	public final BigDogArray_I32 array;
	// tuple that the result is temporarily written to
	public final TupleDesc_B temp;

	// Number of tuples stored in the array
	protected int numElements;

	// Number of integers required to store the descriptor
	protected final int numInts;

	public PackedTupleBigArray_B( int dof ) {
		this.dof = dof;
		this.temp = new TupleDesc_B(dof);
		this.numInts = temp.data.length;
		array = new BigDogArray_I32(dof, (dof/32)*65536, BigDogGrowth.GROW_FIRST);
		array.resize(0);
	}

	@Override public void reset() {
		numElements = 0;
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*numInts);
	}

	@Override public void append( TupleDesc_B element ) {
		array.append(element.data, 0, numInts);
		numElements++;
	}

	@Override public TupleDesc_B getTemp( int index ) {
		array.getArray(index*numInts, temp.data, 0, numInts);
		return temp;
	}

	@Override public void getCopy( int index, TupleDesc_B dst ) {
		array.getArray(index*numInts, dst.data, 0, numInts);
	}

	@Override public void copy( TupleDesc_B src, TupleDesc_B dst ) {
		System.arraycopy(src.data, 0, dst.data, 0, numInts);
	}

	@Override public int size() {
		return numElements;
	}

	@Override public Class<TupleDesc_B> getElementType() {
		return TupleDesc_B.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<TupleDesc_B> op ) {
		array.processByBlock(idx0*numInts, idx1*numInts, ( array, arrayIdx0, arrayIdx1, offset ) -> {
			int pointIndex = idx0 + offset/numInts;
			for (int i = arrayIdx0; i < arrayIdx1; i += numInts) {
				System.arraycopy(array, i, temp.data, 0, numInts);
				op.process(pointIndex++, temp);
			}
		});
	}

	@Override public int getDOF() {
		return dof;
	}
}
