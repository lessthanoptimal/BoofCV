/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

/**
 * Binary descriptor which is stored inside of an array of ints.
 *
 * @author Peter Abeles
 */
public class TupleDesc_B extends TupleDesc {
	public int[] data;
	public int numBits;

	public TupleDesc_B(int numBits) {
		int numInts = numBits/32;
		if( numBits % 32 != 0 ) {
			numInts++;
		}

		this.numBits = numBits;
		data = new int[numInts];
	}

	public TupleDesc_B(int numBits, int numInts) {
		this.numBits = numBits;
		data = new int[numInts];
	}

	public boolean isBitTrue( int bit ) {
		int index = bit/32;
		return ((data[index] >> (bit%32)) & 0x01) == 1;
	}

	public TupleDesc_B copy() {
		TupleDesc_B ret = new TupleDesc_B(numBits);
		System.arraycopy(data,0,ret.data,0,data.length);
		return ret;
	}
}
