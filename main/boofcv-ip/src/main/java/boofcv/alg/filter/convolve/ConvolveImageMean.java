/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve;

import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;
import boofcv.alg.filter.convolve.noborder.ImplConvolveMean;
import boofcv.alg.filter.convolve.noborder.ImplConvolveMean_MT;
import boofcv.alg.filter.convolve.normalized.ConvolveNormalized_JustBorder_SB;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_F64;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_S32;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

/**
 * <p>Convolves a mean filter across the image. The mean value of all the pixels are computed inside the kernel.</p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvolveImageMean</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.GenerateConvolveImageMean")
public class ConvolveImageMean {

	/**
	 * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayU8 input, GrayI8 output, int offset, int length ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.width) {
			ConvolveImageNormalized.horizontal(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayU8 input, GrayI8 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.height) {
			ConvolveImageNormalized.vertical(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayU8 input, GrayI8 output, int offset, int length, @Nullable ImageBorder_S32<GrayU8> binput ) {
		output.reshape(input.width,output.height);

		if (binput != null) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.horizontal(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.width) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayU8 input, GrayI8 output, int offset, int length, @Nullable ImageBorder_S32<GrayU8> binput, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if( binput != null ) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.vertical(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.height) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayS16 input, GrayI16 output, int offset, int length ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.width) {
			ConvolveImageNormalized.horizontal(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayS16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.height) {
			ConvolveImageNormalized.vertical(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayS16 input, GrayI16 output, int offset, int length, @Nullable ImageBorder_S32<GrayS16> binput ) {
		output.reshape(input.width,output.height);

		if (binput != null) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.horizontal(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.width) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayS16 input, GrayI16 output, int offset, int length, @Nullable ImageBorder_S32<GrayS16> binput, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if( binput != null ) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.vertical(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.height) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayU16 input, GrayI16 output, int offset, int length ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.width) {
			ConvolveImageNormalized.horizontal(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayU16 input, GrayI16 output, int offset, int length, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
			return;

		Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
		if (length > input.height) {
			ConvolveImageNormalized.vertical(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayU16 input, GrayI16 output, int offset, int length, @Nullable ImageBorder_S32<GrayU16> binput ) {
		output.reshape(input.width,output.height);

		if (binput != null) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.horizontal(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.width) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayU16 input, GrayI16 output, int offset, int length, @Nullable ImageBorder_S32<GrayU16> binput, @Nullable GrowArray<DogArray_I32> workspaces ) {
		output.reshape(input);

		if( binput != null ) {
			binput.setImage(input);
			Kernel1D_S32 kernel = FactoryKernel.table1D_S32(offset, length);
			ConvolveJustBorder_General_SB.vertical(kernel, binput, output, kernel.computeSum());
		}
		if (length <= input.height) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayF32 input, GrayF32 output, int offset, int length ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
			return;

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(offset, length , true);
		if (length > input.width) {
			ConvolveImageNormalized.horizontal(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length, @Nullable GrowArray<DogArray_F32> workspaces ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
			return;

		Kernel1D_F32 kernel = FactoryKernel.table1D_F32(offset, length , true);
		if (length > input.height) {
			ConvolveImageNormalized.vertical(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayF32 input, GrayF32 output, int offset, int length, @Nullable ImageBorder_F32 binput ) {
		output.reshape(input.width,output.height);

		if (binput != null) {
			binput.setImage(input);
			Kernel1D_F32 kernel = FactoryKernel.table1D_F32(offset, length , true);
			ConvolveJustBorder_General_SB.horizontal(kernel, binput, output);
		}
		if (length <= input.width) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayF32 input, GrayF32 output, int offset, int length, @Nullable ImageBorder_F32 binput, @Nullable GrowArray<DogArray_F32> workspaces ) {
		output.reshape(input);

		if( binput != null ) {
			binput.setImage(input);
			Kernel1D_F32 kernel = FactoryKernel.table1D_F32(offset, length , true);
			ConvolveJustBorder_General_SB.vertical(kernel, binput, output);
		}
		if (length <= input.height) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayF64 input, GrayF64 output, int offset, int length ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeHorizontal(input, output, offset, length))
			return;

		Kernel1D_F64 kernel = FactoryKernel.table1D_F64(offset, length , true);
		if (length > input.width) {
			ConvolveImageNormalized.horizontal(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.horizontal(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Borders are handled by reducing the box size.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param length How long the mean filter is
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length, @Nullable GrowArray<DogArray_F64> workspaces ) {
		output.reshape(input);

		if (BOverrideConvolveImageMean.invokeNativeVertical(input, output, offset, length))
			return;

		Kernel1D_F64 kernel = FactoryKernel.table1D_F64(offset, length , true);
		if (length > input.height) {
			ConvolveImageNormalized.vertical(kernel, input, output);
		} else {
			ConvolveNormalized_JustBorder_SB.vertical(kernel, input, output);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

	/**
	 * Performs a horizontal 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param length How long the mean filter is
	 */
	public static void horizontal( GrayF64 input, GrayF64 output, int offset, int length, @Nullable ImageBorder_F64 binput ) {
		output.reshape(input.width,output.height);

		if (binput != null) {
			binput.setImage(input);
			Kernel1D_F64 kernel = FactoryKernel.table1D_F64(offset, length , true);
			ConvolveJustBorder_General_SB.horizontal(kernel, binput, output);
		}
		if (length <= input.width) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.horizontal(input, output, offset, length);
			} else {
				ImplConvolveMean.horizontal(input, output, offset, length);
			}
		}
	}

	/**
	 * Performs a vertical 1D mean box filter. Outside pixels are specified by a border.
	 *
	 * @param input The input image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param offset Start offset from pixel coordinate
	 * @param binput Used to process image borders. If null borders are not processed.
	 * @param work (Optional) Storage for work array
	 */
	public static void vertical( GrayF64 input, GrayF64 output, int offset, int length, @Nullable ImageBorder_F64 binput, @Nullable GrowArray<DogArray_F64> workspaces ) {
		output.reshape(input);

		if( binput != null ) {
			binput.setImage(input);
			Kernel1D_F64 kernel = FactoryKernel.table1D_F64(offset, length , true);
			ConvolveJustBorder_General_SB.vertical(kernel, binput, output);
		}
		if (length <= input.height) {
			if(BoofConcurrency.USE_CONCURRENT) {
				ImplConvolveMean_MT.vertical(input, output, offset, length, workspaces);
			} else {
				ImplConvolveMean.vertical(input, output, offset, length, workspaces);
			}
		}
	}

}
