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

package boofcv.factory.filter.derivative;

import boofcv.abst.filter.derivative.*;
import boofcv.alg.filter.derivative.*;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.struct.image.*;

import java.lang.reflect.Method;

/**
 * <p>
 * Factory for creating different types of {@link boofcv.abst.filter.derivative.ImageGradient}, which are used to compute
 * the image's derivative.
 * </p>
 *
 * @author Peter Abeles
 */
public class FactoryDerivative {

	/**
	 * Computes the image gradient inside a multi-band image then reduces the output to a single
	 * band before returning the results
	 *
	 * @param gradient Computes the multi-band image gradient
	 * @param type Specifies which method is to be used to reduce the output into single band image
	 * @param outputType Type of output image
	 * @param <I> Input type
	 * @param <M> Intermediate type
	 * @param <D> Output type
	 * @return Gradient
	 */
	public static <I extends ImageMultiBand, M extends ImageMultiBand, D extends ImageGray>
	ImageGradient<I,D> gradientReduce( ImageGradient<I,M> gradient ,
									   DerivativeReduceType type,
									   Class<D> outputType )
	{

		String name;

		switch( type ) {
			case MAX_F: name = "maxf"; break;
			default:
				throw new RuntimeException("Unknown reduce type "+type);
		}

		Class middleType;
		switch( gradient.getDerivativeType().getFamily()) {
			case PLANAR:
				middleType = Planar.class;
				break;

			case GRAY:
				throw new IllegalArgumentException("Can't have gradient output be single band");

			default:
				middleType = gradient.getDerivativeType().getImageClass();
		}


		Method m = findReduce(name,middleType, outputType);
		GradientMultiToSingleBand_Reflection<M,D> reducer =
				new GradientMultiToSingleBand_Reflection<>(m, gradient.getDerivativeType(), outputType);

		return new ImageGradientThenReduce<>(gradient, reducer);
	}

