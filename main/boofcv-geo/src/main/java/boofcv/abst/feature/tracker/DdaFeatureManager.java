/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;

import boofcv.abst.feature.describe.DescriptorInfo;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

/**
 * Interface while provides {@link DetectDescribeAssociate} a specific implementation for detecting features and
 * managing descriptions.
 *
 * @author Peter Abeles
 */
public interface DdaFeatureManager<I extends ImageGray<I>, Desc extends TupleDesc>
		extends DescriptorInfo<Desc>
{
	/**
	 * Detect features in the input image and pass the results into the two lists.  locDst and featDst
	 * are assumed to have been reset prior to this function being called.
	 *
	 * @param input Input image.
	 */
	void detectFeatures( I input );

	/**
	 * Used to get features from one set
	 *
	 * @param set Which set of features should it retrieve
	 * @param locations Location of each feature
	 * @param descriptions Description of each feature
	 */
	void getFeatures( int set , FastQueue<Point2D_F64> locations , FastQueue<Desc> descriptions );

	int getNumberOfSets();
}
