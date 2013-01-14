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

package boofcv.abst.feature.detect.interest;

import georegression.struct.point.Point2D_F64;

/**
 * List of detected features that are invariant to scale and in-plane rotation.  PointSO is short for
 * Point Scale-Orientation.
 *
 * @author Peter Abeles
 */
public interface FoundPointSO {
	/**
	 * Returns the number of interest points found.
	 *
	 * @return Number of interest points.
	 */
	int getNumberOfFeatures();

	/**
	 * <p>
	 * The center location of the feature inside the image.
	 * </p>
	 * <p>
	 * WARNING: Do not save the returned reference, copy instead.  The returned point can be recycled each time
	 * this function is called.
	 * </p>
	 *
	 * @param featureIndex The feature's index.
	 * @return Location of the feature in image pixels.
	 */
	Point2D_F64 getLocation( int featureIndex );

	/**
	 * <p>
	 * Scale of the detected feature in scale space.  This is the standard deviation of the Gaussian blur
	 * applied to the image when the feature was detected.  While the object's size is not exactly defined,
	 * multiplying this number by 2.5 or 3 is typically considered the object's radius.
	 * </p>
	 *
	 * <p>
	 * NOTE: In the future this might be changed to getRadius() to remove any ambiguity over the detected
	 * object's size.
	 * </p>
	 *
	 * @param featureIndex Feature whose scale is being requested.
	 * @return Size of the interest point relative to canonical size.
	 */
	double getScale( int featureIndex );

	/**
	 * Returns the features found orientation.
	 *
	 * @param featureIndex Feature whose
	 * @return Orientation in radians.
	 */
	double getOrientation( int featureIndex );
}
