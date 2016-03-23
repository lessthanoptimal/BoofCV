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

package boofcv.core.encoding;

import boofcv.struct.image.*;

/**
 * NV21:  The format is densely packed.  Y is full resolution and UV are interlaced and 1/2 resolution.
 *        So same UV values within a 2x2 square
 *
 * @author Peter Abeles
 */
public class ImplConvertNV21 {

	/**
	 * First block contains gray-scale information and UV data can be ignored.
	 */
	public static void nv21ToGray(byte[] dataNV, GrayU8 output) {

		final int yStride = output.width;

		// see if the whole thing can be copied as one big block to maximize speed
		if( yStride == output.width && !output.isSubimage() ) {
			System.arraycopy(dataNV,0,output.data,0,output.width*output.height);
		} else {
			// copy one row at a time
			for( int y = 0; y < output.height; y++ ) {
				int indexOut = output.startIndex + y*output.stride;

				System.arraycopy(dataNV,y*yStride,output.data,indexOut,output.width);
			}
		}
	}

	/**
	 * First block contains gray-scale information and UV data can be ignored.
	 */
	public static void nv21ToGray(byte[] dataNV, GrayF32 output) {

		for( int y = 0; y < output.height; y++ ) {
			int indexIn = y*output.width;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width; x++ ) {
				output.data[ indexOut++ ] = dataNV[ indexIn++ ] & 0xFF;
			}
		}
	}

	public static void nv21ToMultiYuv_U8(byte[] dataNV, Planar<GrayU8> output) {

		GrayU8 Y = output.getBand(0);
		GrayU8 U = output.getBand(1);
		GrayU8 V = output.getBand(2);

		final int uvStride = output.width/2;

		nv21ToGray(dataNV, Y);

		int startUV = output.width*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				U.data[indexOut] = dataNV[ indexUV     ];
				V.data[indexOut] = dataNV[ indexUV + 1 ];

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiYuv_F32(byte[] dataNV, Planar<GrayF32> output) {

		GrayF32 Y = output.getBand(0);
		GrayF32 U = output.getBand(1);
		GrayF32 V = output.getBand(2);

		final int uvStride = output.width/2;

		nv21ToGray(dataNV, Y);

		final int startUV = output.width*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				U.data[indexOut] = (dataNV[ indexUV     ]&0xFF)-128;
				V.data[indexOut] = (dataNV[ indexUV + 1 ]&0xFF)-128;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiRgb_U8(byte[] dataNV, Planar<GrayU8> output) {

		GrayU8 R = output.getBand(0);
		GrayU8 G = output.getBand(1);
		GrayU8 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*((dataNV[indexY++] & 0xFF) - 16);
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = (byte)r;
				G.data[indexOut] = (byte)g;
				B.data[indexOut] = (byte)b;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToInterleaved_U8(byte[] dataNV, InterleavedU8 output) {

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ ) {
				int y = 1191*((dataNV[indexY++] & 0xFF) - 16);
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				output.data[indexOut++] = (byte)r;
				output.data[indexOut++] = (byte)g;
				output.data[indexOut++] = (byte)b;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToMultiRgb_F32(byte[] dataNV, Planar<GrayF32> output) {

		GrayF32 R = output.getBand(0);
		GrayF32 G = output.getBand(1);
		GrayF32 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*((dataNV[indexY++] & 0xFF) - 16);
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = r;
				G.data[indexOut] = g;
				B.data[indexOut] = b;

				indexUV += 2*(col&0x1);
			}
		}
	}

	public static void nv21ToInterleaved_F32(byte[] dataNV, InterleavedF32 output) {

		final int yStride = output.width;
		final int uvStride = output.width/2;

		final int startUV = yStride*output.height;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = startUV + (row/2)*(2*uvStride);
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ ) {
				int y = 1191*((dataNV[indexY++] & 0xFF) - 16);
				int cr = (dataNV[ indexUV ] & 0xFF) - 128;
				int cb = (dataNV[ indexUV+1] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				output.data[indexOut++] = r;
				output.data[indexOut++] = g;
				output.data[indexOut++] = b;

				indexUV += 2*(col&0x1);
			}
		}
	}
}
