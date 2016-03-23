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

package boofcv.alg.filter.derivative;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

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
 * This formulation is derived by using the [-1 1 0] and [0 -1 1] difference kernels for the image derivative.  Alternative
 * formulations can be found using other kernels.
 * </p>
 * <p>
 * DEVELOPER NOTE:  This is still a strong candidate for further optimizations due to redundant
 * array accesses.
 * </p>
 *
 * @author Peter Abeles
 */
// TODO process image borders
// TODO create a generator for these functions
public class LaplacianEdge {
	public static Kernel2D_I32 kernel_I32 = new Kernel2D_I32(3, new int[]{0,1,0,1,-4,1,0,1,0});
	public static Kernel2D_F32 kernel_F32 = new Kernel2D_F32(3, new float[]{0,1,0,1,-4,1,0,1,0});

	/**
	 * Computes the Laplacian of input image.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayU8 orig, GrayS16 deriv) {
		InputSanityCheck.checkSameShape(orig, deriv);

		final byte[] data = orig.data;
		final short[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + stride * y + 1;
			int indexOut = deriv.startIndex + deriv.stride * y + 1;
			int endX = index + width - 2;

			for (; index < endX; index++) {

				int v = data[index - stride] & 0xFF;
				v += data[index - 1] & 0xFF;
				v += -4 * (data[index] & 0xFF);
				v += data[index + 1] & 0xFF;
				v += data[index + stride] & 0xFF;

				out[indexOut++] = (short) v;
			}
		}
	}

	public static void process(GrayU8 orig, GrayF32 deriv) {
		InputSanityCheck.checkSameShape(orig, deriv);

		final byte[] data = orig.data;
		final float[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + stride * y + 1;
			int indexOut = deriv.startIndex + deriv.stride * y + 1;
			int endX = index + width - 2;

			for (; index < endX; index++) {

				int v = data[index - stride] & 0xFF;
				v += data[index - 1] & 0xFF;
				v += -4 * (data[index] & 0xFF);
				v += data[index + 1] & 0xFF;
				v += data[index + stride] & 0xFF;

				out[indexOut++] = v;
			}
		}
	}

	/**
	 * Computes the Laplacian of 'orig'.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayF32 orig, GrayF32 deriv) {
		InputSanityCheck.checkSameShape(orig, deriv);

		final float[] data = orig.data;
		final float[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		for (int y = 1; y < height; y++) {
			int index = orig.startIndex + stride * y + 1;
			int indexOut = deriv.startIndex + deriv.stride * y + 1;
			int endX = index + width - 2;

			for (; index < endX; index++) {

				float v = data[index - stride] + data[index - 1];
				v += data[index + 1];
				v += data[index + stride];

				out[indexOut++] = v - 4.0f * data[index];
			}
		}
	}
}
