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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageMultiBand;
import org.ddogleg.stats.UtilGaussian;

/**
 * Computes a local histogram weighted using a Gaussian function.  The weighting function is shaped using a rotated
 * rectangle, where the sigma along each axis is set by the rectangle's width and height.
 *
 * The histogram is stored in a row major format.
 *
 * @author Peter Abeles
**/
public class LocalWeightedHistogramRotRect<T extends ImageMultiBand> {

	// Interpolation function
	private InterpolatePixelMB<T> interpolate;
	// storage for interpolated pixel
	private float value[];

	// number of samples along each axis
	private int numSamples;

	// maximum value of a pixel in any band
	private float maxPixelValue;
	// number of binds in the histogram
	private int numBins;

	// cosine and sine of rotation rectangle angle
	private float c,s;

	// output of conversion from region to image coordinates
	private float imageX,imageY;

	// which element in the histogram does a coordinate in the grid belong to
	private int sampleHistIndex[];

	// storage for sample weights and the histogram
	private float weights[];
	private float histogram[];

	/**
	 * Configures histogram calculation.
	 *
	 * @param numSamples Number of points it samples along each axis of the rectangle.
	 * @param numSigmas Number of standard deviations away the sides will be from the center.  Try 3
	 * @param numHistogramBins Number of bins in the histogram
	 * @param numBands Number of bands in the input image
	 * @param maxPixelValue Maximum value of a pixel across all bands
	 * @param interpolate Function used to interpolate the image
	 */
	public LocalWeightedHistogramRotRect(int numSamples, double numSigmas,
										 int numHistogramBins, int numBands,
										 float maxPixelValue,
										 InterpolatePixelMB<T> interpolate) {
		this.numSamples = numSamples;
		this.numBins = numHistogramBins;
		this.maxPixelValue = maxPixelValue;
		this.interpolate = interpolate;

		sampleHistIndex = new int[ numSamples*numSamples ];
		histogram = new float[ (int)Math.pow(numHistogramBins,numBands) ];
		value = new float[ numBands ];

		// compute the weights by convolving 1D gaussian kernel
		weights = new float[ numSamples*numSamples ];

		float w[] = new float[ numSamples ];
		for( int i = 0; i < numSamples; i++ ) {
			float x = i/(float)(numSamples-1);
			w[i] =  (float)UtilGaussian.computePDF(0, 1, numSigmas*(x-0.5f) );
		}

		for( int y = 0; y < numSamples; y++ ) {
			for( int x = 0; x < numSamples; x++ ) {
				weights[y*numSamples + x] = w[y]*w[x];
			}
		}

	}

	/**
	 * Computes the histogram inside the specified region.  Results are returned by calling {@link #getHistogram()}.
	 *
	 * @param image Input image
	 * @param region Region which is to be sampled
	 */
	public void computeHistogram( T image , RectangleRotate_F32 region ) {

		interpolate.setImage(image);

		c = (float)Math.cos( region.theta );
		s = (float)Math.sin( region.theta );

		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = 0;
		}

		// if it is entirely inside the image, interpolate using a faster technique
		if( isInFastBounds(region) ) {
			computeHistogramInside(image, region);
		} else {
			computeHistogramBorder(image, region);
		}

		normalizeHistogram();
	}

	/**
	 * Computes the histogram quickly inside the image
	 */
	private void computeHistogramInside(T image, RectangleRotate_F32 region) {
		int indexWeight = 0;
		for( int y = 0; y < numSamples; y++ ) {
			float regionY = (y/(numSamples-1.0f) - 0.5f)*region.height;

			for( int x = 0; x < numSamples; x++ , indexWeight++ ) {
				float regionX = (x/(numSamples-1.0f) - 0.5f)*region.width;

				rectToImage(regionX,regionY,region);

				interpolate.get_fast(imageX,imageY,value);

				int indexHistogram = 0;
				int binStride = 1;
				for( int i = 0; i < image.getNumBands(); i++ ) {
					int bin = (int)(numBins*value[i]/maxPixelValue);

					indexHistogram += bin*binStride;
					binStride *= numBins;
				}

				sampleHistIndex[ y*numSamples + x ] = indexHistogram;
				histogram[indexHistogram] += weights[indexWeight];
			}
		}
	}

	/**
	 * Computes the histogram and skips pixels which are outside the image border
	 */
	private void computeHistogramBorder(T image, RectangleRotate_F32 region) {
		int indexWeight = 0;
		for( int y = 0; y < numSamples; y++ ) {
			float regionY = (y/(numSamples-1.0f) - 0.5f)*region.height;

			for( int x = 0; x < numSamples; x++ , indexWeight++ ) {
				float regionX = (x/(numSamples-1.0f) - 0.5f)*region.width;

				rectToImage(regionX,regionY,region);

				// make sure its inside the image
				if( !BoofMiscOps.checkInside(image, imageX, imageY))
					continue;

				// use the slower interpolation which can handle the border
				interpolate.get(imageX, imageY, value);

				int indexHistogram = 0;
				int binStride = 1;
				for( int i = 0; i < image.getNumBands(); i++ ) {
					int bin = (int)(numBins*value[i]/maxPixelValue);

					indexHistogram += bin*binStride;
					binStride *= numBins;
				}

				histogram[indexHistogram] += weights[indexWeight];
			}
		}
	}

	/**
	 * Checks to see if the region can be sampled using the fast algorithm
	 */
	protected boolean isInFastBounds(RectangleRotate_F32 region) {

		float w2 = region.width/2.0f;
		float h2 = region.height/2.0f;

		rectToImage(-w2,-h2,region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		rectToImage(-w2,h2,region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		rectToImage(w2,-h2,region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		rectToImage(w2,h2,region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;

		return true;
	}

	private void normalizeHistogram() {
		float total = 0;
		for( int i = 0; i < histogram.length; i++ ) {
			total += histogram[i];
		}
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] /= total;
		}
	}

	/**
	 * Converts a point from rectangle coordinates into image coordinates
	 */
	protected void rectToImage( float x , float y , RectangleRotate_F32 region ) {
		imageX = x*c - y*s + region.cx;
		imageY = x*s + y*c + region.cy;
	}

	public float[] getHistogram() {
		return histogram;
	}

	public int[] getSampleHistIndex() {
		return sampleHistIndex;
	}
}
