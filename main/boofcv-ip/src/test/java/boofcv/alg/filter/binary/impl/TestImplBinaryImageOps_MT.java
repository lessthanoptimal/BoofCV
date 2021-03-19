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
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class TestImplBinaryImageOps_MT extends CompareIdenticalFunctions {

	int width = 640, height = 480;

	protected TestImplBinaryImageOps_MT() {
		super(ImplBinaryImageOps_MT.class, ImplBinaryImageOps.class);
	}

	@Test
	void performTests() {
		super.performTests(7);
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class[] inputTypes = candidate.getParameterTypes();

		GrayU8 inputA = new GrayU8(width, height);
		GrayU8 inputB = new GrayU8(width, height);
		GrayU8 output = new GrayU8(width, height);

		GImageMiscOps.fillUniform(inputA, rand, 0, 1);
		GImageMiscOps.fillUniform(inputB, rand, 0, 1);
		GImageMiscOps.fillUniform(output, rand, 0, 1);

		switch (candidate.getName()) {
			case "logicAnd":
			case "logicOr":
			case "logicXor":
				return new Object[][]{{inputA, inputB, output}};

			case "invert":
				return new Object[][]{{inputA, output}};

			case "relabel": {
				GrayS32 labeled = new GrayS32(width, height);
				GImageMiscOps.fillUniform(labeled, rand, 0, 32);
				int[] table = new int[33];
				for (int i = 0; i < table.length; i++) {
					table[i] = rand.nextInt(table.length);
				}

				return new Object[][]{{labeled, table}};
			}

			case "labelToBinary": {
				GrayS32 labeled = new GrayS32(width, height);
				GImageMiscOps.fillUniform(labeled, rand, 0, 32);
				boolean[] selected = new boolean[33];
				for (int i = 0; i < selected.length; i++) {
					selected[i] = rand.nextBoolean();
				}

				if (inputTypes.length == 2)
					return new Object[][]{{labeled, output}};
				else {
					return new Object[][]{{labeled, output, selected}};
				}
			}

			default:
				throw new RuntimeException("Unknown function " + candidate.getName());
		}
	}
}

