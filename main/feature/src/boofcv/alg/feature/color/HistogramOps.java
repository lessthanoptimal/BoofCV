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

import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;

import java.util.List;

/**
 * Image histogram operations with a focus on extracting features inside the image.
 *
 * @author Peter Abeles
 */
// TODO get rid of fhistogram just histogram
// Histogram should have different number of bins in each band
// implementation for single band, MS with 2 and 3 bands or manual selection
	// function for converting histogram into a feature descriptor
	// funciton for converting a basic histogram into a Histogram feature

	// todo rename to HistogramFeatureOps
public class HistogramOps {

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
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
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
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param histogram The output histogram.
	 */
	public static void histogram( ImageFloat32 image , float maxPixelValue , TupleDesc_F64 histogram )
	{
		int numBins = histogram.size();
		float divisor = maxPixelValue*1.0001f;

		histogram.fill(0);

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int bin = (int)(numBins*image.data[index]/divisor);

				histogram.value[bin]++;
			}
		}
	}

	/**
	 * Computes a single-band normalized histogram for any single band image.
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param histogram The output histogram.
	 */
	public static <T extends ImageSingleBand>
	void histogram( T image ,  double maxPixelValue , TupleDesc_F64 histogram ) {
		if( image.getClass() == ImageUInt8.class ) {
			histogram((ImageUInt8)image,(int)maxPixelValue,histogram );
		} else if( image.getClass() == ImageUInt16.class ) {
			histogram((ImageUInt16)image,(int)maxPixelValue,histogram);
		} else if( image.getClass() == ImageFloat32.class ) {
			histogram((ImageFloat32)image,(float)maxPixelValue,histogram);
		} else {
			throw new IllegalArgumentException("Unsupported band type");
		}
	}

	/**
	 * Computes a histogram for each band in the image.
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param output Output histograms.  Must have same number of bands as input image. Modified.
	 */
	public static<T extends ImageSingleBand>
	void histogram( MultiSpectral<T> image ,  double maxPixelValue , List<TupleDesc_F64> output ) {
		if (image.getNumBands() != output.size())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		for (int i = 0; i < image.getNumBands(); i++) {
			histogram(image.getBand(i),maxPixelValue,output.get(i));
		}
	}
}
