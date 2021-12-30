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

package boofcv.struct.packed;

import boofcv.misc.BoofLambdas;
import boofcv.struct.PackedArray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_F64;

/**
 * Packed array of {@link Point2D_F64}. Internally the point is stored in an interleaved format.
 *
 * @author Peter Abeles
 */
public class PackedArrayPoint2D_F64 implements PackedArray<Point2D_F64> {
	private static final int DOF = 2;

	// Stores tuple in a single continuous array
	public final DogArray_F64 array;
	// tuple that the result is temporarily written to
	public final Point2D_F64 temp = new Point2D_F64();

	// Number of tuples stored in the array
	protected int numElements;

	public PackedArrayPoint2D_F64() {
		array = new DogArray_F64();
		array.resize(0);
	}

	@Override public void reset() {
		numElements = 0;
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*2);
	}

	@Override public void append( Point2D_F64 element ) {
		array.add(element.x);
		array.add(element.y);

		numElements++;
	}

	@Override public Point2D_F64 getTemp( int index ) {
		temp.x = array.data[index*2];
		temp.y = array.data[index*2 + 1];

		return temp;
	}

	@Override public void getCopy( int index, Point2D_F64 dst ) {
		dst.x = array.data[index*2];
		dst.y = array.data[index*2 + 1];
	}

	@Override public void copy( Point2D_F64 src, Point2D_F64 dst ) {
		dst.setTo(src);
	}

	@Override public int size() {
		return numElements;
	}

	@Override public Class<Point2D_F64> getElementType() {
		return Point2D_F64.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<Point2D_F64> op ) {
		int pointIndex = idx0;
		idx0 *= DOF;
		idx1 *= DOF;
		for (int i = idx0; i < idx1; i += DOF) {
			temp.x = array.data[i];
			temp.y = array.data[i + 1];
			op.process(pointIndex++, temp);
		}
	}
}
