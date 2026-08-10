/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
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
class TestBlockMatchBasic_ZSSD<T extends ImageBase<T>> extends BoofStandardJUnit {

	@Nested
	class U8 extends ChecksDisparityBlockMatchNaive<GrayU8> {

		U8() {
			super(ImageType.single(GrayU8.class));
		}

		@Override
		public BruteForceBlockMatch<GrayU8> createNaive( BorderType borderType, ImageType<GrayU8> imageType ) {
			BruteForceBlockMatch<GrayU8> naive = new BruteForceBlockMatch<>(borderType, imageType) {
				@Override
				public double computeScore( ImageBorder<GrayU8> left, ImageBorder<GrayU8> right,
											int cx, int cy, int disparity ) {
					return zssd(left, right, cx, cy, disparity, radius);
				}
			};
			naive.minimize = true;
			return naive;
		}

		@Override
		public StereoDisparity<GrayU8, GrayU8> createAlg( int blockRadius, int minDisparity, int maxDisparity ) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.errorType = DisparityError.ZSSD;
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
				public double computeScore( ImageBorder<GrayF32> left, ImageBorder<GrayF32> right,
											int cx, int cy, int disparity ) {
					return zssd(left, right, cx, cy, disparity, radius);
				}
			};
			naive.minimize = true;
			return naive;
		}

		@Override
		public StereoDisparity<GrayF32, GrayU8> createAlg( int blockRadius, int minDisparity, int maxDisparity ) {
			ConfigDisparityBM config = createConfigBasicBM(blockRadius, minDisparity, maxDisparity);
			config.errorType = DisparityError.ZSSD;
			config.border = BORDER_TYPE;
			return FactoryStereoDisparity.blockMatch(config, GrayF32.class, GrayU8.class);
		}
	}

	/// Zero-mean SSD computed straight from the definition
	private static double zssd( ImageBorder<?> left, ImageBorder<?> right,
								int cx, int cy, int disparity, int radius ) {
		int area = (2*radius + 1)*(2*radius + 1);
		double meanL = 0, meanR = 0;
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				meanL += GeneralizedImageOps.get(left, cx + x, cy + y);
				meanR += GeneralizedImageOps.get(right, cx + x - disparity, cy + y);
			}
		}
		meanL /= area;
		meanR /= area;

		double total = 0;
		for (int y = -radius; y <= radius; y++) {
			for (int x = -radius; x <= radius; x++) {
				double va = GeneralizedImageOps.get(left, cx + x, cy + y) - meanL;
				double vb = GeneralizedImageOps.get(right, cx + x - disparity, cy + y) - meanR;

				total += (va - vb)*(va - vb);
			}
		}
		return total;
	}
}
