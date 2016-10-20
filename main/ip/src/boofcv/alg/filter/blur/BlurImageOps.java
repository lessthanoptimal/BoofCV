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

package boofcv.alg.filter.blur;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.blur.impl.ImplMedianHistogramInner;
import boofcv.alg.filter.blur.impl.ImplMedianSortEdgeNaive;
import boofcv.alg.filter.blur.impl.ImplMedianSortNaive;
import boofcv.alg.filter.convolve.ConvolveImageMean;
import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_F64;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.image.*;

/**
 * Catch all class for function which "blur" an image, typically used to "reduce" the amount
 * of noise in the image.
 *
 * @author Peter Abeles
 */
public class BlurImageOps {

	/**
	 * Applies a mean box filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 mean(GrayU8 input, GrayU8 output, int radius, GrayU8 storage) {

		if( radius <= 0 )
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		ConvolveImageMean.horizontal(input,storage,radius);
		ConvolveImageMean.vertical(storage, output, radius);

		return output;
	}

	/**
	 * Applies a median filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the median blur function.
	 * @return Output blurred image.
	 */
	public static GrayU8 median(GrayU8 input, GrayU8 output, int radius) {
		if( radius <= 0 )
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.checkDeclare(input,output);

		int w = radius*2+1;
		int offset[] = new int[ w*w ];
		int histogram[] = new int[ 256 ];

		ImplMedianHistogramInner.process(input, output, radius, offset, histogram);
		ImplMedianSortEdgeNaive.process(input, output, radius, offset);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayU8 gaussian(GrayU8 input, GrayU8 output, double sigma , int radius,
								  GrayU8 storage ) {
		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage,GrayU8.class);

		Kernel1D_I32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_I32.class,sigma,radius);

		ConvolveNormalized.horizontal(kernel, input, storage);
		ConvolveNormalized.vertical(kernel,storage,output);

		return output;
	}

	/**
	 * Applies a mean box filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 mean(GrayF32 input, GrayF32 output, int radius, GrayF32 storage) {

		if( radius <= 0 )
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		ConvolveImageMean.horizontal(input,storage,radius);
		ConvolveImageMean.vertical(storage,output,radius);

		return output;
	}

	/**
	 * Applies a mean box filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 mean(GrayF64 input, GrayF64 output, int radius, GrayF64 storage) {

		if( radius <= 0 )
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		ConvolveImageMean.horizontal(input,storage,radius);
		ConvolveImageMean.vertical(storage,output,radius);

		return output;
	}

	/**
	 * Applies a median filter.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the median blur function.
	 * @return Output blurred image.
	 */
	public static GrayF32 median(GrayF32 input, GrayF32 output, int radius) {

		if( radius <= 0 )
			throw new IllegalArgumentException("Radius must be > 0");

		output = InputSanityCheck.checkDeclare(input,output);

		ImplMedianSortNaive.process(input,output,radius,null);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF32 gaussian(GrayF32 input, GrayF32 output,
								   double sigma , int radius,
								   GrayF32 storage ) {
		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class,sigma, radius);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);

		return output;
	}

	/**
	 * Applies Gaussian blur.
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @return Output blurred image.
	 */
	public static GrayF64 gaussian(GrayF64 input, GrayF64 output,
								   double sigma , int radius,
								   GrayF64 storage ) {
		output = InputSanityCheck.checkDeclare(input,output);
		storage = InputSanityCheck.checkDeclare(input,storage);

		Kernel1D_F64 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F64.class,sigma, radius);

		ConvolveNormalized.horizontal(kernel,input,storage);
		ConvolveNormalized.vertical(kernel,storage,output);

		return output;
	}

	/**
	 * Applies mean box filter to a {@link Planar}
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the box blur function.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray>
	Planar<T> mean(Planar<T> input, Planar<T> output, int radius , T storage ) {

		if( storage == null )
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(),input.width,input.height);
		if( output == null )
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.median(input.getBand(band),output.getBand(band),radius);
		}
		return output;
	}

	/**
	 * Applies median filter to a {@link Planar}
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param radius Radius of the median blur function.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray>
	Planar<T> median(Planar<T> input, Planar<T> output, int radius ) {

		if( output == null )
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.median(input.getBand(band),output.getBand(band),radius);
		}
		return output;
	}

	/**
	 * Applies Gaussian blur to a {@link Planar}
	 *
	 * @param input Input image.  Not modified.
	 * @param output (Optional) Storage for output image, Can be null.  Modified.
	 * @param sigma Gaussian distribution's sigma.  If &le; 0 then will be selected based on radius.
	 * @param radius Radius of the Gaussian blur function. If &le; 0 then radius will be determined by sigma.
	 * @param storage (Optional) Storage for intermediate results.  Same size as input image.  Can be null.
	 * @param <T> Input image type.
	 * @return Output blurred image.
	 */
	public static <T extends ImageGray>
	Planar<T> gaussian(Planar<T> input, Planar<T> output, double sigma , int radius, T storage ) {

		if( storage == null )
			storage = GeneralizedImageOps.createSingleBand(input.getBandType(), input.width, input.height);
		if( output == null )
			output = input.createNew(input.width,input.height);

		for( int band = 0; band < input.getNumBands(); band++ ) {
			GBlurImageOps.gaussian(input.getBand(band),output.getBand(band),sigma,radius,storage);
		}
		return output;
	}
}
