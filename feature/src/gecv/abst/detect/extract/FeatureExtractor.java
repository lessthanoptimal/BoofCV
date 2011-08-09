/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.extract;

import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;


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
 * which is useful in scale-space analysis where the threshold will vary for each level.
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
	 * Process a feature intensity image to extract the point features.
	 *
	 * @param intensity	Feature intensity image.  Can be modified.
	 * @param requestedNumber Number of features it should find.  Not always supported.
	 * @param excludeCorners Features which should not be selected again.  Can be null. Not always supported.
	 * @param foundFeature Features which were found.
	 */
	public void process(ImageFloat32 intensity, QueueCorner candidate,
						int requestedNumber ,
						QueueCorner excludeCorners ,
						QueueCorner foundFeature);

	/**
	 * If it requires a list of candidate corners.
	 *
	 * @return true if candidates are required.
	 */
	public boolean getUsesCandidates();

	/**
	 * Returns if the excluded list is used or not.
	 *
	 * @return true if features can be excluded.
	 */
	public boolean getCanExclude();

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
	 * If supported, change the border around the image which is ignored.
	 */
	public void setIgnoreBorder( int border );

}
