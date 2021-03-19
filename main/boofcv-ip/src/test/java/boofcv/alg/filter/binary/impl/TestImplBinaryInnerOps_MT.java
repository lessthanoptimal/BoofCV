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
import boofcv.struct.image.GrayU8;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplBinaryInnerOps_MT extends CompareIdenticalFunctions {
	int width = 640, height = 480;

	protected TestImplBinaryInnerOps_MT() {
		super(ImplBinaryInnerOps_MT.class, ImplBinaryInnerOps.class);
	}

	@Test void performTests() {super.performTests(7);}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		GrayU8 input = new GrayU8(width, height);
		GrayU8 output = new GrayU8(width, height);

		GImageMiscOps.fillUniform(input, rand, 0, 1);
		GImageMiscOps.fillUniform(output, rand, 0, 1);

		return new Object[][]{{input, output}};
	}
}

