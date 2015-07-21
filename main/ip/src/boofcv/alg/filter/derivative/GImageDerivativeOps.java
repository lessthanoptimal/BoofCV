/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.core.image.border.*;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.KernelBase;
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
			return (Class<D>) ImageFloat32.class;
		} else if( imageType == ImageUInt8.class ) {
			return (Class<D>) ImageSInt16.class;
		} else if( imageType == ImageUInt16.class ) {
			return (Class<D>) ImageSInt32.class;
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+imageType.getSimpleName());
		}
	}

	/**
	 * Computes the gradient using the specified image type.
	 *
	 * @param type Type of gradient to compute
	 * @param input Input image
	 * @param derivX Output.  Derivative X
	 * @param derivY Output. Derivative Y
	 * @param borderType How it should handle borders.  null == skip border
	 * @param <I> Input image type
	 * @param <D> Output image type
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void gradient( DerivativeType type , I input , D derivX , D derivY , BorderType borderType ) {

		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		switch( type ) {
			case PREWITT:
				if( input instanceof ImageFloat32 ) {
					GradientPrewitt.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
				} else if( input instanceof ImageUInt8 ) {
					GradientPrewitt.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else if( input instanceof ImageSInt16 ) {
					GradientPrewitt.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;
			case SOBEL:
				if( input instanceof ImageFloat32 ) {
					GradientSobel.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
				} else if( input instanceof ImageUInt8 ) {
					GradientSobel.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else if( input instanceof ImageSInt16 ) {
					GradientSobel.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;
			case THREE:
				if( input instanceof ImageFloat32 ) {
					GradientThree.process((ImageFloat32)input,(ImageFloat32)derivX,(ImageFloat32)derivY,(ImageBorder_F32)border);
				} else if( input instanceof ImageUInt8 ) {
					GradientThree.process((ImageUInt8)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else if( input instanceof ImageSInt16 ) {
					GradientThree.process((ImageSInt16)input,(ImageSInt16)derivX,(ImageSInt16)derivY,(ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;
			case TWO_0:
				if( input instanceof ImageFloat32 ) {
					GradientTwo0.process((ImageFloat32) input, (ImageFloat32) derivX, (ImageFloat32) derivY, (ImageBorder_F32) border);
				} else if( input instanceof ImageUInt8 ) {
					GradientTwo0.process((ImageUInt8) input, (ImageSInt16) derivX, (ImageSInt16) derivY, (ImageBorder_S32) border);
				} else if( input instanceof ImageSInt16 ) {
					GradientTwo0.process((ImageSInt16) input, (ImageSInt16) derivX, (ImageSInt16) derivY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;
			case TWO_1:
				if( input instanceof ImageFloat32 ) {
					GradientTwo1.process((ImageFloat32) input, (ImageFloat32) derivX, (ImageFloat32) derivY, (ImageBorder_F32) border);
				} else if( input instanceof ImageUInt8 ) {
					GradientTwo1.process((ImageUInt8) input, (ImageSInt16) derivX, (ImageSInt16) derivY, (ImageBorder_S32) border);
				} else if( input instanceof ImageSInt16 ) {
					GradientTwo1.process((ImageSInt16) input, (ImageSInt16) derivX, (ImageSInt16) derivY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unknown type: "+type);
		}
	}

	/**
	 * Computes the hessian from the original input image.  Only Sobel and Three supported.
	 *
	 * @param type Type of gradient to compute
	 * @param input Input image
	 * @param derivXX Output.  Derivative XX
	 * @param derivYY Output. Derivative YY
	 * @param derivXY Output. Derivative XY
	 * @param borderType How it should handle borders.  null == skip border
	 * @param <I> Input image type
	 * @param <D> Output image type
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	void hessian( DerivativeType type , I input , D derivXX , D derivYY , D derivXY , BorderType borderType ) {
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(input,borderType);

		switch( type ) {
			case SOBEL:
				if( input instanceof ImageFloat32 ) {
					HessianSobel.process((ImageFloat32) input, (ImageFloat32) derivXX, (ImageFloat32) derivYY, (ImageFloat32) derivXY, (ImageBorder_F32) border);
				} else if( input instanceof ImageUInt8 ) {
					HessianSobel.process((ImageUInt8) input, (ImageSInt16) derivXX, (ImageSInt16) derivYY, (ImageSInt16) derivXY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;

			case THREE:
				if( input instanceof ImageFloat32 ) {
					HessianThree.process((ImageFloat32) input, (ImageFloat32) derivXX, (ImageFloat32) derivYY, (ImageFloat32) derivXY, (ImageBorder_F32) border);
				} else if( input instanceof ImageUInt8 ) {
					HessianThree.process((ImageUInt8) input, (ImageSInt16) derivXX, (ImageSInt16) derivYY, (ImageSInt16) derivXY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+input.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unsupported derivative type "+type);
		}
	}

	/**
	 * Computes the hessian from the gradient.  Only Prewitt, Sobel and Three supported.
	 *
	 * @param type Type of gradient to compute
	 * @param derivX Input derivative X
	 * @param derivY Input derivative Y
	 * @param derivXX Output.  Derivative XX
	 * @param derivYY Output. Derivative YY
	 * @param derivXY Output. Derivative XY
	 * @param borderType How it should handle borders.  null == skip border
	 */
	public static <D extends ImageSingleBand>
	void hessian( DerivativeType type , D derivX , D derivY , D derivXX , D derivYY , D derivXY , BorderType borderType ) {
		ImageBorder<D> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.general(derivX,borderType);

		switch( type ) {
			case PREWITT:
				if( derivX instanceof ImageFloat32 ) {
					HessianFromGradient.hessianPrewitt((ImageFloat32) derivX, (ImageFloat32) derivY, (ImageFloat32) derivXX, (ImageFloat32) derivYY, (ImageFloat32) derivXY, (ImageBorder_F32) border);
				} else if( derivX instanceof ImageSInt16 ) {
					HessianFromGradient.hessianPrewitt((ImageSInt16) derivX, (ImageSInt16) derivY, (ImageSInt16) derivXX, (ImageSInt16) derivYY, (ImageSInt16) derivXY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+derivX.getClass().getSimpleName());
				}
				break;

			case SOBEL:
				if( derivX instanceof ImageFloat32 ) {
					HessianFromGradient.hessianSobel((ImageFloat32) derivX, (ImageFloat32) derivY, (ImageFloat32) derivXX, (ImageFloat32) derivYY, (ImageFloat32) derivXY, (ImageBorder_F32) border);
				} else if( derivX instanceof ImageSInt16 ) {
					HessianFromGradient.hessianSobel((ImageSInt16) derivX, (ImageSInt16) derivY, (ImageSInt16) derivXX, (ImageSInt16) derivYY, (ImageSInt16) derivXY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+derivX.getClass().getSimpleName());
				}
				break;

			case THREE:
				if( derivX instanceof ImageFloat32 ) {
					HessianFromGradient.hessianThree((ImageFloat32) derivX, (ImageFloat32) derivY, (ImageFloat32) derivXX, (ImageFloat32) derivYY, (ImageFloat32) derivXY, (ImageBorder_F32) border);
				} else if( derivX instanceof ImageSInt16 ) {
					HessianFromGradient.hessianThree((ImageSInt16) derivX, (ImageSInt16) derivY, (ImageSInt16) derivXX, (ImageSInt16) derivYY, (ImageSInt16) derivXY, (ImageBorder_S32) border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: "+derivX.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unsupported derivative type "+type);
		}
	}

	/**
	 * Returns the kernel for finding the X derivative.
	 * @param type Type of gradient
	 * @param isInteger integer or floating point kernels
	 * @return The kernel.  Can be 1D or 2D
	 */
	public static KernelBase lookupKernelX( DerivativeType type , boolean isInteger) {
		switch( type ) {
			case PREWITT:
				return GradientPrewitt.getKernelX(isInteger);

			case SOBEL:
				return GradientSobel.getKernelX(isInteger);

			case THREE:
				return GradientThree.getKernelX(isInteger);

			case TWO_0:
				return GradientTwo0.getKernelX(isInteger);

			case TWO_1:
				return GradientTwo1.getKernelX(isInteger);
		}

		throw new IllegalArgumentException("Unknown kernel type: "+type);
	}

	/**
	 * <p>
	 * Convenience function for creating an instance of {@link boofcv.abst.filter.derivative.AnyImageDerivative}.
	 * This class is an any way to compute any derivative of any order using the specified kernel.  It might
	 * use more memory or be more expensive the specialized code but is easy to use.
	 * </p>
	 *
	 * @param type Type of gradient to use
	 * @param inputType Type of input image.
	 * @param derivType Type of derivative image
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	AnyImageDerivative<I,D> createAnyDerivatives( DerivativeType type , Class<I> inputType , Class<D> derivType ) {

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputType);
		KernelBase kernel = lookupKernelX(type,isInteger);

		if( kernel instanceof Kernel1D )
			return new AnyImageDerivative<I,D>((Kernel1D)kernel,inputType,derivType);
		else
			return new AnyImageDerivative<I,D>((Kernel2D)kernel,inputType,derivType);
	}

	/**
	 * Creates an instance of {@link AnyImageDerivative} which is intended for use of calculating scale-spaces.
	 * It uses {@link DerivativeType#THREE} since it does not blur the image.  More typical operators, such as Sobel and
	 * Prewitt, blur the image.
	 *
	 * @param inputType Type of input image.
	 * @param derivType Type of derivative image
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	AnyImageDerivative<I,D> derivativeForScaleSpace( Class<I> inputType , Class<D> derivType ) {
		return createAnyDerivatives(DerivativeType.THREE,inputType,derivType);
	}

}
