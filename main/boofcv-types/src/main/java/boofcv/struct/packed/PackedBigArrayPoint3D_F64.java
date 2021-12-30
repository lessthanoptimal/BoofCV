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
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.BigDogArray_F64;
import org.ddogleg.struct.BigDogGrowth;

/**
 * Packed array of {@link Point3D_F64}. Internally the point is stored in an interleaved format.
 *
 * @author Peter Abeles
 */
public class PackedBigArrayPoint3D_F64 implements PackedArray<Point3D_F64> {
	private static final int DOF = 3;

	// tuple that the result is temporarily written to
	public final Point3D_F64 temp = new Point3D_F64();

	// Storage for the raw data in an array
	private final BigDogArray_F64 dog;

	// Number of points stored in the array
	protected int size;

	/**
	 * Constructor where the default is used for all parameters.
	 */
	public PackedBigArrayPoint3D_F64() {
		this(10);
	}

	/**
	 * Constructor where the initial number of points is specified and everything else is default
	 */
	public PackedBigArrayPoint3D_F64( int reservedPoints ) {
		this(reservedPoints, 50_000, BigDogGrowth.GROW_FIRST);
	}

	/**
	 * Constructor which allows access to all array parameters
	 *
	 * @param reservedPoints Reserve space to store this number of points initially
	 * @param blockSize A single block will be able to store this number of points
	 * @param growth Growth strategy to use
	 */
	public PackedBigArrayPoint3D_F64( int reservedPoints, int blockSize, BigDogGrowth growth ) {
		dog = new BigDogArray_F64(reservedPoints*DOF, blockSize*DOF, growth);
	}

	@Override public void reset() {
		dog.reset();
		size = 0;
	}

	@Override public void reserve( int numPoints ) {
		dog.reserve(numPoints*DOF);
	}

	public void append( double x, double y, double z ) {
		dog.add(x);
		dog.add(y);
		dog.add(z);

		size++;
	}

	@Override public void append( Point3D_F64 element ) {
		dog.add(element.x);
		dog.add(element.y);
		dog.add(element.z);

		size++;
	}

	@Override public Point3D_F64 getTemp( int index ) {
		index *= DOF;
		double[] block = dog.getBlocks().get(index/dog.getBlockSize());
		int element = index%dog.getBlockSize();
		temp.x = block[element];
		temp.y = block[element + 1];
		temp.z = block[element + 2];

		return temp;
	}

	@Override public void getCopy( int index, Point3D_F64 dst ) {
		index *= DOF;
		double[] block = dog.getBlocks().get(index/dog.getBlockSize());
		int element = index%dog.getBlockSize();
		dst.x = block[element];
		dst.y = block[element + 1];
		dst.z = block[element + 2];
	}

	@Override public void copy( Point3D_F64 src, Point3D_F64 dst ) {
		dst.setTo(src);
	}

	@Override public int size() {
		return size;
	}

	@Override public Class<Point3D_F64> getElementType() {
		return Point3D_F64.class;
	}

	@Override public void forIdx( int idx0, int idx1, BoofLambdas.ProcessIndex<Point3D_F64> op ) {
		dog.processByBlock(idx0*3, idx1*3, ( array, arrayIdx0, arrayIdx1, offset ) -> {
			int pointIndex = idx0 + offset/DOF;
			for (int i = arrayIdx0; i < arrayIdx1; i += DOF) {
				temp.x = array[i];
				temp.y = array[i + 1];
				temp.z = array[i + 2];
				op.process(pointIndex++, temp);
			}
		});
	}
}
