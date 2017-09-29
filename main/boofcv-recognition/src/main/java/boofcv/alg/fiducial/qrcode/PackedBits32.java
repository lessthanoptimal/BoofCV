/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import java.util.Arrays;

/**
 * Stores a set of bits inside of an int array.
 *
 * @author Peter Abeles
 */
public class PackedBits32 implements PackedBits {
	/**
	 * Integer array used to store bits
	 */
	public int data[] = new int[1];
	/**
	 * Number of bits stored
	 */
	public int size;

	public PackedBits32(int totalBits ) {
		resize(totalBits);
	}

	public PackedBits32() {
	}

	public int get( int which ) {
		int index = which/32;
		int offset = which%32;

		return (data[index] & (1 << offset)) >> offset;
	}

	public void set( int which , int value ) {
		int index = which/32;
		int offset = which%32;

		data[index] ^= (-value ^ data[index]) & (1 << offset);
	}

	public void resize(int totalBits ) {
		this.size = totalBits;
		int N = totalBits/32 + (totalBits%32 > 0 ? 1 : 0);
		if( data.length < N ) {
			data = new int[ N ];
		}
	}

	public void zero() {
		int N = size/32;
		Arrays.fill(data,0,N,0);
	}

	@Override
	public int length() {
		return size;
	}

	@Override
	public int arrayLength() {
		return size/32+((size%32)>0?1:0);
	}

	@Override
	public int elementBits() {
		return 32;
	}
}
