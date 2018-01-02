/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.struct.ConfigLength;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Applies a threshold to an image by computing the mean values in a regular grid across
 * the image.  When thresholding all the pixels inside a box (grid element) the mean values is found
 * in the surrounding 3x3 grid region.\
 * </p>
 *
 * <p>See {@link ThresholdBlockMinMax} for a more detailed discussion of elements of this strategy</p>
 *
 * @author Peter Abeles
 */
public abstract class ThresholdBlockMean
		<T extends ImageGray<T>> extends ThresholdBlockCommon<T,T>
{
	/**
	 * Configures the detector
	 *
	 * @param requestedBlockWidth About how wide and tall you wish a block to be in pixels.
	 */
	public ThresholdBlockMean(ConfigLength requestedBlockWidth, boolean thresholdFromLocalBlocks, Class<T> imageType) {
		super(requestedBlockWidth,thresholdFromLocalBlocks,imageType);
	}

}
