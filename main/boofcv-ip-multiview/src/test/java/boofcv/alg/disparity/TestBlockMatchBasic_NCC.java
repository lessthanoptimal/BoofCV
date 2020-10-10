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

package boofcv.alg.disparity;

import boofcv.abst.disparity.StereoDisparity;
import boofcv.alg.disparity.block.TestBlockRowScoreNcc;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * Test the entire block matching pipeline against a naive implementation
 *
 * @author Peter Abeles
 */
@SuppressWarnings("InnerClassMayBeStatic")
class TestBlockMatchBasic_NCC<T extends ImageBase<T>> extends BoofStandardJUnit {

	@Nested
	class F32 extends ChecksDisparityBlockMatchNaive<GrayF32> {
		float eps = 1e-4f;

		F32() {
			super(ImageType.single(GrayF32.class));
		}

		@Override
		public BruteForceBlockMatch<GrayF32> createNaive( BorderType borderType, ImageType<GrayF32> imageType ) {
			BruteForceBlockMatch<GrayF32> naive = new BruteForceBlockMatch<GrayF32>(borderType, imageType) {
				@Override
				public double computeScore( ImageBorder<GrayF32> _left, ImageBorder<GrayF32> _right,
											int cx, int cy, int disparity ) {
					ImageBorder_F32 left = (ImageBorder_F32)_left;
					ImageBorder_F32 right = (ImageBorder_F32)_right;
					return TestBlockRowScoreNcc.ncc(left, right, cx, cy, disparity, radius, radius, eps);
				}
			};
			naive.minimize = false;
			return naive;
		}

		@Override
		public StereoDisparity<GrayF32, GrayU8> createAlg( int blockRadius, int minDisparity, int maxDisparity ) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.border = BORDER_TYPE;
			config.errorType = DisparityError.NCC;
			config.configNCC.eps = eps;
			return FactoryStereoDisparity.blockMatch(config, GrayF32.class, GrayU8.class);
		}

		@Override
		protected void fillInStereoImages() {
			GImageMiscOps.fillUniform(left, rand, -1, 1);
			GImageMiscOps.fillUniform(right, rand, -1, 1);
		}
	}
}
