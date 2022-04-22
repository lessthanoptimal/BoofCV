/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point3D_F64;
import lombok.Getter;

/**
 * <p>
 * The observed location of a feature in two camera views. Typically a point in homogenous coordinates a line.
 * </p>
 *
 * @author Peter Abeles
 */
public class AssociatedPair3D {

	/** Location of the feature in the first image */
	@Getter public Point3D_F64 p1;
	/** Location of the feature in the second image. */
	@Getter public Point3D_F64 p2;

	public AssociatedPair3D() {
		p1 = new Point3D_F64();
		p2 = new Point3D_F64();
	}

	/**
	 * Creates a new associated point from the two provided points.
	 *
	 * @param x1 image 1 location x-axis.
	 * @param y1 image 1 location y-axis.
	 * @param x2 image 2 location x-axis.
	 * @param y2 image 2 location y-axis.
	 */
	public AssociatedPair3D( double x1, double y1, double z1,
							 double x2, double y2, double z2 ) {
		p1 = new Point3D_F64(x1, y1, z1);
		p2 = new Point3D_F64(x2, y2, z2);
	}

	/**
	 * Assigns the value to the two passed in features. A copy of the features is made.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 */
	public AssociatedPair3D( Point3D_F64 p1, Point3D_F64 p2 ) {
		this(p1, p2, true);
	}

	/**
	 * Allows features to either be copied or saved as references.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 * @param newInstance Should it create new points or save a reference to these instances.
	 */
	public AssociatedPair3D( Point3D_F64 p1, Point3D_F64 p2, boolean newInstance ) {
		if (newInstance) {
			this.p1 = new Point3D_F64(p1);
			this.p2 = new Point3D_F64(p2);
		} else {
			this.p1 = p1;
			this.p2 = p2;
		}
	}

	public static AssociatedPair3D wrap( Point3D_F64 p1, Point3D_F64 p2 ) {
		return new AssociatedPair3D(p1, p2, false);
	}

	public AssociatedPair3D setTo( AssociatedPair3D original ) {
		this.p1.setTo(original.p1);
		this.p2.setTo(original.p2);
		return this;
	}

	/**
	 * Assigns this object to be equal to the passed in values.
	 */
	public AssociatedPair3D setTo( Point3D_F64 p1, Point3D_F64 p2 ) {
		this.p1.setTo(p1);
		this.p2.setTo(p2);
		return this;
	}

	/**
	 * Assigns this object to be equal to the passed in values.
	 */
	public AssociatedPair3D setTo( double x1, double y1, double z1,
								   double x2, double y2, double z2 ) {
		this.p1.setTo(x1, y1, z1);
		this.p2.setTo(x2, y2, z2);
		return this;
	}

	/** Rescale the points so that their norm is 1 */
	public void normalizePoints() {
		this.p1.divideIP(this.p1.norm());
		this.p2.divideIP(this.p2.norm());
	}

	/**
	 * Changes the references to the passed in objects.
	 */
	public void assign( Point3D_F64 p1, Point3D_F64 p2 ) {
		this.p1 = p1;
		this.p2 = p2;
	}

	/** Sets internal points to zero */
	public void zero() {
		this.p1.zero();
		this.p2.zero();
	}

	public AssociatedPair3D copy() {
		return new AssociatedPair3D(p1, p2, true);
	}

	@Override
	public String toString() {
		return "AssociatedPair3D{" +
				"p1=" + p1 +
				", p2=" + p2 +
				'}';
	}
}
