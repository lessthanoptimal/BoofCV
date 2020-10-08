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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplGradientToEdgeFeatures_MT extends CompareIdenticalFunctions {

	private int width = 105;
	private int height = 100;

	TestImplGradientToEdgeFeatures_MT() {
		super(ImplGradientToEdgeFeatures_MT.class, ImplGradientToEdgeFeatures.class);
	}

	@Test
	void performTests() {
		performTests(12);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] inputTypes = candidate.getParameterTypes();
		ImageGray derivX = GeneralizedImageOps.createSingleBand(inputTypes[0],width,height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(inputTypes[1],width,height);
		ImageGray output = GeneralizedImageOps.createSingleBand(inputTypes[2],width,height);

		GImageMiscOps.fillUniform(derivX,rand,-100,100);
		GImageMiscOps.fillUniform(derivY,rand,-100,100);

		Object[] inputs = new Object[3];

		inputs[0] = derivX;
		inputs[1] = derivY;
		inputs[2] = output;

		return new Object[][]{inputs};
	}
}

