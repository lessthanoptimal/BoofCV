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

package boofcv.struct.lists;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread safe stack for creating and recycling memory
 *
 * @author Peter Abeles
 */
public class RecycleStack<T> {
	List<T> list = new ArrayList<>();
	Factory<T> factory;

	public RecycleStack( Factory<T> factory ) {
		this.factory = factory;
	}

	/**
	 * Frees all memory referenced internally and starts from fresh
	 */
	public synchronized void purge() {
		list = new ArrayList<>();
	}

	/**
	 * Returns an instance. If there are instances queued up internally one of those is returned. Otherwise
	 * a new instance is created.
	 *
	 * @return object instance
	 */
	public synchronized T pop() {
		if (list.isEmpty()) {
			return factory.newInstance();
		} else {
			return list.remove(list.size() - 1);
		}
	}

	/**
	 * Recycles the object for later use
	 *
	 * @param object The object that's to be recycled
	 */
	public synchronized void recycle( T object ) {
		list.add(object);
	}

	public interface Factory<T> {
		T newInstance();
	}
}
