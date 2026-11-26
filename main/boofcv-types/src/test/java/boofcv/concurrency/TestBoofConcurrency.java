/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.concurrency;

import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestBoofConcurrency extends BoofStandardJUnit {
	// Ranges which are empty, smaller than the number of threads, and large with an offset
	static final int[][] RANGES = new int[][]{{0, 0}, {5, 5}, {0, 1}, {2, 5}, {0, 1000}, {7, 2007}};

	int[] valuesI32 = new int[2100];
	long[] valuesI64 = new long[2100];
	float[] valuesF32 = new float[2100];
	double[] valuesF64 = new double[2100];

	TestBoofConcurrency() {
		for (int i = 0; i < valuesI32.length; i++) {
			valuesI32[i] = rand.nextInt() - 5;
			valuesI64[i] = rand.nextLong()/4;
			// a wide range of magnitudes, so that the order values are added in changes a float sum
			valuesF32[i] = (float)((rand.nextDouble() - 0.4)*Math.pow(10, rand.nextInt(8)));
			valuesF64[i] = (rand.nextDouble() - 0.4)*Math.pow(10, rand.nextInt(16));
		}
	}

	@Test void sum_I64() {
		for (int[] r : RANGES) {
			long expected = 0;
			for (int i = r[0]; i < r[1]; i++) {
				expected += valuesI64[i];
			}
			assertEquals(expected, BoofConcurrency.sum_I64(r[0], r[1], i -> valuesI64[i]));
		}
	}

	/// The sum must be identical to adding the values in index order
	@Test void sum_F32() {
		for (int[] r : RANGES) {
			float expected = 0;
			for (int i = r[0]; i < r[1]; i++) {
				expected += valuesF32[i];
			}
			assertEquals(expected, BoofConcurrency.sum_F32(r[0], r[1], i -> valuesF32[i]), 0.0f);
		}
	}

	@Test void sum_F64() {
		for (int[] r : RANGES) {
			double expected = 0;
			for (int i = r[0]; i < r[1]; i++) {
				expected += valuesF64[i];
			}
			assertEquals(expected, BoofConcurrency.sum_F64(r[0], r[1], i -> valuesF64[i]), 0.0);
		}
	}

	@Test void select_I32() {
		for (int[] r : RANGES) {
			int min = 5;
			int max = 5;
			for (int i = r[0]; i < r[1]; i++) {
				min = Math.min(min, valuesI32[i]);
				max = Math.max(max, valuesI32[i]);
			}
			assertEquals(min, BoofConcurrency.select_I32(r[0], r[1], 5, i -> valuesI32[i], ( v, s ) -> v < s));
			assertEquals(max, BoofConcurrency.select_I32(r[0], r[1], 5, i -> valuesI32[i], ( v, s ) -> v > s));
		}
	}

	@Test void select_I64() {
		for (int[] r : RANGES) {
			long min = 5;
			long max = 5;
			for (int i = r[0]; i < r[1]; i++) {
				min = Math.min(min, valuesI64[i]);
				max = Math.max(max, valuesI64[i]);
			}
			assertEquals(min, BoofConcurrency.select_I64(r[0], r[1], 5, i -> valuesI64[i], ( v, s ) -> v < s));
			assertEquals(max, BoofConcurrency.select_I64(r[0], r[1], 5, i -> valuesI64[i], ( v, s ) -> v > s));
		}
	}

	/// Values are selected in index order, which decides between values 'accept' can't tell apart, and NaN is
	/// never accepted by a comparison
	@Test void select_F32() {
		for (int[] r : RANGES) {
			float min = 5;
			float max = 5;
			for (int i = r[0]; i < r[1]; i++) {
				min = Math.min(min, valuesF32[i]);
				max = Math.max(max, valuesF32[i]);
			}
			assertEquals(min, BoofConcurrency.select_F32(r[0], r[1], 5, i -> valuesF32[i], ( v, s ) -> v < s), 0.0f);
			assertEquals(max, BoofConcurrency.select_F32(r[0], r[1], 5, i -> valuesF32[i], ( v, s ) -> v > s), 0.0f);
		}

		// the first zero is selected, whichever sign the zeros after it have
		for (float first : new float[]{0.0f, -0.0f}) {
			float found = BoofConcurrency.select_F32(0, 1000, 1.0f,
					i -> i == 0 ? first : i%3 == 0 ? Float.NaN : (i%2 == 0 ? 0.0f : -0.0f), ( v, s ) -> v < s);
			assertEquals(Float.floatToRawIntBits(first), Float.floatToRawIntBits(found));
		}
	}

	@Test void select_F64() {
		for (int[] r : RANGES) {
			double min = 5;
			double max = 5;
			for (int i = r[0]; i < r[1]; i++) {
				min = Math.min(min, valuesF64[i]);
				max = Math.max(max, valuesF64[i]);
			}
			assertEquals(min, BoofConcurrency.select_F64(r[0], r[1], 5, i -> valuesF64[i], ( v, s ) -> v < s), 0.0);
			assertEquals(max, BoofConcurrency.select_F64(r[0], r[1], 5, i -> valuesF64[i], ( v, s ) -> v > s), 0.0);
		}

		for (double first : new double[]{0.0, -0.0}) {
			double found = BoofConcurrency.select_F64(0, 1000, 1.0,
					i -> i == 0 ? first : i%3 == 0 ? Double.NaN : (i%2 == 0 ? 0.0 : -0.0), ( v, s ) -> v < s);
			assertEquals(Double.doubleToRawLongBits(first), Double.doubleToRawLongBits(found));
		}
	}

	/// Counts each index's value into one of the bins, added to what's already in 'total'
	private int[] histogram( int start, int endExclusive, int[] total ) {
		int[] expected = total.clone();
		for (int i = start; i < endExclusive; i++) {
			expected[bin(i, total.length)]++;
		}
		return expected;
	}

	private int bin( int index, int numBins ) {
		return (valuesI32[index] & 0x7FFFFFFF)%numBins;
	}

	/// Ranges are run largest first too, so a block's saved array from an earlier call must not leak into a later one
	@Test void sumBlocks() {
		for (int trial = 0; trial < 2; trial++) {
			for (int k = 0; k < RANGES.length; k++) {
				int[] r = RANGES[trial == 0 ? k : RANGES.length - 1 - k];
				int numBins = 37 + k;
				int[] total = new int[numBins];
				for (int i = 0; i < numBins; i++) {
					total[i] = i*3;
				}
				int[] expected = histogram(r[0], r[1], total);
				BoofConcurrency.sumBlocks(r[0], r[1], total, ( block, i0, i1 ) -> {
					for (int i = i0; i < i1; i++) {
						block[bin(i, numBins)]++;
					}
				});
				assertArrayEquals(expected, total);
			}
		}
	}

	/// A nested take returns a different array, and a release makes the array available to the next take
	@Test void takeRelease() {
		DogArray_I64 outer = BoofConcurrency.takeArray_I64();
		try {
			DogArray_I64 inner = BoofConcurrency.takeArray_I64();
			BoofConcurrency.releaseArray_I64();
			assertNotSame(outer, inner);
			assertSame(inner, BoofConcurrency.takeArray_I64());
			BoofConcurrency.releaseArray_I64();
		} finally {
			BoofConcurrency.releaseArray_I64();
		}
		assertSame(outer, BoofConcurrency.takeArray_I64());
		BoofConcurrency.releaseArray_I64();

		assertThrows(IllegalStateException.class, BoofConcurrency::releaseArray_I64);
	}

	/// Each type has its own arrays
	@Test void takeRelease_types() {
		BoofConcurrency.takeArray_I32();
		BoofConcurrency.takeArray_F32();
		BoofConcurrency.takeArray_F64();
		assertEquals(1, BoofConcurrency.localI32.get().depth);
		assertEquals(0, BoofConcurrency.localI64.get().depth);
		assertEquals(1, BoofConcurrency.localF32.get().depth);
		assertEquals(1, BoofConcurrency.localF64.get().depth);
		BoofConcurrency.releaseArray_I32();
		BoofConcurrency.releaseArray_F32();
		BoofConcurrency.releaseArray_F64();
		assertEquals(0, BoofConcurrency.localI32.get().depth);
		assertEquals(0, BoofConcurrency.localF32.get().depth);
		assertEquals(0, BoofConcurrency.localF64.get().depth);
	}

	/// Row blocks are separate from the arrays and each type has its own
	@Test void takeRelease_rowBlocks() {
		assertNotSame(BoofConcurrency.takeRowBlocks_I16(), BoofConcurrency.takeRowBlocks_I16());
		BoofConcurrency.takeRowBlocks_F32();
		assertEquals(2, BoofConcurrency.localRowBlocksI16.get().depth);
		assertEquals(0, BoofConcurrency.localRowBlocksI32.get().depth);
		assertEquals(1, BoofConcurrency.localRowBlocksF32.get().depth);
		assertEquals(0, BoofConcurrency.localRowBlocksF64.get().depth);
		assertEquals(0, BoofConcurrency.localF32.get().depth);
		BoofConcurrency.releaseRowBlocks_I16();
		BoofConcurrency.releaseRowBlocks_I16();
		BoofConcurrency.releaseRowBlocks_F32();
		assertEquals(0, BoofConcurrency.localRowBlocksI16.get().depth);
		assertEquals(0, BoofConcurrency.localRowBlocksF32.get().depth);
		assertThrows(IllegalStateException.class, BoofConcurrency::releaseRowBlocks_F64);
	}

	/// Functions release what they take, and clearing discards the arrays
	@Test void clearThreadLocal() {
		// Earlier tests on this thread may have left more arrays
		BoofConcurrency.clearThreadLocal();
		BoofConcurrency.sum_F32(0, 1000, i -> valuesF32[i]);
		BoofConcurrency.sum_F64(0, 1000, i -> valuesF64[i]);
		BoofConcurrency.sumBlocks(0, 1000, new int[5], ( block, i0, i1 ) -> {});
		BoofConcurrency.select_I32(0, 1000, 0, i -> valuesI32[i], ( v, s ) -> v < s);
		BoofConcurrency.sum_I64(0, 1000, i -> valuesI64[i]);
		BoofConcurrency.takeRowBlocks_I16();
		BoofConcurrency.releaseRowBlocks_I16();
		BoofConcurrency.takeRowBlocks_I32();
		BoofConcurrency.releaseRowBlocks_I32();
		BoofConcurrency.takeRowBlocks_F32();
		BoofConcurrency.releaseRowBlocks_F32();
		BoofConcurrency.takeRowBlocks_F64();
		BoofConcurrency.releaseRowBlocks_F64();
		for (BoofConcurrency.LocalArrays<?> local : allLocal()) {
			assertEquals(0, local.depth);
			assertEquals(1, local.arrays.size());
		}

		BoofConcurrency.clearThreadLocal();
		for (BoofConcurrency.LocalArrays<?> local : allLocal()) {
			assertEquals(0, local.arrays.size());
		}

		// Still works after
		float expected = 0;
		for (int i = 0; i < 1000; i++) {
			expected += valuesF32[i];
		}
		assertEquals(expected, BoofConcurrency.sum_F32(0, 1000, i -> valuesF32[i]), 0.0f);
	}

	@Test void clearThreadLocal_taken() {
		BoofConcurrency.takeArray_F64();
		try {
			assertThrows(IllegalStateException.class, BoofConcurrency::clearThreadLocal);
		} finally {
			BoofConcurrency.releaseArray_F64();
		}
		BoofConcurrency.takeRowBlocks_I32();
		try {
			assertThrows(IllegalStateException.class, BoofConcurrency::clearThreadLocal);
		} finally {
			BoofConcurrency.releaseRowBlocks_I32();
		}
		BoofConcurrency.clearThreadLocal();
	}

	private static BoofConcurrency.LocalArrays<?>[] allLocal() {
		return new BoofConcurrency.LocalArrays<?>[]{BoofConcurrency.localI32.get(), BoofConcurrency.localI64.get(),
				BoofConcurrency.localF32.get(), BoofConcurrency.localF64.get(), BoofConcurrency.localBlocks.get(),
				BoofConcurrency.localRowBlocksI16.get(), BoofConcurrency.localRowBlocksI32.get(),
				BoofConcurrency.localRowBlocksF32.get(), BoofConcurrency.localRowBlocksF64.get()};
	}
}
