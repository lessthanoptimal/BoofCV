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

package boofcv.struct.feature;

import lombok.Getter;
import lombok.Setter;

/**
 * Binary descriptor which is stored inside of an array of ints.
 *
 * @author Peter Abeles
 */
public class TupleDesc_B implements TupleDesc<TupleDesc_B> {
	public @Getter @Setter int[] data;
	public int numBits;

	public TupleDesc_B( int numBits ) {
		int numInts = numBits/32;
		if (numBits%32 != 0) {
			numInts++;
		}

		this.numBits = numBits;
		data = new int[numInts];
	}

	public TupleDesc_B( int numBits, int numInts ) {
		this.numBits = numBits;
		data = new int[numInts];
	}

	public boolean isBitTrue( int bit ) {
		int index = bit/32;
		return ((data[index] >> (bit%32)) & 0x01) == 1;
	}

	public void setBit( int bit, boolean value ) {
		int index = bit/32;
		if (value)
			data[index] |= 1 << (bit%32);
		else
			data[index] &= ~(1 << (bit%32));
	}

	@Override
	public void setTo( TupleDesc_B source ) {
		if (data.length < source.data.length)
			throw new IllegalArgumentException("Data array is too small to store the source array.");

		this.numBits = source.numBits;
		System.arraycopy(source.data, 0, data, 0, source.data.length);
	}

	@Override
	public double getDouble( int index ) {
		if (isBitTrue(index))
			return 1;
		else
			return -1;
	}

	@Override
	public int size() {
		return numBits;
	}

	@Override public TupleDesc_B newInstance() {
		return new TupleDesc_B(numBits, data.length);
	}
}
