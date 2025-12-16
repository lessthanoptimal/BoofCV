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

import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.image.GrayF32;
import georegression.struct.packed.PackedArrayPoint2D_I16;
import georegression.struct.point.Point2D_I16;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

/**
 * <p>Concurrent implementation of {@link NonMaxBlock_MT}. Every row of block is run in its own threads.
 * All threads keep track of all the found mins/maxs in seperate lists which are then combined after
 * a thread has finished running. All searches and point lists declared for each thread
 * are saved for future use</p>
 *
 * @author Peter Abeles
 */
public class NonMaxBlock_MT<Storage> extends NonMaxBlock<Storage> {

	// lock for variables below - which are lists used to store work space for individual threads
	final GrowArray<SearchData> searches;

	public NonMaxBlock_MT( Search<Storage> search ) {
		super(search);

		// Each thread will collect result using specific data type to store the points
		searches = new GrowArray<>(this::createSearchData);
	}

	/**
	 * Detects local minimums and/or maximums in the provided intensity image.
	 *
	 * @param intensityImage (Input) Feature intensity image.
	 * @param localMin (Output) storage for found local minimums.
	 * @param localMax (Output) storage for found local maximums.
	 */
	@Override public void process( GrayF32 intensityImage,
								   @Nullable Storage localMin, @Nullable Storage localMax ) {
		if (localMin != null)
			opReset.reset(localMin);
		if (localMax != null)
			opReset.reset(localMax);

		// the defines the region that can be processed
		int endX = intensityImage.width - border;
		int endY = intensityImage.height - border;

		int step = configuration.radius + 1;

		search.initialize(configuration, intensityImage, localMin, localMax);

		// Compute number of y iterations
		int range = endY - border;
		int N = range/step;
		if (range > N*step)
			N += 1;

		// The previous version required locks. In a benchmark in Java 11 this lock free version and the previous
		// had identical performance.
		BoofConcurrency.loopBlocks(0, N, searches, ( blockInfo, iter0, iter1 ) -> {
			final Search<PackedArrayPoint2D_I16> search = blockInfo.search;
			blockInfo.cornersMin.reset();
			blockInfo.cornersMax.reset();
			PackedArrayPoint2D_I16 threadMin = localMin != null ? blockInfo.cornersMin : null;
			PackedArrayPoint2D_I16 threadMax = localMax != null ? blockInfo.cornersMax : null;

			search.initialize(configuration, intensityImage, threadMin, threadMax);

			for (int iterY = iter0; iterY < iter1; iterY++) {
				// search for local peaks along this block row
				int y = border + iterY*step;
				int y1 = y + step;
				if (y1 > endY) y1 = endY;

				for (int x = border; x < endX; x += step) {
					int x1 = x + step;
					if (x1 > endX) x1 = endX;
					search.searchBlock(x, y, x1, y1);
				}
			}
		});

		// Save results outside the thread. This ensures the order is not randomized. That was wrecking havoc
		// on results that needed to be deterministic
		for (int i = 0; i < searches.size(); i++) {
			SearchData data = searches.get(i);

			addAll(data.cornersMin, localMin);
			addAll(data.cornersMax, localMax);
		}
	}

	/// Adds all points stored in src into dst. Checks to see if dst is null
	final void addAll( PackedArrayPoint2D_I16 src, @Nullable Storage dst ) {
		if (dst == null)
			return;

		for (int i = 0; i < src.size(); i++) {
			Point2D_I16 p = src.getTemp(i);
			opAdd.add(dst, p.x, p.y);
		}
	}

	public SearchData createSearchData() {
		return new SearchData(search.newInstance(PackedArrayPoint2D_I16::append));
	}

	public static class SearchData {
		public final Search<PackedArrayPoint2D_I16> search;
		public final PackedArrayPoint2D_I16 cornersMin = new PackedArrayPoint2D_I16();
		public final PackedArrayPoint2D_I16 cornersMax = new PackedArrayPoint2D_I16();

		public SearchData( Search<PackedArrayPoint2D_I16> search ) {
			this.search = search;
		}
	}
}
