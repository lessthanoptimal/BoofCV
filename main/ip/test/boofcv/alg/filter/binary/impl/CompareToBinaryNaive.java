/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CompareToBinaryNaive extends CompareIdenticalFunctions {

	protected Random rand = new Random(0xFF);

	protected int width = 20;
	protected int height = 30;

	public CompareToBinaryNaive(Class<?> testClass) {
		super(testClass, ImplBinaryNaiveOps.class);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		ImageUInt8 input = new ImageUInt8(width, height);
		ImageUInt8 output = new ImageUInt8(width, height);

		ImageMiscOps.fillUniform(input, rand, 0, 1);

		return new Object[][]{{input, output}};
	}
}
