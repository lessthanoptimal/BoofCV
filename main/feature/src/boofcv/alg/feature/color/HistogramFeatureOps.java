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

package boofcv.alg.feature.color;

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * <p>
 * Type specific operations for creating histgrams of image pixel values.  The histogram
 * is a feature descriptor and all the feature descriptor operations can be used on these
 * histograms.
 * <p>
 *
 * <p>
 * Unlike histogram operations in {@link boofcv.alg.misc.ImageStatistics} the initial scale of
 * the image doesn't matter.  More specifically, {@link boofcv.alg.misc.ImageStatistics} simply
 * rounds the value to the nearest one and adds it to the element in a histogram.  While these
 * operations use the image's max values and the number of elements in the histogram to compute
 * the bin period.
 * </p>
 * @author Peter Abeles
 */
public class HistogramFeatureOps {

	/**
	 * Computes a single-band normalized histogram from an integer image..
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param histogram Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogram(GrayU8 image , int maxPixelValue , TupleDesc_F64 histogram )
	{
		int numBins = histogram.size();
		int divisor = maxPixelValue+1;

		histogram.fill(0);

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int value = image.data[index] & 0xFF;
				int bin = numBins*value/divisor;

				histogram.value[bin]++;
			}
		}
	}

	/**
	 * Computes a single-band normalized histogram from an integer image..
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel
	 * @param histogram Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogram(GrayU16 image , int maxPixelValue , TupleDesc_F64 histogram )
	{
		int numBins = histogram.size();
		int divisor = maxPixelValue+1;

		histogram.fill(0);

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int value = image.data[index] & 0xFFFF;
				int bin = numBins*value/divisor;

				histogram.value[bin]++;
			}
		}
	}

	/**
	 * Computes a single-band normalized histogram from a floating point image..
	 *
	 * @param image Input image. Not modified.
	 * @param minPixelValue Minimum possible for for a pixel. Inclusive
	 * @param maxPixelValue Maximum possible value for a pixel. Inclusive
	 * @param histogram The output histogram.
	 */
	public static void histogram(GrayF32 image , float minPixelValue , float maxPixelValue , TupleDesc_F64 histogram )
	{
		int numBins = histogram.size();
		float divisor = maxPixelValue-minPixelValue;

		histogram.fill(0);

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int bin = (int)(numBins*(image.data[index]-minPixelValue)/divisor);
				if( bin == numBins )
					histogram.value[bin-1]++;
				else
					histogram.value[bin]++;
			}
		}
	}

	/**
	 * Constructs an N-D histogram from a {@link Planar} {@link GrayF32} image.
	 *
	 * @param image input image
	 * @param histogram preconfigured histogram to store the output
	 */
	public static void histogram_F32(Planar<GrayF32> image , Histogram_F64 histogram )
	{
		if (image.getNumBands() != histogram.getDimensions())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		if( !histogram.isRangeSet() )
			throw new IllegalArgumentException("Must specify range along each dimension in histogram");

		final int D = histogram.getDimensions();
		int coordinate[] = new int[ D ];

		histogram.fill(0);

		for (int y = 0; y < image.getHeight(); y++) {
			int imageIndex = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.getWidth(); x++ , imageIndex++) {
				for (int i = 0; i < D; i++) {
					coordinate[i] = histogram.getDimensionIndex(i,image.getBand(i).data[imageIndex]);
				}
				int index = histogram.getIndex(coordinate);
				histogram.value[index] += 1;
			}
		}
	}

	/**
	 * Constructs an N-D histogram from a {@link Planar} {@link GrayU8} image.
	 *
	 * @param image input image
	 * @param histogram preconfigured histogram to store the output
	 */
	public static void histogram_U8(Planar<GrayU8> image , Histogram_F64 histogram )
	{
		if (image.getNumBands() != histogram.getDimensions())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		if( !histogram.isRangeSet() )
			throw new IllegalArgumentException("Must specify range along each dimension in histogram");

		final int D = histogram.getDimensions();
		int coordinate[] = new int[ D ];

		histogram.fill(0);

		for (int y = 0; y < image.getHeight(); y++) {
			int imageIndex = image.getStartIndex() + y*image.getStride();
			for (int x = 0; x < image.getWidth(); x++, imageIndex++) {
				for (int i = 0; i < D; i++) {
					coordinate[i] = histogram.getDimensionIndex(i,image.getBand(i).data[imageIndex]&0xFF);
				}
				int index = histogram.getIndex(coordinate);
				histogram.value[index] += 1;
			}
		}
	}
}
