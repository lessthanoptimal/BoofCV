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

package boofcv.abst.feature.detdesc;

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

/**
 * Detects and describes different types of features inside an image.  All detected features are described using the
 * same type of descriptor.  Each type of detected feature is a member of a set and the found features
 * for a set are returned inside of a {@link PointDescSet}.  The order and number of sets remains constant.
 *
 * <TD> Type of feature descriptor
 * @author Peter Abeles
 */
public interface DetectDescribeMulti<T extends ImageGray, Desc extends TupleDesc>
	extends DescriptorInfo<Desc>
{

	/**
	 * Detects features inside the image.
	 *
	 * @param image Image being processed.
	 */
	public void process( T image );

	/**
	 * The number of feature sets.
	 *
	 * @return number of feature sets
	 */
	public int getNumberOfSets();

	/**
	 * Returns the most recently detected features for a specific set.  Each time
	 * {@link #process(ImageGray)} is called the results are modified.
	 * </p>
	 * WARNING: The returned data structure is recycled each time {@link #process(ImageGray)}
	 * is called.  Create a copy if this is a problem.
	 *
	 * @param set Which set of detected features.
	 * @return Results for a set.
	 */
	public PointDescSet<Desc> getFeatureSet(int set);
}
