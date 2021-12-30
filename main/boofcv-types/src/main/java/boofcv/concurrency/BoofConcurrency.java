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

package boofcv.concurrency;

import pabeles.concurrency.ConcurrencyOps;

/**
 * Central class for controlling concurrency in BoofCV.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"CatchAndPrintStackTrace"})
public class BoofConcurrency extends ConcurrencyOps {
	/** If an image has fewer pixels than this it will not run a concurrent algorithm. The overhead makes it slower. */
	public static int SMALL_IMAGE = 100*100;
	/** f set to true it will use a concurrent algorithm */
	public static boolean USE_CONCURRENT = true;

	/**
	 * Sets the maximum number of threads available in the thread pool and adjusts USE_CONCURRENT. If
	 * the number of threads is less than 2 then USE_CONCURRENT will be set to false and the single thread
	 * version of code will be called. Otherwise USE_CONCURRENT will be true and the max threads in the pool
	 * set to the specified number.
	 *
	 * @param maxThreads Maximum number of threads. &le 1 means it will not be threaded.
	 */
	public static void setMaxThreads( int maxThreads ) {
		ConcurrencyOps.setMaxThreads(maxThreads);
		USE_CONCURRENT = maxThreads > 1;
	}

	public static boolean isUseConcurrent() {
		return USE_CONCURRENT;
	}

	/**
	 * Either returns the number of threads in the thread pool or one if threading is disabled
	 */
	public static int getEffectiveActiveThreads() {
		if (USE_CONCURRENT)
			return getThreadPool().getActiveThreadCount();
		return 1;
	}
}
