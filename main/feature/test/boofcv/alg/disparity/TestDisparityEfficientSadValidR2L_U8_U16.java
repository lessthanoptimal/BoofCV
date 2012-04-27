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

package boofcv.alg.disparity;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestDisparityEfficientSadValidR2L_U8_U16 {

	Random rand = new Random(234);

	/**
	 * Basic generic disparity calculation tests
	 */
	@Test
	public void basicTest() {
		BasicDisparityTests<ImageUInt8,ImageUInt16> alg =
				new BasicDisparityTests<ImageUInt8,ImageUInt16>(ImageUInt8.class) {

					DisparityEfficientSadValidR2L_U8_U16 alg;

					@Override
					public ImageUInt16 computeDisparity(ImageUInt8 left, ImageUInt8 right ) {
						ImageUInt16 ret = new ImageUInt16(left.width,left.height);

						alg.process(left,right,ret);

						return ret;
					}

					@Override
					public void initialize(int maxDisparity) {
						alg = new DisparityEfficientSadValidR2L_U8_U16(maxDisparity,2,3);
					}

					@Override public int getBorderX() { return 2; }

					@Override public int getBorderY() { return 3; }
				};

		alg.checkGradient();
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

		ImageUInt16 found = new ImageUInt16(w,h);
		ImageFloat32 expected = new ImageFloat32(w,h);

		GeneralizedImageOps.randomize(left,rand,0,20);
		GeneralizedImageOps.randomize(right,rand,0,20);

		int maxDisparity = 10;
		int radiusX = 3;
		int radiusY = 2;

		DisparityEfficientSadValidR2L_U8_U16 alg =
				new DisparityEfficientSadValidR2L_U8_U16(maxDisparity,radiusX,radiusY);
		StereoDisparityWtoNaive<ImageUInt8> naive =
				new StereoDisparityWtoNaive<ImageUInt8>(maxDisparity,radiusX,radiusY);

		alg.process(left,right,found);
		naive.process(left,right,expected);

//		found.print();
//		System.out.println("--------------");
//		expected.printInt();

		BoofTesting.assertEqualsGeneric(found, expected, 1, 1);
	}
}
