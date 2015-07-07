/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;

import java.util.List;

/**
 * <p>
 * Operations related to using image histograms as a way to compare images.  The histogram
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
// TODO also support computing likelihood of a color belonging to a histogram distribution
public class HistogramFeatureOps {

	/**
	 * Computes a single-band normalized histogram from an integer image..
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param histogram Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogram( ImageUInt8 image , int maxPixelValue , TupleDesc_F64 histogram )
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
	public static void histogram( ImageUInt16 image , int maxPixelValue , TupleDesc_F64 histogram )
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
	 * @param minPixelValue Minimum possible for for a pixel
	 * @param maxPixelValue Maximum possible value for a pixel.
	 * @param histogram The output histogram.
	 */
	public static void histogram( ImageFloat32 image , float minPixelValue , float maxPixelValue , TupleDesc_F64 histogram )
	{
		int numBins = histogram.size();
		float divisor = (maxPixelValue-minPixelValue)*1.0001f;

		histogram.fill(0);

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int bin = (int)(numBins*(image.data[index]-minPixelValue)/divisor);

				histogram.value[bin]++;
			}
		}
	}

	/**
	 * Computes a single-band normalized histogram for any single band image.
	 *
	 * @param image Input image. Not modified.
	 * @param minPixelValue Minimum possible value for a pixel.
	 * @param maxPixelValue Maximum possible value for a pixel.
	 * @param histogram The output histogram.
	 */
	public static <T extends ImageSingleBand>
	void histogram( T image ,  double minPixelValue , double maxPixelValue , TupleDesc_F64 histogram ) {
		if( image.getClass() == ImageUInt8.class ) {
			histogram((ImageUInt8)image,(int)maxPixelValue,histogram );
		} else if( image.getClass() == ImageUInt16.class ) {
			histogram((ImageUInt16)image,(int)maxPixelValue,histogram);
		} else if( image.getClass() == ImageFloat32.class ) {
			histogram((ImageFloat32)image,(float)minPixelValue,(float)maxPixelValue,histogram);
		} else {
			throw new IllegalArgumentException("Unsupported band type");
		}
	}

	/**
	 * Computes a histogram for each band in the image.
	 *
	 * @param image Input image. Not modified.
	 * @param minPixelValue Minimum possible value for a pixel for all bands.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param output Output histograms.  Must have same number of bands as input image. Modified.
	 */
	public static<T extends ImageSingleBand>
	void histogram( MultiSpectral<T> image ,
					double minPixelValue , double maxPixelValue , List<TupleDesc_F64> output ) {
		if (image.getNumBands() != output.size())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		for (int i = 0; i < image.getNumBands(); i++) {
			histogram(image.getBand(i),minPixelValue,maxPixelValue,output.get(i));
		}
	}

	public double distanceSAD( List<TupleDesc_F64> histogramA , List<TupleDesc_F64> histogramB ) {
		if( histogramA.size() != histogramB.size() )
			throw new IllegalArgumentException("Number of bands in histograms doesn't match");

		double total = 0;

		for (int i = 0; i < histogramA.size(); i++) {
			TupleDesc_F64 descA = histogramA.get(i);
			TupleDesc_F64 descB = histogramB.get(i);

			if( descA.size() != descB.size() )
				throw new IllegalArgumentException("Length of band "+i+" doesn't match");

			total += DescriptorDistance.sad(descA,descB);
		}

		return total;
	}

	public double distanceEuclidean( List<TupleDesc_F64> histogramA , List<TupleDesc_F64> histogramB ) {
		if( histogramA.size() != histogramB.size() )
			throw new IllegalArgumentException("Number of bands in histograms doesn't match");

		double total = 0;

		for (int i = 0; i < histogramA.size(); i++) {
			TupleDesc_F64 descA = histogramA.get(i);
			TupleDesc_F64 descB = histogramB.get(i);

			if( descA.size() != descB.size() )
				throw new IllegalArgumentException("Length of band "+i+" doesn't match");

			total += DescriptorDistance.euclidean(descA, descB);
		}

		return total;
	}

	public double distanceEuclideanSq( List<TupleDesc_F64> histogramA , List<TupleDesc_F64> histogramB ) {
		if( histogramA.size() != histogramB.size() )
			throw new IllegalArgumentException("Number of bands in histograms doesn't match");

		double total = 0;

		for (int i = 0; i < histogramA.size(); i++) {
			TupleDesc_F64 descA = histogramA.get(i);
			TupleDesc_F64 descB = histogramB.get(i);

			if( descA.size() != descB.size() )
				throw new IllegalArgumentException("Length of band "+i+" doesn't match");

			total += DescriptorDistance.euclideanSq(descA,descB);
		}

		return total;
	}

	public double distanceNCC( List<TupleDesc_F64> histogramA , List<TupleDesc_F64> histogramB ) {
		if( histogramA.size() != histogramB.size() )
			throw new IllegalArgumentException("Number of bands in histograms doesn't match");

		double total = 0;

		for (int i = 0; i < histogramA.size(); i++) {
			TupleDesc_F64 descA = histogramA.get(i);
			TupleDesc_F64 descB = histogramB.get(i);

			if( descA.size() != descB.size() )
				throw new IllegalArgumentException("Length of band "+i+" doesn't match");

			NccFeature nccA = new NccFeature(descA.size());
			NccFeature nccB = new NccFeature(descB.size());

			ConvertDescriptors.convertNcc(descA,nccA);
			ConvertDescriptors.convertNcc(descB,nccB);

			total += DescriptorDistance.ncc(nccA,nccB);
		}

		return total;
	}
}
