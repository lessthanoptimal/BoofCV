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

package boofcv.alg.color.impl;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Low level implementation of function for converting YUV images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplColorYuv {
	public static void yuvToRgb_F32( Planar<GrayF32> yuv, Planar<GrayF32> rgb ) {
		GrayF32 Y = yuv.getBand(0);
		GrayF32 U = yuv.getBand(1);
		GrayF32 V = yuv.getBand(2);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,yuv.height,row->{
		for (int row = 0; row < yuv.height; row++) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < yuv.width; col++, indexYuv++, indexRgb++) {
				float y = Y.data[indexYuv];
				float u = U.data[indexYuv];
				float v = V.data[indexYuv];

				R.data[indexRgb] = y + 1.13983f*v;
				G.data[indexRgb] = y - 0.39465f*u - 0.58060f*v;
				B.data[indexRgb] = y + 2.032f*u;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToYuv_F32( Planar<GrayF32> rgb, Planar<GrayF32> yuv ) {
		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 Y = yuv.getBand(0);
		GrayF32 U = yuv.getBand(1);
		GrayF32 V = yuv.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,yuv.height,row->{
		for (int row = 0; row < yuv.height; row++) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < yuv.width; col++, indexYuv++, indexRgb++) {
				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				float y = 0.299f*r + 0.587f*g + 0.114f*b;

				Y.data[indexYuv] = y;
				U.data[indexYuv] = 0.492f*(b - y);
				V.data[indexYuv] = 0.877f*(r - y);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void ycbcrToRgb_U8( Planar<GrayU8> yuv, Planar<GrayU8> rgb ) {

		GrayU8 Y = yuv.getBand(0);
		GrayU8 U = yuv.getBand(1);
		GrayU8 V = yuv.getBand(2);

		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,yuv.height,row->{
		for (int row = 0; row < yuv.height; row++) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for (int col = 0; col < yuv.width; col++, indexYuv++, indexRgb++) {
				int y = 1191*((Y.data[indexYuv] & 0xFF) - 16);
				int cb = (U.data[indexYuv] & 0xFF) - 128;
				int cr = (V.data[indexYuv] & 0xFF) - 128;

				if (y < 0) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if (r < 0) r = 0;
				else if (r > 255) r = 255;
				if (g < 0) g = 0;
				else if (g > 255) g = 255;
				if (b < 0) b = 0;
				else if (b > 255) b = 255;

				R.data[indexRgb] = (byte)r;
				G.data[indexRgb] = (byte)g;
				B.data[indexRgb] = (byte)b;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
