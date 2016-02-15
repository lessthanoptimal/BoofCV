/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;

import java.util.List;

// TODO provide way for selecting sampling method  REGULAR_GRID, MAX_SPREAD
// TODO easy way to know coordinate for grid image

/**
 * Computes feature descriptors across the whole image.  No feature detection is performed.  Descriptions are typically
 * computed at regular intervals.  Scale, orientation, and other local geometric information is typically not computed
 * or returned to the user.
 *
 * @author Peter Abeles
 */
public interface DescribeImageDense<T extends ImageBase, Desc extends TupleDesc>
	extends DescriptorInfo<Desc>
{
	/**
	 * Processes the image and computes the dense image features.
	 *
	 * @param input Input image.
	 */
	void process( T input );

	/**
	 * <p>Returns a list of the computed descriptions.</p>
	 *
	 * <p>The list and everything contained inside of it are owned by this class and subject to modification
	 * the next time {@link #process(ImageBase)} is called.</p>
	 *
	 * @return list of descriptions
	 */
	List<Desc> getDescriptions();

	/**
	 * <p>Returns a list of locations that the descriptors are computed at</p>
	 *
	 * <p>The list and everything contained inside of it are owned by this class and subject to modification
	 * the next time {@link #process(ImageBase)} is called.</p>
	 *
	 * @return list of descriptions
	 */
	List<Point2D_I32> getLocations();

	/**
	 * Description of the type of image it can process
	 *
	 * @return ImageDataType
	 */
	ImageType<T> getImageType();
}
