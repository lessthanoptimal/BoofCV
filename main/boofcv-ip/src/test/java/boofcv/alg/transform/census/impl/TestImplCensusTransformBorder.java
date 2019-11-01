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

package boofcv.alg.transform.census.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.census.CensusNaive;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
class TestImplCensusTransformBorder {
	int w = 20, h = 30;

	@Test
	void region3x3() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayU8 output = new GrayU8(w,h);
		GrayU8 expected = new GrayU8(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformBorder.region3x3((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),output);
		CensusNaive.region3x3(input,expected);

		BoofTesting.assertEqualsBorder(expected,output,0,1,1);
	}

	@Test
	void region5x5() {
		Random rand = new Random(234);

		GrayU8 input = new GrayU8(w,h);
		GrayS32 output = new GrayS32(w,h);
		GrayS32 expected = new GrayS32(w,h);

		ImageMiscOps.fillUniform(input,rand,0,255);

		ImplCensusTransformBorder.region5x5((ImageBorder_S32)FactoryImageBorder.wrap(BorderType.EXTENDED,input),output);
		CensusNaive.region5x5(input,expected);

		BoofTesting.assertEqualsBorder(expected,output,0,2,2);
	}
}