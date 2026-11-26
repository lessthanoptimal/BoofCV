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

import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.*;
import pabeles.concurrency.ConcurrencyOps;
import pabeles.concurrency.GrowArray;
import pabeles.concurrency.IntRangeObjectConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/// Central class for controlling concurrency in BoofCV.
///
/// Local work arrays: each thread has its own work arrays, so that a function doesn't declare new memory every
/// time it's called. Take one and release it in a finally block once done:
///
/// ```
/// DogArray_I64 work = BoofConcurrency.takeArray_I64();
/// try {
///     // use work
/// } finally {
///     BoofConcurrency.releaseArray_I64();
/// }
/// ```
///
/// Taking again before releasing, e.g. a nested call, returns a different array. Releases must be in the reverse
/// order of takes, which a finally block ensures. [#clearThreadLocal()] discards the calling thread's arrays.
public class BoofConcurrency extends ConcurrencyOps {
	/// If an image has fewer pixels than this it will not run a concurrent algorithm. The overhead makes it slower.
	public static int SMALL_IMAGE = 100*100;
	/// f set to true it will use a concurrent algorithm
	public static boolean USE_CONCURRENT = true;

	/// A thread's local work arrays, one for each array taken and not yet released
	static final class LocalArrays<T> {
		final List<T> arrays = new ArrayList<>();
		final Supplier<T> factory;
		// Number of arrays taken and not yet released
		int depth;

		LocalArrays( Supplier<T> factory ) {
			this.factory = factory;
		}

		T take() {
			if (depth == arrays.size())
				arrays.add(factory.get());
			return arrays.get(depth++);
		}

		void release() {
			if (depth == 0)
				throw new IllegalStateException("Released more arrays than were taken");
			depth--;
		}
	}

	// TODO Java will not vectorize if its not 100% sure the array isn't reading/writing to the same memory location.
	//      Easiest way to do that is using separate arrays, hence localRowBlocks. However, this design limits array
	//      reuse. Ideally, once the number of arrays needed is known, they would be pre-allocated. Then the location
	//      or a reference to the array block passed in. There currently is no real clean way to do that right now which isn't
	//      brittle or forces it to allocate a small temporary array.

	static final ThreadLocal<LocalArrays<DogArray_I32>> localI32 = ThreadLocal.withInitial(() -> new LocalArrays<>(DogArray_I32::new));
	static final ThreadLocal<LocalArrays<DogArray_I64>> localI64 = ThreadLocal.withInitial(() -> new LocalArrays<>(DogArray_I64::new));
	static final ThreadLocal<LocalArrays<DogArray_F32>> localF32 = ThreadLocal.withInitial(() -> new LocalArrays<>(DogArray_F32::new));
	static final ThreadLocal<LocalArrays<DogArray_F64>> localF64 = ThreadLocal.withInitial(() -> new LocalArrays<>(DogArray_F64::new));
	// Each block's array in sumBlocks
	static final ThreadLocal<LocalArrays<GrowArray<DogArray_I32>>> localBlocks =
			ThreadLocal.withInitial(() -> new LocalArrays<>(() -> new GrowArray<>(DogArray_I32::new)));
	// Row buffers for each block of rows
	static final ThreadLocal<LocalArrays<GrowArray<DogArray<DogArray_I16>>>> localRowBlocksI16 = localRowBlocks(DogArray_I16::new);
	static final ThreadLocal<LocalArrays<GrowArray<DogArray<DogArray_I32>>>> localRowBlocksI32 = localRowBlocks(DogArray_I32::new);
	static final ThreadLocal<LocalArrays<GrowArray<DogArray<DogArray_F32>>>> localRowBlocksF32 = localRowBlocks(DogArray_F32::new);
	static final ThreadLocal<LocalArrays<GrowArray<DogArray<DogArray_F64>>>> localRowBlocksF64 = localRowBlocks(DogArray_F64::new);

