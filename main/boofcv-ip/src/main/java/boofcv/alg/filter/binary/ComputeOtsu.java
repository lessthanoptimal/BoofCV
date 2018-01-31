/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageGray;

/**
 * Computes different variants of Otsu. Can be configured to compute the standard version. This allows the user
 * to better handle textureless regions and can further tune it by scaling the threshold up and down.
 *
 * @see GThresholdImageOps#computeOtsu(ImageGray, int, int)
 * @see GThresholdImageOps#computeOtsu2(ImageGray, int, int)
 *
 * @author Peter Abeles
 */
public class ComputeOtsu {
	// which Otsu variant to use
	private boolean useOtsu2;

	// computed mean and variance
	public double threshold;
	public double variance;

	// Tuning parameter. 0 = standard Otsu. Greater than 0 will penalize zero texture.
	private double tuning;

	// Is the image being thresholded down or up
	public boolean down;

	// scale factor applied to the threshold. 1.0 = unmodified
	private double scale;

	/**
	 *
	 * @param useOtsu2 true to use modified otsu. false uses clasical
	 * @param tuning Tuning parameter. 0 = standard Otsu. Greater than 0 will penalize zero texture.
	 * @param down Is otsu being used to threshold the image up or down
	 * @param scale scale factor applied to the threshold. 1.0 = unmodified
	 */
	public ComputeOtsu(boolean useOtsu2, double tuning, boolean down, double scale) {
		this.useOtsu2 = useOtsu2;
		this.tuning = tuning;
		this.down = down;
		this.scale = scale;
	}

	public ComputeOtsu( boolean useOtsu2 , boolean down ) {
		this(useOtsu2,0,down,1.0);
	}

	/**
	 * Computes the threshold and stores the result in the 'threshold' variable
	 * @param histogram
	 * @param length length of histogram
	 * @param totalPixels total sum of all pixels in histogram
	 */
	public void compute(int histogram[] , int length , int totalPixels) {

		if( useOtsu2 ) {
			computeOtsu2(histogram,length,totalPixels);
		} else {
			computeOtsu(histogram,length,totalPixels);
		}

		// apply optional penalty to low texture regions
		variance += 0.001; // avoid divide by zero
		// multiply by threshold twice in an effort to have the image's scaling not effect the tuning parameter
		int adjustment =  (int)(tuning*threshold*tuning*threshold/variance+0.5);
		threshold += down ? -adjustment : adjustment;
		threshold = (int)(scale*Math.max(threshold,0)+0.5); // TODO  threshold is a double. REmove rounding?
	}

	protected void computeOtsu(int histogram[] , int length , int totalPixels ) {

		double dlength = length;
		double sum = 0;
		for (int i = 0; i < length; i++)
			sum += (i / dlength) * histogram[i];

		double sumB = 0;
		int wB = 0;

		variance = 0;
		threshold = 0;

		int i;
		for (i = 0; i < length; i++) {
			wB += histogram[i];               // Weight Background
			if (wB == 0) continue;

			int wF = totalPixels - wB;        // Weight Foreground
			if (wF == 0) break;

			double f = i / dlength;
			sumB += f * histogram[i];

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > variance) {
				variance = varBetween;
				threshold = i;
			}
		}
	}

	protected void computeOtsu2(int histogram[] , int length , int totalPixels ) {

		double dlength = length;
		double sum = 0;
		for (int i = 0; i < length; i++)
			sum += (i / dlength) * histogram[i];

		double sumB = 0;
		int wB = 0;

		variance = 0;
		threshold = 0;

		double selectedMB=0;
		double selectedMF=0;

		int i;
		for (i = 0; i < length; i++) {
			wB += histogram[i];               // Weight Background
			if (wB == 0) continue;

			int wF = totalPixels - wB;        // Weight Foreground
			if (wF == 0) break;

			double f = i / dlength;
			sumB += f * histogram[i];

			double mB = sumB / wB;            // Mean Background
			double mF = (sum - sumB) / wF;    // Mean Foreground

			// Calculate Between Class Variance
			double varBetween = (double) wB * (double) wF * (mB - mF) * (mB - mF);

			// Check if new maximum found
			if (varBetween > variance) {
				variance = varBetween;
				selectedMB = mB;
				selectedMF = mF;
			}
		}

		// select a threshold which maximizes the distance between the two distributions. In pathological
		// cases there's a dead zone where all the values are equally good and it would select a value with a low index
		// arbitrarily. Then if you scaled the threshold it would reject everything
		threshold = length*(selectedMB+selectedMF)/2.0;
	}

	public boolean isUseOtsu2() {
		return useOtsu2;
	}

	public void setUseOtsu2(boolean useOtsu2) {
		this.useOtsu2 = useOtsu2;
	}

	public double getTuning() {
		return tuning;
	}

	public void setTuning(double tuning) {
		this.tuning = tuning;
	}

	public boolean isDown() {
		return down;
	}

	public void setDown(boolean down) {
		this.down = down;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}
}
