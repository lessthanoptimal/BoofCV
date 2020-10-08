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
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplEdgeNonMaxSuppression_MT extends CompareIdenticalFunctions  {

	private int width = 60,height=70;

	TestImplEdgeNonMaxSuppression_MT() {
		super(ImplEdgeNonMaxSuppression_MT.class, ImplEdgeNonMaxSuppression.class);
	}

	@Test
	void performTests() {
		super.performTests(6);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		String name = candidate.getName();
		int numDirections = Integer.parseInt(name.substring(name.length()-1));

		Class[] inputTypes = candidate.getParameterTypes();
		ImageGray intensity = GeneralizedImageOps.createSingleBand(inputTypes[0],width,height);
		GrayS8 direction = new GrayS8(width,height);
		ImageGray output = GeneralizedImageOps.createSingleBand(inputTypes[2],width,height);

		GImageMiscOps.fillUniform(intensity,rand,0,100);
		if( numDirections == 8 )
			GImageMiscOps.fillUniform(direction,rand,-3,5);
		else
			GImageMiscOps.fillUniform(direction,rand,0,4);

		Object[] inputs = new Object[3];

		inputs[0] = intensity;
		inputs[1] = direction;
		inputs[2] = output;

		return new Object[][]{inputs};
	}
}