/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;

/**
 * Hessian-Three derivative which processes the inner image only
 *
 * @see boofcv.alg.filter.derivative.HessianThreeDeterminant
 *
 * @author Peter Abeles
 */
public class HessianThreeDeterminant_Inner {
	public static void process(GrayU8 input, GrayS16 output) {
		final byte[] src = input.data;
		final short[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight() - 2;
		final int stride = input.stride;
		final int s2 = 2*stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(2,height,y->{
		for (int y = 2; y < height; y++) {
			int index = input.startIndex + stride * y + 2;
			int indexOut = output.startIndex + output.stride * y + 2;
			int endX = index + width - 4;

			for (; index < endX; index++) {
				int center = src[index]&0xFF;
				int Lxx = (src[index-2]&0xFF) - 2*center + (src[index+2]&0xFF);
				int Lyy = (src[index-s2]&0xFF) - 2*center + (src[index+s2]&0xFF);
				int Lxy = (src[index-stride-1] & 0xFF) + (src[index+stride+1] & 0xFF);
				Lxy -= (src[index-stride+1] & 0xFF) + (src[index+stride-1] & 0xFF);

				dst[indexOut++] = (short)(Lxx*Lyy-Lxy*Lxy);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void process(GrayU8 input, GrayF32 output) {
		final byte[] src = input.data;
		final float[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight() - 2;
		final int stride = input.stride;
		final int s2 = 2*stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(2,height,y->{
		for (int y = 2; y < height; y++) {
			int index = input.startIndex + stride * y + 2;
			int indexOut = output.startIndex + output.stride * y + 2;
			int endX = index + width - 4;

			for (; index < endX; index++) {
				int center = src[index]&0xFF;
				int Lxx = (src[index-2]&0xFF) - 2*center + (src[index+2]&0xFF);
				int Lyy = (src[index-s2]&0xFF) - 2*center + (src[index+s2]&0xFF);
				int Lxy = (src[index-stride-1] & 0xFF) + (src[index+stride+1] & 0xFF);
				Lxy -= (src[index-stride+1] & 0xFF) + (src[index+stride-1] & 0xFF);

				dst[indexOut++] = Lxx*Lyy-Lxy*Lxy;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void process(GrayF32 input, GrayF32 output) {
		final float[] src = input.data;
		final float[] dst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight() - 2;
		final int stride = input.stride;
		final int s2 = 2*stride;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(2,height,y->{
		for (int y = 2; y < height; y++) {
			int index = input.startIndex + stride * y + 2;
			int indexOut = output.startIndex + output.stride * y + 2;
			int endX = index + width - 4;

			for (; index < endX; index++) {
				float center = src[index];
				float Lxx = src[index-2] - 2*center + src[index+2];
				float Lyy = src[index-s2] - 2*center + src[index+s2];
				float Lxy = src[index-stride-1] + src[index+stride+1] - (src[index-stride+1] + src[index+stride-1]);

				dst[indexOut++] = Lxx*Lyy-Lxy*Lxy;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
