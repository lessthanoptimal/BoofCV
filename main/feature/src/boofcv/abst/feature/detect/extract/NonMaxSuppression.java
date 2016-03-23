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

package boofcv.abst.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;


/**
 * <p>
 * Detects local minimums and/or maximums in an intensity image inside square regions.  This is known as non-maximum
 * suppression.  The detector can be configured to ignore pixels along the image border by a user specified distance.
 * Some implementations require candidate locations for the features.  This allows for a sparse algorithm to be used,
 * resulting in a significant speed boost.  Pixel values with a value of -Float.MAX_VALUE or Float.MAX_VALUE will
 * not be considered for local minimum/maximum, respectively.  This is a good way to ignore previously detected
 * features.
 * </p>
 *
 * <p>
 * Not all implementations will search for both minimums or maximums.  Be sure you are using the correct one.  If
 * you don't intend on detecting a minimum or maximum pass in null for the candidate list and the output found list.
 * </p>
 *
 * <p>
 * An extractor which uses candidate features must always be provided them.  However, an algorithm which does not
 * use candidate features will simply ignore that input and operate as usual.  Can check capabilities at runtime
 * using the {@link #canDetectMinimums()} and {@link #canDetectMaximums()} functions.
 * </p>
 *
 * <p>
 * A border can be specified around the outside of the image in which extremes can't be detected.  However, a pixel
 * outside this border can influence if a pixel is a maximum inside, if the local search radius extends that far.
 * This is specified by the border parameter and the valid region is defined as follows:<br>
 * border &le; x &lt; width-border AND border &le; y &lt; height-border</p>
 *
 * @author Peter Abeles
 */
public interface NonMaxSuppression {

	/**
	 * Process a feature intensity image to extract the point features.  If a pixel has an intensity
	 * value == -Float.MAX_VALUE  or Float.MAX_VALUE it will not be considered for a local min or max, respectively.
	 * If an algorithm only detect local minimums or maximums and null can be passed in for unused lists.  This is
	 * the recommended procedure since it will force an exception to be thrown if a mistake was made.
	 *
	 * @param intensity (Input) Feature intensity image.  Not modified.
	 * @param candidateMin  (Input) (Optional) List of candidate local minimum features. Can be null if not used.
	 * @param candidateMax  (Input) (Optional) List of candidate local maximum features  Can be null if not used.
	 * @param foundMin (Output) Storage for found minimums. Can be null if not used.
	 * @param foundMax (Output) Storage for found maximums. Can be null if not used.
	 */
	public void process(GrayF32 intensity,
						QueueCorner candidateMin, QueueCorner candidateMax,
						QueueCorner foundMin, QueueCorner foundMax );

	/**
	 * Returns true if the algorithm requires a candidate list of corners.
	 *
	 * @return true if candidates are required.
	 */
	public boolean getUsesCandidates();

	/**
	 * Maximum value for detected minimums
	 *
	 * @return threshold for feature selection
	 */
	public float getThresholdMinimum();

	/**
	 * Minimum value for detected maximums
	 *
	 * @return threshold for feature selection
	 */
	public float getThresholdMaximum();

	/**
	 * Change the feature selection threshold for finding local minimums.
	 *
	 * @param threshold The new selection threshold.
	 */
	public void setThresholdMinimum(float threshold);

	/**
	 * Change the feature selection threshold for finding local maximums.
	 *
	 * @param threshold The new selection threshold.
	 */
	public void setThresholdMaximum(float threshold);

	/**
	 * Defines the region inside the image in which a pixel can be an extreme.
	 * valid region is: border &le; x &lt; width-border AND border &le; y &lt; height-border
	 *
	 * @param border Border size in pixels.
	 */
	public void setIgnoreBorder(int border);

	/**
	 * Returns the size of the image border.
	 *
	 * @return border size
	 */
	public int getIgnoreBorder();

	/**
	 * Species the search radius for the feature
	 *
	 * @param radius Radius in pixels
	 */
	public void setSearchRadius(int radius);

	/**
	 * Describes how large the region is that is being searched.  The radius is the number of
	 * pixels away from the center.
	 *
	 * @return Search radius
	 */
	public int getSearchRadius();

	/**
	 * True if it can detect local maximums.
	 */
	public boolean canDetectMaximums();

	/**
	 * True if it can detect local minimums.
	 */
	public boolean canDetectMinimums();

}
