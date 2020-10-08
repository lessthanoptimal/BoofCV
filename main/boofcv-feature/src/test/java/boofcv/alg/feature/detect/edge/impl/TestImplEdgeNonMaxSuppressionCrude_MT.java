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

class TestImplEdgeNonMaxSuppressionCrude_MT extends CompareIdenticalFunctions {

	private int width = 60,height=70;

	TestImplEdgeNonMaxSuppressionCrude_MT() {
		super(ImplEdgeNonMaxSuppressionCrude_MT.class, ImplEdgeNonMaxSuppressionCrude.class);
	}

	@Test
	void performTests() {
		super.performTests(5);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] inputTypes = candidate.getParameterTypes();
		ImageGray intensity = GeneralizedImageOps.createSingleBand(inputTypes[0],width,height);
		ImageGray derivX = GeneralizedImageOps.createSingleBand(inputTypes[1],width,height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(inputTypes[2],width,height);
		ImageGray output = GeneralizedImageOps.createSingleBand(inputTypes[3],width,height);

		GImageMiscOps.fillUniform(intensity,rand,0,100);
		GImageMiscOps.fillUniform(derivX,rand,-100,100);
		GImageMiscOps.fillUniform(derivY,rand,-100,100);

		Object[] inputs = new Object[4];

		inputs[0] = intensity;
		inputs[1] = derivX;
		inputs[2] = derivY;
		inputs[3] = output;

		return new Object[][]{inputs};
	}
}

