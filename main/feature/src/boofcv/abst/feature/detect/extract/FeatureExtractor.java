/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;


/**
 * <p>
 * Detects features in an intensity image as local maximums.  The extractor can be configured to ignore pixels
 * along the image's border.  Some implementations can be significantly speed up by using candidate features previously
 * computed.  If a pixel's value is less than the minimum pixel intensity it cannot be a feature even if it is a local
 *
 * </p>
 *
 * <p>
 * Almost all feature extraction algorithms use a threshold to determine what can be a feature or not.  Pixels
 * whose intensity is less than the threshold cannot be a feature.  This interface allows the threshold to be changed,
 * which is useful in scale-space analysis where the threshold will vary for each level.  In addition, if
 * a pixel has an intensity of Float.MAX_VALUE then it is ignored and not returned.  This technique is often used to
 * ignore features which have already been detected.
 * </p>
 *
 * <p>
 * Depending in the implementation the following may or may not be supported:
 * <ul>
 * <li> Ignore existing corners.  Corners which are passed in will be ignored. </li>
 * <li> Return the specified number of features, always. </li>
 * </ul>
 *
 * @author Peter Abeles
 */
public interface FeatureExtractor {

	/**
	 * Process a feature intensity image to extract the point features.  If a pixel has an intensity
	 * value == Float.MAX_VALUE it is be ignored.
	 *
	 * @param intensity	Feature intensity image.  Can be modified.
	 * @param candidate Optional list of candidate features computed with the intensity image.
	 * @param requestedNumber Number of features it should find.  Not always supported.
	 * @param foundFeature Features which were found.
	 */
	public void process(ImageFloat32 intensity, QueueCorner candidate,
						int requestedNumber ,
						QueueCorner foundFeature);

	/**
	 * If it requires a list of candidate corners.
	 *
	 * @return true if candidates are required.
	 */
	public boolean getUsesCandidates();

	/**
	 * If it accepts requests to find a specific number of features or not.
	 * @return If requests are accepted for number of features.
	 */
	public boolean getAcceptRequest();

	/**
	 * Features must have the specified threshold.
	 *
	 * @return threshold for feature selection
	 */
	public float getThreshold();

	/**
	 * Change the feature selection threshold.
	 *
	 * @param threshold The new selection threshold.
	 */
	public void setThreshold( float threshold );

	/**
	 * Pixels which are within 'border' of the image border will not be considered.
	 *
	 * @param border Border size in pixels.
	 */
	public void setIgnoreBorder(int border);

	/**
	 * Returns the size of the image border which is not processed.
	 *
	 * @return border size
	 */
	public int getIgnoreBorder();

	/**
	 * Can it detect features which are inside the image border.  For example if a feature
	 * has a radius of 5, but there is a local max at 2, should that be returned?  Can
	 * the output handle feature descriptors which are partially inside the image?
	 *
	 * @return If it can detect features inside the image border.
	 */
	public boolean canDetectBorder();

	/**
	 * Describes how large the region is that is being searched.  The radius is the number of
	 * pixels away from the center.
	 *
	 * @return Search radius
	 */
	public int getSearchRadius();
}
