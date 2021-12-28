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

package boofcv.alg.disparity.block.score;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import lombok.Getter;

/**
 * Base class for computing sparse stereo disparity scores using a block matching approach given a
 * rectified stereo pair.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class DisparitySparseRectifiedScoreBM<ArrayData, Input extends ImageGray<Input>> {
	/** the minimum disparity value (inclusive) */
	protected @Getter int disparityMin;
	/** maximum allowed image disparity (exclusive) */
	protected @Getter int disparityMax;
	/** difference between max and min */
	protected @Getter int disparityRange;
	/** the local disparity range at the current image coordinate in left to right direction */
	protected @Getter int localRangeLtoR;
	/** the local disparity range at the current image coordinate in right to left direction */
	protected @Getter int localRangeRtoL;

	/** radius of the region along x and y axis */
	protected @Getter int radiusX, radiusY;
	// size of the region: radius*2 + 1
	protected int blockWidth, blockHeight;
	// region of size of sampled pixels
	protected int sampledWidth, sampledHeight;

	// input images
	protected Input left;
	protected Input right;

	// handles border pixels
	protected ImageBorder<Input> border;

	/** Input image type */
	protected @Getter Class<Input> inputType;

	// Copies of only the pixels needed to compute the sparse disparity. These local patches are used
	// instead of the raw images to make the code less complex at the image border
	// left to right: template = left, compare = right.
	protected final Input patchTemplate;
	protected final Input patchCompare;

	// Radius around a pixel that is sampled. This is for functions like Census which define a pixel's value based on
	// its neighbors. For SAD this will be 0
	protected int sampleRadiusX = -1;
	protected int sampleRadiusY = -1;

	/**
	 * Configures disparity calculation.
	 *
	 * @param radiusX Radius of the rectangular region along x-axis.
	 * @param radiusY Radius of the rectangular region along y-axis.
	 */
	protected DisparitySparseRectifiedScoreBM( int radiusX, int radiusY, Class<Input> inputType ) {
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.blockWidth = radiusX*2 + 1;
		this.blockHeight = radiusY*2 + 1;
		this.inputType = inputType;

		patchTemplate = GeneralizedImageOps.createSingleBand(inputType, 1, 1);
		patchCompare = GeneralizedImageOps.createSingleBand(inputType, 1, 1);
	}

	/** Default constructor primarily for unit tests */
	protected DisparitySparseRectifiedScoreBM( Class<Input> inputType ) {
		this.inputType = inputType;
		patchTemplate = GeneralizedImageOps.createSingleBand(inputType, 1, 1);
		patchCompare = GeneralizedImageOps.createSingleBand(inputType, 1, 1);
	}

	protected void setSampleRegion( int radiusX, int radiusY ) {
		this.sampleRadiusX = radiusX;
		this.sampleRadiusY = radiusY;
	}

	/**
	 * Specifies how the image border is handled
	 */
	public void setBorder( ImageBorder<Input> border ) {
		this.border = border;
	}

	/**
	 * Configures the disparity search
	 *
	 * @param disparityMin Minimum disparity that it will check. Must be &ge; 0 and < disparityMax
	 * @param disparityRange Number of possible disparity values estimated. The max possible disparity is min+range-1.
	 */
	public void configure( int disparityMin, int disparityRange ) {
		if (disparityMin < 0)
			throw new IllegalArgumentException("Min disparity must be greater than or equal to zero. max=" + disparityMin);
		if (disparityRange <= 0)
			throw new IllegalArgumentException("Disparity range must be more than 0");

		this.disparityMin = disparityMin;
		this.disparityRange = disparityRange;
		this.disparityMax = disparityMin + disparityRange - 1;

		if (sampleRadiusX < 0 || sampleRadiusY < 0)
			throw new RuntimeException("Didn't set the sample radius");

		// size of a single patch that needs to be copied
		this.sampledWidth = 2*(sampleRadiusX + radiusX) + 1;
		this.sampledHeight = 2*(sampleRadiusY + radiusY) + 1;
		patchTemplate.reshape(sampledWidth, sampledHeight);
	}

	/**
	 * Specify inputs for left and right camera images.
	 *
	 * @param left Rectified left camera image.
	 * @param right Rectified right camera image.
	 */
	public void setImages( Input left, Input right ) {
		InputSanityCheck.checkSameShape(left, right);
		this.left = left;
		this.right = right;
	}

	/**
	 * Compute disparity scores for the specified pixel in left to right direction. Be sure that its not too close to
	 * the image border.
	 *
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point.
	 */
	public boolean processLeftToRight( int x, int y ) {
		// can't estimate disparity if there are no pixels it can estimate disparity from
		if (x < disparityMin)
			return false;

		// adjust disparity range for image border
		localRangeLtoR = Math.min(x, disparityMax) - disparityMin + 1;

		patchCompare.reshape(sampledWidth + localRangeLtoR - 1, sampledHeight);
		// -1 because 'w' includes a range of 1 implicitly

		// Create local copies that include the image border
		copy(x, y, 1, left, patchTemplate);
		copy(x - disparityMin - localRangeLtoR + 1, y, localRangeLtoR, right, patchCompare);
		// Maximum disparity will be at the beginning of the right path and decrease as x increases

		// Compute scores from the copied local patches
		scoreDisparity(localRangeLtoR, true);

		return true;
	}

	/**
	 * Compute disparity scores for the specified pixel in right to left direction. Be sure that its not too close to
	 * the image border.
	 *
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point.
	 */
	public boolean processRightToLeft( int x, int y ) {
		// can't estimate disparity if there are no pixels it can estimate disparity from
		if (x + disparityMin >= left.width)
			return false;

		// adjust disparity range for image border
		localRangeRtoL = Math.min(x + disparityMin + disparityRange, left.width) - x - disparityMin;

		patchCompare.reshape(sampledWidth + localRangeRtoL - 1, sampledHeight);
		// -1 because 'w' includes a range of 1 implicitly

		// Create local copies that include the image border
		copy(x, y, 1, right, patchTemplate);
		copy(x + disparityMin, y, localRangeRtoL, left, patchCompare);
		// Maximum disparity will be at the beginning of the right path and decrease as x increases

		// Compute scores from the copied local patches
		scoreDisparity(localRangeRtoL, false);

		return true;
	}

	/**
	 * Copies a local image patch so that the score function doesn't need to deal with image border issues
	 */
	protected final void copy( int startX, int startY, int length, Input src, Input dst ) {
		int x0 = startX - radiusX - sampleRadiusX;
		int y0 = startY - radiusY - sampleRadiusY;
		int x1 = startX + radiusX + sampleRadiusX + length;
		int y1 = startY + radiusY + sampleRadiusY + 1;

		GImageMiscOps.copy(x0, y0, 0, 0, x1 - x0, y1 - y0, src, border, dst);
	}

	/**
	 * Scores the disparity using image patches.
	 *
	 * @param disparityRange The local range for disparity
	 * @param leftToRight If true then the disparity is being from in left to right direction (the typical)
	 */
	protected abstract void scoreDisparity( int disparityRange, boolean leftToRight );

	/**
	 * Array containing disparity score computed by calling {@link #processLeftToRight}. Each element corresponds
	 * to the disparity score relative to {@link #disparityMin} and has a max value specified by
	 * {@link #getLocalRangeLtoR()}
	 */
	public abstract ArrayData getScoreLtoR();

	/**
	 * Array containing disparity score computed by calling {@link #processRightToLeft}. Each element corresponds
	 * to the disparity score relative to {@link #disparityMin} and has a max value specified by
	 * {@link #getLocalRangeRtoL()}
	 */
	public abstract ArrayData getScoreRtoL();
}
