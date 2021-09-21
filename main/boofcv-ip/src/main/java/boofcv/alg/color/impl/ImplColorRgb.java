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

import boofcv.struct.image.*;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Low level implementation of function for converting RGB images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplColorRgb {
	public static void rgbToGray_Weighted_U8( Planar<GrayU8> rgb, GrayU8 gray ) {
		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indexRgb + rgb.width;

			for (; indexRgb < end; indexRgb++) {
				int r = R.data[indexRgb] & 0xFF;
				int g = G.data[indexRgb] & 0xFF;
				int b = B.data[indexRgb] & 0xFF;

				gray.data[indedGra++] = (byte)((299*r + 587*g + 114*b)/1000);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToGray_Weighted_F32( Planar<GrayF32> rgb, GrayF32 gray ) {
		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indexRgb + rgb.width;

			for (; indexRgb < end; indexRgb++) {
				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				gray.data[indedGra++] = 0.299f*r + 0.587f*g + 0.114f*b;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToGray_Weighted_F64( Planar<GrayF64> rgb, GrayF64 gray ) {
		GrayF64 R = rgb.getBand(0);
		GrayF64 G = rgb.getBand(1);
		GrayF64 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indexRgb + rgb.width;

			for (; indexRgb < end; indexRgb++) {
				double r = R.data[indexRgb];
				double g = G.data[indexRgb];
				double b = B.data[indexRgb];

				gray.data[indedGra++] = 0.299*r + 0.587*g + 0.114*b;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToGray_Weighted( InterleavedU8 rgb, GrayU8 gray ) {

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indedGra + rgb.width;

			for (; indedGra < end; indedGra++) {
				int r = rgb.data[indexRgb++] & 0xFF;
				int g = rgb.data[indexRgb++] & 0xFF;
				int b = rgb.data[indexRgb++] & 0xFF;

				gray.data[indedGra] = (byte)((299*r + 587*g + 114*b)/1000);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToGray_Weighted( InterleavedF32 rgb, GrayF32 gray ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indedGra + rgb.width;

			for (; indedGra < end; indedGra++) {
				float r = rgb.data[indexRgb++];
				float g = rgb.data[indexRgb++];
				float b = rgb.data[indexRgb++];

				gray.data[indedGra] = 0.299f*r + 0.587f*g + 0.114f*b;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void rgbToGray_Weighted( InterleavedF64 rgb, GrayF64 gray ) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,rgb.height,row->{
		for (int row = 0; row < rgb.height; row++) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;
			int end = indedGra + rgb.width;

			for (; indedGra < end; indedGra++) {
				double r = rgb.data[indexRgb++];
				double g = rgb.data[indexRgb++];
				double b = rgb.data[indexRgb++];

				gray.data[indedGra] = 0.299*r + 0.587*g + 0.114*b;
			}
		}
		//CONCURRENT_ABOVE });
	}
}
