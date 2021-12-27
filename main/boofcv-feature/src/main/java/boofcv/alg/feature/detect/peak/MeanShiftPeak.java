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

package boofcv.alg.feature.detect.peak;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import lombok.Setter;

/**
 * Simple implementations of mean-shift intended to finding local peaks inside an intensity image.
 * Implementations differ in how they weigh each point. In each iterations the mean of the square sample region
 * centered around the target is computed and the center shifted so that location. It stops when change
 * is less than a delta or the maximum number of iterations has been exceeded.
 *
 * If the image border is hit while searching the square will be push back until it is entirely contained inside
 * the image.
 *
 * This is NOT thread safe.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MeanShiftPeak<T extends ImageGray<T>> {

	// Input image and interpolation function
	protected T image;
	protected @Getter @Setter InterpolatePixelS<T> interpolate;

	// the maximum number of iterations it will perform
	protected int maxIterations;

	// size of the kernel
	protected @Getter int radius;
	protected int width;

	// if the change in x and y is less than this value stop the search
	protected float convergenceTol;

	// The peak in x and y
	protected @Getter float peakX, peakY;

	protected @Getter boolean odd;

	// used to compute the weight each pixel contributes to the mean
	protected WeightPixel_F32 weights;

	/**
	 * Configures search.
	 *
	 * @param maxIterations Maximum number of iterations. Try 10
	 * @param convergenceTol Convergence tolerance. Try 1e-3
	 * @param weights Used to compute the weight each pixel contributes to the mean. Try a uniform distribution.
	 */
	public MeanShiftPeak( int maxIterations, float convergenceTol,
						  WeightPixel_F32 weights, boolean odd,
						  Class<T> imageType, BorderType borderType ) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.odd = odd;
		this.weights = weights;
		interpolate = FactoryInterpolation.bilinearPixelS(imageType, borderType);
	}

	/**
	 * Specifies the input image
	 *
	 * @param image input image
	 */
	public void setImage( T image ) {
		this.image = image;
		this.interpolate.setImage(image);
	}

	public void setRadius( int radius ) {
		this.weights.setRadius(radius, radius, odd);
		this.radius = radius;
		if (odd) {
			this.width = radius*2 + 1;
		} else {
			this.width = radius*2;
		}
	}

	/**
	 * Performs a mean-shift search center at the specified coordinates
	 */
	public void search( float cx, float cy ) {
		peakX = cx;
		peakY = cy;
		if (radius <= 0) { // can turn off refinement by setting radius to zero
			return;
		}
		float offset = -radius + (weights.isOdd() ? 0 : 0.5f);

		for (int i = 0; i < maxIterations; i++) {
			float total = 0;
			float sumX = 0, sumY = 0;

			int kernelIndex = 0;

			// see if it can use fast interpolation otherwise use the safer technique
			if (interpolate.isInFastBounds(peakX + offset, peakY + offset) &&
					interpolate.isInFastBounds(peakX - offset, peakY - offset)) {
				for (int yy = 0; yy < width; yy++) {
					float y = offset + yy;
					for (int xx = 0; xx < width; xx++) {
						float x = offset + xx;
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get_fast(peakX + x, peakY + y);
						total += weight;
						sumX += weight*x;
						sumY += weight*y;
					}
				}
			} else {
				for (int yy = 0; yy < width; yy++) {
					float y = offset + yy;
					for (int xx = 0; xx < width; xx++) {
						float x = offset + xx;
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get(peakX + x, peakY + y);
						total += weight;
						sumX += weight*x;
						sumY += weight*y;
					}
				}
			}

			// avoid divided by zero
			if (total == 0)
				break;

			cx = peakX + sumX/total;
			cy = peakY + sumY/total;

			float dx = cx - peakX;
			float dy = cy - peakY;

			peakX = cx;
			peakY = cy;

			if (Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol) {
				break;
			}
		}
	}

	/**
	 * Performs a mean-shift search center at the specified coordinates but with negative weights ignored
	 */
	public void searchPositive( float cx, float cy ) {
		peakX = cx;
		peakY = cy;
		if (radius <= 0) { // can turn off refinement by setting radius to zero
			return;
		}
		float offset = -radius + (weights.isOdd() ? 0 : 0.5f);

		for (int i = 0; i < maxIterations; i++) {
			float total = 0;
			float sumX = 0, sumY = 0;

			int kernelIndex = 0;

			// see if it can use fast interpolation otherwise use the safer technique
			if (interpolate.isInFastBounds(peakX + offset, peakY + offset) &&
					interpolate.isInFastBounds(peakX - offset, peakY - offset)) {
				for (int yy = 0; yy < width; yy++) {
					float y = offset + yy;
					for (int xx = 0; xx < width; xx++) {
						float x = offset + xx;
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get_fast(peakX + x, peakY + y);
						if (weight > 0f) {
							total += weight;
							sumX += weight*x;
							sumY += weight*y;
						}
					}
				}
			} else {
				for (int yy = 0; yy < width; yy++) {
					float y = offset + yy;
					for (int xx = 0; xx < width; xx++) {
						float x = offset + xx;
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get(peakX + x, peakY + y);
						if (weight > 0f) {
							total += weight;
							sumX += weight*x;
							sumY += weight*y;
						}
					}
				}
			}

			// avoid divided by zero
			if (total == 0)
				break;

			cx = peakX + sumX/total;
			cy = peakY + sumY/total;

			float dx = cx - peakX;
			float dy = cy - peakY;

			peakX = cx;
			peakY = cy;

			if (Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol) {
				break;
			}
		}
	}
}
