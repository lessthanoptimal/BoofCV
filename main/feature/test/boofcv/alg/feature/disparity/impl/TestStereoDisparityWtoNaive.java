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

package boofcv.alg.feature.disparity.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import org.junit.Test;

/**
 * @author Peter Abeles
 */
public class TestStereoDisparityWtoNaive {

	@Test
	public void basicTest() {
		BasicDisparityTests<GrayU8,GrayF32> alg =
				new BasicDisparityTests<GrayU8,GrayF32>(GrayU8.class) {

					StereoDisparityWtoNaive<GrayU8> alg;

					@Override
					public GrayF32 computeDisparity(GrayU8 left, GrayU8 right ) {
						GrayF32 ret = new GrayF32(left.width,left.height);

						alg.process(left,right,ret);

						return ret;
					}

					@Override
					public void initialize(int minDisparity , int maxDisparity) {
						alg = new StereoDisparityWtoNaive<>(minDisparity,maxDisparity,2,3);
					}

					@Override public int getBorderX() { return 2; }

					@Override public int getBorderY() { return 3; }
				};

		alg.allChecks();
	}
}
