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
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray_F64;

/**
 * Packed array of {@link Point3D_F64}. Internally the point is stored in an interleaved format.
 *
 * @author Peter Abeles
 */
public class PackedArrayPoint3D_F64 implements PackedArray<Point3D_F64> {
	private static final int DOF = 3;

	/** Stores tuple in a single continuous array */
	@Getter private final DogArray_F64 array;

	// tuple that the result is temporarily written to
	private final Point3D_F64 temp = new Point3D_F64();

	public PackedArrayPoint3D_F64() {
		array = new DogArray_F64();
		array.resize(0);
	}

	@Override public void reset() {
		array.reset();
	}

	@Override public void reserve( int numTuples ) {
		array.reserve(numTuples*2);
	}

	@Override public void append( Point3D_F64 element ) {
		array.add(element.x);
		array.add(element.y);
		array.add(element.z);
	}

	public void append( double x, double y, double z) {
		array.add(x);
		array.add(y);
		array.add(z);
	}

	@Override public void set( int index, Point3D_F64 element ) {
		index *= 3;
		array.data[index++] = element.x;
		array.data[index++] = element.y;
		array.data[index] = element.z;
	}

	@Override public Point3D_F64 getTemp( int index ) {
		index *= 3;
		temp.x = array.data[index];
		temp.y = array.data[index + 1];
		temp.z = array.data[index + 2];

		return temp;
	}

	@Override public void getCopy( int index, Point3D_F64 dst ) {
		index *= 3;
		dst.x = array.data[index];
		dst.y = array.data[index + 1];
		dst.z = array.data[index + 2];
	}

	@Override public void copy( Point3D_F64 src, Point3D_F64 dst ) {
		dst.setTo(src);
	}

	@Override public int size() {
		return array.size/3;
	}

	@Override public Class<Point3D_F64> getElementType() {
		return Point3D_F64.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<Point3D_F64> op ) {
		int pointIndex = idx0;
		idx0 *= DOF;
		idx1 *= DOF;
		for (int i = idx0; i < idx1; i += DOF) {
			temp.x = array.data[i];
			temp.y = array.data[i + 1];
			temp.z = array.data[i + 2];
			op.process(pointIndex++, temp);
			array.data[i] = temp.x;
			array.data[i + 1] = temp.y;
			array.data[i + 2] = temp.z;
		}
	}

	/**
	 * Makes this array have a value identical to 'src'
	 *
	 * @param src original array being copies
	 * @return Reference to 'this'
	 */
	public PackedArrayPoint3D_F64 setTo( PackedArrayPoint3D_F64 src ) {
		array.setTo(src.array);
		return this;
	}
}
