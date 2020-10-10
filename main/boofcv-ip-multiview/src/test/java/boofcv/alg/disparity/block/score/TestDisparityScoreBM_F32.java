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

package boofcv.alg.disparity.block.score;

import boofcv.alg.disparity.DisparityBlockMatch;
import boofcv.alg.disparity.block.BlockRowScore;
import boofcv.alg.disparity.block.DisparitySelect;
import boofcv.factory.disparity.DisparityError;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
public class TestDisparityScoreBM_F32 extends BoofStandardJUnit {

	@Nested
	public class SAD extends ChecksDisparityBM<GrayF32, GrayU8> {
		SAD() {
			super(0, 200, DisparityError.SAD, GrayF32.class, GrayU8.class);
		}

		@Override
		protected DisparityBlockMatch<GrayF32, GrayU8>
		createAlg( int radiusX, int radiusY, BlockRowScore scoreRow, DisparitySelect compDisp ) {
			return new DisparityScoreBM_F32<>(radiusX, radiusY, scoreRow, compDisp);
		}
	}

	/**
	 * Test this with error that requires normalization
	 */
	@Nested
	public class NCC extends ChecksDisparityBM<GrayF32, GrayU8> {
		NCC() {
			super(-1, 1, DisparityError.NCC, GrayF32.class, GrayU8.class);
		}

		@Override
		protected DisparityBlockMatch<GrayF32, GrayU8>
		createAlg( int radiusX, int radiusY, BlockRowScore scoreRow, DisparitySelect compDisp ) {
			return new DisparityScoreBM_F32<>(radiusX, radiusY, scoreRow, compDisp);
		}
	}
}
