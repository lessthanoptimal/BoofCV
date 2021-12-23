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

import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Generalized interface for filtering images with convolution kernels. Can invoke different
 * techniques for handling image borders.
 *
 * @author Peter Abeles
 */
public class GenericConvolve<Input extends ImageBase<Input>, Output extends ImageBase<Output>>
	implements ConvolveInterface<Input,Output>
{
	Method m;
	KernelBase kernel;
	BorderType type;
	@Nullable ImageBorder borderRule;

	ImageType<Input> inputType;
	ImageType<Output> outputType;

	public GenericConvolve(Method m, KernelBase kernel, BorderType type ,
						   ImageType<Input> inputType, ImageType<Output> outputType) {
		this.m = m;
		this.kernel = kernel;
		this.type = type;
		this.inputType = inputType;
		this.outputType = outputType;

		Class<?>[] params = m.getParameterTypes();
		if( type == BorderType.SKIP || type == BorderType.NORMALIZED )
			this.borderRule = null;
		else
			borderRule = FactoryImageBorder.single(type, (Class) params[1]);
	}

	@Override
	public void process(Input input, Output output) {
		try {
			if( kernel.getDimension() == 1 ) {
				switch (type) {
					case SKIP, NORMALIZED -> m.invoke(null, kernel, input, output);
					default -> m.invoke(null, kernel, input, output, Objects.requireNonNull(borderRule));
				}
			} else {
				switch (type) {
					case SKIP, NORMALIZED -> m.invoke(null, kernel, input, output);
					default -> m.invoke(null, kernel, input, output, Objects.requireNonNull(borderRule));
				}
			}
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int getBorderX() {
		if( type == BorderType.SKIP)
			return kernel.getRadius();
		else
			return 0;
	}

	@Override
	public BorderType getBorderType() {
		return type;
	}

	@Override
	public int getBorderY() {
		return getBorderX();
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
