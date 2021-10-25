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

package boofcv.misc;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for a set of variables which are all controlled by a single lock
 *
 * @author Peter Abeles
 */
public class VariableLockSet {
	protected ReentrantLock lock = new ReentrantLock();

	public void safe( Runnable r ) {
		lock.lock();
		try {
			r.run();
		} finally {
			lock.unlock();
		}
	}

	public <T> T select( SelectObject<T> select ) {
		lock.lock();
		try {
			return select.select();
		} finally {
			lock.unlock();
		}
	}

	public void lock() {lock.lock();}

	public void unlock() {lock.unlock();}

	/** Returns true if the lock is active */
	public boolean isLocked() {return lock.isLocked();}

	@FunctionalInterface public interface SelectObject<T> {
		T select();
	}
}
