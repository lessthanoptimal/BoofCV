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

package boofcv.alg.filter.binary;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayU8;
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
 * <p>The block based approach used were was inspired by a high level description found in AprilTags.</p>
 *
 * @author Peter Abeles
 */
public abstract class ThresholdSquareBlockMinMax
		<T extends ImageGray, I extends ImageInterleaved>
{
	// interleaved image which stores min and max values inside each block
	protected I minmax;

	// if the min and max value's difference is <= to this value then it is considered
	// to be textureless and a default value is used
	protected double minimumSpread;

	// the desired width and height of a block requested by the user
	protected int requestedBlockWidth;

	// the adjusted size to minimize extra pixels near the image upper extreme
	protected int blockWidth,blockHeight;

	/**
	 * Configures the detector
	 * @param minimumSpread If the difference between min max is less than or equal to this
	 *                         value then it is considered textureless.  Set to <= -1 to disable.
	 * @param requestedBlockWidth About how wide and tall you wish a block to be in pixels.
	 */
	public ThresholdSquareBlockMinMax(double minimumSpread, int requestedBlockWidth) {
		this.minimumSpread = minimumSpread;
		this.requestedBlockWidth = requestedBlockWidth;
	}

	/**
	 * Converts the gray scale input image into a binary image
	 * @param input Input image
	 * @param output Output binary image
	 */
	public void process(T input , GrayU8 output ) {
		InputSanityCheck.checkSameShape(input,output);

		if( input.width < requestedBlockWidth || input.height < requestedBlockWidth ) {
			throw new IllegalArgumentException("Image is smaller than block size");
		}

		selectBlockSize(input.width,input.height);

		minmax.reshape(input.width/blockWidth,input.height/blockHeight);

		int innerWidth = input.width%blockWidth == 0 ?
				input.width : input.width-blockWidth-input.width%blockWidth;
		int innerHeight = input.height%blockHeight == 0 ?
				input.height : input.height-blockHeight-input.height%blockHeight;

		computeMinMax(input, innerWidth, innerHeight);
		applyThreshold(input,output);
	}

	/**
	 * Selects a block size which is close to the requested block size by the user
	 */
	void selectBlockSize( int width , int height ) {

		int rows = height/requestedBlockWidth;
		int cols = width/requestedBlockWidth;

		blockHeight = height/rows;
		blockWidth = width/cols;
	}

	/**
	 * Computes the min-max value for each block in the image
	 */
	private void computeMinMax(T input, int innerWidth, int innerHeight) {
		int indexMinMax = 0;
		for (int y = 0; y < innerHeight; y += blockHeight) {
			for (int x = 0; x < innerWidth; x += blockWidth, indexMinMax += 2) {
				computeMinMaxBlock(x,y,blockWidth,blockHeight,indexMinMax,input);
			}
			// handle the case where the image's width isn't evenly divisible by the block's width
			if( innerWidth != input.width ) {
				computeMinMaxBlock(innerWidth,y,input.width-innerWidth,blockHeight,indexMinMax,input);
				indexMinMax += 2;
			}
		}
		// handle the case where the image's height isn't evenly divisible by the block's height
		if( innerHeight != input.height ) {
			int y = innerHeight;
			int blockHeight = input.height-innerHeight;
			for (int x = 0; x < innerWidth; x += blockWidth, indexMinMax += 2) {
				computeMinMaxBlock(x,y,blockWidth,blockHeight,indexMinMax,input);
			}
			if( innerWidth != input.width ) {
				computeMinMaxBlock(innerWidth,y,input.width-innerWidth,blockHeight,indexMinMax,input);
			}
		}
	}

	/**
	 * Applies the dynamically computed threshold to each pixel in the image, one block at a time
	 */
	private void applyThreshold( T input, GrayU8 output ) {
		for (int blockY = 0; blockY < minmax.height; blockY++) {
			for (int blockX = 0; blockX < minmax.width; blockX++) {
				thresholdBlock(blockX,blockY,input,output);
			}
		}
	}

	/**
	 * Thresholds all the pixels inside the specified block
	 * @param blockX0 Block x-coordinate
	 * @param blockY0 Block y-coordinate
	 * @param input Input image
	 * @param output Output image
	 */
	protected abstract void thresholdBlock(int blockX0 , int blockY0 ,
										   T input, GrayU8 output );

	/**
	 * Computes the min-max value inside a block
	 * @param x0 lower bound pixel value of block, x-axis
	 * @param y0 upper bound pixel value of block, y-axis
	 * @param width Block's width
	 * @param height Block's height
	 * @param indexMinMax array index of min-max image pixel
	 * @param input Input image
	 */
	protected abstract void computeMinMaxBlock(int x0 , int y0 , int width , int height ,
											   int indexMinMax , T input);

}
