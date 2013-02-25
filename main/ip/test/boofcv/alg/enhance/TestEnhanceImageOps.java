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

package boofcv.alg.enhance;

import boofcv.alg.enhance.impl.ImplEnhanceHistogram;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestEnhanceImageOps {

	int width = 15;
	int height = 20;
	Random rand = new Random(234);

	@Test
	public void equalize() {
		int histogram[] = new int[]{2,2,2,2,2,0,0,0,0,0};
		int transform[] = new int[10];
		EnhanceImageOps.equalize(histogram,transform);

		assertEquals(1,transform[0]);
		assertEquals(3,transform[1]);
		assertEquals(5,transform[2]);
		assertEquals(7,transform[3]);
		assertEquals(9,transform[4]);
	}

	@Test
	public void equalizeLocal() {

		ImageUInt8 input = new ImageUInt8(width,height);
		ImageUInt8 output = new ImageUInt8(width,height);

		ImageMiscOps.fillUniform(input, rand, 0, 10);

		equalizeLocal(input,output);
	}

	public void equalizeLocal( ImageUInt8 input , ImageUInt8 output ) {

		int transform[] = new int[10];
		int histogram[] = new int[10];

		ImageUInt8 expected = new ImageUInt8(width,height);

		for( int radius = 1; radius < 11; radius++ ) {
			ImplEnhanceHistogram.equalizeLocalNaive(input, radius, expected, histogram);
			EnhanceImageOps.equalizeLocal(input, radius, output, histogram, transform);

			BoofTesting.assertEquals(expected, output, 1e-10);
		}
	}
}
