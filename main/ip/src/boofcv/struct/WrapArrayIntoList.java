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

package boofcv.struct;

import java.util.AbstractList;

/**
 * Wraps an array into a list and allows the size to be set.
 *
 * @author Peter Abeles
 */
public class WrapArrayIntoList<T> extends AbstractList<T> {
	
	T[] data;
	int size;

	public WrapArrayIntoList(T[] data, int size) {
		this.data = data;
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size != 0;
	}

	@Override
	public T get(int index) {
		return data[index];
	}
}
