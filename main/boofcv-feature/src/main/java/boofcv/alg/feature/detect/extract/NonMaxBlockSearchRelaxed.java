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
import georegression.struct.point.Point2D_I32;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * <p>
 * Implementation of {@link NonMaxBlock} which implements a relaxed maximum rule.
 * </p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public abstract class NonMaxBlockSearchRelaxed implements NonMaxBlock.Search {

	// storage for local maximums
	Point2D_I32[] foundMax;
	Point2D_I32[] foundMin;

	// threshold for intensity values when detecting minimums and maximums
	float thresholdMin;
	float thresholdMax;
	int radius;

	private QueueCorner localMin, localMax;
	GrayF32 img;

	@SuppressWarnings({"NullAway"})
	@Override
	public void initialize( NonMaxBlock.Configuration configuration, GrayF32 image,
							@Nullable QueueCorner localMin, @Nullable QueueCorner localMax ) {
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

		int w = 2*radius + 1;

		// only declare this work space if needed
		if (foundMax == null || foundMax.length != w*w) {
			foundMax = new Point2D_I32[w*w];
			for (int i = 0; i < foundMax.length; i++)
				foundMax[i] = new Point2D_I32();
			foundMin = new Point2D_I32[w*w];
			for (int i = 0; i < foundMin.length; i++)
				foundMin[i] = new Point2D_I32();
		}
	}

	public static class Max extends NonMaxBlockSearchRelaxed {
		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int numPeaks = 0;
			float peakVal = thresholdMax;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v > peakVal) {
						peakVal = v;
						foundMax[0].setTo(x, y);
						numPeaks = 1;
					} else if (v == peakVal) {
						foundMax[numPeaks++].setTo(x, y);
					}
				}
			}

			if (numPeaks > 0 && peakVal != Float.MAX_VALUE) {
				for (int i = 0; i < numPeaks; i++) {
					Point2D_I32 p = foundMax[i];
					checkLocalMax(p.x, p.y, peakVal, img);
				}
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

	public static class Min extends NonMaxBlockSearchRelaxed {

		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int numPeaks = 0;
			float peakVal = thresholdMin;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v < peakVal) {
						peakVal = v;
						foundMin[0].setTo(x, y);
						numPeaks = 1;
					} else if (v == peakVal) {
						foundMin[numPeaks++].setTo(x, y);
					}
				}
			}

			if (numPeaks > 0 && peakVal != -Float.MAX_VALUE) {
				for (int i = 0; i < numPeaks; i++) {
					Point2D_I32 p = foundMin[i];
					checkLocalMin(p.x, p.y, peakVal, img);
				}
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

	public static class MinMax extends NonMaxBlockSearchRelaxed {

		@Override
		public void searchBlock( int x0, int y0, int x1, int y1 ) {

			int numMinPeaks = 0;
			float peakMinVal = thresholdMin;
			int numMaxPeaks = 0;
			float peakMaxVal = thresholdMax;

			for (int y = y0; y < y1; y++) {
				int index = img.startIndex + y*img.stride + x0;
				for (int x = x0; x < x1; x++) {
					float v = img.data[index++];

					if (v < peakMinVal) {
						peakMinVal = v;
						foundMin[0].setTo(x, y);
						numMinPeaks = 1;
					} else if (v == peakMinVal) {
						foundMin[numMinPeaks++].setTo(x, y);
					}

					if (v > peakMaxVal) {
						peakMaxVal = v;
						foundMax[0].setTo(x, y);
						numMaxPeaks = 1;
					} else if (v == peakMaxVal) {
						foundMax[numMaxPeaks++].setTo(x, y);
					}
				}
			}

			if (numMinPeaks > 0 && peakMinVal != -Float.MAX_VALUE) {
				for (int i = 0; i < numMinPeaks; i++) {
					Point2D_I32 p = foundMin[i];
					checkLocalMin(p.x, p.y, peakMinVal, img);
				}
			}

			if (numMaxPeaks > 0 && peakMaxVal != Float.MAX_VALUE) {
				for (int i = 0; i < numMaxPeaks; i++) {
					Point2D_I32 p = foundMax[i];
					checkLocalMax(p.x, p.y, peakMaxVal, img);
				}
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

				if (v > peakVal) {
					// not a local maximum
					return;
				}
			}
		}

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

				if (v < peakVal) {
					// not a local minimum
					return;
				}
			}
		}

		localMin.append(x_c, y_c);
	}
}
