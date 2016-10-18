/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.derivative;

import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation of {@link GradientMultiToSingleBand} which uses reflection to invoke static
 * functions.
 *
 * @author Peter Abeles
 */
public class GradientMultiToSingleBand_Reflection<Input extends ImageMultiBand, Output extends ImageGray>
	implements GradientMultiToSingleBand<Input,Output>
{
	private Method m;
	ImageType<Input> inputType;
	Class<Output> outputType;

	public GradientMultiToSingleBand_Reflection(Method m,
												ImageType<Input> inputType,
												Class<Output> outputType) {
		this.m = m;
		this.inputType = inputType;
		this.outputType = outputType;
	}

	@Override
	public void process(Input inDerivX, Input inDerivY, Output outDerivX, Output outDerivY) {
		try {
			m.invoke(null,inDerivX, inDerivY, outDerivX, outDerivY);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ImageType<Input> getInputType() {
		return inputType;
	}

	@Override
	public Class<Output> getOutputType() {
		return outputType;
	}
}
