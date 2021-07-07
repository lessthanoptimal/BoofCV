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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.lists.RecycleStack;

/**
 * <p>Computes image statistics in regularly spaced blocks across the image. Then computes an average
 * of the statistics in a block within a local 3x3 grid region. The average statistics in a local 3x3 grid
 * region is used to reduce the adverse affects of using a grid.
 * Ideally a local region around each pixel would be used, but this is expensive to compute. Since a grid is
 * used instead of a pixel local region boundary conditions can be an issue. For example, consider a black square
 * in the image, if the grid just happens to lie on this black square perfectly then if you look at only a single
 * grid element it will be considered textureless and the edge lost. This problem isn't an issue if you consder
 * a local 3x3 region of blocks.</p>
 *
 * <p>The size each block in the grid in pixels is adjusted depending on image size. This is done to minimize
 * "squares" in the upper image boundaries from having many more pixels than other blocks.</p>
 *
 * <p>The block based approach used here was inspired by a high level description found in AprilTags.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ThresholdBlock<T extends ImageGray<T>, S extends ImageBase<S>>
		implements InputToBinary<T> {
	ImageType<T> imageType;

	// Stores computed block statistics
	protected S stats;

	// the desired width and height of a block requested by the user
	protected ConfigLength requestedBlockWidth;

	// the adjusted size to minimize extra pixels near the image upper extreme
	protected int blockWidth, blockHeight;

	// Should it use the local 3x3 block region
	protected boolean thresholdFromLocalBlocks;

	// Original processor which will be copied
	protected BlockProcessor<T, S> original;
	protected RecycleStack<BlockProcessor<T, S>> processors;

	/**
	 * Configures the detector
	 *
	 * @param requestedBlockWidth About how wide and tall you wish a block to be in pixels.
	 */
	public ThresholdBlock( BlockProcessor<T, S> processor,
						   ConfigLength requestedBlockWidth, boolean thresholdFromLocalBlocks,
						   Class<T> imageClass ) {
		this.requestedBlockWidth = requestedBlockWidth;
		this.imageType = ImageType.single(imageClass);
		this.thresholdFromLocalBlocks = thresholdFromLocalBlocks;
		this.stats = processor.createStats();

		this.original = processor;
		processors = new RecycleStack<>(() -> processor.copy());
	}

	/**
	 * Converts the gray scale input image into a binary image
	 *
	 * @param input Input image
	 * @param output Output binary image
	 */
	@Override
	public void process( T input, GrayU8 output ) {
		output.reshape(input.width, input.height);

		int requestedBlockWidth = this.requestedBlockWidth.computeI(Math.min(input.width, input.height));

		selectBlockSize(input.width, input.height, requestedBlockWidth);

		stats.reshape(input.width/blockWidth, input.height/blockHeight);


		int innerWidth = input.width%blockWidth == 0 ?
				input.width : input.width - blockWidth - input.width%blockWidth;
		int innerHeight = input.height%blockHeight == 0 ?
				input.height : input.height - blockHeight - input.height%blockHeight;

		computeStatistics(input, innerWidth, innerHeight);
		applyThreshold(input, output);
	}

	/**
	 * Selects a block size which is close to the requested block size by the user
	 */
	void selectBlockSize( int width, int height, int requestedBlockWidth ) {

		if (height < requestedBlockWidth) {
			blockHeight = height;
		} else {
			int rows = height/requestedBlockWidth;
			blockHeight = height/rows;
		}

		if (width < requestedBlockWidth) {
			blockWidth = width;
		} else {
			int cols = width/requestedBlockWidth;
			blockWidth = width/cols;
		}
	}

	/**
	 * Applies the dynamically computed threshold to each pixel in the image, one block at a time
	 */
	protected void applyThreshold( T input, GrayU8 output ) {
		for (int blockY = 0; blockY < stats.height; blockY++) {
			for (int blockX = 0; blockX < stats.width; blockX++) {
				original.thresholdBlock(blockX, blockY, input, stats, output);
			}
		}
	}

	/**
	 * Computes the min-max value for each block in the image
	 */
	protected void computeStatistics( T input, int innerWidth, int innerHeight ) {
		original.init(blockWidth, blockHeight, thresholdFromLocalBlocks);

		int statPixelStride = stats.getImageType().getNumBands();

		int indexStats = 0;
		for (int y = 0; y < innerHeight; y += blockHeight) {
			for (int x = 0; x < innerWidth; x += blockWidth, indexStats += statPixelStride) {
				original.computeBlockStatistics(x, y, blockWidth, blockHeight, indexStats, input, stats);
			}
			// handle the case where the image's width isn't evenly divisible by the block's width
			if (innerWidth != input.width) {
				original.computeBlockStatistics(innerWidth, y, input.width - innerWidth, blockHeight, indexStats, input, stats);
				indexStats += statPixelStride;
			}
		}
		// handle the case where the image's height isn't evenly divisible by the block's height
		if (innerHeight != input.height) {
			int y = innerHeight;
			int blockHeight = input.height - innerHeight;
			for (int x = 0; x < innerWidth; x += blockWidth, indexStats += statPixelStride) {
				original.computeBlockStatistics(x, y, blockWidth, blockHeight, indexStats, input, stats);
			}
			if (innerWidth != input.width) {
				original.computeBlockStatistics(innerWidth, y, input.width - innerWidth, blockHeight, indexStats, input, stats);
			}
		}
	}

	public boolean isThresholdFromLocalBlocks() {
		return thresholdFromLocalBlocks;
	}

	public void setThresholdFromLocalBlocks( boolean thresholdFromLocalBlocks ) {
		this.thresholdFromLocalBlocks = thresholdFromLocalBlocks;
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}

	public interface BlockProcessor<T extends ImageGray<T>, S extends ImageBase<S>> {

		S createStats();

		void init( int blockWidth, int blockHeight, boolean thresholdFromLocalBlocks );

		/**
		 * Computes the min-max value inside a block
		 *
		 * @param x0 lower bound pixel value of block, x-axis
		 * @param y0 upper bound pixel value of block, y-axis
		 * @param width Block's width
		 * @param height Block's height
		 * @param indexStats array index of statistics image pixel
		 * @param input Input image
		 */
		void computeBlockStatistics( int x0, int y0, int width, int height,
									 int indexStats, T input, S stats );

		/**
		 * Thresholds all the pixels inside the specified block
		 *
		 * @param blockX0 Block x-coordinate
		 * @param blockY0 Block y-coordinate
		 * @param input Input image
		 * @param output Output image
		 */
		void thresholdBlock( int blockX0, int blockY0, T input, S stats, GrayU8 output );

		/**
		 * Creates a copy. For concurrent code
		 */
		BlockProcessor<T, S> copy();
	}
}
