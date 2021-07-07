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

package boofcv.abst.feature.describe;

import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * High level interface for describing the region around a point when given the pixel coordinate of the point
 * only.
 *
 * @author Peter Abeles
 */
public interface DescribePoint<T extends ImageBase<T>, TD extends TupleDesc<TD>>
		extends DescriptorInfo<TD> {
	/**
	 * Specified the image which is to be processed.
	 *
	 * @param image The image which contains the features.
	 */
	void setImage( T image );

	/**
	 * Extract a description of the local image at the the pixel coordinate of the point.
	 *
	 * WARNING: Check the returned value to make sure a description was actually computed. Some implementations
	 * might now allow features to extend outside the image border and will return false.
	 *
	 * @param x Coordinate of the point.
	 * @param y Coordinate of the point.
	 * @param description (output) Storage for extracted feature. Use {@link #createDescription} to create descriptor.
	 * @return true if a descriptor can computed or false if not.
	 */
	boolean process( double x, double y, TD description );

	/**
	 * Description of the type of image it can process
	 *
	 * @return ImageDataType
	 */
	ImageType<T> getImageType();
}
