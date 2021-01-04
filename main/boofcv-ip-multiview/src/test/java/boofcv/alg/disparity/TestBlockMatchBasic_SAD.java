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
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
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
class TestBlockMatchBasic_SAD<T extends ImageBase<T>> extends BoofStandardJUnit {

	@Nested
	class U8 extends ChecksDisparityBlockMatchNaive<GrayU8> {

		U8() {
			super(ImageType.single(GrayU8.class));
		}

		@Override
		public BruteForceBlockMatch<GrayU8> createNaive( BorderType borderType, ImageType<GrayU8> imageType ) {
			BruteForceBlockMatch<GrayU8> naive = new BruteForceBlockMatch<>(borderType, imageType) {
				@Override
				public double computeScore( ImageBorder<GrayU8> _left, ImageBorder<GrayU8> _right,
											int cx, int cy, int disparity ) {
					ImageBorder_S32<GrayU8> left = (ImageBorder_S32<GrayU8>)_left;
					ImageBorder_S32<GrayU8> right = (ImageBorder_S32<GrayU8>)_right;

					int total = 0;
					for (int y = -radius; y <= radius; y++) {
						for (int x = -radius; x <= radius; x++) {
							int va = left.get(cx + x, cy + y);
							int vb = right.get(cx + x - disparity, cy + y);

							total += Math.abs(va - vb);
						}
					}
					return total;
				}
			};
			naive.minimize = true;
			return naive;
		}

		@Override
		public StereoDisparity<GrayU8, GrayU8> createAlg( int blockRadius, int minDisparity, int maxDisparity ) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.errorType = DisparityError.SAD;
			config.border = BORDER_TYPE;
			return FactoryStereoDisparity.blockMatch(config, GrayU8.class, GrayU8.class);
		}
	}

	@Nested
	class F32 extends ChecksDisparityBlockMatchNaive<GrayF32> {

		F32() {
			super(ImageType.single(GrayF32.class));
		}

		@Override
		public BruteForceBlockMatch<GrayF32> createNaive( BorderType borderType, ImageType<GrayF32> imageType ) {
			BruteForceBlockMatch<GrayF32> naive = new BruteForceBlockMatch<>(borderType, imageType) {
				@Override
				public double computeScore( ImageBorder<GrayF32> _left, ImageBorder<GrayF32> _right,
											int cx, int cy, int disparity ) {
					ImageBorder_F32 left = (ImageBorder_F32)_left;
					ImageBorder_F32 right = (ImageBorder_F32)_right;

					float total = 0;
					for (int y = -radius; y <= radius; y++) {
						for (int x = -radius; x <= radius; x++) {
							float va = left.get(cx + x, cy + y);
							float vb = right.get(cx + x - disparity, cy + y);

							total += Math.abs(va - vb);
						}
					}
					return total;
				}
			};
			naive.minimize = true;
			return naive;
		}

		@Override
		public StereoDisparity<GrayF32, GrayU8> createAlg( int blockRadius, int minDisparity, int maxDisparity ) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.errorType = DisparityError.SAD;
			config.border = BORDER_TYPE;
			return FactoryStereoDisparity.blockMatch(config, GrayF32.class, GrayU8.class);
		}
	}
}
