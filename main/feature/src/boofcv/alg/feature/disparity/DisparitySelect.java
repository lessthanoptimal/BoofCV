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

package boofcv.alg.feature.disparity;

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Selects the best disparity given the set of scores calculated by
 * {@link boofcv.alg.feature.disparity.DisparityScoreSadRect}. The scores
 * are provided as an array of integers or floats.  A disparity of zero either means
 * no match was found or the disparity was in fact zero.
 * </p>
 *
 * <p>
 * The selected disparity written into the output image is equal to the found disparity minus the minDisparity.
 * If a pixel is found to be invalid and no disparity found then its value is set to (maxDisparity-minDisparity) + 1.
 * The first requirement maximizes the useful storage of the output image and the second provides an unambiguous
 * way to identify invalid pixels.
 * </p>
 *
 * @author Peter Abeles
 */
public interface DisparitySelect<Array , T extends ImageGray> {
	/**
	 * Specifies the output and algorithmic configuration.
	 *
	 * @param imageDisparity Output disparity image.
	 * @param minDisparity Minimum disparity that can be computed
	 * @param maxDisparity Maximum disparity that is calculated
	 * @param radiusX Radius of the rectangular region being matched along x-axis.
	 */
	public void configure(T imageDisparity, int minDisparity , int maxDisparity, int radiusX);

	/**
	 * Processes the array of scores. The score format is described in
	 * {@link boofcv.alg.feature.disparity.DisparityScoreSadRect}.  The results are written directly into the
	 * disparity image passed to it in {@link #configure(ImageGray, int, int, int)}.
	 *
	 * @param row Image row the scores are from.
	 * @param scoresArray Array containing scores. (int[] or float[])
	 */
	public void process(int row, Array scoresArray);

	/**
	 * Type of image the disparity is
	 *
	 * @return Image type for disparity
	 */
	public Class<T> getDisparityType();
}
