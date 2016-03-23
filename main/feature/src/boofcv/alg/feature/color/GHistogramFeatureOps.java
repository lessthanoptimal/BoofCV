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
import boofcv.struct.image.*;

/**
 * <p>
 * Generic version of {@link HistogramFeatureOps} which determines image type at runtime.
 * See {@link }
 * </p>
 * @author Peter Abeles
 */
public class GHistogramFeatureOps {

	/**
	 * Computes a single-band normalized histogram for any single band image.
	 *
	 * @param image Input image. Not modified.
	 * @param minPixelValue Minimum possible value for a pixel.
	 * @param maxPixelValue Maximum possible value for a pixel.
	 * @param histogram The output histogram.
	 */
	public static <T extends ImageGray>
	void histogram( T image ,  double minPixelValue , double maxPixelValue , TupleDesc_F64 histogram ) {
		if( image.getClass() == GrayU8.class ) {
			HistogramFeatureOps.histogram((GrayU8) image, (int) maxPixelValue, histogram);
		} else if( image.getClass() == GrayU16.class ) {
			HistogramFeatureOps.histogram((GrayU16) image, (int) maxPixelValue, histogram);
		} else if( image.getClass() == GrayF32.class ) {
			HistogramFeatureOps.histogram((GrayF32) image, (float) minPixelValue, (float) maxPixelValue, histogram);
		} else {
			throw new IllegalArgumentException("Unsupported band type");
		}
	}

	/**
	 * Computes a joint histogram for a planar image.  Since it's a joint distribution the histogram
	 * can become huge (and too sparse) if bin sizes are used that are too big.  Also consider computing the
	 * histogram independently in each band.
	 *
	 * @param image Input image. Not modified.
	 * @param histogram Output for the histogram.  Must be correctly configured first.
	 */
	public static<T extends ImageGray>
	void histogram(Planar<T> image , Histogram_F64 histogram ) {
		if (image.getNumBands() != histogram.getDimensions())
			throw new IllegalArgumentException("Number of bands in the image and histogram must be the same");

		if( image.getBandType() == GrayU8.class ) {
			HistogramFeatureOps.histogram_U8((Planar<GrayU8>)image, histogram);
		} else if( image.getBandType() == GrayF32.class ) {
			HistogramFeatureOps.histogram_F32((Planar<GrayF32>)image, histogram);
		} else {
			throw new IllegalArgumentException("Umage type not yet supportd");
		}
	}

	/**
	 * Computes a coupled histogram from a list of colors.  If the input is for integer values then add one
	 * to the maximum value.  For example if the range of values is 0 to 255, then make it 0 to 256.
	 *
	 * @param colors List of colors stored in an interleaved format
	 * @param length Length of usable portion of colors
	 * @param histogram Output and histogram configuration.
	 */
	public static void histogram( double[] colors, int length , Histogram_F64 histogram )
	{
		if( length % histogram.getDimensions() != 0 )
			throw new IllegalArgumentException("Length does not match dimensions");

		int coordinate[] = new int[ histogram.getDimensions() ];

		histogram.fill(0);

		for (int i = 0; i < length; ) {
			for (int j = 0; j < coordinate.length; j++, i++) {
				coordinate[j] = histogram.getDimensionIndex(j,colors[i]);
			}

			int index = histogram.getIndex(coordinate);
			histogram.value[index] += 1.0;
		}
	}
}
