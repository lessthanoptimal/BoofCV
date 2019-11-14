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

package boofcv.abst.feature.disparity;

import boofcv.alg.feature.disparity.block.score.ImplDisparitySparseScoreBM_SAD_F32;
import boofcv.alg.feature.disparity.block.select.ImplSelectSparseBasicWta_F32;
import boofcv.factory.feature.disparity.ConfigureDisparityBM;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWrapDisparityBlockSparseSad {

	Random rand = new Random(234);
	int w = 30;
	int h = 40;
	int r = 2;

	/**
	 * Compare to the equivalent dense algorithm
	 */
	@Test
	public void compareToDense() {
		// more stressful if not 0
		int minDisparity = 3;
		int maxDisparity = 12;

		GrayF32 left = new GrayF32(w,h);
		GrayF32 right = new GrayF32(w,h);

		ConfigureDisparityBM config = new ConfigureDisparityBM();
		config.regionRadiusX = config.regionRadiusY = r;
		config.subpixel = false;
		config.errorType = DisparityError.SAD;
		config.minDisparity = minDisparity;
		config.maxDisparity = maxDisparity;
		config.maxPerPixelError = -1;
		config.texture = -1;
		config.validateRtoL = -1;

		StereoDisparity<GrayF32,GrayU8> validator =
				FactoryStereoDisparity.blockMatch(config,GrayF32.class,GrayU8.class);
		StereoDisparitySparse<GrayF32> alg =
				new WrapDisparityBlockSparseSad<>(
						new ImplDisparitySparseScoreBM_SAD_F32(minDisparity, maxDisparity, r, r),
						new ImplSelectSparseBasicWta_F32());

		validator.process(left,right);
		GrayU8 expected = validator.getDisparity();
		alg.setImages(left,right);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				if( alg.process(x,y) ) {
					double found = alg.getDisparity();
					assertEquals(minDisparity+expected.get(x,y),(int)found);
				} else {
					assertTrue(expected.get(x, y) > (maxDisparity - minDisparity));
				}
			}
		}
	}

}
