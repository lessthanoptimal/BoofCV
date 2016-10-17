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

package boofcv.alg.transform.pyramid;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestPyramidDiscreteAverage {

	Random rand = new Random(234);

	/**
	 * Basis tests to see if it computes the expected pyramid
	 */
	@Test
	public void basicTest() {
		GrayF32 input = new GrayF32(40,80);
		ImageMiscOps.fillUniform(input, rand, -20, 50);

		PyramidDiscreteAverage<GrayF32> alg = new PyramidDiscreteAverage<>(GrayF32.class,true,1,2,4);

		alg.process(input);

		// request was made use a reference to the input image
		assertTrue(input == alg.getLayer(0));

		float expected = (input.get(0,0) +  input.get(0,1) + input.get(1,0) + input.get(1,1))/4;
		assertEquals(expected,alg.getLayer(1).get(0,0),1e-4);

		GrayF32 layer = alg.getLayer(1);
		expected = (layer.get(0,0) +  layer.get(0,1) + layer.get(1,0) + layer.get(1,1))/4;
		assertEquals(expected,alg.getLayer(2).get(0,0),1e-4);
	}

}
