/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestAdaptiveMeanFilter extends BoofStandardJUnit {
	ImageType[] types = new ImageType[]{ImageType.SB_U8, ImageType.SB_U16, ImageType.SB_F32, ImageType.SB_F64};
	double noiseVariance = 2.5;
	int width = 30;
	int height = 20;
	int radiusX = 3;
	int radiusY = 2;

	@Test void compareToNaive() {
		var alg = new AdaptiveMeanFilter();
		alg.radiusX = radiusX;
		alg.radiusY = radiusY;
		alg.noiseVariance = noiseVariance;

		for (var type : types) {
			var src = (ImageGray)type.createImage(width, height);
			var dst = (ImageGray)type.createImage(1, 1);

			GImageMiscOps.fillUniform(src, rand, 0, 100);

			alg.process(src, dst);

			assertEquals(width, dst.width);
			assertEquals(height, dst.height);

			for (int y = 0; y < dst.height; y++) {
				for (int x = 0; x < dst.width; x++) {
					double found = GeneralizedImageOps.get(dst, x, y);
					assertEquals(naiveFilter(src, x, y, radiusX, radiusY), found, 1.0);
				}
			}
		}
	}

	/**
	 * See how it handles the no variance case
	 */
	@Test void computeFilter_NoVariance() {
		assertEquals(15, AdaptiveMeanFilter.computeFilter(1.5, 15, new int[]{3,3,3}, 3));
		assertEquals(15f, AdaptiveMeanFilter.computeFilter(1.5f, 15f, new float[]{3,3,3}, 3));
		assertEquals(15.0, AdaptiveMeanFilter.computeFilter(1.5, 15, new double[]{3,3,3}, 3));
	}

	private double naiveFilter( ImageGray<?> src, int cx, int cy, int radiusX, int radiusY ) {
		int x0 = Math.max(0, cx - radiusX);
		int x1 = Math.min(src.width, cx + radiusX + 1);
		int y0 = Math.max(0, cy - radiusY);
		int y1 = Math.min(src.height, cy + radiusY + 1);

		int N = (x1 - x0)*(y1 - y0);

		double localMean = 0.0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				localMean += GeneralizedImageOps.get(src, x, y);
			}
		}
		localMean /= N;

		double localVariance = 0.0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				double diff = GeneralizedImageOps.get(src, x, y) - localMean;
				localVariance += diff*diff;
			}
		}
		localVariance /= N;

		double centerValue = GeneralizedImageOps.get(src, cx, cy);

		if (localVariance == 0.0)
			return centerValue;

		return centerValue - Math.min(1.0, noiseVariance/localVariance)*(centerValue - localMean);
	}
}