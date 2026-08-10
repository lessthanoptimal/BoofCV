/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block;

import boofcv.alg.InputSanityCheck;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.disparity.DisparityError;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.ejml.UtilEjml;

public abstract class CommonDisparityBlockMatch<I extends ImageGray<I>> {
	public static final BorderType BORDER_TYPE = BorderType.REFLECT;
	float eps = UtilEjml.F_EPS;

	// left and right camera images
	I left;
	I right;
	ImageBorder<I> bleft, bright;

	// the minimum disparity it will consider
	int minDisparity;
	int maxDisparity;
	int rangeDisparity;
	// where the match scores are stored. Length is max disparity
	double[] score;

	// comparison region's radius
	int radiusX;
	int radiusY;
	// image dimension
	int w, h;

	DisparityError errorType;

	protected CommonDisparityBlockMatch( DisparityError errorType ) {
		this.errorType = errorType;
	}

	public void setBorder( ImageBorder<I> border ) {
		bleft = border.copy();
		bright = border.copy();
	}

	/**
	 * Configure parameters
	 *
	 * @param minDisparity Minimum disparity it will consider in pixels.
	 * @param maxDisparity Maximum allowed disparity in pixels.
	 * @param radiusWidth Radius of the region along x-axis.
	 * @param radiusHeight Radius of the region along y-axis.
	 */
	public void configure( int minDisparity, int maxDisparity, int radiusWidth, int radiusHeight ) {
		this.minDisparity = minDisparity;
		this.maxDisparity = maxDisparity;
		this.rangeDisparity = maxDisparity - minDisparity + 1;
		this.score = new double[rangeDisparity]; // indexed by disparity - minDisparity
		this.radiusX = radiusWidth;
		this.radiusY = radiusHeight;
	}

	/**
	 * Computes the disparity for two stereo images along the image's right axis. Both
	 * image must be rectified.
	 *
	 * @param left Left camera image.
	 * @param right Right camera image.
	 */
	public void process( I left, I right, GrayF32 imageDisparity ) {
		// check inputs and initialize data structures
		InputSanityCheck.checkSameShape(left, right, imageDisparity);
		this.left = left;
		this.right = right;
		this.bleft.setImage(left);
		this.bright.setImage(right);

		w = left.width;
		h = left.height;

		// Columns outside [col0, col1) have no disparity whose match stays inside the image (either border).
		// minDisparity/maxDisparity may be negative.
		int col0 = Math.max(0, minDisparity);
		int col1 = w + Math.min(0, maxDisparity);

		// Compute disparity for each pixel
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < col0; x++) {
				imageDisparity.set(x, y, (float)rangeDisparity);
			}
			for (int x = col0; x < col1; x++) {
				// disparity range at this column, clamped so the matched right column (x-d) stays in the image
				int disparityLow = Math.max(minDisparity, x - (w - 1));
				int disparityHigh = Math.min(maxDisparity, x);

				// compute match score across all candidates
				processPixel(x, y, disparityLow, disparityHigh);

				// select the best disparity
				imageDisparity.set(x, y, (float)selectBest(disparityLow, disparityHigh));
			}
			for (int x = col1; x < w; x++) {
				imageDisparity.set(x, y, (float)rangeDisparity);
			}
		}
	}

	/**
	 * Computes fit score for each possible disparity
	 *
	 * @param c_x Center of region on left image. x-axis
	 * @param c_y Center of region on left image. y-axis
	 * @param disparityLow Lowest disparity to consider at this pixel (inclusive)
	 * @param disparityHigh Highest disparity to consider at this pixel (inclusive)
	 */
	private void processPixel( int c_x, int c_y, int disparityLow, int disparityHigh ) {
		for (int d = disparityLow; d <= disparityHigh; d++) {
			score[d - minDisparity] = computeScore(c_x, c_x - d, c_y);
		}
	}

	/**
	 * Select best disparity using the winner takes all approach
	 *
	 * @param disparityLow Lowest disparity at this pixel (inclusive)
	 * @param disparityHigh Highest disparity at this pixel (inclusive)
	 * @return The best disparity selected, relative to minDisparity.
	 */
	protected double selectBest( int disparityLow, int disparityHigh ) {
		int bestDisparity = disparityLow;
		double bestScore = score[disparityLow - minDisparity];

		if (errorType.isCorrelation()) {
			for (int d = disparityLow + 1; d <= disparityHigh; d++) {
				double s = score[d - minDisparity];
				if (s > bestScore) {
					bestScore = s;
					bestDisparity = d;
				}
			}
		} else {
			for (int d = disparityLow + 1; d <= disparityHigh; d++) {
				double s = score[d - minDisparity];
				if (s < bestScore) {
					bestScore = s;
					bestDisparity = d;
				}
			}
		}

		return bestDisparity - minDisparity;
	}

	/**
	 * Compute the score for five local regions and just use the center + the two best
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	protected abstract double computeScore( int leftX, int rightX, int centerY );

	/**
	 * Compute the block error for the configured error type.
	 *
	 * @param leftX X-axis center left image
	 * @param rightX X-axis center left image
	 * @param centerY Y-axis center for both images
	 * @return Fit score for both regions.
	 */
	protected double computeScoreBlock( int leftX, int rightX, int centerY ) {
		switch (errorType) {
			case SAD: {
				double ret = 0;

				for (int y = -radiusY; y <= radiusY; y++) {
					for (int x = -radiusX; x <= radiusX; x++) {
						double l = GeneralizedImageOps.get(bleft, leftX + x, centerY + y);
						double r = GeneralizedImageOps.get(bright, rightX + x, centerY + y);

						ret += Math.abs(l - r);
					}
				}

				return ret;
			}

			case SSD: {
				double ret = 0;

				for (int y = -radiusY; y <= radiusY; y++) {
					for (int x = -radiusX; x <= radiusX; x++) {
						double l = GeneralizedImageOps.get(bleft, leftX + x, centerY + y);
						double r = GeneralizedImageOps.get(bright, rightX + x, centerY + y);

						ret += (l - r)*(l - r);
					}
				}

				return ret;
			}

			case ZSSD: {
				// Remove the mean of each block before comparing them
				double meanL = 0, meanR = 0;
				int area = (2*radiusX + 1)*(2*radiusY + 1);
				for (int y = -radiusY; y <= radiusY; y++) {
					for (int x = -radiusX; x <= radiusX; x++) {
						meanL += GeneralizedImageOps.get(bleft, leftX + x, centerY + y);
						meanR += GeneralizedImageOps.get(bright, rightX + x, centerY + y);
					}
				}
				meanL /= area;
				meanR /= area;

				double ret = 0;
				for (int y = -radiusY; y <= radiusY; y++) {
					for (int x = -radiusX; x <= radiusX; x++) {
						double l = GeneralizedImageOps.get(bleft, leftX + x, centerY + y) - meanL;
						double r = GeneralizedImageOps.get(bright, rightX + x, centerY + y) - meanR;

						ret += (l - r)*(l - r);
					}
				}

				return ret;
			}

			case NCC: {
				return TestBlockRowScoreNcc.ncc((ImageBorder_F32)bleft, (ImageBorder_F32)bright,
						leftX, centerY, leftX - rightX, radiusX, radiusY, eps);
			}

			default:
				throw new RuntimeException("Egads");
		}
	}
}
