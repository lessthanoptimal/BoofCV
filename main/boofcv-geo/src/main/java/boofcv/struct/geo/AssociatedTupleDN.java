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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Associated set of {@link Point2D_F64} for an arbitrary number of views which can be changed.
 *
 * @author Peter Abeles
 */
public class AssociatedTupleDN implements AssociatedTuple {
	/** Set of associated observations */
	public final DogArray<Point2D_F64> p;

	public AssociatedTupleDN( int num ) {
		p = new DogArray<>(num, Point2D_F64::new);
	}

	public AssociatedTupleDN() {
		this(0);
	}

	@Override
	public double getX( int index ) {
		return p.data[index].x;
	}

	@Override
	public double getY( int index ) {
		return p.data[index].y;
	}

	@Override
	public Point2D_F64 get( int index ) {
		return p.data[index];
	}

	@Override
	public void set( int index, double x, double y ) {
		p.data[index].setTo(x, y);
	}

	@Override
	public void set( int index, Point2D_F64 src ) {
		p.data[index].setTo(src);
	}

	@Override
	public int size() {
		return p.size;
	}

	public void resize( int newSize ) {
		if (p.size == newSize)
			return;
		p.resize(newSize);
	}

	@Override
	public void setTo( AssociatedTuple src ) {
		p.resize(src.size());

		for (int i = 0; i < p.size; i++) {
			p.data[i].setTo(src.get(i));
		}
	}
}
