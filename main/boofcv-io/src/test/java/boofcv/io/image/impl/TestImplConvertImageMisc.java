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

package boofcv.io.image.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Peter Abeles
 */
class TestImplConvertImageMisc extends BoofStandardJUnit {
	int width = 30,height=40;

	@Test
	void convert_F32_U16_and_Reverse() {
		GrayF32 src = new GrayF32(width,height);
		GrayU16 dst = new GrayU16(width,height);
		GrayF32 found = new GrayF32(width,height);

		ImageMiscOps.fillUniform(src,rand,0,255);

		for (int fractionBits = 1; fractionBits <= 8; fractionBits++) {
			ImplConvertImageMisc.convert_F32_U16(src,fractionBits,dst);
			ImplConvertImageMisc.convert_U16_F32(dst,fractionBits,found);

			float resolution = 1.0f/(1<<fractionBits);

			BoofTesting.assertEquals(src,found,resolution);
		}
	}
}
