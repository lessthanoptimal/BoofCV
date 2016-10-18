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

package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;
import org.ddogleg.stats.UtilGaussian;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes a local histogram weighted using a Gaussian function.  The weighting function is shaped using a rotated
 * rectangle, where the sigma along each axis is set by the rectangle's width and height.    For use with
 * {@link TrackerMeanShiftComaniciu2003}.
 *
 * The histogram is stored in a row major format.
 *
 * @author Peter Abeles
**/
public class LocalWeightedHistogramRotRect<T extends ImageBase> {

	// Interpolation function
	private InterpolatePixelMB<T> interpolate;
	// storage for interpolated pixel
	private float value[];

	// maximum value of a pixel in any band
	protected float maxPixelValue;
	// number of binds in the histogram
	private int numBins;

	// cosine and sine of rotation rectangle angle
	protected float c,s;

	// output of conversion from region to image coordinates
	public float imageX,imageY;

	// which element in the histogram does a coordinate in the grid belong to
	protected int sampleHistIndex[];

	// storage for sample weights and the histogram
	protected float weights[];
	protected float histogram[];

	// list of sample points.  in square coordinates.  where 0.5 is 1/2 the width or height
	protected List<Point2D_F32> samplePts = new ArrayList<>();

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
		this.numBins = numHistogramBins;
		this.maxPixelValue = maxPixelValue*1.0001f; // avoid the possibility exceeding the max histogram size
		this.interpolate = interpolate;

		sampleHistIndex = new int[ numSamples*numSamples ];
		histogram = new float[ (int)Math.pow(numHistogramBins,numBands) ];
		value = new float[ numBands ];

		createSamplePoints(numSamples);
		computeWeights(numSamples, numSigmas);
	}

	/**
	 * compute the weights by convolving 1D gaussian kernel
	 */
	protected void computeWeights(int numSamples, double numSigmas) {
		weights = new float[ numSamples*numSamples ];

		float w[] = new float[ numSamples ];
		for( int i = 0; i < numSamples; i++ ) {
			float x = i/(float)(numSamples-1);
			w[i] = (float) UtilGaussian.computePDF(0, 1, 2f*numSigmas * (x - 0.5f));
		}

		for( int y = 0; y < numSamples; y++ ) {
			for( int x = 0; x < numSamples; x++ ) {
				weights[y*numSamples + x] = w[y]*w[x];
			}
		}
	}

	/**
	 * create the list of points in square coordinates that it will sample.  values will range
	 * from -0.5 to 0.5 along each axis.
	 */
	protected void createSamplePoints(int numSamples) {
		for( int y = 0; y < numSamples; y++ ) {
			float regionY = (y/(numSamples-1.0f) - 0.5f);

			for( int x = 0; x < numSamples; x++  ) {
				float regionX = (x/(numSamples-1.0f) - 0.5f);
				samplePts.add( new Point2D_F32(regionX,regionY));
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
			computeHistogramInside( region);
		} else {
			computeHistogramBorder(image, region);
		}

		normalizeHistogram();
	}

	/**
	 * Computes the histogram quickly inside the image
	 */
	protected void computeHistogramInside( RectangleRotate_F32 region) {
		for( int i = 0; i < samplePts.size(); i++ ) {
			Point2D_F32 p = samplePts.get(i);

			squareToImageSample(p.x, p.y, region);

			interpolate.get_fast(imageX,imageY,value);

			int indexHistogram = computeHistogramBin(value);

			sampleHistIndex[ i ] = indexHistogram;
			histogram[indexHistogram] += weights[i];
		}
	}

	/**
	 * Computes the histogram and skips pixels which are outside the image border
	 */
	protected void computeHistogramBorder(T image, RectangleRotate_F32 region) {
		for( int i = 0; i < samplePts.size(); i++ ) {
			Point2D_F32 p = samplePts.get(i);

			squareToImageSample(p.x, p.y, region);

			// make sure its inside the image
			if( !BoofMiscOps.checkInside(image, imageX, imageY)) {
				sampleHistIndex[ i ] = -1;
			} else {
				// use the slower interpolation which can handle the border
				interpolate.get(imageX, imageY, value);

				int indexHistogram = computeHistogramBin(value);

				sampleHistIndex[ i ] = indexHistogram;
				histogram[indexHistogram] += weights[i];
			}
		}
	}

	/**
	 * Given the value of a pixel, compute which bin in the histogram it belongs in
	 */
	protected int computeHistogramBin( float value[] ) {
		int indexHistogram = 0;
		int binStride = 1;
		for( int bandIndex = 0; bandIndex < value.length; bandIndex++ ) {
			int bin = (int)(numBins*value[bandIndex]/maxPixelValue);

			indexHistogram += bin*binStride;
			binStride *= numBins;
		}
		return indexHistogram;
	}

	/**
	 * Checks to see if the region can be sampled using the fast algorithm
	 */
	protected boolean isInFastBounds(RectangleRotate_F32 region) {

		squareToImageSample(-0.5f, -0.5f, region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		squareToImageSample(-0.5f, 0.5f, region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		squareToImageSample(0.5f, 0.5f, region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;
		squareToImageSample(0.5f, -0.5f, region);
		if( !interpolate.isInFastBounds(imageX, imageY))
			return false;

		return true;
	}

	protected void normalizeHistogram() {
		float total = 0;
		for( int i = 0; i < histogram.length; i++ ) {
			total += histogram[i];
		}
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] /= total;
		}
	}

	/**
	 * Converts a point from square coordinates into image coordinates
	 */
	protected void squareToImageSample(float x, float y, RectangleRotate_F32 region) {
		// -1 because it starts counting at 0.  otherwise width+1 samples are made
		x *= region.width-1;
		y *= region.height-1;

		imageX = x*c - y*s + region.cx;
		imageY = x*s + y*c + region.cy;
	}

	public float[] getHistogram() {
		return histogram;
	}

	public int[] getSampleHistIndex() {
		return sampleHistIndex;
	}

	public List<Point2D_F32> getSamplePts() {
		return samplePts;
	}

}
