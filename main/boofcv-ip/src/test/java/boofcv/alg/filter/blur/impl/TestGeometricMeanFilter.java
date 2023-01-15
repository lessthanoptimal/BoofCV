/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.blur.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestGeometricMeanFilter extends BoofStandardJUnit {
	int width = 30;
	int height = 20;
	int radiusX = 3;
	int radiusY = 2;

	/**
	 * Compares to a simple implementation that's easily checked through visual inspection
	 */
	@Test void compareToNaive() {
		for (var type : new ImageType[]{ImageType.SB_U8, ImageType.SB_U16, ImageType.SB_F32, ImageType.SB_F64}) {
			var src = (ImageGray)type.createImage(width, height);
			var dst = (ImageGray)type.createImage(1, 1);

			GImageMiscOps.fillUniform(src, rand, 0, 100);

			double mean = GImageStatistics.mean(src);
			GeometricMeanFilter.filter(src, radiusX, radiusY, mean, dst);

			assertEquals(width, dst.width);
			assertEquals(height, dst.height);

			for (int y = 0; y < dst.height; y++) {
				for (int x = 0; x < dst.width; x++) {
					double found = GeneralizedImageOps.get(dst, x, y);
					assertEquals(naiveMean(src, x, y, radiusX, radiusY), found, 1.0);
				}
			}
		}
	}

	private double naiveMean( ImageGray<?> src, int cx, int cy, int radiusX, int radiusY ) {
		int x0 = Math.max(0, cx - radiusX);
		int x1 = Math.min(src.width, cx + radiusX + 1);
		int y0 = Math.max(0, cy - radiusY);
		int y1 = Math.min(src.height, cy + radiusY + 1);

		double product = 1.0;
		for (int y = y0; y < y1; y++) {
			for (int x = x0; x < x1; x++) {
				product *= GeneralizedImageOps.get(src, x, y);
			}
		}

		double rootOf = (y1 - y0)*(x1 - x0);
		return Math.pow(product, 1.0/rootOf);
	}
}
