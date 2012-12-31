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

package boofcv.abst.filter;

import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Turns functions into implementations of {@link FilterImageInterface} Wraps around any function which has two images as input and output.
 */
public class FilterImageReflection<Input extends ImageSingleBand, Output extends ImageSingleBand>
		implements FilterImageInterface<Input, Output> {

	// method being invoke for the filter
	Method m;

	// input and output image types
	Class<Input> inputType;
	Class<Output> outputType;
	// size of horizontal and vertical borders
	int borderX;
	int borderY;

	public FilterImageReflection(Class owner, String methodName, int borderX, int borderY, Class<Input> inputType, Class<Output> outputType) {
		this.m = BoofTesting.findMethod(owner, methodName, inputType, outputType);
		this.inputType = inputType;
		this.outputType = outputType;
		this.borderX = borderX;
		this.borderY = borderY;

		// sanity check
		Class param[] = m.getParameterTypes();
		if (param.length != 2)
			throw new IllegalArgumentException("Input method must have two inputs");
		if (!ImageSingleBand.class.isAssignableFrom(param[0]) || !ImageSingleBand.class.isAssignableFrom(param[0]))
			throw new IllegalArgumentException("TWo input parameters must be of type ImageSingleBand");
	}


	public FilterImageReflection(Method m, int borderX, int borderY, Class<Input> inputType, Class<Output> outputType) {
		this.m = m;
		this.inputType = inputType;
		this.outputType = outputType;
		this.borderX = borderX;
		this.borderY = borderY;

		// sanity check
		Class param[] = m.getParameterTypes();
		if (param.length != 2)
			throw new IllegalArgumentException("Input method must have two inputs");
		if (!ImageSingleBand.class.isAssignableFrom(param[0]) || !ImageSingleBand.class.isAssignableFrom(param[0]))
			throw new IllegalArgumentException("TWo input parameters must be of type ImageSingleBand");
	}

	@Override
	public void process(Input input, Output output) {
		if (input == null)
			throw new IllegalArgumentException("Input parameter is null");
		if (output == null)
			throw new IllegalArgumentException("Output parameter is null");

		try {
			m.invoke(null, input, output);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getHorizontalBorder() {
		return borderX;
	}

	@Override
	public int getVerticalBorder() {
		return borderY;
	}

	@Override
	public Class<Input> getInputType() {
		return inputType;
	}
}
