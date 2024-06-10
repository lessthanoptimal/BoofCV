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

import boofcv.misc.BoofLambdas;
import boofcv.struct.PackedArray;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.DogArray_F64;

/**
 * Packed array of {@link Point4D_F64}. Internally the point is stored in an interleaved format.
 *
 * @author Peter Abeles
 */
public class PackedArrayPoint4D_F64 implements PackedArray<Point4D_F64> {
	private static final int DOF = 4;

	// Stores tuple in a single continuous array
	public final DogArray_F64 array;
	// tuple that the result is temporarily written to
	public final Point4D_F64 temp = new Point4D_F64();

	public PackedArrayPoint4D_F64() {
		array = new DogArray_F64();
		array.resize(0);
	}

	@Override public void reset() {
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*4);
	}

	@Override public void append( Point4D_F64 element ) {
		array.add(element.x);
		array.add(element.y);
		array.add(element.z);
		array.add(element.w);
	}

	public void append( double x, double y, double z, double w ) {
		array.add(x);
		array.add(y);
		array.add(z);
		array.add(w);
	}

	@Override public void set( int index, Point4D_F64 element ) {
		index *= 4;
		array.data[index++] = element.x;
		array.data[index++] = element.y;
		array.data[index++] = element.z;
		array.data[index] = element.w;
	}

	@Override public Point4D_F64 getTemp( int index ) {
		index *= 4;
		temp.x = array.data[index];
		temp.y = array.data[index + 1];
		temp.z = array.data[index + 2];
		temp.w = array.data[index + 3];

		return temp;
	}

	@Override public void getCopy( int index, Point4D_F64 dst ) {
		index *= 4;
		dst.x = array.data[index];
		dst.y = array.data[index + 1];
		dst.z = array.data[index + 2];
		dst.w = array.data[index + 3];
	}

	@Override public void copy( Point4D_F64 src, Point4D_F64 dst ) {
		dst.setTo(src);
	}

	@Override public int size() {
		return array.size/4;
	}

	@Override public Class<Point4D_F64> getElementType() {
		return Point4D_F64.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<Point4D_F64> op ) {
		int pointIndex = idx0;
		idx0 *= DOF;
		idx1 *= DOF;
		for (int i = idx0; i < idx1; i += DOF) {
			temp.x = array.data[i];
			temp.y = array.data[i + 1];
			temp.z = array.data[i + 2];
			temp.w = array.data[i + 3];
			op.process(pointIndex++, temp);
			array.data[i] = temp.x;
			array.data[i + 1] = temp.y;
			array.data[i + 2] = temp.z;
			array.data[i + 3] = temp.w;
		}
	}

	/**
	 * Makes this array have a value identical to 'src'
	 *
	 * @param src original array being copies
	 * @return Reference to 'this'
	 */
	public PackedArrayPoint4D_F64 setTo( PackedArrayPoint4D_F64 src ) {
		reset();
		reserve(src.size());
		src.forIdx(0, src.size(), ( idx, p ) -> append(p.x, p.y, p.z, p.w));
		return this;
	}
}
