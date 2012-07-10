/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageFloat32;

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
 * <p>
 * [1] Neubeck, A. and Van Gool, L. "Efficient non-maximum suppression" ICPR 2006
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class NonMaxBlock {

	// search region
	protected int radius;
	// minimum intensity value
	protected float threshold;
	// should it ignore border pixels?
	protected int border;

	// found peaks
	protected QueueCorner peaks;

	protected NonMaxBlock() {
	}

	protected NonMaxBlock(int radius, float threshold, int border) {
		this.radius = radius;
		this.threshold = threshold;
		this.border = border;
	}

	public void process(ImageFloat32 intensityImage, QueueCorner peaks) {

		this.peaks = peaks;

		int endY = intensityImage.height-border;
		int endX = intensityImage.width-border;

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

	protected abstract void searchBlock( int x0 , int y0 , int x1 , int y1 , ImageFloat32 img );

	public void setSearchRadius(int minSeparation) {
		this.radius = minSeparation;
	}

	public void setThreshold(float thresh) {
		this.threshold = thresh;
	}

	public int getBorder() {
		return border;
	}

	public void setBorder(int border) {
		this.border = border;
	}

	public float getThreshold() {
		return threshold;
	}
}
