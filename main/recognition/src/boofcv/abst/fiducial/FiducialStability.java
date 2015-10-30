/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.fiducial;

/**
 * Results from fiducial stability computation.
 *
 * @author Peter Abeles
 */
public class FiducialStability {
	/**
	 * Sensitivity of location estimate.  Represents the maximum error found at the given pixel error in the fiducial's
	 * native units.  Larger number means less stable the estimate is.
	 */
	public double location;
	/**
	 * Sensitivity of orientation estimate.  Represents the maximum error found at the given pixel error in the radians.
	 * Larger number means less stable the estimate is.
	 */
	public double orientation;

	public FiducialStability(double location, double orientation) {
		this.location = location;
		this.orientation = orientation;
	}

	public FiducialStability() {
	}

	@Override
	public String toString() {
		return "FiducialStability{" +
				"location=" + location +
				", orientation=" + orientation +
				'}';
	}
}
