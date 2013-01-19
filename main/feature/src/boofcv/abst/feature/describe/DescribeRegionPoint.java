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

package boofcv.abst.feature.describe;

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;


/**
 * Describes the region around a point in the image.  The number of features used to compute the descriptor
 * is algorithm and scale dependent. The object's scale is defined as the scale according to scale-space theory.
 * Thus, scale is the magnitude of Gaussian blur when the feature was detected.
 *
 * @author Peter Abeles
 */
public interface DescribeRegionPoint<T extends ImageSingleBand, Desc extends TupleDesc>
	extends DescriptorInfo<Desc>
{

	/**
	 * Specified the image which is to be processed.
	 *
	 * @param image The image which contains the features.
	 */
	public void setImage( T image );

	/**
	 * Checks to see if a description can be extracted at the specified location.  Some descriptors
	 * cannot handle intersections with the image boundary and will crash if there is an intersection.
	 *
	 * @return true if the feature is inside the legal bounds and can be processed.
	 */
	public boolean isInBounds(double x , double y , double orientation , double scale);

	/**
	 * Extract feature information from point at the specified scale.  Before this function
	 * is called {@link #isInBounds(double, double,double, double)} should be called it make
	 * sure it is safe to compute the descriptor.
	 *
	 * @param x Coordinate of the point.
	 * @param y Coordinate of the point.
	 * @param orientation Direction the feature is pointing at in radians. 0 = x-axis PI/2 = y-axis
	 * @param scale Scale at which the feature was found.
	 * @param ret Storage for extracted feature.  If null a new descriptor will be declared and returned..
	 * @return The descriptor.
	 */
	public Desc process( double x , double y , double orientation , double scale , Desc ret );

	/**
	 * If scale information is used when computing the descriptor.
	 *
	 * @return if scale needs to be provided or not
	 * @deprecated Likely to be removed in the near future.  if this flag is true or not won't change the input it
	 * gets when paired with {@link boofcv.abst.feature.detect.interest.InterestPointDetector}
	 */
	public boolean requiresScale();

	/**
	 * True if the descriptor uses orientation information.
	 *
	 * @deprecated Likely to be removed in the near future.  if this flag is true or not won't change the input it
	 * gets when paired with {@link boofcv.abst.feature.detect.interest.InterestPointDetector}
	 * @return if orientation needs to be provided or not
	 */
	public boolean requiresOrientation();
}
