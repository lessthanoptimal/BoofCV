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

package boofcv.abst.feature.detdesc;

import boofcv.struct.feature.TupleDesc;
import georegression.struct.point.Point2D_F64;

/**
 * A set of point image features which were detected and described using the same techniques.
 *
 * @author Peter Abeles
 */
public interface PointDescSet<TD extends TupleDesc> {

	/**
	 * Returns the number of detected features
	 *
	 * @return Number of interest points.
	 */
	public int getNumberOfFeatures();

	/**
	 * <p>
	 * The center location of the feature inside the image.
	 * </p>
	 * <p>
	 * WARNING: The returned point is overwritten when a new image is processed.
	 * </p>
	 *
	 * @param featureIndex The feature's index.
	 * @return Location of the feature in image pixels.
	 */
	public Point2D_F64 getLocation( int featureIndex );

	/**
	 * <p>Returns the description of the specified feature.</p>
	 *
	 * <p>
	 * WARNING: The returned description will be overwritten when a new image is processed. Create a copy if this
	 * is a problem.
	 * </p>
	 *
	 * @param index Which feature
	 * @return Feature descriptor
	 */
	public TD getDescription( int index );
}
