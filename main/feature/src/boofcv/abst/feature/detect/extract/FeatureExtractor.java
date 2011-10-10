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

package boofcv.abst.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;


/**
 * <p>
 * Extracts features from an intensity image.  The intensity image indicates the location of features
 * across the image based the intensity value.  Typically local maximums are considered to be the location of
 * features.
 * </p>
 *
 * <p>
 * There are many different ways in which features can be extracted.  For example, depending on the application, having features
 * spread across the whole image can be more advantageous than simply selecting the features with the highest
 * intensity can be preferred.  This interface is designed to allow a diverse set of algorithms to be used.
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
 * <p>
 * DESIGN NOTES:<br>
 * The input border is specified because its faster to ignore it internally than create a sub-image,
 * adjust candidate and output feature positions.  VERIFY THIS WITH BENCHMARK?
 * </p>
 *
 * @author Peter Abeles
 */
public interface FeatureExtractor {

	/**
	 * Process a feature intensity image to extract the point features.  If a pixel has an intensity
	 * value == Float.MAX_VALUE it is be ignored.
	 *
	 * @param intensity	Feature intensity image.  Can be modified.
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
	 * Specify the size of the image border which it will not process.  If the extractor
	 * is configured to only extract whole regions then this will increase the size
	 * of its own ignore region to avoid these border pixels.
	 *
	 * @param border Border size in pixels.
	 */
	public void setInputBorder(int border);

	/**
	 * Returns the size of the image border which is not processed.
	 *
	 * @return border size
	 */
	public int getInputBorder();

	/**
	 * Can it detect features which are inside the image border.  For example if a feature
	 * has a radius of 5, but there is a local max at 2, should that be returned?  Can
	 * the output handle feature descriptors which are partially inside the image?
	 *
	 * @return If it can detect features inside the image border.
	 */
	public boolean canDetectBorder();
}
