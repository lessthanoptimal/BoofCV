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

package boofcv.alg.border;

import boofcv.struct.image.*;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestGrowBorderSB extends BoofStandardJUnit
{
	@Nested
	public class SB_I8 extends CheckGrowBodersSB<GrayU8> {

		public SB_I8() {
			super(0, 255, ImageType.SB_U8);
		}

		@Override
		protected GrowBorderSB<GrayU8, byte[]> createAlg() {
			return new GrowBorderSB.SB_I8<>(ImageType.SB_U8);
		}
	}

	@Nested
	public class SB_I16 extends CheckGrowBodersSB<GrayU16> {
		public SB_I16() {
			super(0, 3000, ImageType.SB_U16);
		}

		@Override
		protected GrowBorderSB<GrayU16, short[]> createAlg() {
			return new GrowBorderSB.SB_I16<>(ImageType.SB_U16);
		}
	}

	@Nested
	public class SB_S32 extends CheckGrowBodersSB<GrayS32> {
		public SB_S32() {
			super(-20_000, 20_000, ImageType.SB_S32);
		}

		@Override
		protected GrowBorderSB<GrayS32, int[]> createAlg() {
			return new GrowBorderSB.SB_S32();
		}
	}

	@Nested
	public class SB_F32 extends CheckGrowBodersSB<GrayF32> {
		public SB_F32() {
			super(-1.0, 1.0, ImageType.SB_F32);
		}

		@Override
		protected GrowBorderSB<GrayF32, float[]> createAlg() {
			return new GrowBorderSB.SB_F32();
		}
	}

	@Nested
	public class SB_F64 extends CheckGrowBodersSB<GrayF64> {
		public SB_F64() {
			super(-1.0, 1.0, ImageType.SB_F64);
		}

		@Override
		protected GrowBorderSB<GrayF64, double[]> createAlg() {
			return new GrowBorderSB.SB_F64();
		}
	}
}
