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
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;


/**
 * Computes a description of the local region around a point.  Scale sample regions size relative to an implementation
 * specific standard.  This if a region is sampled with a radius of 10 pixels at scale of one, it will be 25 pixels
 * at a scale of 2.5.  Orientation rotates the sample points.  Exactly how and if scale and orientation are used is
 * implementation specific.
 *
 * @author Peter Abeles
 */
public interface DescribeRegionPoint<T extends ImageBase, Desc extends TupleDesc>
	extends DescriptorInfo<Desc>
{
	/**
	 * Specified the image which is to be processed.
	 *
	 * @param image The image which contains the features.
	 */
	public void setImage( T image );

	/**
	 * Extract a description of the local image at the given point, scale, and orientation.
	 *
	 * WARNING: Check the returned value to make sure a description was actually computed.  Some implementations
	 * might now allow features to extend outside the image border and will return false.
	 *
	 * @param x Coordinate of the point.
	 * @param y Coordinate of the point.
	 * @param orientation Direction the feature is pointing at in radians. 0 = x-axis PI/2 = y-axis
	 * @param scale Scale at which the feature was found.
	 * @param description (output) Storage for extracted feature.  Use {@link #createDescription} to create descriptor.
	 * @return true if a descriptor can computed or false if not.
	 */
	public boolean process( double x , double y , double orientation , double scale , Desc description );

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

	/**
	 * Description of the type of image it can process
	 *
	 * @return ImageDataType
	 */
	public ImageType<T> getImageType();
}
