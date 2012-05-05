/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.DisparitySparseSelect;
import boofcv.alg.feature.disparity.impl.ImplDisparitySparseScoreSadRect_F32;
import boofcv.alg.feature.disparity.impl.ImplSelectSparseBasicWta_F32;
import boofcv.alg.feature.disparity.impl.SelectSparseStandardSubpixel;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

		ImageFloat32 left = new ImageFloat32(w,h);
		ImageFloat32 right = new ImageFloat32(w,h);

		ImageUInt8 expected = new ImageUInt8(w,h);

		StereoDisparity<ImageFloat32,ImageUInt8> validator =
				FactoryStereoDisparity.regionWta(minDisparity,12,r,r,-1,-1,-1,ImageFloat32.class);
		StereoDisparitySparse<ImageFloat32> alg =
				new WrapDisparitySparseSadRect<float[],ImageFloat32>(
						new ImplDisparitySparseScoreSadRect_F32(minDisparity,12,r,r),
						new ImplSelectSparseBasicWta_F32());

		validator.process(left,right,expected);
		alg.setImages(left,right);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				if( alg.process(x,y) ) {
					double found = alg.getDisparity();
					assertEquals(expected.get(x,y),(int)found);
				} else {
					assertEquals(expected.get(x,y),0);
				}
			}
		}
	}

}
