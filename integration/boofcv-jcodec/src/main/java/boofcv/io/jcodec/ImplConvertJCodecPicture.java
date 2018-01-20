/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.io.jcodec;

import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

/**
 * @author Peter Abeles
 */
public class ImplConvertJCodecPicture {
	/**
	 * Converts a picture in JCodec RGB into RGB BoofCV
	 */
	public static void RGB_to_PLU8(Picture input, Planar<GrayU8> output) {
		if( input.getColor() != ColorSpace.RGB )
			throw new RuntimeException("Unexpected input color space!");
		if( output.getNumBands() != 3 )
			throw new RuntimeException("Unexpected number of bands in output image!");

		output.reshape(input.getWidth(),input.getHeight());

		int dataIn[] = input.getData()[0];

		GrayU8 out0 = output.getBand(0);
		GrayU8 out1 = output.getBand(1);
		GrayU8 out2 = output.getBand(2);

		int indexIn = 0;
		int indexOut = 0;
		for (int i = 0; i < output.height; i++) {
			for (int j = 0; j < output.width; j++, indexOut++ ) {
				int r = dataIn[indexIn++];
				int g = dataIn[indexIn++];
				int b = dataIn[indexIn++];

				out2.data[indexOut] = (byte)r;
				out1.data[indexOut] = (byte)g;
				out0.data[indexOut] = (byte)b;
			}
		}
	}


	public static void yuv420_to_PlRgb_U8(Picture input, Planar<GrayU8> output) {

		int[] Y = input.getPlaneData(0);
		int[] U = input.getPlaneData(1);
		int[] V = input.getPlaneData(2);

		GrayU8 R = output.getBand(0);
		GrayU8 G = output.getBand(1);
		GrayU8 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = (row/2)*uvStride;
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(Y[indexY++] & 0xFF) - 16;
				int cr = (U[indexUV] & 0xFF) - 128;
				int cb = (V[indexUV] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int b = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int r = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = (byte)r;
				G.data[indexOut] = (byte)g;
				B.data[indexOut] = (byte)b;

				indexUV += col&0x1;
			}
		}
	}

	public static void yuv420_to_PlRgb_F32(Picture input, Planar<GrayF32> output) {

		int[] Y = input.getPlaneData(0);
		int[] U = input.getPlaneData(1);
		int[] V = input.getPlaneData(2);

		GrayF32 R = output.getBand(0);
		GrayF32 G = output.getBand(1);
		GrayF32 B = output.getBand(2);

		final int yStride = output.width;
		final int uvStride = output.width/2;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexUV = (row/2)*uvStride;
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(Y[indexY++] & 0xFF) - 16;
				int cr = (U[indexUV] & 0xFF) - 128;
				int cb = (V[indexUV] & 0xFF) - 128;

				if( y < 0 ) y = 0;

				int b = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int r = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				R.data[indexOut] = r;
				G.data[indexOut] = g;
				B.data[indexOut] = b;

				indexUV += col&0x1;
			}
		}
	}

	public static void yuv420_to_U8(Picture input, GrayU8 output) {

		int[] Y = input.getPlaneData(0);

		final int yStride = output.width;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(Y[indexY++] & 0xFF) - 16;
				if( y < 0 ) y = 0;
				y = y >> 10;

				if( y < 0 ) y = 0; else if( y > 255 ) y = 255;

				output.data[indexOut] = (byte)y;
			}
		}
	}

	public static void yuv420_to_F32(Picture input, GrayF32 output) {

		int[] Y = input.getPlaneData(0);

		final int yStride = output.width;

		for( int row = 0; row < output.height; row++ ) {
			int indexY = row*yStride;
			int indexOut = output.startIndex + row*output.stride;

			for( int col = 0; col < output.width; col++ , indexOut++ ) {
				int y = 1191*(Y[indexY++] & 0xFF) - 16;
				if( y < 0 ) y = 0;
				y = y >> 10;

				if( y < 0 ) y = 0; else if( y > 255 ) y = 255;

				output.data[indexOut] = y;
			}
		}
	}
}
