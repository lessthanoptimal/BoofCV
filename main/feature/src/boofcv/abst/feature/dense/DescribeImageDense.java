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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

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
	 * @param descriptions (Output) Storage for descriptors.  The grow() command is used to request more data.
	 * @param locations (output) (Optional) Storage for location of feature sample center points.
	 *                  New locations are added by invoking grow().   If null then it's ignored.
	 */
	public void process( T input , FastQueue<Desc> descriptions , FastQueue<Point2D_I32> locations );

	/**
	 * Description of the type of image it can process
	 *
	 * @return ImageDataType
	 */
	public ImageType<T> getImageType();
}
