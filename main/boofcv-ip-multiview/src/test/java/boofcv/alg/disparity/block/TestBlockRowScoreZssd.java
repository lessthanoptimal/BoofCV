/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBlockRowScoreZssd extends BoofStandardJUnit {

	@Nested
	class F32 extends ChecksBlockRowScore<GrayF32, float[], float[]> {
		F32() {super(100, ImageType.single(GrayF32.class));}

		@Override
		public BlockRowScore<GrayF32, float[], float[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreZssd.F32(radiusWidth, radiusHeight);
		}

		@Override
		public float[] createArray( int length ) {return new float[length];}

		/// Before normalization the score is plain SSD
		@Override
		public double naiveScoreRow( int cx, int cy, int disparity, int radius ) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = ((ImageBorder_F32)bleft).get(cx + x, cy);
				double vb = ((ImageBorder_F32)bright).get(cx + x - disparity, cy);
				total += (va - vb)*(va - vb);
			}
			return total;
		}

		/// Computed directly from the definition, so this checks the SSD - N*(meanL - meanR)^2 identity
		/// that the implementation relies on
		@Override
		public double naiveScoreRegion( int cx, int cy, int disparity, int radius ) {
			return zssd((ImageBorder_F32)bleft, (ImageBorder_F32)bright, cx, cy, disparity, radius, radius);
		}

		/// Removing the mean is what separates ZSSD from SSD, so adding a constant to one image must leave
		/// the score alone. Checked on the row score itself rather than through a disparity algorithm.
		@Test void invariantToIntensityOffset() {
			int minDisparity = 0, maxDisparity = 5, radius = 2, row = 4;

			GImageMiscOps.fillUniform(left, rand, 0, maxPixelValue);
			GImageMiscOps.fillUniform(right, rand, 0, maxPixelValue);

			float[] before = computeRegionScores(minDisparity, maxDisparity, radius, row);

			// Same images, except every pixel in the right one is brighter by a fixed amount
			PixelMath.plus(right, 40.0f, right);

			float[] after = computeRegionScores(minDisparity, maxDisparity, radius, row);

			for (int d = minDisparity; d < maxDisparity; d++) {
				int colLo = Math.max(0, d);
				int colEnd = width + Math.min(0, d);
				int idx = width*(d - minDisparity) + colLo;
				for (int x = colLo; x < colEnd; x++, idx++) {
					double expected = before[idx];
					assertEquals(expected, after[idx], Math.max(1, Math.abs(expected))*tol,
							"x = " + x + " d = " + d);
				}
			}
		}

		@Override
		public double get( int index, float[] array ) {return array[index];}
	}

	/// Zero-mean SSD computed straight from the definition
	public static double zssd( ImageBorder_F32 bleft, ImageBorder_F32 bright,
							   int cx, int cy, int disparity, int radiusX, int radiusY ) {
		double meanLeft = TestBlockRowScoreNcc.meanR(bleft, cx, cy, radiusX, radiusY);
		double meanRight = TestBlockRowScoreNcc.meanR(bright, cx - disparity, cy, radiusX, radiusY);

		double total = 0;
		for (int y = -radiusY; y <= radiusY; y++) {
			for (int x = -radiusX; x <= radiusX; x++) {
				double va = bleft.get(cx + x, cy + y) - meanLeft;
				double vb = bright.get(cx + x - disparity, cy + y) - meanRight;
				total += (va - vb)*(va - vb);
			}
		}
		return total;
	}
}
