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

	public AssociatedTriple( Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3 ) {
		this(p1, p2, p3, true);
	}

	public AssociatedTriple( Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3, boolean newInstance ) {
		if (newInstance) {
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

	public void setTo( AssociatedTriple a ) {
		p1.setTo(a.p1);
		p2.setTo(a.p2);
		p3.setTo(a.p3);
	}

	public void setTo( Point2D_F64 p1, Point2D_F64 p2, Point2D_F64 p3 ) {
		this.p1.setTo(p1);
		this.p2.setTo(p2);
		this.p3.setTo(p3);
	}

	public void setTo( double x1, double y1, double x2, double y2, double x3, double y3 ) {
		this.p1.setTo(x1, y1);
		this.p2.setTo(x2, y2);
		this.p3.setTo(x3, y3);
	}

	public AssociatedTriple copy() {
		AssociatedTriple r = new AssociatedTriple();
		r.setTo(this);
		return r;
	}

	public Point2D_F64 get( int i ) {
		return switch (i) {
			case 0 -> p1;
			case 1 -> p2;
			case 2 -> p3;
			default -> throw new IllegalArgumentException("index must be 0,1,2");
		};
	}

	public void set( int i, double x, double y ) {
		switch (i) {
			case 0 -> p1.setTo(x, y);
			case 1 -> p2.setTo(x, y);
			case 2 -> p3.setTo(x, y);
			default -> throw new IllegalArgumentException("index must be 0,1,2");
		}
	}

	public boolean isIdentical( AssociatedTriple o, double tol ) {
		if (tol < o.p1.distance(p1))
			return false;
		if (tol < o.p2.distance(p2))
			return false;
		return tol >= o.p3.distance(p3);
	}

	public void print() {
		System.out.println("AssociatedTriple( " + p1 + " , " + p2 + " , " + p3 + " )");
	}
}
