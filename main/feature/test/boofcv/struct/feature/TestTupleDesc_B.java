/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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


import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTupleDesc_B {

	Random rand = new Random(234);

	@Test
	public void isBitTrue() {
		int N = 40;
		TupleDesc_B desc = new TupleDesc_B(N);

		boolean expected[] = new boolean[N];
		for( int i = 0; i < N; i++ ) {
			expected[i] = rand.nextBoolean();

			int index = i/32;
			desc.data[index] |= expected[i] ? 1 << (i%32) : 0;
		}

		for( int i = 0; i < N; i++ ) {
			assertEquals(desc.isBitTrue(i),expected[i]);
		}
	}

	@Test
	public void setTo() {
		int N = 40;
		TupleDesc_B a = new TupleDesc_B(N);

		for( int i = 0; i < a.data.length; i++ ) {
			a.data[i] = rand.nextInt();
		}

		TupleDesc_B b = new TupleDesc_B(80);
		b.setTo(a);

		for( int i = 0; i < a.data.length; i++ ) {
			assertEquals(a.data[i],b.data[i]);
		}
		assertEquals(a.numBits,b.numBits);
	}

	@Test
	public void copy() {
		TupleDesc_B a = new TupleDesc_B(512);
		for( int i = 0; i < a.data.length; i++ ) {
			a.data[i] = 100+i;
		}

		TupleDesc_B b = a.copy();
		assertEquals(a.numBits,b.numBits);
		for( int i = 0; i < a.data.length; i++ ) {
			assertEquals(100+i,a.data[i]);
		}

		assertEquals(a.numBits,b.numBits);
	}
}
