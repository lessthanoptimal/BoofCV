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

import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General_SB;
import boofcv.alg.filter.derivative.impl.DerivativeLaplacian_Inner;
import boofcv.alg.filter.derivative.impl.DerivativeLaplacian_Inner_MT;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.border.ImageBorder_F32;
import boofcv.struct.border.ImageBorder_S32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_S32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * The Laplacian is convolved across an image to find second derivative of the image.
 * It is often a faster way to compute the intensity of an edge than first derivative algorithms.
 * </p>
 * <p>
 * <pre>
 * This implementation of the laplacian has a 3 by 3 kernel.
 *
 *            partial^2 f     partial^2 f
 * f(x,y) =   ~~~~~~~~~~~  +  ~~~~~~~~~~~
 *            partial x^2     partial x^2
 *
 *          [ 0   1   0 ]
 * kernel = [ 1  -4   1 ]
 *          [ 0   1   0 ]
 * </pre>
 * </p>
 * <p>
 * This formulation is derived by using the [-1 1 0] and [0 -1 1] difference kernels for the image derivative. Alternative
 * formulations can be found using other kernels.
 * </p>
 * <p>
 * DEVELOPER NOTE:  This is still a strong candidate for further optimizations due to redundant
 * array accesses.
 * </p>
 *
 * @author Peter Abeles
 */
public class DerivativeLaplacian {
	public static Kernel2D_S32 kernel_I32 = new Kernel2D_S32(3, new int[]{0,1,0,1,-4,1,0,1,0});
	public static Kernel2D_F32 kernel_F32 = new Kernel2D_F32(3, new float[]{0,1,0,1,-4,1,0,1,0});

	/**
	 * Computes the Laplacian of input image.
	 *
	 * @param orig  Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayU8 orig, GrayS16 deriv, @Nullable ImageBorder_S32<GrayU8> border ) {
		deriv.reshape(orig.width,orig.height);

		if( BoofConcurrency.USE_CONCURRENT ) {
			DerivativeLaplacian_Inner_MT.process(orig,deriv);
		} else {
			DerivativeLaplacian_Inner.process(orig,deriv);
		}

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernel_I32, border,deriv);
		}
	}

	/**
	 * Computes Laplacian on an U8 image but outputs derivative in a F32 image. Removes a step when processing
	 * images for feature detection
	 */
	public static void process(GrayU8 orig, GrayF32 deriv) {
		deriv.reshape(orig.width,orig.height);

		if( BoofConcurrency.USE_CONCURRENT ) {
			DerivativeLaplacian_Inner_MT.process(orig,deriv);
		} else {
			DerivativeLaplacian_Inner.process(orig,deriv);
		}

//		if( border != null ) {
//			border.setImage(orig);
//			ConvolveJustBorder_General_SB.convolve(kernel_I32, border,deriv);
//		}
	}

	/**
	 * Computes the Laplacian of 'orig'.
	 *
	 * @param orig  Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayF32 orig, GrayF32 deriv, @Nullable ImageBorder_F32 border) {
		deriv.reshape(orig.width,orig.height);

		if( BoofConcurrency.USE_CONCURRENT ) {
			DerivativeLaplacian_Inner_MT.process(orig,deriv);
		} else {
			DerivativeLaplacian_Inner.process(orig,deriv);
		}

		if( border != null ) {
			border.setImage(orig);
			ConvolveJustBorder_General_SB.convolve(kernel_F32, border,deriv);
		}
	}
}
