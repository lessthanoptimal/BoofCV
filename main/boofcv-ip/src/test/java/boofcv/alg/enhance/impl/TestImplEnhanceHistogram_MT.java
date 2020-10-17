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
import boofcv.struct.image.ImageType;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplEnhanceHistogram_MT extends CompareIdenticalFunctions {
	int width = 70,height=80;

	public TestImplEnhanceHistogram_MT() {
		super(ImplEnhanceHistogram_MT.class, ImplEnhanceHistogram.class);
	}

	@Test
	void performTests() {
		performTests(13);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		String name = m.getName();
		// skip these methods because how CompareIdenticalFunctions compares methods doesn't work in this case
		return super.isTestMethod(m) && !name.equals("localHistogram");
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] parameters = new Object[types.length];

		ImageGray input = GeneralizedImageOps.createSingleBand(types[0],width,height);
		if( input.getDataType().isSigned() )
			GImageMiscOps.fillUniform(input,rand,-100,100);
		else
			GImageMiscOps.fillUniform(input,rand,0,200);

		parameters[0] = input;

		switch( candidate.getName() ) {
			case "applyTransform":
				parameters[1] = rarray(256);
				if( types.length == 3 ) {
					parameters[2] = GeneralizedImageOps.createSingleBand(types[2], width, height);
				} else {
					parameters[2] = BoofTesting.primitive(-100,types[2]);
					parameters[3] = GeneralizedImageOps.createSingleBand(types[3], width, height);
				}
				break;
			case "equalizeLocalNaive":
			case "equalizeLocalInner":
				parameters[1] = BoofTesting.primitive(2,types[1]);
				parameters[2] = BoofTesting.primitive(256,types[2]);
				parameters[3] = GeneralizedImageOps.createSingleBand(types[3],width,height);
				parameters[4] = GeneralizedImageOps.createGrowArray(ImageType.single(types[3]));
				break;
			case "equalizeLocalRow":
			case "equalizeLocalCol":
				parameters[1] = BoofTesting.primitive(2,types[1]);
				parameters[2] = BoofTesting.primitive(256,types[2]);
				parameters[3] = BoofTesting.primitive(2,types[3]);
				parameters[4] = GeneralizedImageOps.createSingleBand(types[4],width,height);
				parameters[5] = GeneralizedImageOps.createGrowArray(ImageType.single(types[4]));
				break;
		}

		return new Object[][]{parameters};
	}

	private int[] rarray( int length ) {
		int[] d = new int[length];
		for (int i = 0; i < length; i++) {
			d[i] = rand.nextInt(length);
		}
		return d;
	}
}

