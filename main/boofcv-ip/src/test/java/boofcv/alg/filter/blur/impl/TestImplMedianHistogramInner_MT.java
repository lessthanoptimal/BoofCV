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

package boofcv.alg.filter.blur.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImplMedianHistogramInner_MT {
	Random rand = new Random(234);

	@Test
	void compareToSingle() {
		GrayU8 input = new GrayU8(200,210);
		GrayU8 expected = input.createSameShape();
		GrayU8 found = input.createSameShape();

		ImageMiscOps.fillUniform(input,rand,0,200);

		BoofTesting.checkSubImage(this, "compareToSingle", true, input, found, expected);
	}

	public void compareToSingle(GrayU8 image, GrayU8 found, GrayU8 expected) {
		for( int radius = 1; radius <= 5; radius += 2 ) {
			ImageMiscOps.fill(found,0);
			ImageMiscOps.fill(expected,0);

			ImplMedianHistogramInner.process(image,expected,radius,null);
			ImplMedianHistogramInner_MT.process(image,found,radius,null);

			BoofTesting.assertEquals(expected,found,0);
		}
	}
}

