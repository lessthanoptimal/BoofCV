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

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
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

	/**
	 * Safe but with a timeout for aquiring the lock
	 */
	public boolean safe( long timeoutMS, Runnable r ) {
		try {
			if (lock.tryLock(timeoutMS, TimeUnit.MILLISECONDS)) {
				r.run();
			} else {
				return false;
			}
		} catch (InterruptedException e) {
			return false;
		} finally {
			lock.unlock();
		}
		return true;
	}

	/**
	 * Selects an object with a timeout for acquiring the lock.
	 *
	 * @param timeoutMS How long it will wait to get the lock in milliseconds
	 * @param select Function used to select object
	 * @return Results which indicate of the lock was acquired and the found object
	 */
	public <T> SelectResults<T> select( long timeoutMS, SelectObject<T> select ) {
		var results = new SelectResults<T>();
		try {
			results.success = lock.tryLock(timeoutMS, TimeUnit.MILLISECONDS);
			if (results.success)
				results.selected = select.select();
		} catch (InterruptedException e) {
			results.success = false;
		} finally {
			lock.unlock();
		}
		return results;
	}

	public <T> @Nullable T selectNull( SelectObjectNull<T> select ) {
		lock.lock();
		try {
			return select.select();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Selects an object with a timeout for acquiring the lock.
	 *
	 * @param timeoutMS How long it will wait to get the lock in milliseconds
	 * @param select Function used to select object
	 * @return Results which indicate of the lock was acquired and the found object
	 */
	public <T> @Nullable SelectResults<T> selectNull( long timeoutMS, SelectObjectNull<T> select ) {
		var results = new SelectResults<T>();
		try {
			results.success = lock.tryLock(timeoutMS, TimeUnit.MILLISECONDS);
			if (results.success) {
				T found = select.select();
				if (found == null)
					return null;
				results.selected = found;
			}
		} catch (InterruptedException e) {
			results.success = false;
		} finally {
			lock.unlock();
		}
		return results;
	}

	public void lock() {lock.lock();}

	public void unlock() {lock.unlock();}

	/** Returns true if the lock is active */
	public boolean isLocked() {return lock.isLocked();}

	@FunctionalInterface public interface SelectObject<T> {
		T select();
	}

	@FunctionalInterface public interface SelectObjectNull<T> {
		@Nullable T select();
	}

	@SuppressWarnings("NullAway.Init")
	public static class SelectResults<T> {
		/** True if it was able to acquire a lock */
		public boolean success = false;
		/** The object it selected */
		public T selected;
	}
}
