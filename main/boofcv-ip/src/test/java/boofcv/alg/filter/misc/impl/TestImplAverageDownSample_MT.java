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

package boofcv.alg.filter.misc.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

@SuppressWarnings({"rawtypes", "unchecked"})
class TestImplAverageDownSample_MT extends CompareIdenticalFunctions
{
	Random rand = new Random(234);
	int width = 640,height=480;

	protected TestImplAverageDownSample_MT() {
		super(ImplAverageDownSample_MT.class, ImplAverageDownSample.class);
	}

	@Test
	void performTests() {
		super.performTests(8);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] inputTypes = candidate.getParameterTypes();

		ImageBase input = GeneralizedImageOps.createImage(inputTypes[0],width,height,1);
		ImageBase output;

		if( candidate.getName().contains("horizontal"))
			output = GeneralizedImageOps.createImage(inputTypes[1],width/3,height,1);
		else
			output = GeneralizedImageOps.createImage(inputTypes[1],width,height/2,1);

		GImageMiscOps.fillUniform(input,rand,0,255);
		GImageMiscOps.fillUniform(output,rand,0,100);

		return new Object[][]{{input, output}};
	}
}