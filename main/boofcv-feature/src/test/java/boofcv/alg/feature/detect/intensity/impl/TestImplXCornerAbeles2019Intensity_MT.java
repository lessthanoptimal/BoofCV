/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
class TestImplXCornerAbeles2019Intensity_MT extends BoofStandardJUnit {

	int width = 60;
	int height = 50;

	@Test
	void compareToSingle() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 found = input.createSameShape();
		GrayF32 expected = input.createSameShape();

		ImageMiscOps.fillUniform(input,rand,-1,1);
		ImageMiscOps.fillUniform(found,rand,-1,1);
		ImageMiscOps.fillUniform(expected,rand,-1,1);

		ImplXCornerAbeles2019Intensity.process(input,expected);
		ImplXCornerAbeles2019Intensity_MT.process(input,found);

		BoofTesting.assertEquals(expected,found, 1e-8);
	}
}