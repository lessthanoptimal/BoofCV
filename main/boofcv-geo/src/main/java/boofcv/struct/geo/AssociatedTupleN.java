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

import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;

/**
 * Associated set of {@link Point2D_F64} for an arbitrary number of views that is fixed.
 *
 * @author Peter Abeles
 */
public class AssociatedTupleN implements AssociatedTuple {
	/** Set of associated observations */
	public final Point2D_F64[] p;

	public AssociatedTupleN( int num ) {
		p = new Point2D_F64[num];
		for (int i = 0; i < num; i++) {
			p[i] = new Point2D_F64();
		}
	}

	public AssociatedTupleN( final Point2D_F64... src ) {
		p = new Point2D_F64[src.length];
		for (int i = 0; i < src.length; i++) {
			p[i] = src[i].copy();
		}
	}

	@Override
	public double getX( int index ) {
		return p[index].x;
	}

	@Override
	public double getY( int index ) {
		return p[index].y;
	}

	@Override
	public Point2D_F64 get( int index ) {
		return p[index];
	}

	@Override
	public void set( int index, double x, double y ) {
		p[index].setTo(x, y);
	}

	@Override
	public void set( int index, Point2D_F64 src ) {
		p[index].setTo(src);
	}

	@Override
	public int size() {
		return p.length;
	}

	@Override
	public void setTo( AssociatedTuple src ) {
		BoofMiscOps.checkTrue(src.size() == size());

		for (int i = 0; i < p.length; i++) {
			p[i].setTo(src.get(i));
		}
	}
}