	static <T> ThreadLocal<LocalArrays<GrowArray<DogArray<T>>>> localRowBlocks( Supplier<T> row ) {
		return ThreadLocal.withInitial(() -> new LocalArrays<>(() -> new GrowArray<>(() -> new DogArray<>(row::get))));
	}

	/// Sets the maximum number of threads available in the thread pool and adjusts USE\_CONCURRENT. If
	/// the number of threads is less than 2 then USE\_CONCURRENT will be set to false and the single thread
	/// version of code will be called. Otherwise, USE\_CONCURRENT will be true and the max threads in the pool
	/// set to the specified number.
	///
	/// @param maxThreads Maximum number of threads. &le 1 means it will not be threaded.
	public static void setMaxThreads( int maxThreads ) {
		ConcurrencyOps.setMaxThreads(maxThreads);
		USE_CONCURRENT = maxThreads > 1;
	}

	public static boolean isUseConcurrent() {
		return USE_CONCURRENT;
	}

	/// Either returns the number of threads in the thread pool or one if threading is disabled
	public static int getEffectiveActiveThreads() {
		if (USE_CONCURRENT)
			return getThreadPool().getActiveThreadCount();
		return 1;
	}

	/// Discards the calling thread's local work arrays, freeing their memory. See the class description.
	///
	/// @throws IllegalStateException If an array is taken and not released
	public static void clearThreadLocal() {
		if (localI32.get().depth + localI64.get().depth + localF32.get().depth + localF64.get().depth +
				localBlocks.get().depth + localRowBlocksI16.get().depth + localRowBlocksI32.get().depth +
				localRowBlocksF32.get().depth + localRowBlocksF64.get().depth != 0)
			throw new IllegalStateException("A local work array is taken and not released");
		localI32.remove();
		localI64.remove();
		localF32.remove();
		localF64.remove();
		localBlocks.remove();
		localRowBlocksI16.remove();
		localRowBlocksI32.remove();
		localRowBlocksF32.remove();
		localRowBlocksF64.remove();
	}

	/// Takes one of the calling thread's local work arrays. See the class description
	public static DogArray_I32 takeArray_I32() {return localI32.get().take();}

	/// Releases the array last taken by {@link #takeArray_I32()}. See the class description
	public static void releaseArray_I32() {localI32.get().release();}

	/// Takes one of the calling thread's local work arrays. See the class description
	public static DogArray_I64 takeArray_I64() {return localI64.get().take();}

	/// Releases the array last taken by {@link #takeArray_I64()}. See the class description
	public static void releaseArray_I64() {localI64.get().release();}

	/// Takes one of the calling thread's local work arrays. See the class description
	public static DogArray_F32 takeArray_F32() {return localF32.get().take();}

	/// Releases the array last taken by {@link #takeArray_F32()}. See the class description
	public static void releaseArray_F32() {localF32.get().release();}

	/// Takes one of the calling thread's local work arrays. See the class description
	public static DogArray_F64 takeArray_F64() {return localF64.get().take();}

	/// Releases the array last taken by {@link #takeArray_F64()}. See the class description
	public static void releaseArray_F64() {localF64.get().release();}

	/// Takes one of the calling thread's local sets of row buffers, one set for each block of rows. See the class
	/// description
	public static GrowArray<DogArray<DogArray_I16>> takeRowBlocks_I16() {return localRowBlocksI16.get().take();}

	/// Releases the row buffers last taken by {@link #takeRowBlocks_I16()}. See the class description
	public static void releaseRowBlocks_I16() {localRowBlocksI16.get().release();}

	/// Takes one of the calling thread's local sets of row buffers, one set for each block of rows. See the class
	/// description
	public static GrowArray<DogArray<DogArray_I32>> takeRowBlocks_I32() {return localRowBlocksI32.get().take();}

	/// Releases the row buffers last taken by {@link #takeRowBlocks_I32()}. See the class description
	public static void releaseRowBlocks_I32() {localRowBlocksI32.get().release();}

