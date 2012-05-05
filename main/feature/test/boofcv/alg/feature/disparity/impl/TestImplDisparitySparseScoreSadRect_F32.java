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

package boofcv.alg.feature.disparity.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplDisparitySparseScoreSadRect_F32 {

	Random rand = new Random(234);

	ImplSelectSparseBasicWta_F32 selectAlg = new ImplSelectSparseBasicWta_F32();

	/**
	 * Compute disparity using the equivalent dense algorithm and see if the sparse one produces the
	 * same results.
	 */
	@Test
	public void compareToDense() {
		int w = 20, h = 25;
		ImageFloat32 left = new ImageFloat32(w,h);
		ImageFloat32 right = new ImageFloat32(w,h);

		ImageUInt8 expected = new ImageUInt8(w,h);

		GeneralizedImageOps.randomize(left, rand, 0, 20);
		GeneralizedImageOps.randomize(right,rand,0,20);

		int minDisparity = 0;
		int maxDisparity = 10;
		int radiusX = 3;
		int radiusY = 2;

		ImplDisparityScoreSadRect_F32<ImageUInt8> denseAlg =
				new ImplDisparityScoreSadRect_F32<ImageUInt8>(minDisparity,maxDisparity,radiusX,radiusY,new ImplSelectRectBasicWta_F32_U8());
		ImplDisparitySparseScoreSadRect_F32 alg = new ImplDisparitySparseScoreSadRect_F32(0,maxDisparity,radiusX,radiusY);

		denseAlg.process(left, right, expected);
		alg.setImages(left,right);

		for( int y = radiusY; y < h-radiusY; y++ ) {
			for( int x = radiusX; x < w-radiusX; x++ ) {
				alg.process(x,y);
				selectAlg.select(alg.scores,0,alg.getLocalMaxDisparity());
				int found = (int)selectAlg.getDisparity();

				assertEquals(x+" "+y,expected.get(x,y),found);
			}
		}
	}
}