	/**
	 * Returns the gradient for single band images of the specified type
	 * @param type Type of gradient
	 * @param inputType Type of input image
	 * @param derivType Type of gradient image.  null for default
	 * @param <I> Input image
	 * @param <D> Derivative image
	 * @return gradient filter
	 */
	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> gradientSB( DerivativeType type , Class<I> inputType , Class<D> derivType )
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);

		Class which;

		switch( type ) {
			case PREWITT:
				which = GradientPrewitt.class;
				break;

			case SOBEL:
				which = GradientSobel.class;
				break;

			case THREE:
				which = GradientThree.class;
				break;

			case TWO_0:
				which = GradientTwo0.class;
				break;

			case TWO_1:
				which = GradientTwo1.class;
				break;

			default:
				throw new IllegalArgumentException("Unknown type "+type);
		}

		Method m = findDerivative(which,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	/**
	 * Filters for computing the gradient of {@link Planar} images.
	 *
	 * @param type Which gradient to compute
	 * @param numBands Number of bands in the image
	 * @param inputType Type of data on input
	 * @param derivType Type of data on output (null for default)
	 * @param <I> Image type
	 * @param <D> Derivative type
	 * @return the filter
	 */
	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<Planar<I>,Planar<D>>
	gradientPL(DerivativeType type , int numBands , Class<I> inputType , Class<D> derivType )
	{
		ImageGradient<I,D> g = gradientSB(type,inputType,derivType);
		return new ImageGradient_PL<>(g, numBands);
	}


	public static <I extends ImageBase, D extends ImageBase>
	ImageGradient<I,D>
	gradient(DerivativeType type , ImageType<I> inputType , ImageType<D> derivType )
	{
		if( derivType != null ) {
			if( inputType.getFamily() != derivType.getFamily() )
				throw new IllegalArgumentException("input and output must be of the same family");
		}

		switch( inputType.getFamily() ) {
			case GRAY: {
				Class derivClass = derivType != null ? derivType.getImageClass() : null;
				return gradientSB(type,inputType.getImageClass(),derivClass);
			}

			case PLANAR: {
				int numBands = inputType.getNumBands();
				Class derivClass = derivType != null ? derivType.getImageClass() : null;
				return gradientPL(type,numBands,inputType.getImageClass(),derivClass);
			}

			case INTERLEAVED:
				throw new IllegalArgumentException("INTERLEAVED images not yet supported");

			default:
				throw new IllegalArgumentException("Unknown image type");
		}
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> prewitt( Class<I> inputType , Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);

		Method m = findDerivative(GradientPrewitt.class,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> sobel( Class<I> inputType , Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);

		Method m = findDerivative(GradientSobel.class,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> three( Class<I> inputType , Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		Method m = findDerivative(GradientThree.class,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> two0(Class<I> inputType, Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		Method m = findDerivative(GradientTwo0.class,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> two1(Class<I> inputType, Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		Method m = findDerivative(GradientTwo1.class,inputType,derivType);
		return new ImageGradient_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageHessianDirect<I,D> hessianDirectThree( Class<I> inputType , Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		Method m = findHessian(HessianThree.class,inputType,derivType);
		return new ImageHessianDirect_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageHessianDirect<I,D> hessianDirectSobel( Class<I> inputType , Class<D> derivType)
	{
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		Method m = findHessian(HessianSobel.class,inputType,derivType);
		return new ImageHessianDirect_Reflection<>(m);
	}

	public static <D extends ImageGray>
	ImageHessian<D> hessian( Class<?> gradientType , Class<D> derivType ) {
		Method m = findHessianFromGradient(gradientType,derivType);
		return new ImageHessian_Reflection<>(m);
	}

	public static <I extends ImageGray, D extends ImageGray>
	ImageGradient<I,D> gaussian( double sigma , int radius , Class<I> inputType , Class<D> derivType) {
		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		return new ImageGradient_Gaussian<>(sigma, radius, inputType, derivType);
	}

	public static <D extends ImageGray> ImageHessian<D> hessianSobel(Class<D> derivType ) {
		if( derivType == GrayF32.class )
			return (ImageHessian<D>)hessian(GradientSobel.class,GrayF32.class);
		else if( derivType == GrayS16.class )
			return (ImageHessian<D>)hessian(GradientSobel.class,GrayS16.class);
		else
			throw new IllegalArgumentException("Not supported yet");
	}

	public static <D extends ImageGray> ImageHessian<D> hessianPrewitt(Class<D> derivType ) {
		if( derivType == GrayF32.class )
			return (ImageHessian<D>)hessian(GradientPrewitt.class,GrayF32.class);
		else if( derivType == GrayS16.class )
			return (ImageHessian<D>)hessian(GradientPrewitt.class,GrayS16.class);
		else
			throw new IllegalArgumentException("Not supported yet");
	}

	public static <D extends ImageGray> ImageHessian<D> hessianThree(Class<D> derivType ) {
		if( derivType == GrayF32.class )
			return (ImageHessian<D>)hessian(GradientThree.class,GrayF32.class);
		else if( derivType == GrayS16.class )
			return (ImageHessian<D>)hessian(GradientThree.class,GrayS16.class);
		else
			throw new IllegalArgumentException("Not supported yet");
	}

	private static Method findReduce( String name , Class<?> inputType , Class<?> derivType  ) {
		Method m;
		try {
			m = GradientReduceToSingle.class.getDeclaredMethod(name, inputType,inputType,derivType,derivType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Input and derivative types are probably not compatible",e);
		}
		return m;
	}

	private static Method findDerivative(Class<?> derivativeClass,
										 Class<?> inputType , Class<?> derivType ) {
		Method m;
		try {
			Class<?> borderType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageBorder_F32.class : ImageBorder_S32.class;
			m = derivativeClass.getDeclaredMethod("process", inputType,derivType,derivType,borderType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Input and derivative types are probably not compatible",e);
		}
		return m;
	}

	private static Method findHessian(Class<?> derivativeClass,
										Class<?> inputType , Class<?> derivType ) {
		Method m;
		try {
			Class<?> borderType = GeneralizedImageOps.isFloatingPoint(inputType) ? ImageBorder_F32.class : ImageBorder_S32.class;
			m = derivativeClass.getDeclaredMethod("process", inputType,derivType,derivType,derivType,borderType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Input and derivative types are probably not compatible",e);
		}
		return m;
	}

	private static Method findHessianFromGradient(Class<?> derivativeClass, Class<?> imageType ) {
		String name = derivativeClass.getSimpleName().substring(8);
		Method m;
		try {
			Class<?> borderType = GeneralizedImageOps.isFloatingPoint(imageType) ? ImageBorder_F32.class : ImageBorder_S32.class;
			m = HessianFromGradient.class.getDeclaredMethod("hessian"+name, imageType,imageType,imageType,imageType,imageType,borderType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Input and derivative types are probably not compatible",e);
		}
		return m;
	}
}