	/// Takes one of the calling thread's local sets of row buffers, one set for each block of rows. See the class
	/// description
	public static GrowArray<DogArray<DogArray_F32>> takeRowBlocks_F32() {return localRowBlocksF32.get().take();}

	/// Releases the row buffers last taken by {@link #takeRowBlocks_F32()}. See the class description
	public static void releaseRowBlocks_F32() {localRowBlocksF32.get().release();}

	/// Takes one of the calling thread's local sets of row buffers, one set for each block of rows. See the class
	/// description
	public static GrowArray<DogArray<DogArray_F64>> takeRowBlocks_F64() {return localRowBlocksF64.get().take();}

	/// Releases the row buffers last taken by {@link #takeRowBlocks_F64()}. See the class description
	public static void releaseRowBlocks_F64() {localRowBlocksF64.get().release();}

	/// Each block of indexes from 'start' to 'endExclusive' is given its own zeroed array, which 'consumer' adds to,
	/// and the arrays are then added to 'total'. Only the first total.length elements of a block's array are used.
	/// Integer addition doesn't depend on order, so the result doesn't depend on the number of threads
	public static void sumBlocks( int start, int endExclusive, int[] total, IntRangeObjectConsumer<int[]> consumer ) {
		GrowArray<DogArray_I32> blocks = localBlocks.get().take();
		try {
			loopBlocks(start, endExclusive, blocks, ( block, i0, i1 ) ->
					consumer.accept(BoofMiscOps.checkDeclare(block, total.length, true), i0, i1));
			for (int i = 0; i < blocks.size(); i++) {
				final int[] values = blocks.get(i).data;
				for (int j = 0; j < total.length; j++) {
					total[j] += values[j];
				}
			}
		} finally {
			localBlocks.get().release();
		}
	}

	/// Sum of 'producer' for each index from 'start' to 'endExclusive'. Each block of indexes is summed by
	/// one thread, then the blocks are added together
	public static long sum_I64( int start, int endExclusive, IntToLongFunction producer ) {
		DogArray_I64 work = takeArray_I64();
		try {
			work.resize(1);
			final long[] total = work.data;
			total[0] = 0;
			loopBlocks(start, endExclusive, ( i0, i1 ) -> {
				long blockTotal = 0;
				for (int i = i0; i < i1; i++) {
					blockTotal += producer.applyAsLong(i);
				}
				synchronized (total) {
					total[0] += blockTotal;
				}
			});
			return total[0];
		} finally {
			releaseArray_I64();
		}
	}

	/// Sum of 'producer' for each index from 'start' to 'endExclusive'. Values are added in index order, so the
	/// result is the same as a single threaded loop and doesn't depend on the number of threads
	public static float sum_F32( int start, int endExclusive, IntToFloatFunction producer ) {
		DogArray_F32 work = takeArray_F32();
		try {
			final float[] values = values(start, endExclusive, producer, work);
			float total = 0;
			for (int i = 0; i < endExclusive - start; i++) {
				total += values[i];
			}
			return total;
		} finally {
			releaseArray_F32();
		}
	}

	/// Same as {@link #sum_F32} but for doubles
	public static double sum_F64( int start, int endExclusive, IntToDoubleFunction producer ) {
		DogArray_F64 work = takeArray_F64();
		try {
			final double[] values = values(start, endExclusive, producer, work);
			double total = 0;
			for (int i = 0; i < endExclusive - start; i++) {
				total += values[i];
			}
			return total;
		} finally {
			releaseArray_F64();
		}
	}

