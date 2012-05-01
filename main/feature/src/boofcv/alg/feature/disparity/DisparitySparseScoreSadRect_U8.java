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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;

/**
 * Computes disparity SAD scores using a rectangular region at the specified points only along
 * the x-axis. Scores are returned in an array where the index refers to the disparity.
 *
 * @author Peter Abeles
 */
public class DisparitySparseScoreSadRect_U8 {
	// maximum allowed image disparity
	int maxDisparity;
	// maximum disparity at the most recently processed point
	int localMaxDisparity;

	// radius of the region along x and y axis
	int radiusX,radiusY;
	// size of the region: radius*2 + 1
	int regionWidth,regionHeight;

	// scores up to the maximum baseline
	int scores[];

	// input images
	ImageUInt8 left;
	ImageUInt8 right;

	public DisparitySparseScoreSadRect_U8(int maxDisparity , int radiusX , int radiusY ) {
		this.maxDisparity = maxDisparity;
		this.radiusX = radiusX;
		this.radiusY = radiusY;
		this.regionWidth = radiusX*2 + 1;
		this.regionHeight = radiusY*2 + 1;

		scores = new int[ maxDisparity ];
	}

	/**
	 * Specify inputs for left and right camera images.
	 *
	 * @param left Rectified left camera image.
	 * @param right Rectified right camera image.
	 */
	public void setImages( ImageUInt8 left , ImageUInt8 right ) {
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
	public void process( int x , int y ) {
		if( x < radiusX || x >= left.width-radiusX || x < radiusY || x >= right.width-radiusY )
			throw new IllegalArgumentException("Too close to image border");

		// adjust disparity for image border
		localMaxDisparity = Math.min(maxDisparity,x-radiusX+1);

		Arrays.fill(scores,0);

		// sum up horizontal errors in the region
		for( int row = 0; row < regionHeight; row++ ) {
			// pixel indexes
			int startLeft = left.startIndex + left.stride*(y-radiusY+row) + x-radiusX;
			int startRight = right.startIndex + right.stride*(y-radiusY+row) + x-radiusX;

			for( int i = 0; i < localMaxDisparity; i++ ) {
				int indexLeft = startLeft;
				int indexRight = startRight-i;

				int score = 0;
				for( int j = 0; j < regionWidth; j++ ) {
					int diff = (left.data[ indexLeft++ ] & 0xFF) - (right.data[ indexRight++ ] & 0xFF);

					score += Math.abs(diff);
				}
				scores[i] += score;
			}
		}
	}

	/**
	 * Array containing disparity score values at most recently processed point.  Array
	 * indices correspond to disparity.  score[i] = score at disparity i.  To know how many
	 * disparity values there are call {@link #getLocalMaxDisparity()}
	 */
	public int[] getScore() {
		return scores;
	}

	/**
	 * How many disparity values were considered.
	 */
	public int getLocalMaxDisparity() {
		return localMaxDisparity;
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
}
