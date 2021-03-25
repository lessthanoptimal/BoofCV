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

package boofcv.alg.filter.convolve;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.convolve.noborder.*;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

/**
 * <p>
 * Provides functions for convolving 1D and 2D kernels across an image, excluding the image border. 1D kernels can either
 * be convolved along each row or column in the image. No checks are done for overflow or underflow.
 * </p>
 * <p>
 * When convolving with division the convolution is computed as usual, but then the result is divided by
 * the divisor. This is typically done when performing convolution inside of integer images to normalize
 * it by the sum of all the elements in the convolution kernel.
 * </p>
 *
 * <p>
 * Image Edges: There is no general purpose way for handling convolutions along the image edges. Therefore unless
 * the whole kernel can be convolved image borders are skipped. In special cases where there is a clear way to
 * handle image edges specialized functions are provided.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvolveImageNoBorder</p>
 *
 * @author Peter Abekes
 */
@Generated("boofcv.alg.filter.convolve.GenerateConvolveImageNoBorder")
@SuppressWarnings({"ForLoopReplaceableByForEach", "rawtypes"})
public class ConvolveImageNoBorder {

	public static void horizontal( Kernel1D_F32 kernel, GrayF32 input, GrayF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F32_F32.horizontal(kernel, input, output))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F32_F32.horizontal(kernel, input, output))
				ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_F32 kernel, GrayF32 input, GrayF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F32_F32.vertical(kernel, input, output))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F32_F32.vertical(kernel, input, output))
				ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_F32 kernel, GrayF32 input, GrayF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F32_F32.convolve(kernel, input, output))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F32_F32.convolve(kernel, input, output))
				ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_F32 kernel, InterleavedF32 input, InterleavedF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_F32 kernel, InterleavedF32 input, InterleavedF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_F32 kernel, InterleavedF32 input, InterleavedF32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_F64 kernel, GrayF64 input, GrayF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F64_F64.horizontal(kernel, input, output))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F64_F64.horizontal(kernel, input, output))
				ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_F64 kernel, GrayF64 input, GrayF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F64_F64.vertical(kernel, input, output))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F64_F64.vertical(kernel, input, output))
				ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_F64 kernel, GrayF64 input, GrayF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_F64_F64.convolve(kernel, input, output))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_F64_F64.convolve(kernel, input, output))
				ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_F64 kernel, InterleavedF64 input, InterleavedF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_F64 kernel, InterleavedF64 input, InterleavedF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_F64 kernel, InterleavedF64 input, InterleavedF64 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I16.horizontal(kernel, input, output))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I16.horizontal(kernel, input, output))
				ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I16.vertical(kernel, input, output))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I16.vertical(kernel, input, output))
				ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I16.convolve(kernel, input, output))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I16.convolve(kernel, input, output))
				ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedU8 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedU8 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU16 input, GrayI8 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedU16 input, InterleavedI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16.horizontal(kernel, input, output))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16.horizontal(kernel, input, output))
				ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16.vertical(kernel, input, output))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16.vertical(kernel, input, output))
				ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS16 input, GrayI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16.convolve(kernel, input, output))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16.convolve(kernel, input, output))
				ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedS16 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedS16 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedS16 input, InterleavedI16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 input, GrayI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I8_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output, divisor);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I8_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 input, GrayI8 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I8_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I8_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 input, GrayI8 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U8_I8_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_U8_I8_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.convolve(kernel, input, output, divisor, work);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedU8 input, InterleavedI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedU8 input, InterleavedI8 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output, divisor);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output, divisor);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS16 input, GrayI16 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S16_I16_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_S16_I16_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.convolve(kernel, input, output, divisor, work);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedS16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedS16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedS16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output, divisor);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU16 input, GrayI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U16_I16_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output, divisor);
		} else {
			if (!ConvolveImageUnrolled_SB_U16_I16_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU16 input, GrayI16 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U16_I16_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_U16_I16_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU16 input, GrayI16 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_U16_I16_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_U16_I16_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.convolve(kernel, input, output, divisor, work);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedU16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedU16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedU16 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS32 input, GrayI16 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedS32 input, InterleavedI16 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32.horizontal(kernel, input, output))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32.horizontal(kernel, input, output))
				ConvolveImageStandard_SB.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32.vertical(kernel, input, output))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32.vertical(kernel, input, output))
				ConvolveImageStandard_SB.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS32 input, GrayS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32.convolve(kernel, input, output))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32.convolve(kernel, input, output))
				ConvolveImageStandard_SB.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedS32 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedS32 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedS32 input, InterleavedS32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB_MT.horizontal(kernel, input, output, divisor);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32_Div.horizontal(kernel, input, output, divisor))
				ConvolveImageStandard_SB.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.vertical(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32_Div.vertical(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.vertical(kernel, input, output, divisor, work);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS32 input, GrayS32 output, int divisor, 
								 @Nullable GrowArray<DogArray_I32> work ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			if (!ConvolveImageUnrolled_SB_MT_S32_S32_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB_MT.convolve(kernel, input, output, divisor, work);
		} else {
			if (!ConvolveImageUnrolled_SB_S32_S32_Div.convolve(kernel, input, output, divisor, work))
				ConvolveImageStandard_SB.convolve(kernel, input, output, divisor, work);
		}
	}

	public static void horizontal( Kernel1D_S32 kernel, InterleavedS32 input, InterleavedS32 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.horizontal(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.horizontal(kernel, input, output, divisor);
		}
	}

	public static void vertical( Kernel1D_S32 kernel, InterleavedS32 input, InterleavedS32 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.vertical(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.vertical(kernel, input, output, divisor);
		}
	}

	public static void convolve( Kernel2D_S32 kernel, InterleavedS32 input, InterleavedS32 output, int divisor ) {
		InputSanityCheck.checkSameShape(input, output);

		if (BoofConcurrency.USE_CONCURRENT) {
			ConvolveImageStandard_IL_MT.convolve(kernel, input, output, divisor);
		} else {
			ConvolveImageStandard_IL.convolve(kernel, input, output, divisor);
		}
	}

}
