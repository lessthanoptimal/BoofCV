/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.convolve.ConvolveDown;
import boofcv.abst.filter.convolve.GenericConvolveDown;
import boofcv.abst.filter.convolve.PlanarConvolveDown;
import boofcv.alg.filter.convolve.ConvolveImageDownNoBorder;
import boofcv.alg.filter.convolve.ConvolveImageDownNormalized;
import boofcv.core.image.border.BorderType;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofTesting;

import java.lang.reflect.Method;


/**
 * Factory class for creating abstracted convolve down filters.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryConvolveDown {

	public static <In extends ImageBase<In>, Out extends ImageBase<Out>>
	ConvolveDown<In,Out> convolve(Kernel1D kernel, BorderType border ,
								   boolean isHorizontal , int skip , ImageType<In> inputType , ImageType<Out> outputType )
	{
		switch( inputType.getFamily() ) {
			case PLANAR:
				return convolvePL(kernel, border, isHorizontal, skip, inputType.getNumBands(),
						inputType.getImageClass(), outputType.getImageClass());
			case GRAY:
				return convolveSB(kernel, border, isHorizontal, skip, inputType.getImageClass(), outputType.getImageClass());

			case INTERLEAVED:
				throw new IllegalArgumentException("Interleaved images are not yet supported");
		}
		throw new IllegalArgumentException("Unknown image type");
	}

	public static <In extends ImageBase<In>, Out extends ImageBase<Out>>
	ConvolveDown<In,Out> convolve(Kernel2D kernel, BorderType border ,
								  int skip , ImageType<In> inputType , ImageType<Out> outputType )
	{
		switch( inputType.getFamily() ) {
			case PLANAR:
				return convolvePL(kernel, border, skip, inputType.getNumBands(),
						inputType.getImageClass(), outputType.getImageClass());
			case GRAY:
				return convolveSB(kernel, border, skip, inputType.getImageClass(), outputType.getImageClass());

			case INTERLEAVED:
				throw new IllegalArgumentException("Interleaved images are not yet supported");
		}
		throw new IllegalArgumentException("Unknown image type");
	}

	/**
	 * Creates a filter for convolving 1D kernels along the image.
	 *
	 * @param kernel Convolution kernel.
	 * @param border How the image border is handled.
	 * @param inputType Specifies input image type.
	 * @param outputType Specifies input image type.
	 * @return FilterInterface which will perform the specified convolution.
	 */
	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	GenericConvolveDown<Input,Output>
	convolveSB(Kernel1D kernel, BorderType border, boolean isHorizontal, int skip, Class<Input> inputType, Class<Output> outputType)
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		String direction = isHorizontal ? "horizontal" : "vertical";
		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveImageDownNoBorder.class.
							getMethod(direction,kernel.getClass(),inputType,outputType,int.class);
					break;

				case EXTENDED:
					throw new IllegalArgumentException("Extended border is currently not supported.");

				case NORMALIZED:
					m = ConvolveImageDownNormalized.class.
							getMethod(direction,kernel.getClass(),inputType,outputType,int.class);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolveDown<>(m, kernel, border, skip, ImageType.single(inputType), ImageType.single(outputType));
	}

	/**
	 * Creates a filter for convolving 2D kernels along the image axis.
	 *
	 * @param kernel Convolution kernel.
	 * @param border How the image border is handled.
	 * @param inputType Specifies input image type.
	 * @param outputType Specifies input image type.
	 * @return FilterInterface which will perform the specified convolution.
	 */
	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	GenericConvolveDown<Input,Output>
	convolveSB(Kernel2D kernel, BorderType border, int skip, Class<Input> inputType, Class<Output> outputType)
	{
		outputType = BoofTesting.convertToGenericType(outputType);

		Method m;
		try {
			switch( border ) {
				case SKIP:
					m = ConvolveImageDownNoBorder.class.getMethod(
							"convolve",kernel.getClass(),inputType,outputType,int.class);
					break;

				case EXTENDED:
					throw new IllegalArgumentException("Extended border is currently not supported.");

				case NORMALIZED:
					m = ConvolveImageDownNormalized.class.getMethod(
							"convolve",kernel.getClass(),inputType,outputType,int.class);
					break;

				default:
					throw new IllegalArgumentException("Unknown border type "+border);

			}
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("The specified convolution cannot be found");
		}

		return new GenericConvolveDown<>(m, kernel, border, skip, ImageType.single(inputType), ImageType.single(outputType));
	}

	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	ConvolveDown<Planar<Input>,Planar<Output>>
	convolvePL(Kernel1D kernel, BorderType border, boolean isHorizontal, int skip,
			   int numBands, Class<Input> inputType, Class<Output> outputType)
	{
		ConvolveDown<Input,Output> grayDown = convolveSB(kernel, border, isHorizontal, skip, inputType, outputType);
		return new PlanarConvolveDown<>(grayDown, numBands);
	}

	public static <Input extends ImageGray<Input>, Output extends ImageGray<Output>>
	ConvolveDown<Planar<Input>,Planar<Output>>
	convolvePL(Kernel2D kernel, BorderType border, int skip,
			   int numBands, Class<Input> inputType, Class<Output> outputType)
	{
		ConvolveDown<Input,Output> grayDown = convolveSB(kernel, border, skip, inputType, outputType);
		return new PlanarConvolveDown<>(grayDown, numBands);
	}
}
