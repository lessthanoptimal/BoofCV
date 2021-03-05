/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
class TestImplIntegralImageFeatureIntensity_MT extends CompareIdenticalFunctions {

	private final Random rand = new Random(234);
	private final int width = 60;
	private final int height = 70;

	TestImplIntegralImageFeatureIntensity_MT() {
		super(ImplIntegralImageFeatureIntensity_MT.class, ImplIntegralImageFeatureIntensity.class);
	}

	@Test
	void performTests() {
		super.performTests(4);
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		int skip = 2;
		Class[] inputTypes = candidate.getParameterTypes();
		ImageGray integral = GeneralizedImageOps.createSingleBand(inputTypes[0], width, height);
		ImageGray intensity = GeneralizedImageOps.createSingleBand(inputTypes[3], width/skip, height/skip);

		GImageMiscOps.fillUniform(integral, rand, 0, 100);
		Object[] inputs = new Object[candidate.getParameterTypes().length];

		inputs[0] = integral;
		inputs[1] = skip;
		inputs[2] = 5;
		inputs[3] = intensity;
		// if it has 7 arguments the last 3 can all be null

		return new Object[][]{inputs};
	}
}