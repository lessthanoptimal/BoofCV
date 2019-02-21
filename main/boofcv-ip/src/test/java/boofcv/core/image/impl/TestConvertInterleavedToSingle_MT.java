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

package boofcv.core.image.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageInterleaved;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

class TestConvertInterleavedToSingle_MT extends CompareIdenticalFunctions {
	private Random rand = new Random(234);
	private int width = 105;
	private int height = 100;


	TestConvertInterleavedToSingle_MT() {
		super(ConvertInterleavedToSingle_MT.class, ConvertInterleavedToSingle.class);
	}

	@Test
	void performTests() {
		performTests(8);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] type = candidate.getParameterTypes();
		Class grayType = type[1];

		return new Object[][] {
				createTest(grayType,1),
				createTest(grayType,2),
				createTest(grayType,3),
				createTest(grayType,4)};
	}

	private Object[] createTest( Class type , int numBands ) {
		Object[] params = new Object[2];

		params[1] = GeneralizedImageOps.createSingleBand(type,width,height);
		ImageDataType dt = ((ImageBase)params[1]).getImageType().getDataType();
		params[0] = GeneralizedImageOps.createInterleaved(dt,width,height,numBands);

		GImageMiscOps.fillUniform((ImageInterleaved)params[0],rand,0,200);


		return params;
	}
}

