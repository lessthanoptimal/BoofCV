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

package boofcv.alg.enhance.impl;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplEnhanceFilter_MT extends CompareIdenticalFunctions {
	int width = 70,height=80;

	public TestImplEnhanceFilter_MT() {
		super(ImplEnhanceFilter_MT.class, ImplEnhanceFilter.class);
	}

	@Test
	void performTests() {
		performTests(8);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		return super.isTestMethod(m) && !m.getName().contains("safeGet");
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		ImageGray input = GeneralizedImageOps.createSingleBand(types[0],width,height);

		GImageMiscOps.fillUniform(input,rand,0,200);
		ImageGray output = (ImageGray)input.clone();

		Object[] parameters = new Object[types.length];
		parameters[0] = input;
		parameters[1] = output;
		parameters[2] = BoofTesting.primitive(0,types[2]);
		parameters[3] = BoofTesting.primitive(255,types[2]);

		return new Object[][]{parameters};
	}
}

