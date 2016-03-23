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

package boofcv.alg.tracker.tld;

import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.*;

/**
 * Compute the variance for a rectangular region using the integral image.  Supports both U8 and F32 input images.
 * For each new image in the sequence a call to {@link #setImage(ImageGray)} must be done
 * so that it can compute the required integral images.  See paper for mathematical details on how the variance
 * is computed using integral images.
 *
 * @author Peter Abeles
 */
public class TldVarianceFilter<T extends ImageGray> {

	// threshold for selecting candidate regions
	private double thresholdLower;

	// integral image used to compute mean
	private ImageGray integral;
	// integral image of the pixel value squared
	private ImageGray integralSq;

	/**
	 * Constructor which specifies the input image type.
	 *
	 * @param imageType  Either {@link GrayU8} or {@link GrayF32}
	 */
	public TldVarianceFilter( Class<T> imageType ) {

		// declare integral images.
		if(GeneralizedImageOps.isFloatingPoint(imageType) ) {
			integral = new GrayF32(1,1);
			integralSq = new GrayF64(1,1);
		} else {
			integral = new GrayS32(1,1);
			integralSq = new GrayS64(1,1);
		}
	}

	protected TldVarianceFilter() {
	}

	/**
	 * Sets the input image.  Must be called before other functions/
	 *
	 * @param gray input image
	 */
	public void setImage(T gray) {
		integral.reshape(gray.width,gray.height);
		integralSq.reshape(gray.width,gray.height);

		GIntegralImageOps.transform(gray,integral);
		if( gray.getDataType().isInteger())
			transformSq((GrayU8)gray,(GrayS64)integralSq);
		else
			transformSq((GrayF32)gray,(GrayF64)integralSq);
	}

	/**
	 * Selects a threshold based on image statistics.  The paper suggestions 1/2 the variance in the initial patch
	 */
	public void selectThreshold( ImageRectangle r ) {
		double variance = computeVarianceSafe(r.x0, r.y0, r.x1, r.y1);

		thresholdLower = variance*0.5;
	}

	/**
	 * Performs variance test at the specified rectangle
	 *
	 * @return true if it passes and false if not
	 */
	public boolean checkVariance( ImageRectangle r ) {

		double sigma2 = computeVariance(r.x0,r.y0,r.x1,r.y1);

		return sigma2 >= thresholdLower;
	}

	/**
	 * Computes the variance inside the specified rectangle.  x0 and y0 must be &gt; 0.
	 *
	 * @return variance
	 */
	protected double computeVariance(int x0, int y0, int x1, int y1) {
		// can use unsafe operations here since x0 > 0 and y0 > 0
		double square = GIntegralImageOps.block_unsafe(integralSq, x0 - 1, y0 - 1, x1 - 1, y1 - 1);

		double area = (x1-x0)*(y1-y0);
		double mean = GIntegralImageOps.block_unsafe(integral, x0 - 1, y0 - 1, x1 - 1, y1 - 1)/area;

		return square/area - mean*mean;
	}

	/**
	 * Computes the variance inside the specified rectangle.
	 * @return variance
	 */
	protected double computeVarianceSafe(int x0, int y0, int x1, int y1) {
		// can use unsafe operations here since x0 > 0 and y0 > 0
		double square = GIntegralImageOps.block_zero(integralSq, x0 - 1, y0 - 1, x1 - 1, y1 - 1);

		double area = (x1-x0)*(y1-y0);
		double mean = GIntegralImageOps.block_zero(integral, x0 - 1, y0 - 1, x1 - 1, y1 - 1)/area;

		return square/area - mean*mean;
	}

	/**
	 * Integral image of pixel value squared. integer
	 */
	public static void transformSq(final GrayU8 input , final GrayS64 transformed )
	{
		int indexSrc = input.startIndex;
		int indexDst = transformed.startIndex;
		int end = indexSrc + input.width;

		long total = 0;
		for( ; indexSrc < end; indexSrc++ ) {
			int value = input.data[indexSrc]& 0xFF;
			transformed.data[indexDst++] = total += value*value;
		}

		for( int y = 1; y < input.height; y++ ) {
			indexSrc = input.startIndex + input.stride*y;
			indexDst = transformed.startIndex + transformed.stride*y;
			int indexPrev = indexDst - transformed.stride;

			end = indexSrc + input.width;

			total = 0;
			for( ; indexSrc < end; indexSrc++ ) {
				int value = input.data[indexSrc]& 0xFF;
				total +=  value*value;
				transformed.data[indexDst++] = transformed.data[indexPrev++] + total;
			}
		}
	}

	/**
	 * Integral image of pixel value squared.  floating point
	 */
	public static void transformSq(final GrayF32 input , final GrayF64 transformed )
	{
		int indexSrc = input.startIndex;
		int indexDst = transformed.startIndex;
		int end = indexSrc + input.width;

		double total = 0;
		for( ; indexSrc < end; indexSrc++ ) {
			float value = input.data[indexSrc];
			transformed.data[indexDst++] = total += value*value;
		}

		for( int y = 1; y < input.height; y++ ) {
			indexSrc = input.startIndex + input.stride*y;
			indexDst = transformed.startIndex + transformed.stride*y;
			int indexPrev = indexDst - transformed.stride;

			end = indexSrc + input.width;

			total = 0;
			for( ; indexSrc < end; indexSrc++ ) {
				float value = input.data[indexSrc];
				total +=  value*value;
				transformed.data[indexDst++] = transformed.data[indexPrev++] + total;
			}
		}
	}

	public double getThresholdLower() {
		return thresholdLower;
	}
}
