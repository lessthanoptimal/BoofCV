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

package boofcv.alg.filter.blur;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.blur.impl.*;
import boofcv.alg.filter.convolve.ConvolveImageMean;
import boofcv.alg.filter.convolve.ConvolveImageNormalized;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.ImageBorder;
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
 * Catch all class for function which "blur" an image, typically used to "reduce" the amount
 * of noise in the image.
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateBlurImageOps</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.blur.GenerateBlurImageOps")
@SuppressWarnings("Duplicates")
public class BlurImageOps {
	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 mean(GrayU8 input, @Nullable GrayU8 output, int radius,
							  @Nullable GrayU8 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		return mean(input, output, radius, radius, storage, workVert);
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 mean( GrayU8 input, @Nullable GrayU8 output, int radiusX, int radiusY,
							  @Nullable GrayU8 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanWeighted(input, output, radiusX, radiusY, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, workVert);

		return output;
	}

	/**
	 * Applies a mean box filter with image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 meanB( GrayU8 input, @Nullable GrayU8 output, int radiusX, int radiusY,
							  @Nullable ImageBorder_S32<GrayU8> binput,
							  @Nullable GrayU8 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanBorder(input, output, radiusX, radiusY, binput, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1, binput);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, binput, workVert);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 gaussian( GrayU8 input, @Nullable GrayU8 output, double sigma, int radius,
								  @Nullable GrayU8 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 gaussian( GrayU8 input, @Nullable GrayU8 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable GrayU8 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_S32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaX, radiusX);
			Kernel1D_S32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedU8 gaussian( InterleavedU8 input, @Nullable InterleavedU8 output, double sigma, int radius,
								  @Nullable InterleavedU8 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedU8 gaussian( InterleavedU8 input, @Nullable InterleavedU8 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable InterleavedU8 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_S32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaX, radiusX);
			Kernel1D_S32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU16 mean(GrayU16 input, @Nullable GrayU16 output, int radius,
							  @Nullable GrayU16 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		return mean(input, output, radius, radius, storage, workVert);
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU16 mean( GrayU16 input, @Nullable GrayU16 output, int radiusX, int radiusY,
							  @Nullable GrayU16 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanWeighted(input, output, radiusX, radiusY, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, workVert);

		return output;
	}

	/**
	 * Applies a mean box filter with image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU16 meanB( GrayU16 input, @Nullable GrayU16 output, int radiusX, int radiusY,
							  @Nullable ImageBorder_S32<GrayU16> binput,
							  @Nullable GrayU16 storage, @Nullable GrowArray<DogArray_I32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanBorder(input, output, radiusX, radiusY, binput, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1, binput);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, binput, workVert);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU16 gaussian( GrayU16 input, @Nullable GrayU16 output, double sigma, int radius,
								  @Nullable GrayU16 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU16 gaussian( GrayU16 input, @Nullable GrayU16 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable GrayU16 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_S32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaX, radiusX);
			Kernel1D_S32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedU16 gaussian( InterleavedU16 input, @Nullable InterleavedU16 output, double sigma, int radius,
								  @Nullable InterleavedU16 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedU16 gaussian( InterleavedU16 input, @Nullable InterleavedU16 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable InterleavedU16 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_S32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaX, radiusX);
			Kernel1D_S32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_S32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 mean(GrayF32 input, @Nullable GrayF32 output, int radius,
							  @Nullable GrayF32 storage, @Nullable GrowArray<DogArray_F32> workVert ) {
		return mean(input, output, radius, radius, storage, workVert);
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 mean( GrayF32 input, @Nullable GrayF32 output, int radiusX, int radiusY,
							  @Nullable GrayF32 storage, @Nullable GrowArray<DogArray_F32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanWeighted(input, output, radiusX, radiusY, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, workVert);

		return output;
	}

	/**
	 * Applies a mean box filter with image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 meanB( GrayF32 input, @Nullable GrayF32 output, int radiusX, int radiusY,
							  @Nullable ImageBorder_F32 binput,
							  @Nullable GrayF32 storage, @Nullable GrowArray<DogArray_F32> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanBorder(input, output, radiusX, radiusY, binput, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1, binput);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, binput, workVert);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 gaussian( GrayF32 input, @Nullable GrayF32 output, double sigma, int radius,
								  @Nullable GrayF32 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 gaussian( GrayF32 input, @Nullable GrayF32 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable GrayF32 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_F32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigmaX, radiusX);
			Kernel1D_F32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedF32 gaussian( InterleavedF32 input, @Nullable InterleavedF32 output, double sigma, int radius,
								  @Nullable InterleavedF32 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedF32 gaussian( InterleavedF32 input, @Nullable InterleavedF32 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable InterleavedF32 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_F32 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigmaX, radiusX);
			Kernel1D_F32 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 mean(GrayF64 input, @Nullable GrayF64 output, int radius,
							  @Nullable GrayF64 storage, @Nullable GrowArray<DogArray_F64> workVert ) {
		return mean(input, output, radius, radius, storage, workVert);
	}

	/**
	 * Applies a mean box filter with re-weighted image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 mean( GrayF64 input, @Nullable GrayF64 output, int radiusX, int radiusY,
							  @Nullable GrayF64 storage, @Nullable GrowArray<DogArray_F64> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanWeighted(input, output, radiusX, radiusY, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, workVert);

		return output;
	}

	/**
	 * Applies a mean box filter with image borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 meanB( GrayF64 input, @Nullable GrayF64 output, int radiusX, int radiusY,
							  @Nullable ImageBorder_F64 binput,
							  @Nullable GrayF64 storage, @Nullable GrowArray<DogArray_F64> workVert ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeMeanBorder(input, output, radiusX, radiusY, binput, storage);

		if (processed)
			return output;

		ConvolveImageMean.horizontal(input, storage, radiusX, radiusX*2 + 1, binput);
		ConvolveImageMean.vertical(storage, output, radiusY, radiusY*2 + 1, binput, workVert);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 gaussian( GrayF64 input, @Nullable GrayF64 output, double sigma, int radius,
								  @Nullable GrayF64 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 gaussian( GrayF64 input, @Nullable GrayF64 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable GrayF64 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_F64 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_F64.class, sigmaX, radiusX);
			Kernel1D_F64 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_F64.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedF64 gaussian( InterleavedF64 input, @Nullable InterleavedF64 output, double sigma, int radius,
								  @Nullable InterleavedF64 storage ) {
		return gaussian(input,output,sigma,radius,sigma,radius,storage);
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static InterleavedF64 gaussian( InterleavedF64 input, @Nullable InterleavedF64 output, 
								  double sigmaX, int radiusX, double sigmaY, int radiusY,
								  @Nullable InterleavedF64 storage ) {
		output = InputSanityCheck.declareOrReshape(input, output);
		storage = InputSanityCheck.declareOrReshape(input, storage);

		boolean processed = BOverrideBlurImageOps.invokeNativeGaussian(input, output, sigmaX,radiusX,sigmaY,radiusY, storage);

		if (!processed) {
			Kernel1D_F64 kernelX = FactoryKernelGaussian.gaussian(Kernel1D_F64.class, sigmaX, radiusX);
			Kernel1D_F64 kernelY = sigmaX==sigmaY&&radiusX==radiusY ? 
					kernelX:
					FactoryKernelGaussian.gaussian(Kernel1D_F64.class, sigmaY, radiusY);

			ConvolveImageNormalized.horizontal(kernelX, input, storage);
			ConvolveImageNormalized.vertical(kernelY, storage, output);
		}

		return output;
	}

	/**
	 * Applies Gaussian blur to a {@link Planar}
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigma Gaussian distribution's sigma. If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> gaussian( Planar<T> input, @Nullable Planar<T> output, double sigma, int radius, @Nullable T storage ) {

		if (storage == null)
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(), input.width, input.height);
		if (output == null)
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.gaussian(input.getBand(band), output.getBand(band), sigma, radius, storage);
		}
		return output;
	}

	/**
	 * Applies Gaussian blur to a {@link Planar}
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param sigmaX Gaussian distribution's sigma along x-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusX Radius of the Gaussian blur function along x-axis. If &le; 0 then radius will be determined by sigma.
	 * @param sigmaY Gaussian distribution's sigma along y-axis. If &le; 0 then will be selected based on radius.
	 * @param radiusY Radius of the Gaussian blur function along y-axis. If &le; 0 then radius will be determined by sigma.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> gaussian(Planar<T> input, @Nullable Planar<T> output, double sigmaX, int radiusX, double sigmaY, int radiusY, @Nullable T storage ) {

		if (storage == null)
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(), input.width, input.height);
		if (output == null)
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.gaussian(input.getBand(band), output.getBand(band), sigmaX, radiusX, sigmaY, radiusY, storage);
		}
		return output;
	}

	/**
	 * Applies mean box filter to a {@link Planar}
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> mean(Planar<T> input, @Nullable Planar<T> output, int radius ,
				   @Nullable T storage, @Nullable GrowArray workVert ) {
		return mean(input,output,radius,radius,storage,workVert);
	}

	/**
	 * Applies a mean box filter with weighted borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> mean( Planar<T> input, @Nullable Planar<T> output, int radiusX, int radiusY,
				   @Nullable T storage, @Nullable GrowArray workVert ) {
		if (storage == null)
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(), input.width, input.height);
		if (output == null)
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.mean(input.getBand(band), output.getBand(band), radiusX, radiusY, storage, workVert);
		}
		return output;
	}

	/**
	 * Applies a mean box filter with extended borders.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Radius of the box blur function along the x-axis
	 * @param radiusY Radius of the box blur function along the y-axis
	 * @param storage (Optional) Storage for intermediate results. Same size as input image. Can be null.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> meanB( Planar<T> input, @Nullable Planar<T> output, int radiusX, int radiusY,
				   @Nullable ImageBorder<T> binput,
				   @Nullable T storage, @Nullable GrowArray workVert ) {
		if (storage == null)
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(), input.width, input.height);
		if (output == null)
			output = input.createNew(input.width,input.height);
		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.meanB(input.getBand(band), output.getBand(band), radiusX, radiusY, binput, storage, workVert);
		}
		return output;
	}

	/**
	 * Applies a median filter.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. y-axis
	 * @param work (Optional) Creates local workspace arrays. Nullable.
	 * @return Output blurred image.
	 */
	public static GrayU8 median( GrayU8 input, @Nullable GrayU8 output, int radiusX, int radiusY,
								 @Nullable GrowArray<DogArray_I32> work ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		boolean processed = BOverrideBlurImageOps.invokeNativeMedian(input, output, radiusX, radiusY);

		if (!processed) {
			work = BoofMiscOps.checkDeclare(work, DogArray_I32::new);
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplMedianHistogramInner_MT.process(input, output, radiusX, radiusY, work);
			} else {
				ImplMedianHistogramInner.process(input, output, radiusX, radiusY, work);
			}
			// TODO Optimize this algorithm. It is taking up a large percentage of the CPU time
			ImplMedianSortEdgeNaive.process(input, output, radiusX, radiusY, work.grow());
		}

		return output;
	}

