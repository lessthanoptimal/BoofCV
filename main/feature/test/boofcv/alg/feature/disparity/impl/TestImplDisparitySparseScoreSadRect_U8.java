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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplDisparitySparseScoreSadRect_U8 {

	Random rand = new Random(234);

	ImplSelectSparseBasicWta_S32 selectAlg = new ImplSelectSparseBasicWta_S32();

	/**
	 * Compute disparity using the equivalent dense algorithm and see if the sparse one produces the
	 * same results.
	 */
	@Test
	public void compareToDense() {
		int w = 20, h = 25;
		ImageUInt8 left = new ImageUInt8(w,h);
		ImageUInt8 right = new ImageUInt8(w,h);

		GImageMiscOps.fillUniform(left, rand, 0, 20);
		GImageMiscOps.fillUniform(right, rand, 0, 20);

		compareToDense(left, right, 0);
		compareToDense(left, right, 2);
	}

	private void compareToDense(ImageUInt8 left, ImageUInt8 right, int minDisparity) {
		int w = left.width; int h = left.height;
		int maxDisparity = 10;
		int radiusX = 3;
		int radiusY = 2;

		ImplDisparityScoreSadRect_U8<ImageUInt8> denseAlg =
				new ImplDisparityScoreSadRect_U8<ImageUInt8>(minDisparity,maxDisparity,radiusX,radiusY,new ImplSelectRectBasicWta_S32_U8());
		ImplDisparitySparseScoreSadRect_U8 alg = new ImplDisparitySparseScoreSadRect_U8(minDisparity,maxDisparity,radiusX,radiusY);

		ImageUInt8 expected = new ImageUInt8(w,h);
		denseAlg.process(left, right, expected);
		alg.setImages(left,right);

		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < w; x++ ) {
				if( !alg.process(x,y) )  {
					assertEquals(x+" "+y,expected.get(x,y),0);
				} else {
					selectAlg.select(alg.scores,alg.getLocalMaxDisparity());
					int found = (int)(alg.getMinDisparity()+selectAlg.getDisparity());

					assertEquals(x+" "+y,minDisparity+expected.get(x,y),found);
				}
			}
		}
	}
}
