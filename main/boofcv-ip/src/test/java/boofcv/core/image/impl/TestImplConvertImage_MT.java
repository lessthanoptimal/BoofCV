/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.core.image.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.Planar;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

public class TestImplConvertImage_MT extends CompareIdenticalFunctions {

	private Random rand = new Random(234);
	private int width = 105;
	private int height = 100;


	TestImplConvertImage_MT() {
		super(ImplConvertImage_MT.class, ImplConvertImage.class);
	}

	@Test
	void performTests() {
		performTests(108);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class[] type = candidate.getParameterTypes();

		ImageBase input = null;
		ImageBase output = null;

		String name = candidate.getName();
		if( name.length() == "convertU8F32".length() ) {
			boolean firstU8 = name.indexOf("U8")==7;

			ImageDataType typeA = firstU8 ? ImageDataType.U8  : ImageDataType.F32;
			ImageDataType typeB = firstU8 ? ImageDataType.F32 : ImageDataType.U8;

			if (!type[0].isAssignableFrom(Planar.class)) {
				input = GeneralizedImageOps.createImage(type[0], width, height, 2);
			}
			if (!type[1].isAssignableFrom(Planar.class)) {
				output = GeneralizedImageOps.createImage(type[1], width, height, 2);
			}

			if (input == null) {
				input = new Planar(ImageDataType.typeToSingleClass(typeA), width, height, 2);
			} else if (output == null) {
				output = new Planar(ImageDataType.typeToSingleClass(typeB), width, height, 2);
			}

		} else {
			if (!type[0].isAssignableFrom(Planar.class)) {
				input = GeneralizedImageOps.createImage(type[0], width, height, 2);
			}
			if (!type[1].isAssignableFrom(Planar.class)) {
				output = GeneralizedImageOps.createImage(type[1], width, height, 2);
			}

			if (input == null) {
				ImageDataType dataType = output.imageType.getDataType();
				input = new Planar(ImageDataType.typeToSingleClass(dataType), width, height, 2);
			} else if (output == null) {
				ImageDataType dataType = input.imageType.getDataType();
				output = new Planar(ImageDataType.typeToSingleClass(dataType), width, height, 2);
			}

		}

		GImageMiscOps.fillUniform(input, rand, 0, 200);

		return new Object[][]{{input, output}};
	}
}

