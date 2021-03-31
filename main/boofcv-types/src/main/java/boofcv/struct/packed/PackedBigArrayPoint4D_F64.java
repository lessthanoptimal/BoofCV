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

import boofcv.struct.PackedArray;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.BigDogArray_F64;

/**
 * Packed array of {@link Point4D_F64}. Internally the point is stored in an interleaved format.
 *
 * @author Peter Abeles
 */
public class PackedBigArrayPoint4D_F64 extends BigDogArray_F64 implements PackedArray<Point4D_F64> {
	private static final int DOF = 3;

	// tuple that the result is temporarily written to
	public final Point4D_F64 temp = new Point4D_F64();

	// Number of points stored in the array
	protected int numPoints;

	/**
	 * Constructor where the default is used for all parameters.
	 */
	public PackedBigArrayPoint4D_F64() {
		this(10);
	}

	/**
	 * Constructor where the initial number of points is specified and everything else is default
	 */
	public PackedBigArrayPoint4D_F64( int reservedPoints ) {
		this(reservedPoints, 50_000, Growth.GROW_FIRST);
	}

	/**
	 * Constructor which allows access to all array parameters
	 *
	 * @param reservedPoints Reserve space to store this number of points initially
	 * @param blockSize A single block will be able to store this number of points
	 * @param growth Growth strategy to use
	 */
	public PackedBigArrayPoint4D_F64( int reservedPoints, int blockSize, Growth growth ) {
		// Ensure that blocks
		super(reservedPoints*DOF, blockSize*DOF, growth);
	}

	@Override public void reset() {
		super.reset();
		numPoints = 0;
	}

	@Override public void reserve( int numPoints ) {
		super.reserve(numPoints*DOF);
	}

	@Override public void append( Point4D_F64 element ) {
		super.add(element.x);
		super.add(element.y);
		super.add(element.z);
		super.add(element.w);

		numPoints++;
	}

	@Override public Point4D_F64 getTemp( int index ) {
		index *= DOF;
		double[] block = super.blocks.get(index/blockSize);
		int element = index%blockSize;
		temp.x = block[element];
		temp.y = block[element+1];
		temp.z = block[element+2];
		temp.w = block[element+3];

		return temp;
	}

	@Override public void getCopy( int index, Point4D_F64 dst ) {
		index *= DOF;
		double[] block = super.blocks.get(index/blockSize);
		int element = index%blockSize;
		dst.x = block[element];
		dst.y = block[element+1];
		dst.z = block[element+2];
		dst.w = block[element+3];
	}

	@Override public void copy( Point4D_F64 src, Point4D_F64 dst ) {
		dst.setTo(src);
	}

	@Override public int size() {
		return numPoints;
	}

	@Override public Class<Point4D_F64> getElementType() {
		return Point4D_F64.class;
	}
}
