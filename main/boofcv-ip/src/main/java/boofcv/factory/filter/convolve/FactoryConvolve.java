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

package boofcv.factory.filter.convolve;

import boofcv.BoofTesting;
import boofcv.abst.filter.convolve.ConvolveInterface;
import boofcv.abst.filter.convolve.GenericConvolve;
import boofcv.alg.filter.convolve.ConvolveImage;
import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import java.lang.reflect.Method;

/**
 * Factory for {@link ConvolveInterface}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes", "NullAway"})
public class FactoryConvolve {

	/**
	 * Creates a filter for convolving 1D kernels along the image.
	 *
	 * @param kernel Convolution kernel.
	 * @param inputType Specifies input image type.
	 * @param outputType Specifies input image type.
	 * @param border How the image border is handled.
	 * @return FilterInterface which will perform the specified convolution.
	 */
	public static <Input extends ImageBase<Input>, Output extends ImageBase<Output>>
	ConvolveInterface<Input, Output>
	convolve( Kernel1D kernel, ImageType<Input> inputType, ImageType<Output> outputType, BorderType border, boolean isHorizontal ) {
		if (inputType.getFamily() != ImageType.Family.GRAY)
			throw new IllegalArgumentException("Currently only gray scale image supported");

		Class _inputType = inputType.getImageClass();
		Class _outputType = outputType == null ? null : outputType.getImageClass();

		_outputType = BoofTesting.convertToGenericType(_outputType);

		Class<?> borderClassType = FactoryImageBorder.lookupBorderClassType(_inputType);
		String direction = isHorizontal ? "horizontal" : "vertical";
		Method m;
		try {
			m = switch (border) {
				case SKIP -> ConvolveImageNoBorder.class.getMethod(direction, kernel.getClass(), _inputType, _outputType);
				case EXTENDED -> BoofTesting.findMethod(ConvolveImage.class, direction, kernel.getClass(), _inputType, _outputType, borderClassType);
				case REFLECT -> BoofTesting.findMethod(ConvolveImage.class, direction, kernel.getClass(), _inputType, _outputType, borderClassType);
				case WRAP -> BoofTesting.findMethod(ConvolveImage.class, direction, kernel.getClass(), _inputType, _outputType, borderClassType);
				case NORMALIZED -> ConvolveImageNormalized.class.getMethod(direction, kernel.getClass(), _inputType, _outputType);
				default -> throw new IllegalArgumentException("Unknown border type " + border);
			};
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolve<>(m, kernel, border, inputType, outputType);
	}

	/**
	 * Creates a filter for convolving 2D kernels along the image axis.
	 *
	 * @param kernel Convolution kernel.
	 * @param inputType Specifies input image type.
	 * @param outputType Specifies input image type.
	 * @param borderType How the image border is handled.
	 * @return FilterInterface which will perform the specified convolution.
	 */
	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	ConvolveInterface<Input, Output>
	convolve( Kernel2D kernel, Class<Input> inputType, Class<Output> outputType, BorderType borderType ) {
		outputType = BoofTesting.convertToGenericType(outputType);

		Class<?> borderClassType = FactoryImageBorder.lookupBorderClassType((Class)inputType);
		Method m;
		try {
			m = switch (borderType) {
				case SKIP -> ConvolveImageNoBorder.class.
						getMethod("convolve", kernel.getClass(), inputType, outputType);
				case EXTENDED -> BoofTesting.findMethod(ConvolveImage.class, "convolve", kernel.getClass(), inputType, outputType, borderClassType);
				case REFLECT -> BoofTesting.findMethod(ConvolveImage.class, "convolve", kernel.getClass(), inputType, outputType, borderClassType);
				case WRAP -> BoofTesting.findMethod(ConvolveImage.class, "convolve", kernel.getClass(), inputType, outputType, borderClassType);
				case NORMALIZED -> ConvolveImageNormalized.class.
						getMethod("convolve", kernel.getClass(), inputType, outputType);
				default -> throw new IllegalArgumentException("Unknown border type " + borderType);
			};
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolve<>(m, kernel, borderType, ImageType.single(inputType), ImageType.single(outputType));
	}
}
