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

package boofcv.alg.filter.derivative.impl;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;
import boofcv.alg.filter.derivative.DerivativeLaplacian;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * Laplacian which processes the inner image only
 *
 * @see DerivativeLaplacian
 *
 * @author Peter Abeles
 */
public class DerivativeLaplacian_Inner {
	/**
	 * Computes the Laplacian of input image.
	 *
	 * @param orig  Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayU8 orig, GrayS16 deriv) {
		final byte[] data = orig.data;
		final short[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
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
		//CONCURRENT_ABOVE });
	}

	public static void process(GrayU8 orig, GrayF32 deriv) {
		final byte[] data = orig.data;
		final float[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
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
		//CONCURRENT_ABOVE });
	}

	/**
	 * Computes the Laplacian of 'orig'.
	 *
	 * @param orig  Input image. Not modified.
	 * @param deriv Where the Laplacian is written to. Modified.
	 */
	public static void process(GrayF32 orig, GrayF32 deriv) {
		final float[] data = orig.data;
		final float[] out = deriv.data;

		final int width = orig.getWidth();
		final int height = orig.getHeight() - 1;
		final int stride = orig.stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
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
		//CONCURRENT_ABOVE });
	}
}
