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

import java.util.Iterator;
import java.util.List;

/**
 * Implements {@link Iterator} for a range of numbers in a List.
 *
 * @author Peter Abeles
 */
public class IteratorRange<T> implements IteratorReset<T> {
	// List it's iterating through
	List<T> list;
	// low extent and upper extent
	int idx0, idx1;

	// The next index will return
	int index;

	public IteratorRange( List<T> list, int idx0, int idx1 ) {
		this.list = list;
		this.idx0 = idx0;
		this.idx1 = idx1;
		this.index = idx0;
	}

	public void reset( int idx0, int idx1 ) {
		this.idx0 = idx0;
		this.idx1 = idx1;
		this.index = idx0;
	}

	@Override public void reset() {
		this.index = idx0;
	}

	@Override public boolean hasNext() {
		return index < idx1;
	}

	@Override public T next() {
		return list.get(index++);
	}
}
