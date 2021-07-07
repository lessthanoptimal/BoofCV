/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Applies a threshold to an image by computing the mean values in a regular grid across
 * the image. When thresholding all the pixels inside a box (grid element) the mean values is found
 * in the surrounding 3x3 grid region.\
 * </p>
 *
 * <p>See {@link ThresholdBlockMinMax} for a more detailed discussion of elements of this strategy</p>
 *
 * @author Peter Abeles
 */
public abstract class ThresholdBlockMean<T extends ImageGray<T>>
		implements ThresholdBlock.BlockProcessor<T, T> {
	protected int blockWidth, blockHeight;
	protected boolean thresholdFromLocalBlocks;
	// defines 0 or 1 when thresholding
	protected byte a, b;

	protected ThresholdBlockMean( boolean down ) {
		if (down) {
			a = 1;
			b = 0;
		} else {
			a = 0;
			b = 1;
		}
	}

	@Override
	public void init( int blockWidth, int blockHeight, boolean thresholdFromLocalBlocks ) {
		this.blockWidth = blockWidth;
		this.blockHeight = blockHeight;
		this.thresholdFromLocalBlocks = thresholdFromLocalBlocks;
	}

	public boolean isDown() {
		return a == 1;
	}
}
