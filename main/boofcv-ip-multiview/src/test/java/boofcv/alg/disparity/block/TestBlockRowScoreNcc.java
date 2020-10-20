/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.disparity.block;

import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestBlockRowScoreNcc extends BoofStandardJUnit {

	@Nested
	class F32 extends ChecksBlockRowScore<GrayF32, float[], float[]> {
		float eps = 1e-3f;

		F32() {
			super(0, ImageType.single(GrayF32.class));
			this.maxPixelValue = 1;
			this.minPixelValue = -1;
		}

		@Override
		public BlockRowScore<GrayF32, float[], float[]> createAlg( int radiusWidth, int radiusHeight ) {
			BlockRowScoreNcc.F32 alg = new BlockRowScoreNcc.F32(radiusWidth, radiusHeight);
			alg.eps = eps;
			return alg;
		}

		@Override
		public float[] createArray( int length ) {
			return new float[length];
		}

		@Override
		public double naiveScoreRow( int cx, int cy, int disparity, int radius ) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = ((ImageBorder_F32)bleft).get(cx + x, cy);
				double vb = ((ImageBorder_F32)bright).get(cx + x - disparity, cy);
				total += va*vb;
			}
			return total;
		}

		@Override
		public double naiveScoreRegion( int cx, int cy, int disparity, int radius ) {
			return ncc((ImageBorder_F32)bleft, (ImageBorder_F32)bright,
					cx, cy, disparity, radius, radius, eps);
		}

		@Override
		public double get( int index, float[] array ) {
			return array[index];
		}
	}

	public static double ncc( ImageBorder_F32 bleft, ImageBorder_F32 bright,
							  int cx, int cy, int disparity, int radiusX, int radiusY, float eps ) {
		float meanLeft = meanR(bleft, cx, cy, radiusX, radiusY);
		float stdLeft = stdevR(bleft, cx, cy, radiusX, radiusY, meanLeft);
		float meanRight = meanR(bright, cx - disparity, cy, radiusX, radiusY);
		float stdRight = stdevR(bright, cx - disparity, cy, radiusX, radiusY, meanRight);

		float total = 0;
		for (int y = -radiusY; y <= radiusY; y++) {
			float sumRow = 0;
			for (int x = -radiusX; x <= radiusX; x++) {
				float va = bleft.get(cx + x, cy + y);
				float vb = bright.get(cx + x - disparity, cy + y);
				sumRow += va*vb;
			}
			total += sumRow/(2*radiusX + 1);
		}
		total /= (2*radiusY + 1);

		return (total - meanLeft*meanRight)/(eps + stdLeft*stdRight);
	}

	public static float meanR( ImageBorder_F32 img, int cx, int cy, int radiusX, int radiusY ) {
		int x0 = cx - radiusX;
		int x1 = cx + radiusX + 1;
		int y0 = cy - radiusY;
		int y1 = cy + radiusY + 1;
		return mean(img, x0, y0, x1, y1);
	}

	public static float stdevR( ImageBorder_F32 img, int cx, int cy, int radiusX, int radiusY, float mean ) {
		int x0 = cx - radiusX;
		int x1 = cx + radiusX + 1;
		int y0 = cy - radiusY;
		int y1 = cy + radiusY + 1;
		return stdev(img, x0, y0, x1, y1, mean);
	}

	public static float mean( ImageBorder_F32 img, int x0, int y0, int x1, int y1 ) {
		float total = 0;
		for (int y = y0; y < y1; y++) {
			float rowTotal = 0.0f;
			for (int x = x0; x < x1; x++) {
				rowTotal += img.get(x, y);
			}
			total += rowTotal/(x1 - x0);
		}
		return total/(y1 - y0);
	}

	public static float stdev( ImageBorder_F32 img, int x0, int y0, int x1, int y1, float mean ) {
		float total = 0;
		for (int y = y0; y < y1; y++) {
			float rowTotal = 0.0f;
			for (int x = x0; x < x1; x++) {
				float delta = img.get(x, y) - mean;
				rowTotal += delta*delta;
			}
			total += rowTotal/(x1 - x0);
		}
		return (float)Math.sqrt(total/(y1 - y0));
	}
}
