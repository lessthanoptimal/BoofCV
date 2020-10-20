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
import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestBlockRowScoreSad extends BoofStandardJUnit {
	@Nested
	class U8 extends ChecksBlockRowScore.ArrayIntI<GrayU8, byte[]> {
		U8() {super(255, ImageType.single(GrayU8.class));}

		@Override
		public BlockRowScore<GrayU8, int[], byte[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreSad.U8();
		}

		@Override
		protected int computeError( int a, int b ) {
			return Math.abs(a - b);
		}
	}

	@Nested
	class U16 extends ChecksBlockRowScore.ArrayIntI<GrayU16, short[]> {
		U16() {super(5000, ImageType.single(GrayU16.class));}

		@Override
		public BlockRowScore<GrayU16, int[], short[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreSad.U16();
		}

		@Override
		protected int computeError( int a, int b ) {
			return Math.abs(a - b);
		}
	}

	@Nested
	class S16 extends ChecksBlockRowScore.ArrayIntI<GrayS16, short[]> {
		S16() {super(5000, ImageType.single(GrayS16.class));}

		@Override
		public BlockRowScore<GrayS16, int[], short[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreSad.S16();
		}

		@Override
		protected int computeError( int a, int b ) {
			return Math.abs(a - b);
		}
	}

	@Nested
	class F32 extends ChecksBlockRowScore<GrayF32, float[], float[]> {
		F32() {super(1000, ImageType.single(GrayF32.class));}

		@Override
		public BlockRowScore<GrayF32, float[], float[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreSad.F32();
		}

		@Override
		public float[] createArray( int length ) {return new float[length];}

		@Override
		public double naiveScoreRow( int cx, int cy, int disparity, int radius ) {
			int x0 = cx - radius;
			int x1 = cx + radius + 1;

			float total = 0;
			for (int x = x0; x < x1; x++) {
				float va = ((ImageBorder_F32)bleft).get(x, cy);
				float vb = ((ImageBorder_F32)bright).get(x - disparity, cy);
				total += Math.abs(va - vb);
			}
			return total;
		}

		@Override
		public double naiveScoreRegion( int cx, int cy, int disparity, int radius ) {
			int y0 = cy - radius;
			int y1 = cy + radius + 1;

			float total = 0;
			for (int y = y0; y < y1; y++) {
				total += (float)naiveScoreRow(cx, y, disparity, radius);
			}
			return total;
		}

		@Override
		public double get( int index, float[] array ) {return array[index];}
	}
}
