/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import javax.annotation.Nullable;

/**
 * <p/>
 * Performs a sparse search for local minimums/maximums by only examine around candidates.
 * <p/>
 *
 * @author Peter Abeles
 */
public class NonMaxCandidate {
	// size of the search area
	protected int radius;
	// threshold for intensity values when detecting minimums and maximums
	protected float thresholdMin;
	protected float thresholdMax;
	// does not process pixels this close to the image border
	protected int ignoreBorder;

	protected GrayF32 input;

	// local search algorithm
	protected Search search;

	// upper bound on detectable extremes in the image
	protected int endBorderX, endBorderY;

	public NonMaxCandidate( Search search ) {
		this.search = search;
	}

	/**
	 * Checks to see if the specified candidates are local minimums or maximums.  If a candidate list is
	 * null then that test is skipped.
	 */
	public void process(GrayF32 intensityImage,
						@Nullable QueueCorner candidatesMin, @Nullable QueueCorner candidatesMax,
						QueueCorner foundMin , QueueCorner foundMax ) {

		this.input = intensityImage;

		// pixels indexes larger than these should not be examined
		endBorderX = intensityImage.width-ignoreBorder;
		endBorderY = intensityImage.height-ignoreBorder;

		search.initialize(intensityImage);

		if( candidatesMin != null ) {
			foundMin.reset();
			examineMinimum(intensityImage, candidatesMin, foundMin);
		}
		if( candidatesMax != null ) {
			foundMax.reset();
			examineMaximum(intensityImage, candidatesMax, foundMax);
		}

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

			int x0 = Math.max(0,pt.x - radius);
			int y0 = Math.max(0,pt.y - radius);
			int x1 = Math.min(intensityImage.width, pt.x + radius + 1);
			int y1 = Math.min(intensityImage.height, pt.y + radius + 1);

			if( search.searchMin(x0,y0,x1,y1,center,val) )
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

			int x0 = Math.max(0,pt.x - radius);
			int y0 = Math.max(0,pt.y - radius);
			int x1 = Math.min(intensityImage.width, pt.x + radius + 1);
			int y1 = Math.min(intensityImage.height, pt.y + radius + 1);

			if( search.searchMax(x0,y0,x1,y1,center,val) )
				found.add(pt.x,pt.y);
		}
	}

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

	/**
	 * Interface for local search algorithm around the candidates
	 */
	public interface Search {
		void initialize( GrayF32 intensity );

		/**
		 * Verifies that the candidate is a local minimum
		 *
		 * @param x0 lower extent X. Inclusive
		 * @param y0 lower extent Y. Inclusive
		 * @param x1 upper extent X. Exclusive
		 * @param y1 upper extent Y. Exclusive
		 * @param centerIdx index of candidate pixel in the image
		 * @param val value at the candidate pixel
		 * @return true if it's a local min
		 */
		boolean searchMin( int x0 , int y0 , int x1 , int y1, int centerIdx , float val );
		boolean searchMax( int x0 , int y0 , int x1 , int y1, int centerIdx , float val );

		/**
		 * Create a new instance of this search algorithm. Useful for concurrent implementations
		 */
		Search newInstance();
	}

	/**
	 * Search with a relaxes rule. &le;
	 */
	public static class Relaxed implements NonMaxCandidate.Search {
		GrayF32 intensity;

		@Override
		public void initialize(GrayF32 intensity) {
			this.intensity = intensity;
		}

		@Override
		public boolean searchMin(int x0, int y0, int x1, int y1, int centerIdx, float val) {
			for( int i = y0; i < y1; i++ ) {
				int index = intensity.startIndex + i * intensity.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					if (val > intensity.data[index]) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public boolean searchMax(int x0, int y0, int x1, int y1, int centerIdx, float val) {
			for( int i = y0; i < y1; i++ ) {
				int index = intensity.startIndex + i * intensity.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					if (val < intensity.data[index]) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public Search newInstance() {
			return new Relaxed();
		}
	}

	/**
	 * Search with a strict rule &lt;
	 */
	public static class Strict implements NonMaxCandidate.Search {
		GrayF32 intensity;

		@Override
		public void initialize(GrayF32 intensity) {
			this.intensity = intensity;
		}

		@Override
		public boolean searchMin(int x0, int y0, int x1, int y1, int centerIdx, float val) {
			for( int i = y0; i < y1; i++ ) {
				int index = intensity.startIndex + i * intensity.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					// don't compare the center point against itself
					if ( centerIdx == index )
						continue;

					if (val >= intensity.data[index]) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public boolean searchMax(int x0, int y0, int x1, int y1, int centerIdx, float val) {
			for( int i = y0; i < y1; i++ ) {
				int index = intensity.startIndex + i * intensity.stride + x0;
				for( int j = x0; j < x1; j++ , index++ ) {
					// don't compare the center point against itself
					if ( centerIdx == index )
						continue;

					if (val <= intensity.data[index]) {
						return false;
					}
				}
			}
			return true;
		}

		@Override
		public Search newInstance() {
			return new Strict();
		}
	}
}
