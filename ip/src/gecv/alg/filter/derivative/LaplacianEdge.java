/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.derivative;

import gecv.alg.InputSanityCheck;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

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
 *          Integer Images    Floating Point Images
 *          [ 0   1   0 ]      [  0   0.25  0   ]
 * kernel = [ 1  -4   1 ]  or  [ 0.25  -1  0.25 ]
 *          [ 0   1   0 ]      [  0   0.25  0   ]
 * </pre>
 * </p>
 * <p>
 * DEVELOPER NOTE:  This is still a strong candidate for further optimizations due to redundant
 * array accesses.
 * </p>
 *
 * @author Peter Abeles
 */
public class LaplacianEdge {


	/**
	 * Computes the Laplacean of 'orig'.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process_I8(ImageUInt8 orig, ImageSInt16 deriv) {
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

	/**
	 * Computes the Laplacean of 'orig'.
	 *
	 * @param orig  Input image.  Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process_F32(ImageFloat32 orig, ImageFloat32 deriv) {
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

				out[indexOut++] = 0.25f * v - data[index];
			}
		}
	}
}
