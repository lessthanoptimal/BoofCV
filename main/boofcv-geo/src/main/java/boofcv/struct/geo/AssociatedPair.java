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

/**
 * <p>
 * The observed location of a point feature in two camera views. Can be in pixels or normalized image coordinates.
 * </p>
 *
 * @author Peter Abeles
 */
public class AssociatedPair {

	/**
	 * Location of the feature in the first image
	 */
	public Point2D_F64 p1;
	/**
	 * Location of the feature in the second image.
	 */
	public Point2D_F64 p2;

	public AssociatedPair() {
		p1 = new Point2D_F64();
		p2 = new Point2D_F64();
	}

	/**
	 * Creates a new associated point from the two provided points.
	 *
	 * @param x1 image 1 location x-axis.
	 * @param y1 image 1 location y-axis.
	 * @param x2 image 2 location x-axis.
	 * @param y2 image 2 location y-axis.
	 */
	public AssociatedPair( double x1, double y1,
						   double x2, double y2 ) {
		p1 = new Point2D_F64(x1, y1);
		p2 = new Point2D_F64(x2, y2);
	}

	/**
	 * Assigns the value to the two passed in features. A copy of the features is made.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 */
	public AssociatedPair( Point2D_F64 p1, Point2D_F64 p2 ) {
		this(p1, p2, true);
	}

	/**
	 * Allows features to either be copied or saved as references.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 * @param newInstance Should it create new points or save a reference to these instances.
	 */
	public AssociatedPair( Point2D_F64 p1, Point2D_F64 p2, boolean newInstance ) {
		if (newInstance) {
			this.p1 = new Point2D_F64(p1);
			this.p2 = new Point2D_F64(p2);
		} else {
			this.p1 = p1;
			this.p2 = p2;
		}
	}

	public void setTo( AssociatedPair original ) {
		this.p1.setTo(original.p1);
		this.p2.setTo(original.p2);
	}

	/**
	 * Assigns this object to be equal to the passed in values.
	 */
	public void setTo( Point2D_F64 p1, Point2D_F64 p2 ) {
		this.p1.setTo(p1);
		this.p2.setTo(p2);
	}

	/**
	 * Assigns this object to be equal to the passed in values.
	 */
	public void setTo( double p1_x, double p1_y, double p2_x, double p2_y ) {
		this.p1.setTo(p1_x, p1_y);
		this.p2.setTo(p2_x, p2_y);
	}

	/**
	 * Changes the references to the passed in objects.
	 */
	public void assign( Point2D_F64 p1, Point2D_F64 p2 ) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public Point2D_F64 getP1() {
		return p1;
	}

	public Point2D_F64 getP2() {
		return p2;
	}

	public AssociatedPair copy() {
		return new AssociatedPair(p1, p2, true);
	}

	/**
	 * Euclidean distance from p1 to p2
	 */
	public double distance() {
		return p1.distance(p2);
	}

	/**
	 * Euclidean distance squared from p1 to p2
	 */
	public double distance2() {
		return p1.distance2(p2);
	}

	@Override
	public String toString() {
		return "AssociatedPair{" +
				"p1=(" + p1.x + ", " + p1.y + ")" +
				", p2=(" + p2.x + ", " + p2.y + ")" +
				'}';
	}
}
