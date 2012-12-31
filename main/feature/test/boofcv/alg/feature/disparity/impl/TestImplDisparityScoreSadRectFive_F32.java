/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.disparity.DisparitySelect;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplDisparityScoreSadRectFive_F32 {

	Random rand = new Random(234);

	DisparitySelect<float[],ImageUInt8> compDisp = new ImplSelectRectBasicWta_F32_U8();

	/**
	 * Basic generic disparity calculation tests
	 */
	@Test
	public void basicTest() {
		BasicDisparityTests<ImageFloat32,ImageUInt8> alg =
				new BasicDisparityTests<ImageFloat32,ImageUInt8>(ImageFloat32.class) {

					ImplDisparityScoreSadRectFive_F32<ImageUInt8> alg;

					@Override
					public ImageUInt8 computeDisparity(ImageFloat32 left, ImageFloat32 right ) {
						ImageUInt8 ret = new ImageUInt8(left.width,left.height);

						alg.process(left,right,ret);

						return ret;
					}

					@Override
					public void initialize(int minDisparity , int maxDisparity) {
						alg = new ImplDisparityScoreSadRectFive_F32<ImageUInt8>(minDisparity,maxDisparity,2,3,compDisp);
					}

					@Override public int getBorderX() { return 2*2; }

					@Override public int getBorderY() { return 3*2; }
				};

		alg.allChecks();
	}

	/**
	 * Compare to a simplistic implementation of stereo disparity.  Need to turn off special
	 * configurations
	 */
	@Test
	public void compareToNaive() {
		int w = 20, h = 25;
		ImageUInt8 left = new ImageUInt8(w,h);
		ImageUInt8 right = new ImageUInt8(w,h);

		GImageMiscOps.fillUniform(left, rand, 0, 20);
		GImageMiscOps.fillUniform(right, rand, 0, 20);

		int radiusX = 3;
		int radiusY = 2;

		// compare to naive with different settings
		compareToNaive(left, right, 0, 10, radiusX, radiusY);
		compareToNaive(left, right, 4, 10, radiusX, radiusY);
	}

	private void compareToNaive(ImageUInt8 left, ImageUInt8 right,
								int minDisparity, int maxDisparity,
								int radiusX, int radiusY)
	{
		int w = left.width;
		int h = left.height;

		ImageFloat32 leftF32 = new ImageFloat32(w,h);
		ImageFloat32 rightF32 = new ImageFloat32(w,h);
		GeneralizedImageOps.convert(left,leftF32);
		GeneralizedImageOps.convert(right,rightF32);

		ImplDisparityScoreSadRectFive_F32<ImageUInt8> alg =
				new ImplDisparityScoreSadRectFive_F32<ImageUInt8>(minDisparity,maxDisparity,radiusX,radiusY,compDisp);
		StereoDisparityWtoNaiveFive<ImageUInt8> naive =
				new StereoDisparityWtoNaiveFive<ImageUInt8>(minDisparity,maxDisparity,radiusX,radiusY);

		ImageUInt8 found = new ImageUInt8(w,h);
		ImageFloat32 expected = new ImageFloat32(w,h);

		alg.process(leftF32, rightF32, found);
		naive.process(left, right, expected);

		BoofTesting.assertEquals(found, expected, 1);
	}
}