	/**
	 * Applies a median filter.
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. y-axis
	 * @param work (Optional) Creates local workspace arrays. Nullable.
	 * @return Output blurred image.
	 */
	public static GrayF32 median( GrayF32 input, @Nullable GrayF32 output, int radiusX, int radiusY,
								 @Nullable GrowArray<DogArray_F32> work ) {
		if (radiusX <= 0 || radiusY <= 0)
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.declareOrReshape(input, output);

		boolean processed = BOverrideBlurImageOps.invokeNativeMedian(input, output, radiusX, radiusY);

		if (!processed) {
			if (BoofConcurrency.USE_CONCURRENT) {
				ImplMedianSortNaive_MT.process(input, output, radiusX, radiusY, work);
			} else {
				ImplMedianSortNaive.process(input, output, radiusX, radiusY, work);
			}
		}
		return output;
	}

	/**
	 * Applies median filter to a {@link Planar}
	 *
	 * @param input Input image. Not modified.
	 * @param output (Optional) Storage for output image, Can be null. Modified.
	 * @param radiusX Size of the filter region. x-axis
	 * @param radiusY Size of the filter region. y-axis
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray<T>>
	Planar<T> median( Planar<T> input, @Nullable Planar<T> output, int radiusX, int radiusY,
					  @Nullable GrowArray<?> work ) {

		if (output == null)
			output = input.createNew(input.width, input.height);

		for (int band = 0; band < input.getNumBands(); band++) {
			GBlurImageOps.median(input.getBand(band), output.getBand(band), radiusX, radiusY, work);
		}
		return output;
	}

}
