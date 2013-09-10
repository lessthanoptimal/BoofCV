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

package boofcv.alg.feature.color;

import boofcv.alg.color.ColorHsv;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * Image histogram operations with a focus on extracting features inside the image.
 *
 * @author Peter Abeles
 */
public class FHistogramOps {

	public void histogram_U8( ImageUInt8 image , int maxPixelValue ,
							  int x0 , int y0 , int x1 ,int y1 ,
							  FHistogramNormalized output )
	{
		int numValues = maxPixelValue+1;

		for( int y = y0; y < y1; y++ ) {

			int index = image.startIndex + y*image.stride + x0;
			for( int x = x0; x < x1; x++ , index++ ) {
				int value = image.data[index] & 0xFF;
				int bin = output.numBins*value/numValues;

				output.hist[bin]++;
			}
		}

		float total = (x1-x0)*(y1-y0);
		for( int i = 0; i < output.hist.length; i++ ) {
			output.hist[i] /= total;
		}
	}

	public void histogram_U8( MultiSpectral<ImageUInt8> image , int maxPixelValue ,
							  int x0 , int y0 , int x1 ,int y1 ,
							  FHistogramNormalized output )
	{
		int numValues = maxPixelValue+1;

		for( int y = y0; y < y1; y++ ) {
			int index = image.startIndex + y*image.stride + x0;
			for( int x = x0; x < x1; x++ , index++ ) {
				int indexBin = 0;
				int binStride = 1;
				for( int i = 0; i < image.getNumBands(); i++ ) {
					ImageUInt8 band = image.getBand(i);
					int value = band.data[index] & 0xFF;
					int bin = output.numBins*value/numValues;

					indexBin += bin*binStride;
					binStride *= output.numBins;
				}

				output.hist[indexBin]++;
			}
		}

		float total = (x1-x0)*(y1-y0);
		for( int i = 0; i < output.hist.length; i++ ) {
			output.hist[i] /= total;
		}
	}

	public void histogram_RGB_to_HS_U8( MultiSpectral<ImageUInt8> image ,
										int x0 , int y0 , int x1 ,int y1 ,
										float minimumValue ,
										float hsv[] ,
										FHistogramNormalized output )
	{
		// divide it by a number slightly larger than the max to avoid the special case where it is equal to the max
		float sizeH = (float)(2.01*Math.PI/output.numBins);
		float sizeS = 1.01f/output.numBins;

		ImageUInt8 imageRed = image.getBand(0);
		ImageUInt8 imageGreen = image.getBand(1);
		ImageUInt8 imageBlue = image.getBand(2);

		float total = 0;

		for( int y = y0; y < y1; y++ ) {
			int index = image.startIndex + y*image.stride + x0;
			for( int x = x0; x < x1; x++ , index++ ) {
				int r = imageRed.data[index] & 0xFF;
				int g = imageGreen.data[index] & 0xFF;
				int b = imageBlue.data[index] & 0xFF;

				ColorHsv.rgbToHsv(r, g, b, hsv);

				if( hsv[2] < minimumValue )
					continue;

				int binH = (int)(hsv[0] / sizeH);
				int binS = (int)(hsv[1] / sizeS);

				output.hist[ binH*output.numBins + binS ]++;

				total++;
			}
		}

		for( int i = 0; i < output.hist.length; i++ ) {
			output.hist[i] /= total;
		}
	}

}
