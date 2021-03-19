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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static boofcv.BoofTesting.primitive;

/**
 * @author Peter Abeles
 */
public abstract class CompareToImplThresholdImageOps extends CompareIdenticalFunctions {
	final int width = 100, height = 110;

	protected CompareToImplThresholdImageOps( Class testClass ) {
		super(testClass, ImplThresholdImageOps.class);
	}

	@Test
	void performTests() {
		performTests(12);
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		String name = candidate.getName();
		Class[] inputTypes = candidate.getParameterTypes();

		switch (name) {
			case "threshold":
				return threshold(inputTypes);

			case "localMean":
			case "localGaussian":
				return localThresh(inputTypes);
		}
		throw new RuntimeException("Unknown " + name + " " + inputTypes.length);
	}

	protected Object[][] threshold( Class[] inputTypes ) {

		return new Object[][]{
				threshold(inputTypes, true),
				threshold(inputTypes, false)};
	}

	private Object[] threshold( Class[] inputTypes, boolean down ) {
		Object[] inputs = new Object[4];

		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0], width, height, 1);
		GImageMiscOps.fillUniform(a, rand, 0, 200);

		inputs[0] = a;
		inputs[1] = new GrayU8(width, height);
		inputs[2] = primitive(110, inputTypes[2]);
		inputs[3] = down;
		return inputs;
	}

	protected Object[][] localThresh( Class[] inputTypes ) {
		return new Object[][]{
				localThresh(inputTypes, true),
				localThresh(inputTypes, false)};
	}

	private Object[] localThresh( Class[] inputTypes, boolean down ) {
		Object[] inputs = new Object[inputTypes.length];

		ImageBase a = GeneralizedImageOps.createImage(inputTypes[0], width, height, 1);
		GImageMiscOps.fillUniform(a, rand, 0, 200);

		inputs[0] = a;
		inputs[1] = new GrayU8(width, height);
		inputs[2] = ConfigLength.fixed(10);
		inputs[3] = primitive(0.9, inputTypes[3]);
		inputs[4] = down;
		if (inputs.length > 5)
			inputs[5] = GeneralizedImageOps.createImage(inputTypes[5], width, height, 1);
		if (inputs.length > 6)
			inputs[6] = GeneralizedImageOps.createImage(inputTypes[6], width, height, 1);
		return inputs;
	}
}
