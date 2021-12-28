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

package boofcv.abst.filter.convolve;

import boofcv.struct.border.BorderType;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Generalized interface for filtering images with convolution kernels while skipping pixels.
 * Can invoke different techniques for handling image borders. The first pixel sampled is always (0,0) and the
 * sampled pixels are (x*skip,y*skip).
 *
 * @author Peter Abeles
 */
public class GenericConvolveDown<Input extends ImageBase<Input>, Output extends ImageBase<Output>>
		implements ConvolveDown<Input, Output> {
	Method m;
	KernelBase kernel;
	BorderType type;
	int skip;
	ImageType<Input> inputType;
	ImageType<Output> outputType;

	public GenericConvolveDown( Method m, KernelBase kernel,
								BorderType type, int skip,
								ImageType<Input> inputType,
								ImageType<Output> outputType ) {
		this.m = m;
		this.kernel = kernel;
		this.type = type;
		this.skip = skip;
		this.inputType = inputType;
		this.outputType = outputType;
	}

	@Override
	public int getSkip() {
		return skip;
	}

	@Override
	public void setSkip( int skip ) {
		this.skip = skip;
	}

	@Override
	public void process( Input input, Output output ) {
		try {
			m.invoke(null, kernel, input, output, skip);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getBorderX() {
		if (type == BorderType.SKIP)
			return kernel.getRadius();
		else
			return 0;
	}

	@Override
	public int getBorderY() {
		return getBorderX();
	}

	@Override
	public BorderType getBorderType() {
		return type;
	}

	@Override
	public ImageType<Input> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<Output> getOutputType() {
		return outputType;
	}
}
