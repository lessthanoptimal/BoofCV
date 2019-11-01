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

package boofcv.alg.transform.census.impl;

import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * @author Peter Abeles
 */
public class ImplCensusTransformInner {
	public static void region3x3(final GrayU8 input , final GrayU8 output ) {
		final int height = input.height-1;
		final byte[] src = input.data;

		// pre-compute offsets to pixels. row-major starting from upper row
		final int offset0 = -input.stride-1;
		final int offset1 = -input.stride;
		final int offset2 = -input.stride+1;
		final int offset3 = -1;
		final int offset5 = +1;
		final int offset6 = input.stride-1;
		final int offset7 = input.stride;
		final int offset8 = input.stride+1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(1,height,y->{
		for (int y = 1; y < height; y++) {
			int indexSrc = input.startIndex  + y*input.stride  + 1;
			int indexDst = output.startIndex + y*output.stride + 1;

			final int end = indexDst+input.width-2;
//			for (int x = 1; x < width-1; x++) {
			while( indexDst < end ) {
				int center = src[indexSrc]&0xFF;

				int census = 0;

				if( (src[indexSrc+offset0]&0xFF) > center)
					census |= 0x01;
				if( (src[indexSrc+offset1]&0xFF) > center)
					census |= 0x02;
				if( (src[indexSrc+offset2]&0xFF) > center)
					census |= 0x04;
				if( (src[indexSrc+offset3]&0xFF) > center)
					census |= 0x08;
				if( (src[indexSrc+offset5]&0xFF) > center)
					census |= 0x10;
				if( (src[indexSrc+offset6]&0xFF) > center)
					census |= 0x20;
				if( (src[indexSrc+offset7]&0xFF) > center)
					census |= 0x40;
				if( (src[indexSrc+offset8]&0xFF) > center)
					census |= 0x80;

				output.data[indexDst] = (byte)census;

				indexDst++;
				indexSrc++;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void region5x5(final GrayU8 input , final GrayS32 output ) {
		final int height = input.height-2;
		final byte[] src = input.data;

		// pre-compute offsets to pixels. row-major starting from upper row
		final int offset00 = -2*input.stride-2;
		final int offset01 = -2*input.stride-1;
		final int offset02 = -2*input.stride;
		final int offset03 = -2*input.stride+1;
		final int offset04 = -2*input.stride+2;
		final int offset10 = -input.stride-2;
		final int offset11 = -input.stride-1;
		final int offset12 = -input.stride;
		final int offset13 = -input.stride+1;
		final int offset14 = -input.stride+2;
		final int offset20 = -2;
		final int offset21 = -1;
		final int offset23 = +1;
		final int offset24 = +2;
		final int offset30 = input.stride-2;
		final int offset31 = input.stride-1;
		final int offset32 = input.stride;
		final int offset33 = input.stride+1;
		final int offset34 = input.stride+2;
		final int offset40 = 2*input.stride-2;
		final int offset41 = 2*input.stride-1;
		final int offset42 = 2*input.stride;
		final int offset43 = 2*input.stride+1;
		final int offset44 = 2*input.stride+2;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(2,height,y->{
		for (int y = 2; y < height; y++) {
			int indexSrc = input.startIndex  + y*input.stride  + 2;
			int indexDst = output.startIndex + y*output.stride + 2;

			final int end = indexDst+input.width-4;
//			for (int x = 2; x < width-2; x++) {
			while( indexDst < end ) {
				int center = src[indexSrc]&0xFF;

				int census = 0;

				if( (src[indexSrc+offset00]&0xFF) > center)
					census |= 0x000001;
				if( (src[indexSrc+offset01]&0xFF) > center)
					census |= 0x000002;
				if( (src[indexSrc+offset02]&0xFF) > center)
					census |= 0x000004;
				if( (src[indexSrc+offset03]&0xFF) > center)
					census |= 0x000008;
				if( (src[indexSrc+offset04]&0xFF) > center)
					census |= 0x000010;
				if( (src[indexSrc+offset10]&0xFF) > center)
					census |= 0x000020;
				if( (src[indexSrc+offset11]&0xFF) > center)
					census |= 0x000040;
				if( (src[indexSrc+offset12]&0xFF) > center)
					census |= 0x000080;
				if( (src[indexSrc+offset13]&0xFF) > center)
					census |= 0x000100;
				if( (src[indexSrc+offset14]&0xFF) > center)
					census |= 0x000200;
				if( (src[indexSrc+offset20]&0xFF) > center)
					census |= 0x000400;
				if( (src[indexSrc+offset21]&0xFF) > center)
					census |= 0x000800;
				if( (src[indexSrc+offset23]&0xFF) > center)
					census |= 0x001000;
				if( (src[indexSrc+offset24]&0xFF) > center)
					census |= 0x002000;
				if( (src[indexSrc+offset30]&0xFF) > center)
					census |= 0x004000;
				if( (src[indexSrc+offset31]&0xFF) > center)
					census |= 0x008000;
				if( (src[indexSrc+offset32]&0xFF) > center)
					census |= 0x010000;
				if( (src[indexSrc+offset33]&0xFF) > center)
					census |= 0x020000;
				if( (src[indexSrc+offset34]&0xFF) > center)
					census |= 0x040000;
				if( (src[indexSrc+offset40]&0xFF) > center)
					census |= 0x080000;
				if( (src[indexSrc+offset41]&0xFF) > center)
					census |= 0x100000;
				if( (src[indexSrc+offset42]&0xFF) > center)
					census |= 0x200000;
				if( (src[indexSrc+offset43]&0xFF) > center)
					census |= 0x400000;
				if( (src[indexSrc+offset44]&0xFF) > center)
					census |= 0x800000;

				output.data[indexDst] = census;

				indexDst++;
				indexSrc++;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
