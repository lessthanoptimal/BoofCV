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

package boofcv.alg.disparity.block;

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.image.*;

/**
 * Computes the block disparity score using a {@link boofcv.alg.transform.census.CensusTransform}.
 *
 * @author Peter Abeles
 */
public interface BlockRowScoreCensus {
	abstract class CensusArrayS32_B32<T extends GrayI<T>, ImageData> extends BlockRowScore.ArrayS32_BS32<T, ImageData> {
		CensusArrayS32_B32( int maxPerPixel ) { super(maxPerPixel); }

		@Override
		public boolean isRequireNormalize() {
			return false;
		}
	}

	class U8 extends CensusArrayS32_B32<GrayU8, byte[]> {
		public U8( int maxPerPixel ) { super(maxPerPixel); }

		@Override
		public void score( byte[] leftRow, byte[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore ) {
			for (int i = 0; i < length; i++) {
				final int a = leftRow[indexLeft++] & 0xFF;
				final int b = rightRow[indexRight++] & 0xFF;
				elementScore[offset + i] = DescriptorDistance.hamming(a ^ b);
			}
		}

		@Override
		public ImageType<GrayU8> getImageType() {
			return ImageType.SB_U8;
		}
	}

	class S32 extends CensusArrayS32_B32<GrayS32, int[]> {
		public S32( int maxPerPixel ) { super(maxPerPixel); }

		@Override
		public void score( int[] leftRow, int[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore ) {
			for (int i = 0; i < length; i++) {
				final int a = leftRow[indexLeft++];
				final int b = rightRow[indexRight++];
				elementScore[offset + i] = DescriptorDistance.hamming(a ^ b);
			}
		}

		@Override
		public ImageType<GrayS32> getImageType() {
			return ImageType.single(GrayS32.class);
		}
	}

	class S64 extends BlockRowScore.ArrayS32_BS64 {
		public S64( int maxPerPixel ) { super(maxPerPixel); }

		@Override
		public void score( long[] leftRow, long[] rightRow, int indexLeft, int indexRight, int offset, int length, int[] elementScore ) {
			for (int i = 0; i < length; i++) {
				final long a = leftRow[indexLeft++];
				final long b = rightRow[indexRight++];
				elementScore[offset + i] = DescriptorDistance.hamming(a ^ b);
			}
		}

		@Override
		public boolean isRequireNormalize() {
			return false;
		}

		@Override
		public ImageType<GrayS64> getImageType() {
			return ImageType.single(GrayS64.class);
		}
	}
}
