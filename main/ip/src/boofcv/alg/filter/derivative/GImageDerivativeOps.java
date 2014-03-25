/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.ImageGenerator;
import boofcv.core.image.border.*;
import boofcv.core.image.inst.FactoryImageGenerator;
import boofcv.struct.image.*;


/**
 * Generalized operations related to compute different image derivatives.
 *
 * @author Peter Abeles
 */
public class GImageDerivativeOps {

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void laplace( I input , D output ) {
		if( input instanceof ImageFloat32 ) {
			LaplacianEdge.process((ImageFloat32)input,(ImageFloat32)output);
		} else if( input instanceof ImageUInt8 ) {
			LaplacianEdge.process((ImageUInt8)input,(ImageSInt16)output);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());

		}
	}

	/**
	 * Returns the type of image the derivative should be for the specified input type.
	 * @param imageType Input image type.
	 * @return Appropriate output image type.
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
		Class<D> getDerivativeType( Class<I> imageType ) {
		if( imageType == ImageFloat32.class ) {
			return (Class<D>)ImageFloat32.class;
		} else if( imageType == ImageUInt8.class ) {
			return (Class<D>) ImageSInt16.class;
		} else if( imageType == ImageUInt16.class ) {
			return (Class<D>) ImageSInt32.class;
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+imageType.getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void sobel( I input , D derivX , D derivY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			GradientSobel.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			GradientSobel.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else if( input instanceof ImageSInt16 ) {
			GradientSobel.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void prewitt( I input , D derivX , D derivY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			GradientPrewitt.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			GradientPrewitt.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else if( input instanceof ImageSInt16 ) {
			GradientPrewitt.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void three( I input , D derivX , D derivY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			GradientThree.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			GradientThree.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else if( input instanceof ImageSInt16 ) {
			GradientThree.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void two( I input , D derivX , D derivY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			GradientTwo.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			GradientTwo.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else if( input instanceof ImageSInt16 ) {
			GradientTwo.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void hessianSobel( I input , D derivXX , D derivYY , D derivXY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			HessianSobel.process((ImageFloat32)input,(ImageFloat32)derivXX,(ImageFloat32)derivYY,(ImageFloat32)derivXY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			HessianSobel.process((ImageUInt8)input,(ImageSInt16)derivXX,(ImageSInt16)derivYY,(ImageSInt16)derivXY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void hessianThree( I input , D derivXX , D derivYY , D derivXY , BorderType borderType )
	{
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		if( input instanceof ImageFloat32 ) {
			HessianThree.process((ImageFloat32)input,(ImageFloat32)derivXX,(ImageFloat32)derivYY,(ImageFloat32)derivXY,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			HessianThree.process((ImageUInt8)input,(ImageSInt16)derivXX,(ImageSInt16)derivYY,(ImageSInt16)derivXY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
		}
	}

	public static <D extends ImageSingleBand>
	void hessianSobel( D inputDerivX , D inputDerivY , D derivXX , D derivYY , D derivXY , BorderType borderType )
	{
		ImageBorder<D> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(inputDerivX,borderType);

		if( inputDerivX instanceof ImageFloat32 ) {
			HessianFromGradient.hessianSobel((ImageFloat32)inputDerivX,(ImageFloat32)inputDerivY,(ImageFloat32)derivXX,(ImageFloat32)derivYY,(ImageFloat32)derivXY,(ImageBorder_F32)border);
		} else if( inputDerivX instanceof ImageSInt16 ) {
			HessianFromGradient.hessianSobel((ImageSInt16)inputDerivX,(ImageSInt16)inputDerivY,(ImageSInt16)derivXX,(ImageSInt16)derivYY,(ImageSInt16)derivXY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+inputDerivX.getClass().getSimpleName());
		}
	}

	public static <D extends ImageSingleBand>
	void hessianPrewitt( D inputDerivX , D inputDerivY , D derivXX , D derivYY , D derivXY , BorderType borderType )
	{
		ImageBorder<D> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(inputDerivX,borderType);

		if( inputDerivX instanceof ImageFloat32 ) {
			HessianFromGradient.hessianPrewitt((ImageFloat32)inputDerivX,(ImageFloat32)inputDerivY,(ImageFloat32)derivXX,(ImageFloat32)derivYY,(ImageFloat32)derivXY,(ImageBorder_F32)border);
		} else if( inputDerivX instanceof ImageSInt16 ) {
			HessianFromGradient.hessianPrewitt((ImageSInt16)inputDerivX,(ImageSInt16)inputDerivY,(ImageSInt16)derivXX,(ImageSInt16)derivYY,(ImageSInt16)derivXY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+inputDerivX.getClass().getSimpleName());
		}
	}

	public static <D extends ImageSingleBand>
	void hessianThree( D inputDerivX , D inputDerivY , D derivXX , D derivYY , D derivXY , BorderType borderType )
	{
		ImageBorder<D> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(inputDerivX,borderType);

		if( inputDerivX instanceof ImageFloat32 ) {
			HessianFromGradient.hessianThree((ImageFloat32)inputDerivX,(ImageFloat32)inputDerivY,(ImageFloat32)derivXX,(ImageFloat32)derivYY,(ImageFloat32)derivXY,(ImageBorder_F32)border);
		} else if( inputDerivX instanceof ImageSInt16 ) {
			HessianFromGradient.hessianThree((ImageSInt16)inputDerivX,(ImageSInt16)inputDerivY,(ImageSInt16)derivXX,(ImageSInt16)derivYY,(ImageSInt16)derivXY,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+inputDerivX.getClass().getSimpleName());
		}
	}

	/**
	 * <p>
	 * Creates an {@link boofcv.abst.filter.derivative.AnyImageDerivative} for use when processing scale space images.
	 * </p>
	 *
	 * <p>
	 * The derivative is calculating using a kernel which does not involve any additional blurring.
	 * Using a Gaussian kernel is equivalent to blurring the image an additional time then computing the derivative
	 * Other derivatives such as Sobel and Prewitt also blur the image.   Image bluing has already been done
	 * once before the derivative is computed.
	 * </p>
	 *
	 * @param inputType Type of input image.
	 * @param derivGen Generator for derivative images.
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	AnyImageDerivative<I,D> createDerivatives( Class<I> inputType , ImageGenerator<D> derivGen ) {

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputType);

		return new AnyImageDerivative<I,D>(GradientThree.getKernelX(isInteger),inputType,derivGen);
	}

	/**
	 * <p>
	 * See {@link #createDerivatives(Class, boofcv.core.image.ImageGenerator)}.
	 * </p>
	 *
	 * @param inputType Type of input image.
	 * @param derivType Type of output image.
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	AnyImageDerivative<I,D> createDerivatives( Class<I> inputType , Class<D> derivType ) {

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputType);

		ImageGenerator<D> gen = FactoryImageGenerator.create(derivType);

		return new AnyImageDerivative<I,D>(GradientThree.getKernelX(isInteger),inputType,gen);
	}
}
