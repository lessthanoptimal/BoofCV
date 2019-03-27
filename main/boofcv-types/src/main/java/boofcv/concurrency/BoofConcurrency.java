/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

/**
 * Location of controls for turning on and off concurrent (i.e. threaded) algorithms.
 *
 * -Djava.util.concurrent.ForkJoinPool.common.parallelism=16
 *
 * @author Peter Abeles
 */
public class BoofConcurrency {
	/**
	 * If set to true it will use a concurrent algorithm
	 */
	public static boolean USE_CONCURRENT = true;

	// Custom thread pool for streams so that the number of threads can be controlled
	private static ForkJoinPool pool = new ForkJoinPool();

	public static void setMaxThreads( int maxThreads ) {
		pool = new ForkJoinPool(maxThreads);
	}

	/**
	 * Returns the maximum number of threads which can be run at once in this pool
	 */
	public static int getMaxThreads() {
		return pool.getParallelism();
	}

	/**
	 * Concurrent for loop. Each loop with spawn as a thread up to the maximum number of threads.
	 *
	 * @param start starting value, inclusive
	 * @param endExclusive ending value, exclusive
	 * @param consumer The consumer
	 */
	public static void loopFor(int start , int endExclusive , IntConsumer consumer ) {
		try {
			pool.submit(() ->IntStream.range(start, endExclusive).parallel().forEach(consumer)).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Concurrent for loop. Each loop with spawn as a thread up to the maximum number of threads.
	 *
	 * @param start starting value, inclusive
	 * @param endExclusive ending value, exclusive
	 * @param consumer The consumer
	 */
	public static void loopFor(int start , int endExclusive , int step , IntConsumer consumer ) {
		try {
			int range = endExclusive-start;
			pool.submit(() ->IntStream.range(0, range/step).parallel().forEach(i-> consumer.accept(start+i*step))).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Automatically breaks the problem up into blocks based on the number of threads available. It is assumed
	 * that there is some cost associated with processing a block and the number of blocks is minimized.
	 *
	 * Examples:
	 * <ul>
	 *     <li>Given a range of 0 to 100, and minBlock is 5, and 10 threads. Blocks will be size 10.</li>
	 *     <li>Given a range of 0 to 100, and minBlock is 20, and 10 threads. Blocks will be size 20.</li>
	 *     <li>Given a range of 0 to 100, and minBlock is 15, and 10 threads. Blocks will be size 16 and 20.</li>
	 *     <li>Given a range of 0 to 100, and minBlock is 80, and 10 threads. Blocks will be size 100.</li>
	 * </ul>
	 *
	 * @param start First index, inclusive
	 * @param endExclusive Last index, exclusive
	 * @param minBlock Minimum size of a block
	 * @param consumer The consumer
	 */
	public static void loopBlocks(int start , int endExclusive , int minBlock,
								  IntRangeConsumer consumer ) {
		final ForkJoinPool pool = BoofConcurrency.pool;
		int numThreads = pool.getParallelism();

		int range = endExclusive-start;
		if( range == 0 ) // nothing to do here!
			return;
		if( range < 0 )
			throw new IllegalArgumentException("end must be more than start. "+start+" -> "+endExclusive);

		int block = selectBlockSize(range,minBlock,numThreads);

		try {
			pool.submit(new IntRangeTask(start,endExclusive,block,consumer)).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

	static int selectBlockSize( int range , int minBlock , int numThreads ) {
		// attempt to split the load between each thread equally
		int block = Math.max(minBlock,range/numThreads);
		// now attempt to make each block the same size
		int N = Math.max(1,range/block);
		return range/N;
	}

	/**
	 * Splits the range of values up into blocks. It's assumed the cost to process a block is small so
	 * more can be created.
	 * @param start First index, inclusive
	 * @param endExclusive Last index, exclusive
	 * @param consumer The consumer
	 */
	public static void loopBlocks(int start , int endExclusive , IntRangeConsumer consumer ) {
		final ForkJoinPool pool = BoofConcurrency.pool;
		int numThreads = pool.getParallelism();

		int range = endExclusive-start;
		if( range == 0 ) // nothing to do here!
			return;
		if( range < 0 )
			throw new IllegalArgumentException("end must be more than start. "+start+" -> "+endExclusive);

		// Did some experimentation here. Gave it more threads than were needed or exactly what was needed
		// exactly seemed to do better in the test cases
		int blockSize = Math.max(1,range/numThreads);

		try {
			pool.submit(new IntRangeTask(start,endExclusive,blockSize,consumer)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Computes sums up the results using the specified primitive type
	 *
	 * @param start First index, inclusive
	 * @param endExclusive Last index, exclusive
	 * @param type Primtive data type, e.g. int.class, float.class, double.class
	 * @param producer Given an integer input produce a Number output
	 * @return The sum
	 */
	public static Number sum(int start , int endExclusive , Class type, IntProducerNumber producer ) {
		try {
			return pool.submit(new IntOperatorTask.Sum(start,endExclusive,type,producer)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Computes the maximum value
	 *
	 * @param start First index, inclusive
	 * @param endExclusive Last index, exclusive
	 * @param type Primtive data type, e.g. int.class, float.class, double.class
	 * @param producer Given an integer input produce a Number output
	 * @return The sum
	 */
	public static Number max(int start , int endExclusive , Class type, IntProducerNumber producer ) {
		try {
			return pool.submit(new IntOperatorTask.Max(start,endExclusive,type,producer)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Computes the maximum value
	 *
	 * @param start First index, inclusive
	 * @param endExclusive Last index, exclusive
	 * @param type Primtive data type, e.g. int.class, float.class, double.class
	 * @param producer Given an integer input produce a Number output
	 * @return The sum
	 */
	public static Number min(int start , int endExclusive , Class type, IntProducerNumber producer ) {
		try {
			return pool.submit(new IntOperatorTask.Min(start,endExclusive,type,producer)).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

}
