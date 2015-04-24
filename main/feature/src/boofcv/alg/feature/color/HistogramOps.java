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

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

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
	 * @param output Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogram( ImageUInt8 image , int maxPixelValue , HistogramMultiNorm output )
	{
		if( 1 != output.getNumBands() )
			throw new IllegalArgumentException("Histogram must have a single band");

		int divisor = maxPixelValue+1;
		int histStride = output.stride[0];

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int value = image.data[index] & 0xFF;
				int bin = histStride*value/divisor;

				output.hist[bin]++;
			}
		}

		output.normalize();
	}

	/**
	 * Computes a single-band normalized histogram from a floating point image..
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param output Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogram( ImageFloat32 image , float maxPixelValue , HistogramMultiNorm output )
	{
		if( 1 != output.getNumBands() )
			throw new IllegalArgumentException("Histogram must have a single band");


		float divisor = maxPixelValue*1.0001f;
		int histStride = output.stride[0];

		for( int y = 0; y < image.height; y++ ) {

			int index = image.startIndex + y*image.stride;
			for( int x = 0; x < image.width; x++ , index++ ) {
				int bin = (int)(histStride*image.data[index]/divisor);

				output.hist[bin]++;
			}
		}

		output.normalize();
	}

	/**
	 * Computes a multi-band normalized histogram.
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel for all bands.
	 * @param output Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogramU8( MultiSpectral<ImageUInt8> image ,  int maxPixelValue ,
									HistogramMultiNorm output ) {
		if (image.getNumBands() != output.getNumBands())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		int divisor = maxPixelValue + 1;

		for (int y = 0; y < image.height; y++) {
			int index = image.startIndex + y * image.stride;
			for (int x = 0; x < image.width; x++, index++) {
				int indexBin = 0;
				int binStride = 1;
				for (int i = 0; i < image.getNumBands(); i++) {
					ImageUInt8 band = image.getBand(i);
					int value = band.data[index] & 0xFF;
					int bin = output.stride[i] * value / divisor;

					indexBin += bin * binStride;
					binStride *= output.stride[i];
				}

				output.hist[indexBin]++;
			}
		}

		output.normalize();
	}

	/**
	 * Computes a multi-band normalized histogram.
	 *
	 * @param image Input image. Not modified.
	 * @param maxPixelValue Maximum possible value for a pixel in each band.
	 * @param output Output histogram.  Must have same number of bands as input image. Modified.
	 */
	public static void histogramF32( MultiSpectral<ImageFloat32> image ,  float[] maxPixelValue ,
									 HistogramMultiNorm output ) {
		if (image.getNumBands() != output.getNumBands())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		for (int y = 0; y < image.height; y++) {
			int index = image.startIndex + y * image.stride;
			for (int x = 0; x < image.width; x++, index++) {
				int indexBin = 0;
				int binStride = 1;
				for (int i = 0; i < image.getNumBands(); i++) {
					ImageFloat32 band = image.getBand(i);
					int bin = (int)(output.stride[i] * band.data[index] / (maxPixelValue[i]*1.00001f));

					indexBin += bin * binStride;
					binStride *= output.stride[i];
				}

				output.hist[indexBin]++;
			}
		}

		output.normalize();
	}

}
