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

import java.util.Objects;

/**
 * <p>
 * Implementation of {@link NonMaxBlock} which implements a strict maximum rule.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class NonMaxBlockSearchStrict implements NonMaxBlock.Search {

	// threshold for intensity values when detecting minimums and maximums
	float thresholdMin;
	float thresholdMax;
	int radius;

	private QueueCorner localMin, localMax;
	GrayF32 img;

	@SuppressWarnings({"NullAway"})
	@Override
	public void initialize( NonMaxBlock.Configuration configuration,
							GrayF32 image, @Nullable QueueCorner localMin, @Nullable QueueCorner localMax ) {

		this.thresholdMin = configuration.thresholdMin;
		this.thresholdMax = configuration.thresholdMax;
		this.radius = configuration.radius;
		this.img = image;

		// we want these to be null so that it blows up if a local min/max is searched when it shouldn't
		// but enforcing nullability is tedious, so we turn it off for this function
		this.localMin = localMin;
		this.localMax = localMax;

		// Sanity check contract
		if (isDetectMinimums())
			Objects.requireNonNull(localMin);
		if (isDetectMaximums())
			Objects.requireNonNull(localMax);
	}

	public static class Max extends NonMaxBlockSearchStrict {
		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int peakX = 0;
			int peakY = 0;

			float peakVal = -Float.MAX_VALUE;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v > peakVal) {
						peakVal = v;
						peakX = x;
						peakY = y;
					}
				}
			}

			if (peakVal >= thresholdMax && peakVal != Float.MAX_VALUE) {
				checkLocalMax(peakX, peakY, peakVal, img);
			}
		}

		@Override
		public boolean isDetectMinimums() {
			return false;
		}

		@Override
		public boolean isDetectMaximums() {
			return true;
		}

		@Override
		public NonMaxBlock.Search newInstance() {
			return new Max();
		}
	}

	public static class Min extends NonMaxBlockSearchStrict {

		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int peakX = 0;
			int peakY = 0;

			float peakVal = Float.MAX_VALUE;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v < peakVal) {
						peakVal = v;
						peakX = x;
						peakY = y;
					}
				}
			}

			if (peakVal <= thresholdMin && peakVal != -Float.MAX_VALUE) {
				checkLocalMin(peakX, peakY, peakVal, img);
			}
		}

		@Override
		public boolean isDetectMinimums() {
			return true;
		}

		@Override
		public boolean isDetectMaximums() {
			return false;
		}

		@Override
		public NonMaxBlock.Search newInstance() {
			return new Min();
		}
	}

	public static class MinMax extends NonMaxBlockSearchStrict {

		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int maxX = 0;
			int maxY = 0;
			int minX = 0;
			int minY = 0;

			float maxVal = -Float.MAX_VALUE;
			float minVal = Float.MAX_VALUE;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v > maxVal) {
						maxVal = v;
						maxX = x;
						maxY = y;
					}
					if (v < minVal) {
						minVal = v;
						minX = x;
						minY = y;
					}
				}
			}

			if (maxVal >= thresholdMax && maxVal != Float.MAX_VALUE) {
				checkLocalMax(maxX, maxY, maxVal, img);
			}
			if (minVal <= thresholdMin && minVal != -Float.MAX_VALUE) {
				checkLocalMin(minX, minY, minVal, img);
			}
		}

		@Override
		public boolean isDetectMinimums() {
			return true;
		}

		@Override
		public boolean isDetectMaximums() {
			return true;
		}

		@Override
		public NonMaxBlock.Search newInstance() {
			return new MinMax();
		}
	}

	protected void checkLocalMax( int x_c, int y_c, float peakVal, GrayF32 img ) {
		int x0 = x_c - radius;
		int x1 = x_c + radius;
		int y0 = y_c - radius;
		int y1 = y_c + radius;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 >= img.width) x1 = img.width - 1;
		if (y1 >= img.height) y1 = img.height - 1;

		for (int y = y0; y <= y1; y++) {
			int index = img.startIndex + y*img.stride + x0;
			for (int x = x0; x <= x1; x++) {
				float v = img.data[index++];

				if (v >= peakVal && !(x == x_c && y == y_c)) {
					// not a local max
					return;
				}
			}
		}

		// save location of local max
		localMax.append(x_c, y_c);
	}

	protected void checkLocalMin( int x_c, int y_c, float peakVal, GrayF32 img ) {
		int x0 = x_c - radius;
		int x1 = x_c + radius;
		int y0 = y_c - radius;
		int y1 = y_c + radius;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 >= img.width) x1 = img.width - 1;
		if (y1 >= img.height) y1 = img.height - 1;

		for (int y = y0; y <= y1; y++) {
			int index = img.startIndex + y*img.stride + x0;
			for (int x = x0; x <= x1; x++) {
				float v = img.data[index++];

				if (v <= peakVal && !(x == x_c && y == y_c)) {
					// not a local min
					return;
				}
			}
		}

		// save location of local min
		localMin.append(x_c, y_c);
	}
}
