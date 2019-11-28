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

package boofcv.alg.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.feature.disparity.block.DisparityBlockMatchNaive;
import boofcv.alg.feature.disparity.block.TestBlockRowScoreNcc;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;

/**
 * Test the entire block matching pipeline against a naive implementation
 *
 * @author Peter Abeles
 */
@SuppressWarnings("InnerClassMayBeStatic")
class TestBlockMatchBasic_NCC<T extends ImageBase<T>> {

	@Nested
	class F32 extends ChecksBlockMatchBasic<GrayF32> {

		F32() {
			super(ImageType.single(GrayF32.class));
		}

		@Override
		public DisparityBlockMatchNaive<GrayF32> createNaive(int blockRadius, int minDisparity, int maxDisparity) {
			DisparityBlockMatchNaive<GrayF32> naive = new DisparityBlockMatchNaive<GrayF32>(blockRadius,minDisparity,maxDisparity) {
				public double computeScore(GrayF32 left, GrayF32 right, int cx, int cy, int disparity) {
					return TestBlockRowScoreNcc.ncc(left,right,cx,cy,disparity,radius, UtilEjml.F_EPS);
				}
			};
			naive.minimize = false;
			return naive;
		}

		@Override
		public StereoDisparity<GrayF32, GrayU8> createAlg(int blockRadius, int minDisparity, int maxDisparity) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.errorType = DisparityError.NCC;
			return FactoryStereoDisparity.blockMatch(config,GrayF32.class,GrayU8.class);
		}
	}
}
