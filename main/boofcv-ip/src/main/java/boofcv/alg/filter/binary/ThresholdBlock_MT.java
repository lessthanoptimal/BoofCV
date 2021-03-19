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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;

/**
 * <p>Concurrent version of {@link ThresholdBlock}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdBlock_MT<T extends ImageGray<T>, S extends ImageBase<S>>
		extends ThresholdBlock<T, S> {
	public ThresholdBlock_MT( BlockProcessor<T, S> processor,
							  ConfigLength requestedBlockWidth,
							  boolean thresholdFromLocalBlocks,
							  Class<T> imageClass ) {
		super(processor, requestedBlockWidth, thresholdFromLocalBlocks, imageClass);
	}

	/**
	 * Computes the min-max value for each block in the image
	 */
	@Override protected void computeStatistics( T input, int innerWidth, int innerHeight ) {
		final int statPixelStride = stats.getImageType().getNumBands();
		final int statStride = stats.stride;

		int vblocks = innerHeight/blockHeight;
		if (vblocks*blockHeight < innerHeight)
			vblocks++;

		//		for (int y = 0; y < innerHeight; y += blockHeight) {
		BoofConcurrency.loopFor(0, vblocks, vblock -> {
			BlockProcessor<T, S> processor = processors.pop();
			processor.init(blockWidth, blockHeight, thresholdFromLocalBlocks);
			int y = vblock*blockHeight;

			int indexStats = (y/blockHeight)*statStride;
			for (int x = 0; x < innerWidth; x += blockWidth, indexStats += statPixelStride) {
				processor.computeBlockStatistics(x, y, blockWidth, blockHeight, indexStats, input, stats);
			}
			// handle the case where the image's width isn't evenly divisible by the block's width
			if (innerWidth != input.width) {
				processor.computeBlockStatistics(innerWidth, y, input.width - innerWidth, blockHeight, indexStats, input, stats);
			}
			processors.recycle(processor);
		});

		// NOTE: below could be thrown into its own thread before the code above. Not easy with current thread design
		// handle the case where the image's height isn't evenly divisible by the block's height
		if (innerHeight != input.height) {
			BlockProcessor<T, S> processor = processors.pop();
			processor.init(blockWidth, blockHeight, thresholdFromLocalBlocks);

			int indexStats = (innerHeight/blockHeight)*statStride;
			int y = innerHeight;
			int blockHeight = input.height - innerHeight;
			for (int x = 0; x < innerWidth; x += blockWidth, indexStats += statPixelStride) {
				processor.computeBlockStatistics(x, y, blockWidth, blockHeight, indexStats, input, stats);
			}
			if (innerWidth != input.width) {
				processor.computeBlockStatistics(innerWidth, y, input.width - innerWidth, blockHeight, indexStats, input, stats);
			}
		}
	}

	@Override protected void applyThreshold( T input, GrayU8 output ) {
		BoofConcurrency.loopFor(0, stats.height, blockY -> {
			BlockProcessor<T, S> processor = processors.pop();
			processor.init(blockWidth, blockHeight, thresholdFromLocalBlocks);

			for (int blockX = 0; blockX < stats.width; blockX++) {
				processor.thresholdBlock(blockX, blockY, input, stats, output);
			}
		});
	}
}
