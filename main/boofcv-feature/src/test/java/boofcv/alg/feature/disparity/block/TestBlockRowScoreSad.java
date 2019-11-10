/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.disparity.block;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("InnerClassMayBeStatic")
class TestBlockRowScoreSad {
	@Nested
	class U8 extends ChecksBlockRowScore<GrayU8,int[]> {

		U8() {
			super(255, ImageType.single(GrayU8.class));
		}

		@Override
		public BlockRowScore<GrayU8, int[]> createAlg(int radiusWidth, int radiusHeight) {
			return new BlockRowScoreSad.U8();
		}

		@Override
		public int[] createArray(int length) {
			return new int[length];
		}

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = left.get(cx+x,cy);
				double vb = right.get(cx+x-disparity,cy);

				total += Math.abs(va-vb);
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int y = -radius; y <= radius; y++) {
				for (int x = -radius; x <= radius; x++) {
					double va = left.get(cx + x, cy + y);
					double vb = right.get(cx + x - disparity, cy + y);

					total += Math.abs(va - vb);
				}
			}
			return total;
		}

		@Override
		public double get(int index, int[] array) {
			return array[index];
		}
	}

	@Nested
	class F32 extends ChecksBlockRowScore<GrayF32,float[]> {

		F32() {
			super(1000, ImageType.single(GrayF32.class));
		}

		@Override
		public BlockRowScore<GrayF32, float[]> createAlg(int radiusWidth, int radiusHeight) {
			return new BlockRowScoreSad.F32();
		}

		@Override
		public float[] createArray(int length) {
			return new float[length];
		}

		@Override
		public double naiveScoreRow(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int x = -radius; x <= radius; x++) {
				double va = left.get(cx+x,cy);
				double vb = right.get(cx+x-disparity,cy);

				total += Math.abs(va-vb);
			}
			return total;
		}

		@Override
		public double naiveScoreRegion(int cx, int cy, int disparity, int radius) {
			double total = 0;
			for (int y = -radius; y <= radius; y++) {
				for (int x = -radius; x <= radius; x++) {
					double va = left.get(cx + x, cy + y);
					double vb = right.get(cx + x - disparity, cy + y);

					total += Math.abs(va - vb);
				}
			}
			return total;
		}

		@Override
		public double get(int index, float[] array) {
			return array[index];
		}
	}
}