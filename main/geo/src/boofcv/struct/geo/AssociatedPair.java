/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
 * Contains the location of a point feature in an image in the key frame and the current frame.
 * Useful for applications where the motion or structure of a scene is computed between
 * two images.
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
	 * Constructor which allows the points to not be declared.
	 *
	 * @param declare If true then new points will be declared
	 */
	public AssociatedPair( boolean declare ) {
		if( declare ) {
			p1 = new Point2D_F64();
			p2 = new Point2D_F64();
		}
	}

	/**
	 * Creates a new associated point from the two provided points.
	 *
	 * @param x1 image 1 location x-axis.
	 * @param y1 image 1 location y-axis.
	 * @param x2 image 2 location x-axis.
	 * @param y2 image 2 location y-axis.
	 */
	public AssociatedPair(double x1, double y1,
						  double x2, double y2) {
		p1 = new Point2D_F64(x1, y1);
		p2 = new Point2D_F64(x2, y2);
	}

	/**
	 * Creates a new associated point from the two provided points.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 */
	public AssociatedPair(Point2D_F64 p1, Point2D_F64 p2) {
		this(p1, p2,true);
	}

	/**
	 * Creates a new associated point from the two provided points.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 * @param newInstance Should it create new points or save a reference to these instances.
	 */
	public AssociatedPair(Point2D_F64 p1, Point2D_F64 p2, boolean newInstance) {
		if (newInstance) {
			this.p1 = new Point2D_F64(p1);
			this.p2 = new Point2D_F64(p2);
		} else {
			this.p1 = p1;
			this.p2 = p2;
		}
	}

	/**
	 * Sets the value of p1 and p2 to be equal to the values of the passed in objects
	 */
	public void set( Point2D_F64 p1 , Point2D_F64 p2 ) {
		this.p1.set(p1);
		this.p2.set(p2);
	}

	/**
	 * Sets the value of p1 and p2 to be equal to the values of the passed in objects
	 */
	public void set( double p1_x , double p1_y , double p2_x , double p2_y ) {
		this.p1.set(p1_x,p1_y);
		this.p2.set(p2_x,p2_y);
	}

	/**
	 * Sets p1 and p2 to reference the passed in objects.
	 */
	public void assign( Point2D_F64 p1 , Point2D_F64 p2 ) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public Point2D_F64 getP1() {
		return p1;
	}

	public Point2D_F64 getP2() {
		return p2;
	}
}
