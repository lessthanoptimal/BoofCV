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

package boofcv.factory.filter.convolve;

import boofcv.abst.filter.convolve.ConvolveInterface;
import boofcv.abst.filter.convolve.GenericConvolve;
import boofcv.alg.filter.convolve.ConvolveImageNoBorder;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.filter.convolve.ConvolveWithBorder;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;

import java.lang.reflect.Method;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
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
	public static <Input extends ImageGray, Output extends ImageGray>
	ConvolveInterface<Input,Output>
	convolve( Kernel1D kernel, Class<Input> inputType, Class<Output> outputType , BorderType border , boolean isHorizontal )
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		Class<?> borderClassType = FactoryImageBorder.lookupBorderClassType((Class)inputType);
		String direction = isHorizontal ? "horizontal" : "vertical";
		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveImageNoBorder.class.getMethod(direction, kernel.getClass(), inputType, outputType);
					break;

				case EXTENDED:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,direction,kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case REFLECT:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,direction,kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case WRAP:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,direction,kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case NORMALIZED:
					m = ConvolveNormalized.class.getMethod(direction,kernel.getClass(),inputType,outputType);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
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
	public static <Input extends ImageGray, Output extends ImageGray>
	ConvolveInterface<Input,Output>
	convolve( Kernel2D kernel, Class<Input> inputType, Class<Output> outputType , BorderType borderType)
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		Class<?> borderClassType = FactoryImageBorder.lookupBorderClassType((Class)inputType);
		Method m;
		try {
			switch(borderType) {
				case SKIP:
					m = ConvolveImageNoBorder.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType);
					break;

				case EXTENDED:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,"convolve",kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case REFLECT:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,"convolve",kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case WRAP:
					m = BoofTesting.findMethod(ConvolveWithBorder.class,"convolve",kernel.getClass(),inputType,outputType,borderClassType);
					break;

				case NORMALIZED:
					m = ConvolveNormalized.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+ borderType);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolve<>(m, kernel, borderType, inputType, outputType);
	}
}
