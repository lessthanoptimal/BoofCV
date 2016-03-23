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

package boofcv.alg.filter.derivative.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestGradientThree_Share {
	Random rand = new Random(234);

	int width = 200;
	int height = 250;


	@Test
	public void derivX_F32() {
		GrayF32 img = new GrayF32(width, height);
		ImageMiscOps.fillUniform(img, rand, 0f, 255f);

		GrayF32 derivX = new GrayF32(width, height);

		GrayF32 derivX2 = new GrayF32(width, height);

		GradientThree_Standard.process(img, derivX2,new GrayF32(width,height));
		GradientThree_Share.derivX_F32(img, derivX);

		BoofTesting.assertEqualsInner(derivX2, derivX, 1e-4f, 1, 1, true);
	}
}
