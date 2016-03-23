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

package boofcv.alg.feature.disparity.impl;

import boofcv.alg.InputSanityCheck;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.util.Arrays;

/**
 * Naive version of {@link boofcv.alg.feature.disparity.DisparityScoreWindowFive}.
 *
 * @author Peter Abeles
 */
public class StereoDisparityWtoNaiveFive<I extends ImageGray> {
	// left and right camera images
	I imageLeft;
	I imageRight;

	// the minimum disparity it will consider
	int minDisparity;
	// where the match scores are stored.  Length is max disparity
	double[] score;

	// SCores of the four surrounding regions
	double []four = new double[4];

	// comparison region's radius
	int radiusX;
	int radiusY;

	// image dimension
	int w,h;

	/**
	 * Configure parameters
	 *
	 * @param minDisparity Minimum disparity it will consider in pixels.
	 * @param maxDisparity Maximum allowed disparity in pixels.
	 * @param radiusWidth Radius of the region along x-axis.
	 * @param radiusHeight Radius of the region along y-axis.
	 */
	public StereoDisparityWtoNaiveFive(int minDisparity, int maxDisparity, int radiusWidth, int radiusHeight) {
		this.minDisparity = minDisparity;
		this.score = new double[maxDisparity];
		this.radiusX = radiusWidth;
		this.radiusY = radiusHeight;
	}

	/**
	 * Computes the disparity for two stereo images along the image's right axis.  Both
	 * image must be rectified.
	 *
	 * @param left Left camera image.
	 * @param right Right camera image.
	 */
	public void process( I left , I right , GrayF32 imageDisparity ) {
		// check inputs and initialize data structures
		InputSanityCheck.checkSameShape(left,right,imageDisparity);
		this.imageLeft = left;
		this.imageRight = right;

		w = left.width; h = left.height;

		// Compute disparity for each pixel
		for( int y = radiusY*2; y < h-radiusY*2; y++ ) {
			for( int x = radiusX*2+minDisparity; x < w-radiusX*2; x++ ) {
				// take in account image border when computing max disparity
				int max = x-Math.max(radiusX*2-1,x-score.length);

				// compute match score across all candidates
				processPixel(x, y, max);

				// select the best disparity
				imageDisparity.set(x,y,(float)selectBest(max));
			}
		}
	}

	/**
	 * Computes fit score for each possible disparity
	 *
	 * @param c_x Center of region on left image. x-axis
	 * @param c_y Center of region on left image. y-axis
	 * @param maxDisparity Max allowed disparity
	 */
	private void processPixel( int c_x , int c_y ,  int maxDisparity ) {

		for( int i = minDisparity; i < maxDisparity; i++ ) {
			score[i] = computeScore( c_x , c_x-i,c_y);
		}
	}

	/**
	 * Select best disparity using the inner takes all approach
	 *
	 * @param length The max allowed disparity at this pixel
	 * @return The best disparity selected.
	 */
	protected double selectBest( int length ) {
		double best = Double.MAX_VALUE;
		int index = -1;
		for( int i = minDisparity; i < length; i++ ) {
			if( score[i] < best ) {
				best = score[i];
				index = i;
			}
		}

		return index-minDisparity;
	}

	/**
	 * Compute the score for five local regions and just use the center + the two best
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	protected double computeScore( int leftX , int rightX , int centerY ) {

		double center = computeScoreRect(leftX,rightX,centerY);
		four[0] =  computeScoreRect(leftX-radiusX,rightX-radiusX,centerY-radiusY);
		four[1] =  computeScoreRect(leftX+radiusX,rightX+radiusX,centerY-radiusY);
		four[2] =  computeScoreRect(leftX-radiusX,rightX-radiusX,centerY+radiusY);
		four[3] =  computeScoreRect(leftX+radiusX,rightX+radiusX,centerY+radiusY);

		Arrays.sort(four);

		return four[0] + four[1] + center;
	}

	/**
	 * Compute SAD (Sum of Absolute Difference) error.
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	protected double computeScoreRect( int leftX , int rightX , int centerY ) {

		double ret=0;

		for( int y = -radiusY; y <= radiusY; y++ ) {
			for( int x = -radiusX; x <= radiusX; x++ ) {
				double l = GeneralizedImageOps.get(imageLeft,leftX+x,centerY+y);
				double r = GeneralizedImageOps.get(imageRight,rightX+x,centerY+y);

				ret += Math.abs(l-r);
			}
		}

		return ret;
	}
}
