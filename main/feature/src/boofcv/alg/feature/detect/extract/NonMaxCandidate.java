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
import georegression.struct.point.Point2D_I16;

/**
 * <p/>
 * Performs a sparse search for local minimums/maximums by only examine around candidates.
 * <p/>
 *
 * @author Peter Abeles
 */
public abstract class NonMaxCandidate {
	// size of the search area
	int radius;
	// threshold for intensity values when detecting minimums and maximums
	protected float thresholdMin;
	protected float thresholdMax;
	// does not process pixels this close to the image border
	int ignoreBorder;

	protected GrayF32 input;

	// upper bound on detectable extremes in the image
	int endBorderX, endBorderY;

	// local area that's examined and cropped for the image border
	int x0,y0,x1,y1;

	public NonMaxCandidate() {
	}

	/**
	 * Checks to see if the specified candidates are local minimums or maximums.  If a candidate list is
	 * null then that test is skipped.
	 */
	public void process(GrayF32 intensityImage,
						QueueCorner candidatesMin, QueueCorner candidatesMax,
						QueueCorner foundMin , QueueCorner foundMax ) {

		this.input = intensityImage;

		// pixels indexes larger than these should not be examined
		endBorderX = intensityImage.width-ignoreBorder;
		endBorderY = intensityImage.height-ignoreBorder;

		if( candidatesMin != null )
			examineMinimum(intensityImage,candidatesMin,foundMin);
		if( candidatesMax != null )
			examineMaximum(intensityImage,candidatesMax,foundMax);

	}

	protected void examineMinimum(GrayF32 intensityImage , QueueCorner candidates , QueueCorner found ) {
		final int stride = intensityImage.stride;
		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.size; iter++) {
			Point2D_I16 pt = candidates.data[iter];

			if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endBorderX || pt.y >= endBorderY)
				continue;

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val > thresholdMin || val == -Float.MAX_VALUE ) continue;

			x0 = Math.max(0,pt.x - radius);
			y0 = Math.max(0,pt.y - radius);
			x1 = Math.min(intensityImage.width, pt.x + radius + 1);
			y1 = Math.min(intensityImage.height, pt.y + radius + 1);

			if( searchMin(center,val) )
				found.add(pt.x,pt.y);
		}
	}

	protected void examineMaximum(GrayF32 intensityImage , QueueCorner candidates , QueueCorner found ) {
		final int stride = intensityImage.stride;
		final float inten[] = intensityImage.data;

		for (int iter = 0; iter < candidates.size; iter++) {
			Point2D_I16 pt = candidates.data[iter];

			if( pt.x < ignoreBorder || pt.y < ignoreBorder || pt.x >= endBorderX || pt.y >= endBorderY)
				continue;

			int center = intensityImage.startIndex + pt.y * stride + pt.x;

			float val = inten[center];
			if (val < thresholdMax || val == Float.MAX_VALUE ) continue;

			x0 = Math.max(0,pt.x - radius);
			y0 = Math.max(0,pt.y - radius);
			x1 = Math.min(intensityImage.width, pt.x + radius + 1);
			y1 = Math.min(intensityImage.height, pt.y + radius + 1);

			if( searchMax(center,val) )
				found.add(pt.x,pt.y);
		}
	}

	protected abstract boolean searchMin( int center , float val );
	protected abstract boolean searchMax( int center , float val );

	public void setSearchRadius(int radius) {
		this.radius = radius;
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

	public void setBorder( int border ) {
		this.ignoreBorder = border;

	}

	public int getBorder() {
		return ignoreBorder;
	}
}
