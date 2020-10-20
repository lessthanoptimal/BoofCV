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

import boofcv.alg.descriptor.DescriptorDistance;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayS64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestBlockRowScoreCensus extends BoofStandardJUnit {
	@Nested
	class U8 extends ChecksBlockRowScore.ArrayIntI<GrayU8, byte[]> {
		U8() {super(255, ImageType.single(GrayU8.class));}

		@Override
		public BlockRowScore<GrayU8, int[], byte[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreCensus.U8(-1);
		}

		@Override
		protected int computeError( int a, int b ) {
			return DescriptorDistance.hamming(a ^ b);
		}
	}

	@Nested
	class S32 extends ChecksBlockRowScore.ArrayIntI<GrayS32, int[]> {
		S32() {super(Integer.MAX_VALUE - 1000, ImageType.single(GrayS32.class));}

		@Override
		public BlockRowScore<GrayS32, int[], int[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreCensus.S32(-1);
		}

		@Override
		protected int computeError( int a, int b ) {
			return DescriptorDistance.hamming(a ^ b);
		}
	}

	@Nested
	class S64 extends ChecksBlockRowScore.ArrayIntL {
		S64() {super(Integer.MAX_VALUE - 1000);}

		@Override
		public BlockRowScore<GrayS64, int[], long[]> createAlg( int radiusWidth, int radiusHeight ) {
			return new BlockRowScoreCensus.S64(-1);
		}

		@Override
		protected int computeError( long a, long b ) {
			return DescriptorDistance.hamming(a ^ b);
		}
	}
}
