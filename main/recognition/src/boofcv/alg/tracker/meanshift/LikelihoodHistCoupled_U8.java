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

import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.shapes.Rectangle2D_I32;

/**
 * Creates a histogram in a color image.  The histogram is computed in N-dimensional space, where N is the number
 * of bands in the color image.  The number of bins for each band is specified in the constructor.  There
 * is a total of N*numBins elements in the histogram.
 *
 * @author Peter Abeles
 */
public class LikelihoodHistCoupled_U8 implements PixelLikelihood<MultiSpectral<ImageUInt8>>
{
	MultiSpectral<ImageUInt8> image;

	// maximum value a pixel can have.
	int maxPixelValue;
	// Number of bins for each channel in the histogram
	int numBins;
	float hist[] = new float[0];

	public LikelihoodHistCoupled_U8(int maxPixelValue, int numBins) {
		this.maxPixelValue = maxPixelValue+1;
		this.numBins = numBins;
	}

	@Override
	public void setImage(MultiSpectral<ImageUInt8> image) {
		this.image = image;

		int histElements = 1;
		for( int i = 0; i < image.getNumBands(); i++ ) {
			histElements *= numBins;
		}

		if( hist.length != histElements ) {
			hist = new float[histElements];
		}
	}

	@Override
	public boolean isInBounds(int x, int y) {
		return image.isInBounds(x,y);
	}

	@Override
	public void createModel(Rectangle2D_I32 target) {
		for( int y = 0; y < target.height; y++ ) {

			int index = image.startIndex + (y+target.tl_y)*image.stride + target.tl_x;
			for( int x = 0; x < target.width; x++ , index++ ) {
				int indexBin = 0;
				int binStride = 1;
				for( int i = 0; i < image.getNumBands(); i++ ) {
					ImageUInt8 band = image.getBand(i);
					int value = band.data[index] & 0xFF;
					int bin = numBins*value/maxPixelValue;

					indexBin += bin*binStride;
					binStride *= numBins;
				}

				hist[indexBin]++;
			}
		}

		float total = target.width*target.height;
		for( int i = 0; i < hist.length; i++ ) {
			hist[i] /= total;
		}
	}

	@Override
	public float compute(int x, int y) {
		int index = image.startIndex + y*image.stride + x;

		int indexBin = 0;
		int binStride = 1;
		for( int i = 0; i < image.getNumBands(); i++ ) {
			ImageUInt8 band = image.getBand(i);
			int value = band.data[index] & 0xFF;
			int bin = numBins*value/maxPixelValue;

			indexBin += bin*binStride;
			binStride *= numBins;
		}

		return hist[indexBin];
	}
}
