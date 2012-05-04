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
 * Score Format:  The index of the score for column i at disparity d is 'index = imgWidth*d + i'.  The
 * first score element refers to column radiusX in the image.<br>
 * Format Comment:<br>
 * This ordering is a bit unnatural when searching for the best disparity, but reduces cache misses
 * when writing.  Performance boost is about 20%-30% depending on max disparity and image size.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class DisparityScoreSadRect
		<Input extends ImageSingleBand, Disparity extends ImageSingleBand>
{
	// the minimum disparity that it will check
	protected int minDisparity;
	// maximum allowed image disparity
	protected int maxDisparity;
	// difference between max and min
	protected int rangeDisparity;

	// number of score elements: (image width - regionWidth)*maxDisparity
	protected int lengthHorizontal;

	// radius of the region along x and y axis
	protected int radiusX,radiusY;
	// size of the region: radius*2 + 1
	protected int regionWidth,regionHeight;

	/**
	 * Configures disparity calculation.
	 *
	 * @param minDisparity Minimum disparity that it will check. Must be >= 0 and < maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be > 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 */
	public DisparityScoreSadRect(int minDisparity, int maxDisparity,
								 int regionRadiusX, int regionRadiusY ) {
		if( maxDisparity <= 0 )
			throw new IllegalArgumentException("Max disparity must be greater than zero");
		if( minDisparity < 0 || minDisparity >= maxDisparity )
			throw new IllegalArgumentException("Min disparity must be >= 0 and < maxDisparity");

		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.radiusX = regionRadiusX;
		this.radiusY = regionRadiusY;

		this.rangeDisparity = maxDisparity - minDisparity;

		this.regionWidth = regionRadiusX*2+1;
		this.regionHeight = regionRadiusY*2+1;
	}

	/**
	 * Computes disparity between two stereo images
	 *
	 * @param left Left rectified stereo image. Input
	 * @param right Right rectified stereo image. Input
	 * @param disparity Disparity between the two images. Output
	 */
	public abstract void process( Input left , Input right , Disparity disparity );

	public abstract Class<Input> getInputType();

	public abstract Class<Disparity> getDisparityType();

	public int getMinDisparity() {
		return minDisparity;
	}

	public int getMaxDisparity() {
		return maxDisparity;
	}

	public int getBorderX() {
		return radiusX;
	}

	public int getBorderY() {
		return radiusY;
	}
}
