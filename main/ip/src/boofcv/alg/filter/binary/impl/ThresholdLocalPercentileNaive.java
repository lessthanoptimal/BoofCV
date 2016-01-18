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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageUInt8;

import java.util.Arrays;

/**
 * @author Peter Abeles
 */
// TODO handle case where image is smaller than regionWidth
public class ThresholdLocalPercentileNaive {

	int minimumSpread;
	int regionWidth;
	int histogram[];

	boolean thresholdDown;

	// histogram integral to be at lower and upper percentile
	int lowerCount,upperCount;
	// histogram index which has the integral summing to at least count
	int lowerIndex,upperIndex;

	public ThresholdLocalPercentileNaive(boolean thresholdDown,
										 int regionWidth , int histogramLength ,
										 int minimumSpread,
										 double lowerFrac , double upperFrac ) {
		this.thresholdDown = thresholdDown;
		this.regionWidth = regionWidth;
		this.minimumSpread = minimumSpread;
		histogram = new int[ histogramLength ];

		int N=regionWidth*regionWidth;
		lowerCount = (int)Math.round(lowerFrac*N);
		upperCount = (int)Math.round(upperFrac*N);
	}

	public void process(ImageUInt8 input , ImageUInt8 output ) {
		InputSanityCheck.checkSameShape(input,output);

		if( thresholdDown)
			processDown(input, output);
		else
			processUp(input,output);
	}

	private void processDown(ImageUInt8 input, ImageUInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				computeHistogram(x,y,input);
				findPercentiles();
				if( upperIndex-lowerIndex <= minimumSpread ) {
					output.set(x,y,1);
				} else {
					int threshold = (upperIndex+lowerIndex)/2;
					if( input.get(x,y) <= threshold) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				}
			}
		}
	}

	private void processUp(ImageUInt8 input, ImageUInt8 output) {
		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				computeHistogram(x,y,input);
				findPercentiles();
				if( upperIndex-lowerIndex <= minimumSpread ) {
					output.set(x,y,0);
				} else {
					int threshold = (upperIndex+lowerIndex)/2;
					if( input.get(x,y) > threshold) {
						output.set(x,y,1);
					} else {
						output.set(x,y,0);
					}
				}
			}
		}
	}


	private void computeHistogram( int xc , int yc , ImageUInt8 input ) {
		int x0 = xc - regionWidth/2;
		int y0 = yc - regionWidth/2;

		if( x0 < 0 )
			x0 = 0;
		if( y0 < 0 )
			y0 = 0;

		int x1 = x0 + regionWidth;
		int y1 = y0 + regionWidth;

		if( x1 >= input.width ) {
			x1 = input.width-1;
			x0 = x1 - regionWidth;
		}
		if( y1 >= input.height ) {
			y1 = input.height-1;
			y0 = y1 - regionWidth;
		}

		Arrays.fill(histogram,0);

		for (int y = y0; y < y1; y++) {
			int index = input.startIndex + y*input.stride + x0;
			for( int x = x0; x < x1; x++ ) {
				histogram[input.data[index++]&0xFF]++;
			}
		}
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

		for (upperIndex = lowerIndex; upperIndex < histogram.length; upperIndex++) {
			count += histogram[upperIndex];
			if( count >= upperCount ) {
				break;
			}
		}
	}

	public int getHistogramLength() {
		return histogram.length;
	}
}
