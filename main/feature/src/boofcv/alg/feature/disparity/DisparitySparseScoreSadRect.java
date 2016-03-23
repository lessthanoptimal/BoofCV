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
 * Computes disparity SAD scores using a rectangular region at the specified points only along
 * the x-axis. Scores are returned in an array where the index refers to the disparity.
 *
 * @author Peter Abeles
 */
public abstract class DisparitySparseScoreSadRect< ArrayData , Input extends ImageGray> {
	// maximum and minimum allowed image disparity
	protected int minDisparity;
	protected int maxDisparity;
	protected int rangeDisparity;
	// maximum disparity at the most recently processed point
	protected int localMaxDisparity;

	// radius of the region along x and y axis
	protected int radiusX,radiusY;
	// size of the region: radius*2 + 1
	protected int regionWidth,regionHeight;

	// input images
	protected Input left;
	protected Input right;

	/**
	 * Configures disparity calculation.
	 *
	 * @param minDisparity Minimum disparity that it will consider.  Must be &ge; 0 && < maxDisparity
	 * @param maxDisparity Maximum disparity that it will calculate. Must be &gt; 0
	 * @param radiusX Radius of the rectangular region along x-axis.
	 * @param radiusY Radius of the rectangular region along y-axis.
	 */
	public DisparitySparseScoreSadRect( int minDisparity , int maxDisparity , int radiusX , int radiusY ) {
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.radiusX = radiusX;
		this.radiusY = radiusY;

		this.rangeDisparity = maxDisparity - minDisparity;
		this.regionWidth = radiusX*2 + 1;
		this.regionHeight = radiusY*2 + 1;
	}

	/**
	 * Specify inputs for left and right camera images.
	 *
	 * @param left Rectified left camera image.
	 * @param right Rectified right camera image.
	 */
	public void setImages( Input left , Input right ) {
		InputSanityCheck.checkSameShape(left, right);

		this.left = left;
		this.right = right;
	}

	/**
	 * Compute disparity scores for the specified pixel.  Be sure that its not too close to
	 * the image border.
	 *
	 * @param x x-coordinate of point
	 * @param y y-coordinate of point.
	 */
	public abstract boolean process( int x , int y );

	/**
	 * How many disparity values were considered.
	 */
	public int getLocalMaxDisparity() {
		return localMaxDisparity;
	}

	public int getMinDisparity() {
		return minDisparity;
	}

	public int getMaxDisparity() {
		return maxDisparity;
	}

	public int getRadiusX() {
		return radiusX;
	}

	public int getRadiusY() {
		return radiusY;
	}

	/**
	 * Array containing disparity score values at most recently processed point.  Array
	 * indices correspond to disparity.  score[i] = score at disparity i.  To know how many
	 * disparity values there are call {@link #getLocalMaxDisparity()}
	 */
	public abstract ArrayData getScore();

	public abstract Class<Input> getImageType();
}
