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

package boofcv.alg.feature.detect.peak;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;

/**
 * Simple implementations of mean-shift intended to finding local peaks inside an intensity image.
 * Implementations differ in how they weigh each point.  In each iterations the mean of the square sample region
 * centered around the target is computed and the center shifted so that location.  It stops when change
 * is less than a delta or the maximum number of iterations has been exceeded.
 *
 * If the image border is hit while searching the square will be push back until it is entirely contained inside
 * the image.
 *
 * @author Peter Abeles
 */
public class MeanShiftPeak<T extends ImageGray> {

	// Input image and interpolation function
	protected T image;
	protected InterpolatePixelS<T> interpolate;

	// the maximum number of iterations it will perform
	protected int maxIterations;

	// size of the kernel
	protected int radius;
	protected int width;

	// if the change in x and y is less than this value stop the search
	protected float convergenceTol;

	// The peak in x and y
	protected float peakX,peakY;

	// top left corner of sample region
	protected float x0,y0;

	// used to compute the weight each pixel contributes to the mean
	protected WeightPixel_F32 weights;

	/**
	 * Configures search.
	 *
	 * @param maxIterations  Maximum number of iterations.  Try 10
	 * @param convergenceTol Convergence tolerance.  Try 1e-3
	 * @param weights Used to compute the weight each pixel contributes to the mean.  Try a uniform distribution.
	 */
	public MeanShiftPeak(int maxIterations, float convergenceTol,
						 WeightPixel_F32 weights,
						 Class<T> imageType) {
		this.maxIterations = maxIterations;
		this.convergenceTol = convergenceTol;
		this.weights = weights;
		interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	/**
	 * Specifies the input image
	 * @param image input image
	 */
	public void setImage(T image) {
		this.image = image;
		this.interpolate.setImage(image);
	}

	public void setRadius(int radius) {
		this.weights.setRadius( radius, radius );
		this.radius = radius;
		this.width = radius*2+1;
	}

	/**
	 * Performs a mean-shift search center at the specified coordinates
	 */
	public void search( float cx , float cy ) {

		peakX = cx; peakY = cy;
		setRegion(cx, cy);

		for( int i = 0; i < maxIterations; i++ ) {
			float total = 0;
			float sumX = 0, sumY = 0;

			int kernelIndex = 0;

			// see if it can use fast interpolation otherwise use the safer technique
			if( interpolate.isInFastBounds(x0, y0) &&
					interpolate.isInFastBounds(x0 + width - 1, y0 + width - 1)) {
				for( int yy = 0; yy < width; yy++ ) {
					for( int xx = 0; xx < width; xx++ ) {
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get_fast(x0 + xx, y0 + yy);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
					}
				}
			} else {
				for( int yy = 0; yy < width; yy++ ) {
					for( int xx = 0; xx < width; xx++ ) {
						float w = weights.weightIndex(kernelIndex++);
						float weight = w*interpolate.get(x0 + xx, y0 + yy);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
					}
				}
			}

			cx = sumX/total;
			cy = sumY/total;

			setRegion(cx, cy);

			float dx = cx-peakX;
			float dy = cy-peakY;

			peakX = cx; peakY = cy;

			if( Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol ) {
				break;
			}
		}
	}

	/**
	 * Updates the location of the rectangular bounding box
	 * @param cx Image center x-axis
	 * @param cy Image center y-axis
	 */
	protected void setRegion(float cx, float cy) {
		x0 = cx - radius;
		y0 = cy - radius;

		if( x0 < 0 ) { x0 = 0;}
		else if( x0+width > image.width ) { x0 = image.width-width; }
		if( y0 < 0 ) { y0 = 0;}
		else if( y0+width > image.height ) { y0 = image.height-width; }

	}

	public float getPeakX() {
		return peakX;
	}

	public float getPeakY() {
		return peakY;
	}
}
