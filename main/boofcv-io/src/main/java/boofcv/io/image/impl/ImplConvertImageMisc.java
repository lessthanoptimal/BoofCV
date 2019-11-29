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

package boofcv.io.image.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU16;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

public class ImplConvertImageMisc {

	public static void convert_F32_U16(GrayF32 src, int fractionBits, GrayU16 dst) {
		final float scale = 1 << fractionBits;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
		for (int y = 0; y < src.height; y++) {
			int idxSrc = src.startIndex + y*src.stride;
			int idxDst = dst.startIndex + y*dst.stride;

			int end = idxSrc + src.width;

			while( idxSrc != end ) {
//			for (int x = 0; x < src.width; x++) {
				dst.data[idxDst++] = (short)(src.data[idxSrc++]*scale + 0.5f);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convert_U16_F32(GrayU16 src, int fractionBits, GrayF32 dst) {
		float scale = 1 << fractionBits;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
		for (int y = 0; y < src.height; y++) {
			int idxSrc = src.startIndex + y*src.stride;
			int idxDst = dst.startIndex + y*dst.stride;

			int end = idxSrc + src.width;

			while( idxSrc != end ) {
//			for (int x = 0; x < src.width; x++) {
				dst.data[idxDst++] = (src.data[idxSrc++]&0xFFFF)/scale;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
