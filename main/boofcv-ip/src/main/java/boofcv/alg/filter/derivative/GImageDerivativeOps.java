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

package boofcv.alg.filter.derivative;

import boofcv.abst.filter.derivative.AnyImageDerivative;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderIndex1D_Extend;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.*;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.convolve.Kernel2D;
import boofcv.struct.convolve.KernelBase;
import boofcv.struct.image.*;

/**
 * Generalized operations related to compute different image derivatives.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GImageDerivativeOps {
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	void laplace( I input, D output, BorderType borderType ) {
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.wrap(borderType, input);
		if (input instanceof GrayF32) {
			DerivativeLaplacian.process((GrayF32)input, (GrayF32)output, (ImageBorder_F32)border);
		} else if (input instanceof GrayU8) {
			DerivativeLaplacian.process((GrayU8)input, (GrayS16)output, (ImageBorder_S32<GrayU8>)border);
		} else {
			throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
		}
	}

	/**
	 * Returns the type of image the derivative should be for the specified input type.
	 *
	 * @param imageType Input image type.
	 * @return Appropriate output image type.
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	Class<D> getDerivativeType( Class<I> imageType ) {
		if (imageType == GrayF32.class) {
			return (Class<D>)GrayF32.class;
		} else if (imageType == GrayU8.class) {
			return (Class<D>)GrayS16.class;
		} else if (imageType == GrayU16.class) {
			return (Class<D>)GrayS32.class;
		} else {
			throw new IllegalArgumentException("Unknown input image type: " + imageType.getSimpleName());
		}
	}

	public static <I extends ImageBase<I>, D extends ImageBase<D>>
	ImageType<D> getDerivativeType( ImageType<I> imageType ) {
		switch (imageType.getFamily()) {
			case GRAY:
				return ImageType.single(getDerivativeType(imageType.getImageClass()));
			case PLANAR: {
				int numBands = imageType.getNumBands();
				return ImageType.pl(numBands, getDerivativeType(imageType.getImageClass()));
			}
			case INTERLEAVED:
				int numBands = imageType.getNumBands();
				return switch (imageType.getDataType()) {
					case F32 -> (ImageType)ImageType.il(numBands, ImageDataType.F32);
					case F64 -> (ImageType)ImageType.il(numBands, ImageDataType.F64);
					case U8 -> (ImageType)ImageType.il(numBands, ImageDataType.S16);
					case U16 -> (ImageType)ImageType.il(numBands, ImageDataType.S32);
					default -> throw new IllegalArgumentException("Unknown image type");
				};
			default:
				throw new IllegalArgumentException("Unknown image type");
		}
	}

	/**
	 * Computes the gradient using the specified image type.
	 *
	 * @param type Type of gradient to compute
	 * @param input Input image
	 * @param derivX Output. Derivative X
	 * @param derivY Output. Derivative Y
	 * @param borderType How it should handle borders. null == skip border
	 * @param <I> Input image type
	 * @param <D> Output image type
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	void gradient( DerivativeType type, I input, D derivX, D derivY, BorderType borderType ) {

		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.wrap(borderType, input);

		switch (type) {
			case PREWITT -> {
				if (input instanceof GrayF32) {
					GradientPrewitt.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientPrewitt.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientPrewitt.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}
			case SOBEL -> {
				if (input instanceof GrayF32) {
					GradientSobel.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientSobel.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientSobel.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}
			case SCHARR -> {
				if (input instanceof GrayF32) {
					GradientScharr.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientScharr.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientScharr.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}
			case THREE -> {
				if (input instanceof GrayF32) {
					GradientThree.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientThree.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientThree.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}
			case TWO_0 -> {
				if (input instanceof GrayF32) {
					GradientTwo0.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientTwo0.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientTwo0.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}
			case TWO_1 -> {
				if (input instanceof GrayF32) {
					GradientTwo1.process((GrayF32)input, (GrayF32)derivX, (GrayF32)derivY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					GradientTwo1.process((GrayU8)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else if (input instanceof GrayS16) {
					GradientTwo1.process((GrayS16)input, (GrayS16)derivX, (GrayS16)derivY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
			}

			default -> throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	/**
	 * Computes the hessian from the original input image. Only Sobel and Three supported.
	 *
	 * @param type Type of gradient to compute
	 * @param input Input image
	 * @param derivXX Output. Derivative XX
	 * @param derivYY Output. Derivative YY
	 * @param derivXY Output. Derivative XY
	 * @param borderType How it should handle borders. null == skip border
	 * @param <I> Input image type
	 * @param <D> Output image type
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	void hessian( DerivativeType type, I input, D derivXX, D derivYY, D derivXY, BorderType borderType ) {
		ImageBorder<I> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.wrap(borderType, input);

		switch (type) {
			case SOBEL:
				if (input instanceof GrayF32) {
					HessianSobel.process((GrayF32)input, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					HessianSobel.process((GrayU8)input, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
				break;

			case THREE:
				if (input instanceof GrayF32) {
					HessianThree.process((GrayF32)input, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
				} else if (input instanceof GrayU8) {
					HessianThree.process((GrayU8)input, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + input.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unsupported derivative type " + type);
		}
	}

	/**
	 * Computes the hessian from the gradient. Only Prewitt, Sobel and Three supported.
	 *
	 * @param type Type of gradient to compute
	 * @param derivX Input derivative X
	 * @param derivY Input derivative Y
	 * @param derivXX Output. Derivative XX
	 * @param derivYY Output. Derivative YY
	 * @param derivXY Output. Derivative XY
	 * @param borderType How it should handle borders. null == skip border
	 */
	public static <D extends ImageGray<D>>
	void hessian( DerivativeType type, D derivX, D derivY, D derivXX, D derivYY, D derivXY, BorderType borderType ) {
		ImageBorder<D> border = BorderType.SKIP == borderType ? null : FactoryImageBorder.wrap(borderType, derivX);

		switch (type) {
			case PREWITT:
				if (derivX instanceof GrayF32) {
					HessianFromGradient.hessianPrewitt((GrayF32)derivX, (GrayF32)derivY, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
				} else if (derivX instanceof GrayS16) {
					HessianFromGradient.hessianPrewitt((GrayS16)derivX, (GrayS16)derivY, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + derivX.getClass().getSimpleName());
				}
				break;

			case SOBEL:
				if (derivX instanceof GrayF32) {
					HessianFromGradient.hessianSobel((GrayF32)derivX, (GrayF32)derivY, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
				} else if (derivX instanceof GrayS16) {
					HessianFromGradient.hessianSobel((GrayS16)derivX, (GrayS16)derivY, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + derivX.getClass().getSimpleName());
				}
				break;

			case THREE:
				if (derivX instanceof GrayF32) {
					HessianFromGradient.hessianThree((GrayF32)derivX, (GrayF32)derivY, (GrayF32)derivXX, (GrayF32)derivYY, (GrayF32)derivXY, (ImageBorder_F32)border);
				} else if (derivX instanceof GrayS16) {
					HessianFromGradient.hessianThree((GrayS16)derivX, (GrayS16)derivY, (GrayS16)derivXX, (GrayS16)derivYY, (GrayS16)derivXY, (ImageBorder_S32)border);
				} else {
					throw new IllegalArgumentException("Unknown input image type: " + derivX.getClass().getSimpleName());
				}
				break;

			default:
				throw new IllegalArgumentException("Unsupported derivative type " + type);
		}
	}

	/**
	 * Returns the kernel for finding the X derivative.
	 *
	 * @param type Type of gradient
	 * @param isInteger integer or floating point kernels
	 * @return The kernel. Can be 1D or 2D
	 */
	public static KernelBase lookupKernelX( DerivativeType type, boolean isInteger ) {
		return switch (type) {
			case PREWITT -> GradientPrewitt.getKernelX(isInteger);
			case SOBEL -> GradientSobel.getKernelX(isInteger);
			case SCHARR -> GradientScharr.getKernelX(isInteger);
			case THREE -> GradientThree.getKernelX(isInteger);
			case TWO_0 -> GradientTwo0.getKernelX(isInteger);
			case TWO_1 -> GradientTwo1.getKernelX(isInteger);
		};
	}

	/**
	 * <p>
	 * Convenience function for creating an instance of {@link boofcv.abst.filter.derivative.AnyImageDerivative}.
	 * This class is an any way to compute any derivative of any order using the specified kernel. It might
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
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	AnyImageDerivative<I, D> createAnyDerivatives( DerivativeType type, Class<I> inputType, Class<D> derivType ) {

		boolean isInteger = !GeneralizedImageOps.isFloatingPoint(inputType);
		KernelBase kernel = lookupKernelX(type, isInteger);

		if (kernel instanceof Kernel1D)
			return new AnyImageDerivative<>((Kernel1D)kernel, inputType, derivType);
		else
			return new AnyImageDerivative<>((Kernel2D)kernel, inputType, derivType);
	}

	/**
	 * Creates an instance of {@link AnyImageDerivative} which is intended for use of calculating scale-spaces.
	 * It uses {@link DerivativeType#THREE} since it does not blur the image. More typical operators, such as Sobel and
	 * Prewitt, blur the image.
	 *
	 * @param inputType Type of input image.
	 * @param derivType Type of derivative image
	 * @param <I> Image type.
	 * @param <D> Image derivative type.
	 * @return AnyImageDerivative
	 */
	public static <I extends ImageGray<I>, D extends ImageGray<D>>
	AnyImageDerivative<I, D> derivativeForScaleSpace( Class<I> inputType, Class<D> derivType ) {
		return createAnyDerivatives(DerivativeType.THREE, inputType, derivType);
	}

	/**
	 * Creates a new instance of the default border for derivatives of integer images
	 */
	public static ImageBorder_S32 borderDerivative_I32() {
		return new ImageBorder1D_S32(BorderIndex1D_Extend::new);
	}

	/**
	 * Creates a new instance of the default border for derivatives of GrayF32
	 */
	public static ImageBorder_F32 borderDerivative_F32() {
		return new ImageBorder1D_F32(BorderIndex1D_Extend::new);
	}
}
