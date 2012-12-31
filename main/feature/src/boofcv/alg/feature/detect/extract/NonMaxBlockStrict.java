/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageFloat32;

/**
 * <p>
 * Implementation of {@link NonMaxBlock} which implements a strict maximum rule.
 * </p>
 *
 * @author Peter Abeles
 */
public class NonMaxBlockStrict extends NonMaxBlock {

	public NonMaxBlockStrict() {
	}

	public NonMaxBlockStrict(int radius, float threshold, int border) {
		super(radius, threshold, border);
	}

	@Override
	protected void searchBlock(int x0, int y0, int x1, int y1, ImageFloat32 img) {

		int peakX = 0;
		int peakY = 0;

		float peakVal = -Float.MAX_VALUE;

		for (int y = y0; y < y1; y++) {
			int index = img.startIndex + y * img.stride + x0;
			for (int x = x0; x < x1; x++) {
				float v = img.data[index++];

				if (v > peakVal) {
					peakVal = v;
					peakX = x;
					peakY = y;
				}
			}
		}

		if (peakVal >= threshold && peakVal != Float.MAX_VALUE) {
			checkLocalMax(peakX, peakY, peakVal, img);
		}
	}

	protected void checkLocalMax(int x_c, int y_c, float peakVal, ImageFloat32 img) {
		int x0 = x_c - radius;
		int x1 = x_c + radius;
		int y0 = y_c - radius;
		int y1 = y_c + radius;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 >= img.width) x1 = img.width - 1;
		if (y1 >= img.height) y1 = img.height - 1;

		for (int y = y0; y <= y1; y++) {
			int index = img.startIndex + y * img.stride + x0;
			for (int x = x0; x <= x1; x++) {
				float v = img.data[index++];

				if (v >= peakVal && !(x == x_c && y == y_c)) {
					// not a local max
					return;
				}
			}
		}

		// save location of local max
		peaks.add(x_c, y_c);
	}
}
