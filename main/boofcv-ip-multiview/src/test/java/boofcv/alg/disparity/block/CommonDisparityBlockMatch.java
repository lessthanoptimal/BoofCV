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

/**
 * @author Peter Abeles
 */
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
		this.score = new double[maxDisparity + 1];
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

		// Compute disparity for each pixel
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < minDisparity; x++) {
				imageDisparity.set(x, y, (float)rangeDisparity);
			}
			for (int x = minDisparity; x < w; x++) {
				// take in account image border when computing max disparity
				int localMaxDisparity = Math.min(x, maxDisparity);

				// compute match score across all candidates
				processPixel(x, y, localMaxDisparity);

				// select the best disparity
				imageDisparity.set(x, y, (float)selectBest(localMaxDisparity));
			}
		}
	}

	/**
	 * Computes fit score for each possible disparity
	 *
	 * @param c_x Center of region on left image. x-axis
	 * @param c_y Center of region on left image. y-axis
	 * @param localMaxDisparity Max allowed disparity
	 */
	private void processPixel( int c_x, int c_y, int localMaxDisparity ) {
		for (int i = minDisparity; i <= localMaxDisparity; i++) {
			score[i] = computeScore(c_x, c_x - i, c_y);
		}
	}

	/**
	 * Select best disparity using the inner takes all approach
	 *
	 * @param localMaxDisparity The max allowed disparity at this pixel
	 * @return The best disparity selected.
	 */
	protected double selectBest( int localMaxDisparity ) {
		int bestIndex = minDisparity;
		double bestScore = score[bestIndex];

		if (errorType.isCorrelation()) {
			for (int i = minDisparity + 1; i <= localMaxDisparity; i++) {
				if (score[i] > bestScore) {
					bestScore = score[i];
					bestIndex = i;
				}
			}
		} else {
			for (int i = minDisparity + 1; i <= localMaxDisparity; i++) {
				if (score[i] < bestScore) {
					bestScore = score[i];
					bestIndex = i;
				}
			}
		}

		return bestIndex - minDisparity;
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
	 * Compute SAD (Sum of Absolute Difference) error.
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

			case NCC: {
				return TestBlockRowScoreNcc.ncc((ImageBorder_F32)bleft, (ImageBorder_F32)bright,
						leftX, centerY, leftX - rightX, radiusX, radiusY, eps);
			}

			default:
				throw new RuntimeException("Egads");
		}
	}
}
