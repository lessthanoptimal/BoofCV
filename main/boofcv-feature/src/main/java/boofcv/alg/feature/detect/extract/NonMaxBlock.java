/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * Non-maximum extractor based on the block algorithm in [1]. The worst case complexity per
 * pixel is 4 - 4/(n+1) where n is the region size. The algorithm works by breaking up the
 * image into a set of evenly spaced blocks with their sides touching. The local maximum is
 * found inside a block and then the region around that maximum is examined to see if it is
 * truly a local max.
 * </p>
 *
 * <p>
 * Each block check is independent of all the others and no information is exchanged. This
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
public class NonMaxBlock {

	protected final Configuration configuration = new Configuration();
	// should it ignore border pixels?
	protected int border;

	// Used to search individual blocks
	protected Search search;

	public NonMaxBlock( Search search ) {
		this.search = search;
	}

	/**
	 * Detects local minimums and/or maximums in the provided intensity image.
	 *
	 * @param intensityImage (Input) Feature intensity image.
	 * @param localMin (Output) storage for found local minimums.
	 * @param localMax (Output) storage for found local maximums.
	 */
	public void process( GrayF32 intensityImage, @Nullable QueueCorner localMin, @Nullable QueueCorner localMax ) {

		if (localMin != null)
			localMin.reset();
		if (localMax != null)
			localMax.reset();

		// the defines the region that can be processed
		int endX = intensityImage.width - border;
		int endY = intensityImage.height - border;

		int step = configuration.radius + 1;

		search.initialize(configuration, intensityImage, localMin, localMax);

		for (int y = border; y < endY; y += step) {
			int y1 = y + step;
			if (y1 > endY) y1 = endY;

			for (int x = border; x < endX; x += step) {
				int x1 = x + step;
				if (x1 > endX) x1 = endX;
				search.searchBlock(x, y, x1, y1);
			}
		}
	}

	public Search getSearch() {
		return search;
	}

	public void setSearchRadius( int radius ) {
		configuration.radius = radius;
	}

	public int getBorder() {
		return border;
	}

	public void setBorder( int border ) {
		this.border = border;
	}

	public int getSearchRadius() {
		return configuration.radius;
	}

	public float getThresholdMin() {
		return configuration.thresholdMin;
	}

	public void setThresholdMin( float thresholdMin ) {
		configuration.thresholdMin = thresholdMin;
	}

	public float getThresholdMax() {
		return configuration.thresholdMax;
	}

	public void setThresholdMax( float thresholdMax ) {
		configuration.thresholdMax = thresholdMax;
	}

	public interface Search {
		/**
		 * Call before each time a search is started on a new image
		 *
		 * @param configuration Describes how it should search
		 * @param image The image that is being searched
		 * @param localMin Storage for local minimums. Can be null if not used.
		 * @param localMax Storage for local maximums. Can be null if not used.
		 */
		void initialize( Configuration configuration,
						 GrayF32 image, @Nullable QueueCorner localMin, @Nullable QueueCorner localMax );

		/**
		 * Search the image for local max/min inside the specified region
		 *
		 * @param x0 lower extent X (inclusive)
		 * @param y0 lower extent Y (inclusive)
		 * @param x1 upper extent X (exclusive)
		 * @param y1 upper extent Y (exclusive)
		 */
		void searchBlock( int x0, int y0, int x1, int y1 );

		boolean isDetectMinimums();

		boolean isDetectMaximums();

		/**
		 * Create a new instance of this search algorithm. Useful for concurrent implementations
		 */
		Search newInstance();
	}

	static class Configuration {
		// threshold for intensity values when detecting minimums and maximums
		public float thresholdMin;
		public float thresholdMax;

		// the search radius
		public int radius;
	}
}
