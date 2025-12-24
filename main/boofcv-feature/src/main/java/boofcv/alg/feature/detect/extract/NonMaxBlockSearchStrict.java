/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.extract.NonMaxSuppression.Add;
import boofcv.alg.feature.detect.extract.NonMaxBlock.Search;
import boofcv.struct.image.GrayF32;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/// Implementation of [NonMaxBlock] which implements a strict maximum rule.
@SuppressWarnings({"NullAway.Init"})
public abstract class NonMaxBlockSearchStrict<Storage> implements Search<Storage> {

	// threshold for intensity values when detecting minimums and maximums
	float thresholdMin;
	float thresholdMax;
	int radius;

	private Storage localMin, localMax;
	GrayF32 img;

	Add<Storage> opAdd = ( a, b, c ) -> {throw new RuntimeException("Must specified storage access");};

	@Override public void storageAccess( Add<Storage> opAdd ) {
		this.opAdd = opAdd;
	}

	@SuppressWarnings({"NullAway"})
	@Override
	public void initialize( NonMaxBlock.Configuration configuration, GrayF32 image,
							@Nullable Storage localMin,
							@Nullable Storage localMax ) {

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

	public static class Max<Storage> extends NonMaxBlockSearchStrict<Storage> {

		public Max( Add<Storage> opAdd ) {
			storageAccess(opAdd);
		}

		public Max() {}

		@Override public void searchBlock( int x0, int y0, int x1, int y1 ) {
			int peakX = 0;
			int peakY = 0;

			// Only consider candidates which are acceptable
			float peakVal = Math.nextDown(thresholdMax);

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

		@Override public boolean isDetectMinimums() {
			return false;
		}

		@Override public boolean isDetectMaximums() {
			return true;
		}

		@Override public <S> Search<S> newInstance( Add<S> opAdd ) {
			return new Max<>(opAdd);
		}
	}

	public static class Min<Storage> extends NonMaxBlockSearchStrict<Storage> {
		public Min( Add<Storage> opAdd ) {
			storageAccess(opAdd);
		}

		public Min() {}

		@Override public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int peakX = 0;
			int peakY = 0;

			// Only consider candidates which are acceptable
			float peakVal = Math.nextUp(thresholdMin);

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

		@Override public boolean isDetectMinimums() {return true;}

		@Override public boolean isDetectMaximums() {return false;}

		@Override public <S> Search<S> newInstance( Add<S> opAdd ) {return new Min<>(opAdd);}
	}

	public static class MinMax<Storage> extends NonMaxBlockSearchStrict<Storage> {
		public MinMax( Add<Storage> opAdd ) {
			storageAccess(opAdd);
		}

		public MinMax() {}

		@Override public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int maxX = 0;
			int maxY = 0;
			int minX = 0;
			int minY = 0;

			// Only consider candidates which are acceptable
			float maxVal = Math.nextDown(thresholdMax);
			float minVal = Math.nextUp(thresholdMin);

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

		@Override public boolean isDetectMinimums() {return true;}

		@Override public boolean isDetectMaximums() {return true;}

		@Override public <S> Search<S> newInstance( Add<S> opAdd ) {return new MinMax<>(opAdd);}
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
		opAdd.add(localMax, x_c, y_c);
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
		opAdd.add(localMin, x_c, y_c);
	}
}
