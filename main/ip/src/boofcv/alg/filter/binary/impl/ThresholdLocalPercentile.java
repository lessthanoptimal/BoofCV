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
public class ThresholdLocalPercentile {

	int minimumSpread;
	int regionWidth;
	int histogram[];

	boolean thresholdDown;

	// histogram integral to be at lower and upper percentile
	int lowerCount,upperCount;
	// histogram index which has the integral summing to at least count
	int lowerIndex,upperIndex;

	Assign assign;

	ImageUInt8 input;
	ImageUInt8 output;

	public ThresholdLocalPercentile(boolean thresholdDown,
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

		if( thresholdDown )
			assign = new AssignDown();
		else
			assign = new AssignUp();
	}

	public void process(ImageUInt8 input , ImageUInt8 output ) {
		this.input = input;
		this.output = output;

		InputSanityCheck.checkSameShape(input,output);

		if( thresholdDown) {
			processInner();
			processCorners();
			processSides();
		}
	}

	public void processInner() {
		int r = regionWidth/2;

		int endX = input.width-regionWidth-1;
		for (int y = 0; y < input.height - regionWidth; y++) {
			int indexIn = input.startIndex + (y+r)*input.stride + r;
			int indexOut = output.startIndex + (y+r)*output.stride + r;
			initializeHistogram(0,y,input);

			for (int x = 0; x <= endX; x++, indexIn++, indexOut++ ) {
				assign.assign(indexIn, indexOut);
				if( x < endX )
					updateHistogramRight(x,y,input);
			}
		}
	}

	/**
	 * Process the border.  The histogram is computed by a square flush against the border.  Then all the points
	 * inside the rectangle "below" the mid point are thresholded using it.
	 */
	public void processSides() {
		int r = regionWidth / 2;
		int x1 = input.width-regionWidth+r;
		int y1 = input.height-regionWidth+r;

		// Border TOP
		initializeHistogram(0,0,input);
		int y0 = 0;
		for (int x = r; x < x1; x++) {
			int indexIn = input.startIndex + y0*input.stride + x; //first pixel it image top and x = region middle
			int indexOut = output.startIndex + y0*output.stride + x;
			for (int y = 0; y < r; y++) { // assign all pixels from image top to region middle
				assign.assign(indexIn, indexOut);
				indexIn += input.stride;
				indexOut += output.stride;
			}
			updateHistogramRight(x-r,0,input);
		}

		// Border Bottom
		y0 = input.height-regionWidth;
		initializeHistogram(0,y0,input);
		for (int x = r; x < x1; x++) {
			int indexIn = input.startIndex + y1*input.stride + x;
			int indexOut = output.startIndex + y1*output.stride + x;
			for (int y = y1; y < input.height; y++) {
				assign.assign(indexIn, indexOut);
				indexIn += input.stride;
				indexOut += output.stride;
			}
			updateHistogramRight(x-r,y0,input);
		}

		// Border LEFT
		initializeHistogram(0,0,input);
		for (int y = r; y < y1; y++) {
			int indexIn = input.startIndex + y*input.stride;
			int indexOut = output.startIndex + y*output.stride;

			for (int x = 0; x < r; x++) {
				assign.assign(indexIn++, indexOut++);
			}
			updateHistogramDown(0,y-r,input);
		}

		// Border RIGHT
		int x0 = input.width-regionWidth;
		initializeHistogram(x0,0,input);
		for (int y = r; y < y1; y++) {
			int indexIn = input.startIndex + y*input.stride + x1;
			int indexOut = output.startIndex + y*output.stride + x1;

			for (int x = x1; x < input.width; x++) {
				assign.assign(indexIn++, indexOut++);
			}
			updateHistogramDown(x0,y-r,input);
		}
	}

	private void processCorners() {
		int r = regionWidth/2;
		int x0 = input.width-regionWidth+r;
		int y0 = input.height-regionWidth+r;

		processCorner(0,0,0,0,r,r);
		processCorner(x0-r,0,x0,0,input.width,r);
		processCorner(x0-r,y0-r,x0,y0,input.width,input.height);
		processCorner(0,y0-r,0,y0,r,input.height);
	}

	private void processCorner( int seedX , int seedY , int x0 , int y0 , int x1 , int y1 ) {
		initializeHistogram(seedX,seedY,input);
		for (int y = y0; y < y1; y++) {
			int indexIn = input.startIndex + y*input.stride + x0;
			int indexOut = output.startIndex + y*output.stride + x0;
			for (int x = x0; x < x1; x++) {
				assign.assign(indexIn++, indexOut++);
			}
		}
	}

	private void initializeHistogram( int x0 , int y0 , ImageUInt8 input ) {
		int x1 = x0 + regionWidth;
		int y1 = y0 + regionWidth;

		Arrays.fill(histogram,0);

		for (int y = y0; y < y1; y++) {
			int index = input.startIndex + y * input.stride + x0;
			for (int x = x0; x < x1; x++) {
				histogram[input.data[index++] & 0xFF]++;
			}
		}
	}

	private void updateHistogramRight(int x0 , int y0 , ImageUInt8 input ) {
		int y1 = y0 + regionWidth;

		for (int y = y0; y < y1; y++) {
			int index = input.startIndex + y*input.stride + x0;
			histogram[input.data[index]&0xFF]--;
			histogram[input.data[index+regionWidth]&0xFF]++;
		}
	}

	private void updateHistogramDown(int x0 , int y0 , ImageUInt8 input ) {
		int x1 = x0 + regionWidth;

		int bottom = regionWidth*input.stride;

		int index = input.startIndex + y0*input.stride + x0;
		for (int x = x0; x < x1; x++, index++) {
			histogram[input.data[index]&0xFF]--;
			histogram[input.data[index+bottom]&0xFF]++;
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

		upperIndex = lowerIndex;
		for (upperIndex = 0; upperIndex < histogram.length; upperIndex++) {
			count += histogram[upperIndex];
			if( count >= upperCount ) {
				break;
			}
		}
	}

	public int getHistogramLength() {
		return histogram.length;
	}

	private interface Assign {
		void assign(int indexIn, int indexOut);
	}

	private class AssignDown implements Assign {
		@Override
		public void assign(int indexIn, int indexOut) {
			findPercentiles();
			if( upperIndex-lowerIndex <= minimumSpread ) {
				output.data[indexOut] = 1;
			} else {
				int threshold = (upperIndex+lowerIndex)/2;
				if( (input.data[indexIn]&0xFF) <= threshold) {
					output.data[indexIn] = 1;
				} else {
					output.data[indexIn] = 0;
				}
			}
		}
	}

	private class AssignUp implements Assign {
		@Override
		public void assign(int indexIn, int indexOut) {
			findPercentiles();
			if( upperIndex-lowerIndex <= minimumSpread ) {
				output.data[indexOut] = 1;
			} else {
				int threshold = (upperIndex+lowerIndex)/2;
				if( (input.data[indexIn]&0xFF) <= threshold) {
					output.data[indexIn] = 0;
				} else {
					output.data[indexIn] = 1;
				}
			}
		}
	}
}
