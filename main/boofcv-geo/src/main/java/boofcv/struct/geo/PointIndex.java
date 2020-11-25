/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import georegression.struct.GeoTuple;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class for all PointIndex implementations.
 *
 * @author Peter Abeles
 */
public abstract class PointIndex<T extends PointIndex<T, P>, P extends GeoTuple<P>> {
	public @Getter final P p;
	public @Getter @Setter int index;

	protected PointIndex( P p ) {
		this.p = p;
	}

	public void setTo( P point, int index ) {
		this.p.setTo(point);
		this.index = index;
	}

	public void setTo( T src ) {
		this.p.setTo(src.p);
		this.index = src.index;
	}

	public abstract T copy();
}
