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

import georegression.struct.curve.ConicGeneral_F64;

/**
 * <p>
 * The observed location of a conic feature in two camera views. Can be in pixels or normalized image coordinates.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AssociatedPairConic {

	/**
	 * Location of the feature in the first image
	 */
	public ConicGeneral_F64 p1;
	/**
	 * Location of the feature in the second image.
	 */
	public ConicGeneral_F64 p2;

	public AssociatedPairConic() {
		p1 = new ConicGeneral_F64();
		p2 = new ConicGeneral_F64();
	}

	/**
	 * Constructor which allows the points to not be declared.
	 *
	 * @param declare If true then new points will be declared
	 */
	public AssociatedPairConic( boolean declare ) {
		if (declare) {
			p1 = new ConicGeneral_F64();
			p2 = new ConicGeneral_F64();
		}
	}

	/**
	 * Assigns the value to the two passed in features. A copy of the features is made.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 */
	public AssociatedPairConic( ConicGeneral_F64 p1, ConicGeneral_F64 p2 ) {
		this(p1, p2, true);
	}

	/**
	 * Allows features to either be copied or saved as references.
	 *
	 * @param p1 image 1 location
	 * @param p2 image 2 location
	 * @param newInstance Should it create new points or save a reference to these instances.
	 */
	public AssociatedPairConic( ConicGeneral_F64 p1, ConicGeneral_F64 p2, boolean newInstance ) {
		if (newInstance) {
			this.p1 = new ConicGeneral_F64(p1);
			this.p2 = new ConicGeneral_F64(p2);
		} else {
			this.p1 = p1;
			this.p2 = p2;
		}
	}

	public void setTo( AssociatedPairConic original ) {
		this.p1.setTo(original.p1);
		this.p2.setTo(original.p2);
	}

	/**
	 * Assigns this object to be equal to the passed in values.
	 */
	public void setTo( ConicGeneral_F64 p1, ConicGeneral_F64 p2 ) {
		this.p1.setTo(p1);
		this.p2.setTo(p2);
	}

	/**
	 * Changes the references to the passed in objects.
	 */
	public void assign( ConicGeneral_F64 p1, ConicGeneral_F64 p2 ) {
		this.p1 = p1;
		this.p2 = p2;
	}

	public ConicGeneral_F64 getP1() {
		return p1;
	}

	public ConicGeneral_F64 getP2() {
		return p2;
	}

	public AssociatedPairConic copy() {
		return new AssociatedPairConic(p1, p2, true);
	}

	@Override
	public String toString() {
		return "AssociatedPairConic{" +
				"p1=" + p1 +
				", p2=" + p2 +
				'}';
	}
}
