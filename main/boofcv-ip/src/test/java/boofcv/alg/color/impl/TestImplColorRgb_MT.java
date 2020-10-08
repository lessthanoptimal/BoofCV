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

package boofcv.alg.color.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplColorRgb_MT extends CompareIdenticalFunctions {
	int width = 70,height=80;

	TestImplColorRgb_MT() {
		super(ImplColorRgb_MT.class,ImplColorRgb.class);
	}

	@Test
	void performTests() {
		performTests(6);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] parameters = new Object[types.length];

		if( types[0] == Planar.class ) {
			parameters[0] = new Planar<>(types[1],width,height,3);
		} else {
			parameters[0] = GeneralizedImageOps.createImage(types[0],width,height,3);
		}
		GImageMiscOps.fillUniform((ImageBase)parameters[0],rand,0,100);
		parameters[1] = GeneralizedImageOps.createSingleBand(types[1],width,height);

		return new Object[][]{parameters};
	}
}

