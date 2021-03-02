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

package boofcv.alg.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.impl.FastCornerInterface;
import boofcv.concurrency.BoofConcurrency;
import boofcv.misc.DiscretizedCircle;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import pabeles.concurrency.GrowArray;

/**
 * Concurrent version of {@link FastCornerDetector}
 *
 * @author Peter Abeles
 */
public class FastCornerDetector_MT<T extends ImageGray<T>> extends FastCornerDetector<T> {

	protected GrowArray<ThreadHelper> threadWorkspace;

	/**
	 * Constructor
	 *
	 * @param helper Provide the image type specific helper.
	 */
	public FastCornerDetector_MT( FastCornerInterface<T> helper ) {
		this.helper = helper;
		threadWorkspace = new GrowArray<>(() -> new ThreadHelper(helper.newInstance()));
	}

	/**
	 * Computes fast corner features and their intensity. The intensity is needed if non-max suppression is
	 * used
	 */
	@Override public void process( T image, GrayF32 intensity ) {
		this.image = image;

		if (stride != image.stride) {
			stride = image.stride;
			offsets = DiscretizedCircle.imageOffsets(radius, image.stride);
		}

		BoofConcurrency.loopBlocks(radius, image.height - radius, threadWorkspace, ( thread, y0, y1 ) -> {
			thread.reset(image.width, image.height);
			final ListIntPoint2D candidatesLow = thread.candidatesLow;
			final ListIntPoint2D candidatesHigh = thread.candidatesHigh;
			final FastCornerInterface<T> helper = thread.helper;
			helper.setImage(image, offsets);

			for (int y = y0; y < y1; y++) {
				int indexIntensity = intensity.startIndex + y*intensity.stride + radius;
				int index = image.startIndex + y*image.stride + radius;
				for (int x = radius; x < image.width - radius; x++, index++, indexIntensity++) {

					int result = helper.checkPixel(index);

					if (result < 0) {
						intensity.data[indexIntensity] = helper.scoreLower(index);
						candidatesLow.add(x, y);
					} else if (result > 0) {
						intensity.data[indexIntensity] = helper.scoreUpper(index);
						candidatesHigh.add(x, y);
					} else {
						intensity.data[indexIntensity] = 0;
					}
				}
			}
		});

		candidatesLow.configure(image.width, image.height);
		candidatesHigh.configure(image.width, image.height);

		for (int i = 0; i < threadWorkspace.size(); i++) {
			ThreadHelper thread = threadWorkspace.get(i);
			candidatesLow.getPoints().addAll(thread.candidatesLow.getPoints());
			candidatesHigh.getPoints().addAll(thread.candidatesHigh.getPoints());
		}
	}

	/**
	 * Computes fast corner features
	 */
	@Override public void process( T image ) {
		this.image = image;

		if (stride != image.stride) {
			stride = image.stride;
			offsets = DiscretizedCircle.imageOffsets(radius, image.stride);
		}

		BoofConcurrency.loopBlocks(radius, image.height - radius, threadWorkspace, ( thread, y0, y1 ) -> {
			thread.reset(image.width, image.height);
			final ListIntPoint2D candidatesLow = thread.candidatesLow;
			final ListIntPoint2D candidatesHigh = thread.candidatesHigh;
			final FastCornerInterface<T> helper = thread.helper;
			helper.setImage(image, offsets);

			for (int y = y0; y < y1; y++) {
				int index = image.startIndex + y*image.stride + radius;
				for (int x = radius; x < image.width - radius; x++, index++) {

					int result = helper.checkPixel(index);

					if (result < 0) {
						candidatesLow.add(x, y);
					} else if (result > 0) {
						candidatesHigh.add(x, y);
					}
				}
			}
		});

		candidatesLow.configure(image.width, image.height);
		candidatesHigh.configure(image.width, image.height);

		for (int i = 0; i < threadWorkspace.size(); i++) {
			ThreadHelper thread = threadWorkspace.get(i);
			candidatesLow.getPoints().addAll(thread.candidatesLow.getPoints());
			candidatesHigh.getPoints().addAll(thread.candidatesHigh.getPoints());
		}
	}

	class ThreadHelper {
		final FastCornerInterface<T> helper;
		final ListIntPoint2D candidatesLow = new ListIntPoint2D();
		final ListIntPoint2D candidatesHigh = new ListIntPoint2D();

		public ThreadHelper( FastCornerInterface<T> helper ) {
			this.helper = helper;
		}

		public void reset( int width, int height ) {
			candidatesLow.configure(width, height);
			candidatesHigh.configure(width, height);
		}
	}
}
