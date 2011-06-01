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

package gecv.abst.filter.convolve;

import gecv.abst.filter.FilterImageInterface;
import gecv.alg.filter.convolve.ConvolveExtended;
import gecv.alg.filter.convolve.ConvolveImageNoBorder;
import gecv.alg.filter.convolve.ConvolveNormalized;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;

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
	public static <Input extends ImageBase, Output extends ImageBase>
	FilterImageInterface<Input,Output>
	convolve( Kernel1D kernel, Class<Input> inputType, Class<Output> outputType , BorderType border , boolean isHorizontal )
	{
		outputType = GecvTesting.convertToGenericType(outputType);

		String direction = isHorizontal ? "horizontal" : "vertical";
		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveImageNoBorder.class.
							getMethod(direction,kernel.getClass(),inputType,outputType,boolean.class);
					break;

				case EXTENDED:
					m = ConvolveExtended.class.
							getMethod(direction,kernel.getClass(),inputType,outputType);
					break;

				case NORMALIZED:
					m = ConvolveNormalized.class.
							getMethod(direction,kernel.getClass(),inputType,outputType);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolve<Input,Output>(m,kernel,border);
	}

	/**
	 * Creates a filter for convolving 2D kernels along the image axis.
	 *
	 * @param kernel Convolution kernel.
	 * @param inputType Specifies input image type.
	 * @param outputType Specifies input image type.
	 * @param border How the image border is handled.
	 * @return FilterInterface which will perform the specified convolution.
	 */
	public static <Input extends ImageBase, Output extends ImageBase>
	FilterImageInterface<Input,Output>
	convolve( Kernel2D kernel, Class<Input> inputType, Class<Output> outputType , BorderType border )
	{
		outputType = GecvTesting.convertToGenericType(outputType);

		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveImageNoBorder.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType);
					break;

				case EXTENDED:
					m = ConvolveExtended.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType);
					break;

				case NORMALIZED:
					m = ConvolveNormalized.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolve<Input,Output>(m,kernel,border);
	}
}
