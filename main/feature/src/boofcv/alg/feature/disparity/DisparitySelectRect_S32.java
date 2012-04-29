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

package boofcv.alg.feature.disparity;

import boofcv.struct.image.ImageSingleBand;

/**
 * Computes the disparity given disparity score calculations provided by
 * {@link DisparityScoreSadRect_U8}. The S32 in name refers to the score being provided
 * as an integer array.
 *
 * @author Peter Abeles
 */
public interface DisparitySelectRect_S32<T extends ImageSingleBand> {

	/**
	 *
	 * @param imageDisparity
	 * @param maxDisparity Maximum disparity that is calculated
	 * @param radiusX Radius of the rectangular region being matched along x-axis.
	 */
	public void configure( T imageDisparity , int maxDisparity , int radiusX );

	/**
	 * Processes the array of scores.  The score format is described in {@link DisparityScoreSadRect_U8}.
	 *
	 *
	 * @param row Image row the scores are from.
	 * @param scores Array containing scores.
	 */
	public void process( int row , int scores[] );
}
