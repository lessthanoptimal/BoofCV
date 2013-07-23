/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
 * TODO comment
 *
 * @author Peter Abeles
 */
public class TldVarianceFilter<T extends ImageSingleBand> {

	// threshold for selecting candidate regions
	private double threshold;

	// integral image used to compute mean
	private ImageSingleBand integral;
	// integral image of the pixel value squared
	private ImageSingleBand integralSq;

	ImageRectangle r = new ImageRectangle();

	public TldVarianceFilter( Class<T> imageType ) {

		// declare integral images.
		if(GeneralizedImageOps.isFloatingPoint(imageType) ) {
			integral = new ImageFloat32(1,1);
			integralSq = new ImageFloat64(1,1);
		} else {
			integral = new ImageSInt32(1,1);
			integralSq = new ImageSInt64(1,1);
		}
	}

	/**
	 * Sets the input image
	 *
	 * @param gray input image
	 */
	public void setImage(T gray) {
		integral.reshape(gray.width,gray.height);
		integralSq.reshape(gray.width,gray.height);

		GIntegralImageOps.transform(gray,integral);
		if( gray.getTypeInfo().isInteger())
			transformSq((ImageUInt8)gray,(ImageSInt64)integralSq);
		else
			transformSq((ImageFloat32)gray,(ImageFloat64)integralSq);
	}

	/**
	 * Selects a threshold based on image statistics.  The paper suggestions 1/2 the variance in the initial patch
	 */
	public void selectThreshold( ImageRectangle r ) {
		threshold = computeVariance(r.x0,r.y0,r.x1,r.y1)*0.5;
	}

	/**
	 * Performs variance test at the specified rectangle
	 *
	 * @return true if it passes and false if not
	 */
	public boolean checkVariance( ImageRectangle r ) {

		double sigma2 = computeVariance(r.x0,r.y0,r.x1,r.y1);

		return sigma2 >= threshold;
	}

	/**
	 * Computes the variance inside the specified rectangle.
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
	 * Integral image of pixel value squared. integer
	 */
	public static void transformSq( final ImageUInt8 input , final ImageSInt64 transformed )
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
	public static void transformSq( final ImageFloat32 input , final ImageFloat64 transformed )
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

	public double getThreshold() {
		return threshold;
	}
}
