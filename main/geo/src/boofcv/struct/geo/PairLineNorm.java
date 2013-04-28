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

import georegression.struct.point.Vector3D_F64;

/**
 * A pair of line observations found in two different images.  The line is described by a vector, which is the normal
 * of the plane defined by the line on the image plane and the camera's origin.  The cross product of the two end
 * points (in homogeneous pixels) would define a line in this manor.
 * 
 * @author Peter Abeles
 */
public class PairLineNorm {
	/**
	 * Location of the feature in the first image
	 */
	public Vector3D_F64 l1;
	/**
	 * Location of the feature in the second image.
	 */
	public Vector3D_F64 l2;

	public PairLineNorm() {
		l1 = new Vector3D_F64();
		l2 = new Vector3D_F64();
	}

	/**
	 * Constructor which allows the lines to not be declared.
	 *
	 * @param declare If true then new lines will be declared
	 */
	public PairLineNorm(boolean declare) {
		if( declare ) {
			l1 = new Vector3D_F64();
			l2 = new Vector3D_F64();
		}
	}

	/**
	 * Creates a new instance by copying the values of the two lines.
	 *
	 * @param l1 image 1 location
	 * @param l2 image 2 location
	 */
	public PairLineNorm(Vector3D_F64 l1, Vector3D_F64 l2) {
		this(l1, l2,true);
	}

	/**
	 * Creates a new instance by either copying or saving a reference to the two lines
	 *
	 * @param l1 image 1 location
	 * @param l2 image 2 location
	 * @param newInstance Should it create new lines or save a reference to these instances.
	 */
	public PairLineNorm(Vector3D_F64 l1, Vector3D_F64 l2, boolean newInstance) {
		if (newInstance) {
			this.l1 = l1.copy();
			this.l2 = l2.copy();
		} else {
			this.l1 = l1;
			this.l2 = l2;
		}
	}

	/**
	 * Sets the value of p1 and p2 to be equal to the values of the passed in objects
	 */
	public void set( Vector3D_F64 l1 , Vector3D_F64 l2 ) {
		this.l1.set(l1);
		this.l2.set(l2);
	}

	/**
	 * Sets p1 and p2 to reference the passed in objects.
	 */
	public void assign( Vector3D_F64 l1 , Vector3D_F64 l2 ) {
		this.l1 = l1;
		this.l2 = l2;
	}

	public Vector3D_F64 getL1() {
		return l1;
	}

	public void setL1(Vector3D_F64 l1) {
		this.l1 = l1;
	}

	public Vector3D_F64 getL2() {
		return l2;
	}

	public void setL2(Vector3D_F64 l2) {
		this.l2 = l2;
	}
}
