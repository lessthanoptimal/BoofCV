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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageGray;

/**
 * <p>
 * Base class for all dense stereo disparity score algorithms whose score's can be processed by
 * {@link DisparitySelect}. The scores for all possible disparities at each pixel is computed for
 * an entire row at once.  Then {@link DisparitySelect} is called to process this score.
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
 * @author Peter Abeles
 */
public abstract class DisparityScoreRowFormat
		<Input extends ImageGray, Disparity extends ImageGray>
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
	 * @param minDisparity Minimum disparity that it will check. Must be &ge; 0 and < maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param regionRadiusX Radius of the rectangular region along x-axis.
	 * @param regionRadiusY Radius of the rectangular region along y-axis.
	 */
	public DisparityScoreRowFormat(int minDisparity, int maxDisparity,
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
	public void process( Input left , Input right , Disparity disparity ) {
		// initialize data structures
		InputSanityCheck.checkSameShape(left, right, disparity);

		if( maxDisparity >  left.width-2*radiusX )
			throw new RuntimeException(
					"The maximum disparity is too large for this image size: max size "+(left.width-2*radiusX));

		lengthHorizontal = left.width*rangeDisparity;

		_process(left,right,disparity);
	}

	/**
	 * Inner function that computes the disparity.
	 */
	public abstract void _process( Input left , Input right , Disparity disparity );

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
