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

package boofcv.alg.weights;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestWeightPixelUniform_F32 {

	@Test
	public void inside() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();
		alg.setRadius(2,3);

		float expected = 1.0f/(5f*7f);

		int index = 0;
		for( int y = -3; y <= 3; y++ )
			for( int x = -2; x <= 2; x++ , index++) {
				assertEquals(expected,alg.weight(x,y),1e-4);
				assertEquals(expected,alg.weightIndex(index),1e-4);
			}
	}

	/**
	 * Well if it is outside the square region it should be zero, but the class specifies that it doesn't actually
	 * check to see if that's the case.
	 */
	@Test
	public void outside() {
		WeightPixelUniform_F32 alg = new WeightPixelUniform_F32();
		alg.setRadius(2,2);

		float expected = 1.0f/25.0f;

		assertEquals(expected,alg.weight(-3,0),1e-4);
	}
}
