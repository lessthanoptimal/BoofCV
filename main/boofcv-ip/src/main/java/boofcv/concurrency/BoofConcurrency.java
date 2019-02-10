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
	public static boolean USE_CONCURRENT = false;

	// Custom thread pool for streams so that the number of threads can be controlled
	private static ForkJoinPool pool = new ForkJoinPool();

	public static void setMaxThreads( int maxThreads ) {
		pool = new ForkJoinPool(maxThreads);
	}

	public static void range(int start , int endExclusive , IntConsumer consumer ) {
		try {
			pool.submit(() ->IntStream.range(start, endExclusive).parallel().forEach(consumer)).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}

}
