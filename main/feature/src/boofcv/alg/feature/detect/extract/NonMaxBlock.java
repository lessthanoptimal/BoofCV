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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;

/**
 * <p>
 * Non-maximum extractor based on the block algorithm in [1].  The worst case complexity per
 * pixel is 4 - 4/(n+1) where n is the region size.  The algorithm works by breaking up the
 * image into a set of evenly spaced blocks with their sides touching.  The local maximum is
 * found inside a block and then the region around that maximum is examined to see if it is
 * truly a local max.
 * </p>
 *
 * <p>
 * Each block check is independent of all the others and no information is exchanged.  This
 * algorithm could be paralyzed easily and has no memory overhead.
 * </p>
 *
 * <p>See {@link boofcv.abst.feature.detect.extract.NonMaxSuppression} for a definition of parameters
 * not described in this document</p>
 *
 * <p>
 * [1] Neubeck, A. and Van Gool, L. "Efficient non-maximum suppression" ICPR 2006
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class NonMaxBlock {

	// search region
	protected int radius;
	// threshold for intensity values when detecting minimums and maximums
	protected float thresholdMin;
	protected float thresholdMax;
	// should it ignore border pixels?
	protected int border;

	// the defines the region that can be processed
	int endX,endY;

	// found minimums
	protected QueueCorner localMin;
	// found maximums
	protected QueueCorner localMax;

	// indicates the algorithm's behavior
	public boolean detectsMinimum;
	public boolean detectsMaximum;


	protected NonMaxBlock(boolean detectsMinimum, boolean detectsMaximum) {
		this.detectsMinimum = detectsMinimum;
		this.detectsMaximum = detectsMaximum;
	}

	/**
	 * Detects local minimums and/or maximums in the provided intensity image.
	 *
	 * @param intensityImage (Input) Feature intensity image.
	 * @param localMin (Output) storage for found local minimums.
	 * @param localMax (Output) storage for found local maximums.
	 */
	public void process(GrayF32 intensityImage, QueueCorner localMin, QueueCorner localMax) {

		this.localMin = localMin;
		this.localMax = localMax;

		endX = intensityImage.width-border;
		endY = intensityImage.height-border;

		int step = radius+1;

		for( int y = border; y < endY; y += step ) {
			int y1 = y + step;
			if( y1 > endY ) y1 = endY;

			for( int x = border; x < endX; x += step ) {
				int x1 = x + step;
				if( x1 > endX ) x1 = endX;
				searchBlock(x,y,x1,y1,intensityImage);
			}
		}
	}

	protected abstract void searchBlock( int x0 , int y0 , int x1 , int y1 , GrayF32 img );

	public void setSearchRadius(int radius) {
		this.radius = radius;
	}

	public int getBorder() {
		return border;
	}

	public void setBorder(int border) {
		this.border = border;
	}

	public int getSearchRadius() {
		return radius;
	}

	public float getThresholdMin() {
		return thresholdMin;
	}

	public void setThresholdMin(float thresholdMin) {
		this.thresholdMin = thresholdMin;
	}

	public float getThresholdMax() {
		return thresholdMax;
	}

	public void setThresholdMax(float thresholdMax) {
		this.thresholdMax = thresholdMax;
	}
}
