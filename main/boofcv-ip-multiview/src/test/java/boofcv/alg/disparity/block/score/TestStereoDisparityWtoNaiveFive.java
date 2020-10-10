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

import boofcv.alg.disparity.block.DisparityBlockMatchBestFiveNaive;
import boofcv.alg.disparity.block.DisparityBlockMatchNaive;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.disparity.DisparityError;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Nested;

/**
 * @author Peter Abeles
 */
class TestStereoDisparityWtoNaiveFive extends BoofStandardJUnit {
	@Nested
	class BasicTests extends BasicDisparityTests<GrayU8, GrayF32> {
		DisparityBlockMatchBestFiveNaive<GrayU8> alg;

		BasicTests() { super(0, 200, GrayU8.class); }

		@Override
		public void initialize( int minDisparity, int maxDisparity ) {
			alg = new DisparityBlockMatchBestFiveNaive<>(DisparityError.SAD);
			alg.configure(minDisparity, maxDisparity, 2, 3);
			alg.setBorder(FactoryImageBorder.
					generic(DisparityBlockMatchNaive.BORDER_TYPE, ImageType.single(GrayU8.class)));
		}

		@Override
		public GrayF32 computeDisparity( GrayU8 left, GrayU8 right ) {
			GrayF32 ret = new GrayF32(left.width, left.height);
			alg.process(left, right, ret);
			return ret;
		}
	}
}