	/// Selects one of the values of 'producer' for each index from 'start' to 'endExclusive', starting from
	/// 'initial'. A value replaces the selected one when 'accept'(value, selected) is true, e.g. (v, s) -&gt; v &lt; s
	/// for the minimum. Each block of indexes is selected by one thread, then the blocks. 'accept' must be a strict
	/// ordering so that the result doesn't depend on the order blocks are combined in
	public static int select_I32( int start, int endExclusive, int initial, IntUnaryOperator producer,
								  IntBinaryPredicate accept ) {
		DogArray_I32 work = takeArray_I32();
		try {
			work.resize(1);
			final int[] selected = work.data;
			selected[0] = initial;
			loopBlocks(start, endExclusive, ( i0, i1 ) -> {
				int block = initial;
				for (int i = i0; i < i1; i++) {
					int value = producer.applyAsInt(i);
					if (accept.test(value, block))
						block = value;
				}
				synchronized (selected) {
					if (accept.test(block, selected[0]))
						selected[0] = block;
				}
			});
			return selected[0];
		} finally {
			releaseArray_I32();
		}
	}

	/// Same as {@link #select_I32} but for longs
	public static long select_I64( int start, int endExclusive, long initial, IntToLongFunction producer,
								   LongBinaryPredicate accept ) {
		DogArray_I64 work = takeArray_I64();
		try {
			work.resize(1);
			final long[] selected = work.data;
			selected[0] = initial;
			loopBlocks(start, endExclusive, ( i0, i1 ) -> {
				long block = initial;
				for (int i = i0; i < i1; i++) {
					long value = producer.applyAsLong(i);
					if (accept.test(value, block))
						block = value;
				}
				synchronized (selected) {
					if (accept.test(block, selected[0]))
						selected[0] = block;
				}
			});
			return selected[0];
		} finally {
			releaseArray_I64();
		}
	}

	/// Selects one of the values of 'producer' for each index from 'start' to 'endExclusive', starting from
	/// 'initial'. A value replaces the selected one when 'accept'(value, selected) is true, e.g. (v, s) -&gt; v &lt; s
	/// for the minimum. Values are selected in index order, the same as a single threaded loop. Unlike integers,
	/// values which 'accept' can't tell apart, e.g. -0.0 and 0.0, can differ, so blocks aren't combined
	public static float select_F32( int start, int endExclusive, float initial, IntToFloatFunction producer,
									FloatBinaryPredicate accept ) {
		DogArray_F32 work = takeArray_F32();
		try {
			final float[] values = values(start, endExclusive, producer, work);
			float selected = initial;
			for (int i = 0; i < endExclusive - start; i++) {
				if (accept.test(values[i], selected))
					selected = values[i];
			}
			return selected;
		} finally {
			releaseArray_F32();
		}
	}

	/// Same as {@link #select_F32} but for doubles
	public static double select_F64( int start, int endExclusive, double initial, IntToDoubleFunction producer,
									 DoubleBinaryPredicate accept ) {
		DogArray_F64 work = takeArray_F64();
		try {
			final double[] values = values(start, endExclusive, producer, work);
			double selected = initial;
			for (int i = 0; i < endExclusive - start; i++) {
				if (accept.test(values[i], selected))
					selected = values[i];
			}
			return selected;
		} finally {
			releaseArray_F64();
		}
	}

	/// Value of 'producer' for each index from 'start' to 'endExclusive', computed concurrently into 'work'
	private static float[] values( int start, int endExclusive, IntToFloatFunction producer, DogArray_F32 work ) {
		final float[] values = BoofMiscOps.checkDeclare(work, endExclusive - start, false);
		loopBlocks(start, endExclusive, ( i0, i1 ) -> {
			for (int i = i0; i < i1; i++) {
				values[i - start] = producer.applyAsFloat(i);
			}
		});
		return values;
	}

	/// Same as {@link #values(int, int, IntToFloatFunction, DogArray_F32)} but for doubles
	private static double[] values( int start, int endExclusive, IntToDoubleFunction producer, DogArray_F64 work ) {
		final double[] values = BoofMiscOps.checkDeclare(work, endExclusive - start, false);
		loopBlocks(start, endExclusive, ( i0, i1 ) -> {
			for (int i = i0; i < i1; i++) {
				values[i - start] = producer.applyAsDouble(i);
			}
		});
		return values;
	}
}
