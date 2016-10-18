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

import boofcv.abst.filter.convolve.GenericConvolveDown;
import boofcv.alg.filter.convolve.ConvolveDownNoBorder;
import boofcv.alg.filter.convolve.ConvolveDownNormalized;
import boofcv.core.image.border.BorderType;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;

import java.lang.reflect.Method;


/**
 * Factory class for creating abstracted convolve down filters.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryConvolveDown {

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
	GenericConvolveDown<Input,Output>
	convolve( Kernel1D kernel, Class<Input> inputType, Class<Output> outputType , BorderType border ,
			  boolean isHorizontal , int skip )
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		String direction = isHorizontal ? "horizontal" : "vertical";
		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveDownNoBorder.class.
							getMethod(direction,kernel.getClass(),inputType,outputType,int.class);
					break;

				case EXTENDED:
					throw new IllegalArgumentException("Extended border is currently not supported.");

				case NORMALIZED:
					m = ConvolveDownNormalized.class.
							getMethod(direction,kernel.getClass(),inputType,outputType,int.class);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolveDown<>(m, kernel, border, skip, inputType, outputType);
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
	public static <Input extends ImageGray, Output extends ImageGray>
	GenericConvolveDown<Input,Output>
	convolve( Kernel2D kernel, Class<Input> inputType, Class<Output> outputType , BorderType border , int skip )
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveDownNoBorder.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType,int.class);
					break;

				case EXTENDED:
					throw new IllegalArgumentException("Extended border is currently not supported.");

				case NORMALIZED:
					m = ConvolveDownNormalized.class.
							getMethod("convolve",kernel.getClass(),inputType,outputType,int.class);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolveDown<>(m, kernel, border, skip, inputType, outputType);
	}
}
