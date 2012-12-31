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

package boofcv.struct.geo;

import georegression.struct.point.Point2D_F64;

/**
 * Contains a set of three observations of the same point feature in three different views.
 *
 * @author Peter Abeles
 */
public class AssociatedTriple {
	/** Observation in View 1 */
	public Point2D_F64 p1;
	/** Observation in View 2 */
	public Point2D_F64 p2;
	/** Observation in View 3 */
	public Point2D_F64 p3;

	public AssociatedTriple(Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3) {
		this(p1,p2,p3,true);
	}

	public AssociatedTriple(Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3 , boolean newInstance ) {
		if( newInstance ) {
			this.p1 = p1.copy();
			this.p2 = p2.copy();
			this.p3 = p3.copy();
		} else {
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
		}
	}

	public AssociatedTriple() {
		this.p1 = new Point2D_F64();
		this.p2 = new Point2D_F64();
		this.p3 = new Point2D_F64();
	}

	public void set( AssociatedTriple a ) {
		p1.set(a.p1);
		p2.set(a.p2);
		p3.set(a.p3);
	}

	public void set( Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3 ) {
		this.p1.set(p1);
		this.p2.set(p2);
		this.p3.set(p3);
	}

	public AssociatedTriple copy() {
		AssociatedTriple r = new AssociatedTriple();
		r.set(this);
		return r;
	}

	public void print() {
		System.out.println("AssociatedTriple( "+p1+" , "+p2+" , "+p3+" )");
	}
}
