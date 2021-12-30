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

package boofcv.alg.mvs;

import boofcv.misc.IteratorReset;
import boofcv.struct.geo.PointIndex;
import georegression.struct.GeoTuple;

import java.util.List;

/**
 * Specialized iterator that takes in a list of points but iterates over PointIndex. The same instance of a PointIndex
 * is returned every iteration to improve performance.
 *
 * @author Peter Abeles
 */
public class PointToIndexIterator<T extends PointIndex<T, P>, P extends GeoTuple<P>> implements IteratorReset<T> {
	List<P> list;
	int idx0, idx1;
	int index;
	T point;

	public PointToIndexIterator( List<P> list, int idx0, int idx1, T point ) {
		this.list = list;
		this.idx0 = idx0;
		this.idx1 = idx1;
		this.index = idx0;
		this.point = point;
	}

	@Override public void reset() {
		index = idx0;
	}

	public void setRange( int idx0, int idx1 ) {
		this.idx0 = idx0;
		this.idx1 = idx1;
	}

	@Override public boolean hasNext() {
		return index < idx1;
	}

	@Override public T next() {
		point.setTo(list.get(index), index++);
		return point;
	}
}
