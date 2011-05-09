/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.basic.impl;

import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.struct.image.ImageInt8;
import gecv.testing.CompareIdenticalFunctions;

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
		super(testClass, BinaryNaiveOps.class);
	}

	@Override
	protected Object[][] createInputParam(Method m) {

		ImageInt8 input = new ImageInt8(width, height);
		ImageInt8 output = new ImageInt8(width, height);

		BasicDrawing_I8.randomize(input, rand, 0, 1);

		return new Object[][]{{input, output}};
	}
}
