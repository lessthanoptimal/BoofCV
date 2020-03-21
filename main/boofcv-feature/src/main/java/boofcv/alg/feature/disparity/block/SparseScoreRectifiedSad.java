/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.block.score.DisparitySparseRectifiedScoreBM_F32;
import boofcv.alg.feature.disparity.block.score.DisparitySparseRectifiedScoreBM_S32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;

/**
 * Computes sparse SAD scores from rectified input images
 *
 * @author Peter Abeles
 */
public interface SparseScoreRectifiedSad {
	class F32 extends DisparitySparseRectifiedScoreBM_F32 {
		public F32(int radiusX, int radiusY) {
			super(radiusX, radiusY);
			setSampleRegion(0,0);
		}

		@Override
		protected void scoreDisparity(final int disparityRange) {
			final float[] dataLeft = patchLeft.data;
			final float[] dataRight = patchRight.data;
			for (int d = 0; d < disparityRange; d++) {
				float total = 0;
				int idxLeft = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxRight = y*patchRight.stride+d;
					for (int x = 0; x < blockWidth; x++) {
						total += Math.abs(dataLeft[idxLeft++] - dataRight[idxRight++]);
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}

	class U8 extends DisparitySparseRectifiedScoreBM_S32<GrayU8> {
		public U8(int radiusX, int radiusY) {
			super(radiusX, radiusY, GrayU8.class);
			setSampleRegion(0,0);
		}

		@Override
		protected void scoreDisparity(final int disparityRange) {
			final byte[] dataLeft = patchLeft.data;
			final byte[] dataRight = patchRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				int idxLeft = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxRight = y*patchRight.stride+d;
					for (int x = 0; x < blockWidth; x++) {
						total += Math.abs( (dataLeft[idxLeft++]&0xFF) - (dataRight[idxRight++]&0xFF) );
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}

	class U16 extends DisparitySparseRectifiedScoreBM_S32<GrayU16> {
		public U16(int radiusX, int radiusY) {
			super(radiusX, radiusY, GrayU16.class);
			setSampleRegion(0,0);
		}

		@Override
		protected void scoreDisparity(final int disparityRange) {
			final short[] dataLeft = patchLeft.data;
			final short[] dataRight = patchRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				int idxLeft = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxRight = y*patchRight.stride+d;
					for (int x = 0; x < blockWidth; x++) {
						total += Math.abs( (dataLeft[idxLeft++]&0xFFFF) - (dataRight[idxRight++]&0xFFFF) );
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}

	class S16 extends DisparitySparseRectifiedScoreBM_S32<GrayS16> {
		public S16(int radiusX, int radiusY) {
			super(radiusX, radiusY, GrayS16.class);
			setSampleRegion(0,0);
		}

		@Override
		protected void scoreDisparity(final int disparityRange) {
			final short[] dataLeft = patchLeft.data;
			final short[] dataRight = patchRight.data;
			for (int d = 0; d < disparityRange; d++) {
				int total = 0;
				int idxLeft = 0;
				for (int y = 0; y < blockHeight; y++) {
					int idxRight = y*patchRight.stride+d;
					for (int x = 0; x < blockWidth; x++) {
						total += Math.abs( (dataLeft[idxLeft++]&0xFFFF) - (dataRight[idxRight++]&0xFFFF) );
					}
				}
				scores[disparityRange-d-1] = total;
			}
		}
	}
}
