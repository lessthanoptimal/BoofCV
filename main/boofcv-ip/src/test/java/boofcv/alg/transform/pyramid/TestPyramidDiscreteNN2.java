/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TestPyramidDiscreteNN2 extends BoofStandardJUnit {

	@Test
	void basicTest() {
		GrayF32 input = new GrayF32(40, 80);
		ImageMiscOps.fillUniform(input, rand, -20, 50);

		PyramidDiscreteNN2<GrayF32> alg = new PyramidDiscreteNN2<>(ImageType.single(GrayF32.class));
		alg.getConfigLayers().numLevelsRequested = 3;
		checkSolution(input, alg);
	}

	private void checkSolution( GrayF32 input, PyramidDiscreteNN2<GrayF32> alg ) {
		alg.process(input);

		// Level zero is the input image
		assertSame(input, alg.getLayer(0));

		// There should be 3 levels
		assertEquals(3, alg.getLevelsCount());

		// nearest-neighbor interpolation
		assertEquals(input.get(0, 0), alg.getLayer(1).get(0, 0), 1e-4);
		assertEquals(input.get(2, 2), alg.getLayer(1).get(1, 1), 1e-4);
		assertEquals(input.get(6, 4), alg.getLayer(1).get(3, 2), 1e-4);
	}
}
