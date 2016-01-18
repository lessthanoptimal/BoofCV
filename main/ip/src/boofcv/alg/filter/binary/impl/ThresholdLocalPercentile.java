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

package boofcv.alg.filter.binary.impl;

import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;

/**
 * @author Peter Abeles
 */
// TODO handle case where image is smaller than regionWidth
public class ThresholdLocalPercentile {

	int minimumSpread;
	int regionWidth;
	int histogram[];

	byte defaultValue;

	// histogram integral to be at lower and upper percentile
	int lowerCount,upperCount;
	// hitstogram index which has the integral summing to at least count
	int lowerIndex,upperIndex;

	public ThresholdLocalPercentile( int regionWith , int maxValue ) {
		this.regionWidth = regionWith;
		histogram = new int[ maxValue ];
	}

	public void processInner(ImageUInt8 input , ImageUInt8 output ) {
		int r = regionWidth/2;
		int endX = input.width-regionWidth-1;
		for (int y = 0; y < input.height - regionWidth; y++) {
			int indexIn = input.startIndex + (y+r)*input.stride + r;
			int indexOut = output.startIndex + (y+r)*output.stride + r;
			initializeHistogram(r,y+r,input);

			for (int x = 0; x <= endX; x++, indexIn++, indexOut++ ) {
				findPercentiles();
				if( upperIndex-lowerIndex >= minimumSpread ) {
					output.data[indexOut] = defaultValue;
				} else {
					int threshold = (upperIndex+lowerIndex)/2;
					if( (input.data[indexIn]&0xFF) <= threshold) {
						output.data[indexIn] = 1;
					} else {
						output.data[indexIn] = 0;
					}
				}
				if( x < endX )
					updateHistogramRight(x,y+r,input);
			}
		}
	}

	public void processBorder(ImageUInt8 input , ImageUInt8 output ) {
		int r = regionWidth / 2;

	}

	private void initializeHistogram( int x0 , int y0 , ImageUInt8 input ) {
		int x1 = x0 + regionWidth;
		int y1 = y0 + regionWidth;

		Arrays.fill(histogram,0);

		for (int y = y0; y < y1; y++) {
			int index = input.startIndex + y*input.stride + x0;
			for( int x = x0; x < x1; x++ ) {
				histogram[input.data[index++]&0xFF]++;
			}
		}
	}

	private void updateHistogramRight(int x0 , int y0 , ImageUInt8 input ) {

	}

	private void updateHistogramDown(int x0 , int y0 , ImageUInt8 input ) {

	}

	private void findPercentiles() {
		int count = 0;
		lowerIndex = 0;
		for (lowerIndex = 0; lowerIndex < histogram.length; lowerIndex++) {
			count += histogram[lowerIndex];
			if( count >= lowerCount ) {
				break;
			}
		}

		upperIndex = lowerIndex;
		for (upperIndex = 0; upperIndex < histogram.length; upperIndex++) {
			count += histogram[upperIndex];
			if( count >= upperCount ) {
				break;
			}
		}
	}
}
