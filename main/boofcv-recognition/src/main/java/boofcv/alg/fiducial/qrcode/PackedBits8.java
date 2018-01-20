/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
 * Stores a set of bits inside of a byte array
 *
 * @author Peter Abeles
 */
public class PackedBits8 implements PackedBits {
	/**
	 * Integer array used to store bits
	 */
	public byte data[] = new byte[1];
	/**
	 * Number of bits stored
	 */
	public int size;

	public static PackedBits8 wrap( byte data[] , int numberOfBits ) {
		PackedBits8 a = new PackedBits8();
		a.data = data;
		a.size = numberOfBits;
		return a;
	}

	public PackedBits8(int totalBits ) {
		resize(totalBits);
	}

	public PackedBits8() {
	}

	public int get( int which ) {
		int index = which/8;
		int offset = which%8;

		return (data[index] & (1 << offset)) >> offset;
	}

	public void set( int which , int value ) {
		int index = which/8;
		int offset = which%8;

		data[index] ^= (-value ^ data[index]) & (1 << offset);
	}

	/**
	 * Appends bits on to the end of the stack.
	 * @param bits Storage for bits. Relevant bits start at the front.
	 * @param numberOfBits Number of relevant bits in 'bits'
	 * @param swapOrder If true then the first bit in 'bits' will be the last bit in this array.
	 */
	public void append( int bits , int numberOfBits , boolean swapOrder ) {
		if( numberOfBits > 32 )
			throw new IllegalArgumentException("Number of bits exceeds the size of bits");
		int indexTail = size;
		growArray(numberOfBits,true);

		if( swapOrder ) {
			for (int i = 0; i < numberOfBits; i++) {
				set( indexTail + i , ( bits >> i ) & 1 );
			}
		} else {
			for (int i = 0; i < numberOfBits; i++) {
				set( indexTail + numberOfBits-i-1 , ( bits >> i ) & 1 );
			}
		}
	}

	/**
	 * Read bits from the array and store them in an int
	 * @param location The index of the first bit
	 * @param length Number of bits to real up to 32
	 * @param swapOrder Should the order be swapped?
	 * @return The read in data
	 */
	public int read( int location , int length , boolean swapOrder ) {
		if( length < 0 || length > 32 )
			throw new IllegalArgumentException("Length can't exceed 32");
		if( location + length > size )
			throw new IllegalArgumentException("Attempting to read past the end");

		// TODO speed up by reading in byte chunks
		int output = 0;
		if( swapOrder ) {
			for (int i = 0; i < length; i++) {
				output |= get(location+i) << (length-i-1);
			}
		} else {
			for (int i = 0; i < length; i++) {
				output |= get(location+i) << i;
			}
		}
		return output;
	}

	public int getArray( int index ) {
		return data[index]&0xFF;
	}

	public void resize(int totalBits ) {
		this.size = totalBits;
		int N = arrayLength();
		if( data.length < N ) {
			data = new byte[ N ];
		}
	}

	/**
	 * Increases the size of the data array so that it can store an addition number of bits
	 * @param amountBits Number of bits beyond 'size' that you wish the array to be able to store
	 * @param saveValue if true it will save the value of the array. If false it will not copy it
	 */
	public void growArray( int amountBits , boolean saveValue ) {
		size = size+amountBits;
		int N = size/8 + (size%8==0?0:1);

		if( N > data.length ) {
			// add in some buffer to avoid lots of calls to new
			int extra = Math.min(1024,N+10);
			byte[] tmp = new byte[N+extra];
			if( saveValue )
				System.arraycopy(data,0,tmp,0,data.length);
			this.data = tmp;
		}

	}

	public void zero() {
		Arrays.fill(data,0,arrayLength(),(byte)0);
	}

	public int length() {
		return size;
	}

	public int arrayLength() {
		if( (size%8) == 0 )
			return size/8;
		else
			return size/8 + 1;
	}


	public void print() {
		System.out.println("size = "+size);
		for (int i = 0; i < size; i++) {
			System.out.print(get(i));
		}
		System.out.println();
	}

	@Override
	public int elementBits() {
		return 8;
	}
}
