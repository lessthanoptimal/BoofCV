/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.abst.feature.describe;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;


/**
 * Describes the region around a point in the image.  The number of features used to compute the descriptor
 * is algorithm and scale dependent. To find out how many pixels the descriptor uses at a scale of one
 * call {@link #getCanonicalRadius()}.  The size at other scales is simply that number multiplied by the scale,
 * rounded up.
 *
 * @author Peter Abeles
 */
public interface DescribeRegionPoint<T extends ImageSingleBand> {

	/**
	 * Specified the image which is to be processed.
	 * 
	 * @param image The image which contains the features.
	 */
	public void setImage( T image );

	/**
	 * Returns the descriptor length
	 */
	public int getDescriptionLength();

	/**
	 * Returns the description's radius at a scale of 1
	 */
	public int getCanonicalRadius();

	/**
	 * Extract feature information from point at the specified scale.
	 *
	 * @param x Coordinate of the point.
	 * @param y Coordinate of the point.
	 * @param orientation Direction the feature is pointing at in radians. 0 = x-axis PI/2 = y-axis
	 * @param scale Scale at which the feature was found.
	 * @param ret Used to store the extracted feature.  If null a new instance will be created.
	 * @return  Description of the point.  If one could not be computed then null is returned.
	 */
	// todo would this be easier to use if it returned a boolean and required a descriptor be passed in
	public TupleDesc_F64 process( double x , double y , double orientation , double scale , TupleDesc_F64 ret );

	/**
	 *
	 * @return if scale needs to be provided or not
	 */
	public boolean requiresScale();

	/**
	 *
	 * @return if orientation needs to be provided or not
	 */
	public boolean requiresOrientation();
}
