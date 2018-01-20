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
import boofcv.struct.image.ImageInterleaved;

/**
 * <p>
 * Applies a threshold to an image by computing the min and max values in a regular grid across
 * the image.  When thresholding all the pixels inside a box (grid element) the min max values is found
 * in the surrounding 3x3 grid region.  If the difference between min and max is &le; textureThreshold then
 * it will be marked as one, since it is considered a textureless region.  Otherwise the pixel threshold
 * is set to (min+max)/2.
 * </p>
 *
 * <p>This thresholding strategy is designed to quickly detect shapes with nearly uniform values both inside
 * the image and along the image border, with locally variable intensity values.  The image border is
 * particularly problematic since there are no neighboring pixels outside the image from which to compute
 * a local threshold.  This is why if a region is considered textureless it is marked as 1.</p>
 *
 * <p>The min-max values inside a local 3x3 grid region is used to reduce the adverse affects of using a grid.
 * Ideally a local region around each pixel would be used, but this is expensive to compute.  Since a grid is
 * used instead of a pixel local region boundary conditions can be an issue.  For example, consider a black square
 * in the image, if the grid just happens to lie on this black square perfectly then if you look at only a single
 * grid element it will be considered textureless and the edge lost.  This problem isn't an issue if you consder
 * a local 3x3 region of blocks.</p>
 *
 * <p>The size each block in the grid in pixels is adjusted depending on image size.  This is done to minimize
 * "squares" in the upper image boundaries from having many more pixels than other blocks.</p>
 *
 * <p>The block based approach used here was inspired by a high level description found in AprilTags.</p>
 *
 * @author Peter Abeles
 */
public abstract class ThresholdBlockMinMax
		<T extends ImageGray<T>, I extends ImageInterleaved<I>> extends ThresholdBlockCommon<T,I>
{
	// if the min and max value's difference is <= to this value then it is considered
	// to be textureless and a default value is used
	protected double minimumSpread;

	/**
	 * Configures the detector
	 * @param minimumSpread If the difference between min max is less than or equal to this
	 *                         value then it is considered textureless.  Set to &le; -1 to disable.
	 * @param requestedBlockWidth About how wide and tall you wish a block to be in pixels.
	 */
	public ThresholdBlockMinMax(double minimumSpread, ConfigLength requestedBlockWidth,
								boolean thresholdFromLocalBlocks, Class<T> imageType ) {
		super(requestedBlockWidth,thresholdFromLocalBlocks,imageType);
		this.minimumSpread = minimumSpread;
	}

}
