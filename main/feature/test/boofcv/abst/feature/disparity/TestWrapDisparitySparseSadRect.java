/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.impl.ImplDisparitySparseScoreSadRect_F32;
import boofcv.alg.feature.disparity.impl.ImplSelectSparseBasicWta_F32;
import boofcv.factory.feature.disparity.DisparityAlgorithms;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestWrapDisparitySparseSadRect {

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

		StereoDisparity<GrayF32,GrayU8> validator =
				FactoryStereoDisparity.regionWta(DisparityAlgorithms.RECT,
						minDisparity,maxDisparity,r,r,-1,-1,-1,GrayF32.class);
		StereoDisparitySparse<GrayF32> alg =
				new WrapDisparitySparseSadRect<>(
						new ImplDisparitySparseScoreSadRect_F32(minDisparity, maxDisparity, r, r),
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
