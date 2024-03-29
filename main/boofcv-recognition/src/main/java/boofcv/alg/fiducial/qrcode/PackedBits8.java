/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
 * Stores a set of bits inside a byte array
 *
 * @author Peter Abeles
 */
public class PackedBits8 implements PackedBits {
	/**
	 * Integer array used to store bits
	 */
	public byte[] data = new byte[1];
	/**
	 * Number of bits stored
	 */
	public int size;

	public static PackedBits8 wrap( byte[] data, int numberOfBits ) {
		PackedBits8 a = new PackedBits8();
		a.data = data;
		a.size = numberOfBits;
		return a;
	}

	public PackedBits8( int totalBits ) {
		resize(totalBits);
	}

	public PackedBits8() {}

	public void setTo( PackedBits8 src ) {
		resize(src.size);
		System.arraycopy(src.data, 0, data, 0, src.arrayLength());
	}

	/**
	 * Checks to see if the usable data in 'other' is identical to the usable data in this.
	 */
	public boolean isIdentical( PackedBits8 other ) {
		if (size != other.size)
			return false;

		int numBytes = size/8 + (size%8 == 0 ? 0 : 1);
		for (int i = 0; i < numBytes; i++) {
			if (data[i] != other.data[i])
				return false;
		}
		return true;
	}

	@Override
	public int get( int which ) {
		int index = which/8;
		int offset = which%8;

		return (data[index] & (1 << offset)) >> offset;
	}

	@Override
	public void set( int which, int value ) {
		int index = which/8;
		int offset = which%8;

		data[index] ^= (byte)((-value ^ data[index]) & (1 << offset));
	}

	/**
	 * Appends data in the array. Fractions of a byte are allowed
	 */
	public void append( byte[] data, int numberOfBits, boolean swapOrder ) {
		// pre-declare required memory. TODO remove this hack in the future
		int oldSize = size;
		growArray(numberOfBits, true);
		size = oldSize;

		// Copy over data one byte at a time
		int numBytes = numberOfBits/8;
		for (int i = 0; i < numBytes; i++) {
			append(data[i] & 0xFF, 8, swapOrder);
		}
		int remaining = numberOfBits - numBytes*8;
		if (remaining == 0)
			return;
		append(data[numBytes], remaining, swapOrder);
	}

	/**
	 * Appends bits on to the end of the stack.
	 *
	 * @param bits Storage for bits. Relevant bits start at the front.
	 * @param numberOfBits Number of relevant bits in 'bits'
	 * @param swapOrder If true then the first bit in 'bits' will be the last bit in this array.
	 */
	public void append( int bits, int numberOfBits, boolean swapOrder ) {
		if (numberOfBits > 32)
			throw new IllegalArgumentException("Number of bits exceeds the size of bits");
		int indexTail = size;
		growArray(numberOfBits, true);

		if (swapOrder) {
			for (int i = 0; i < numberOfBits; i++) {
				set(indexTail + i, (bits >> i) & 1);
			}
		} else {
			for (int i = 0; i < numberOfBits; i++) {
				set(indexTail + numberOfBits - i - 1, (bits >> i) & 1);
			}
		}
	}

	/**
	 * Appends the bit array onto the end
	 */
	public void append( PackedBits8 bits, int numberOfBits ) {
		if (numberOfBits > bits.size)
			throw new IllegalArgumentException("numberOfBits must be <= bits.size");

		int numWords = bits.size/8;
		for (int i = 0; i < numWords; i++) {
			append(bits.data[i] & 0xFF, 8, true);
		}
		int remaining = bits.size - numWords*8;
		if (remaining == 0)
			return;
		int tail = bits.read(numWords*8, remaining, true);
		append(tail, remaining, false);
	}

	/**
	 * Adds bits encoded as a binary string, i.e. "100010010011"
	 */
	public PackedBits8 append( String text ) {
		// Add in units of 8-bits since it's more efficient
		int location;
		for (location = 0; location + 8 < text.length(); location += 8) {
			int value = 0;
			for (int i = 0; i < 8; i++) {
				if (text.charAt(location + i) == '0')
					continue;
				value |= 1 << i;
			}
			append(value, 8, true);
		}
		// Add the remainder one bit a t a time
		while (location < text.length()) {
			int value = text.charAt(location++) == '0' ? 0 : 1;
			append(value, 1, true);
		}
		return this;
	}

	/**
	 * Read bits from the array and store them in an int
	 *
	 * @param location The index of the first bit
	 * @param length Number of bits to real up to 32
	 * @param swapOrder Should the order be swapped?
	 * @return The read in data
	 */
	public int read( int location, int length, boolean swapOrder ) {
		if (length < 0 || length > 32)
			throw new IllegalArgumentException("Length can't exceed 32");
		if (location + length > size)
			throw new IllegalArgumentException("Attempting to read past the end. length=" + length +
					" remaining=" + (size - location));

		// TODO speed up by reading in byte chunks
		int output = 0;
		if (swapOrder) {
			for (int i = 0; i < length; i++) {
				output |= get(location + i) << (length - i - 1);
			}
		} else {
			for (int i = 0; i < length; i++) {
				output |= get(location + i) << i;
			}
		}
		return output;
	}

	public int getArray( int index ) {
		return data[index] & 0xFF;
	}

	@Override
	public void resize( int totalBits ) {
		this.size = totalBits;
		int N = arrayLength();
		if (data.length < N) {
			data = new byte[N];
		}
	}

	/**
	 * Increases the size of the data array so that it can store an addition number of bits
	 *
	 * @param amountBits Number of bits beyond 'size' that you wish the array to be able to store
	 * @param saveValue if true it will save the value of the array. If false it will not copy it
	 */
	public void growArray( int amountBits, boolean saveValue ) {
		size = size + amountBits;
		int N = size/8 + (size%8 == 0 ? 0 : 1);

		if (N > data.length) {
			// add in some buffer to avoid lots of calls to new
			int extra = Math.min(1024, N + 10);
			byte[] tmp = new byte[N + extra];
			if (saveValue)
				System.arraycopy(data, 0, tmp, 0, data.length);
			this.data = tmp;
		}
	}

	@Override
	public void zero() {
		Arrays.fill(data, 0, arrayLength(), (byte)0);
	}

	@Override
	public int length() {
		return size;
	}

	@Override
	public int arrayLength() {
		if ((size%8) == 0)
			return size/8;
		else
			return size/8 + 1;
	}

	@Override
	public int elementBits() {
		return 8;
	}
}
