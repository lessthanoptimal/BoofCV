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
 * Computes the disparity SAD score efficiently for a single rectangular region while minimizing CPU cache misses.
 * After the score has been computed for an entire row it is passed onto another algorithm to compute the actual
 * disparity. Provides support for fast right to left validation.  First the sad score is computed horizontally
 * then summed up vertically while minimizing redundant calculations that naive implementation would have.
 * </p>
 *
 * <p>
 * Memory usage is minimized by only saving disparity scores for the row being considered.  The more
 * straight forward implementation is to compute the disparity score for the whole image at once,
 * which can be quite expensive.
 * </p>
 *
 * <p>
 * Score Format:  The index of the score for column i &ge; radiusX + minDisparity at disparity d is: <br>
 * index = imgWidth*(d-minDisparity-radiusX) + i - minDisparity-radiusX<br>
 * Format Comment:<br>
 * This ordering is a bit unnatural when searching for the best disparity, but reduces cache misses
 * when writing.  Performance boost is about 20%-30% depending on max disparity and image size.
 * </p>
 *
 * <p>
 * This implementation is not based off of any individual paper but ideas commonly expressed in several different
 * sources.  A good study and summary of similar algorithms can be found in:<br>
 * [1] Wannes van der Mark and Dariu M. Gavrila, "Real-Time Dense Stereo for Intelligent Vehicles"
 * IEEE TRANSACTIONS ON INTELLIGENT TRANSPORTATION SYSTEMS, VOL. 7, NO. 1, MARCH 2006
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DisparityScoreSadRect
		<Input extends ImageGray, Disparity extends ImageGray>
	extends DisparityScoreRowFormat<Input,Disparity>
{
	public DisparityScoreSadRect(int minDisparity, int maxDisparity,
								 int regionRadiusX, int regionRadiusY) {
		super(minDisparity, maxDisparity, regionRadiusX, regionRadiusY);
	}
}
