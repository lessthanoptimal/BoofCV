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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplColorHsv_MT extends CompareIdenticalFunctions {
	int width = 70,height=80;

	TestImplColorHsv_MT() {
		super(ImplColorHsv_MT.class,ImplColorHsv.class);
	}

	@Test
	void performTests() {
		performTests(2);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] parameters = new Object[types.length];

		switch( candidate.getName() ) {
			case "hsvToRgb_F32":
			case "rgbToHsv_F32":
				parameters[0] = new Planar<>(GrayF32.class,width,height,3);
				parameters[1] = new Planar<>(GrayF32.class,width,height,3);
				break;
		}

		GImageMiscOps.fillUniform((ImageBase)parameters[0],rand,0,100);

		return new Object[][]{parameters};
	}
}

