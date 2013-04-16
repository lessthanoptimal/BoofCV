/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.openkinect;

import boofcv.gui.d3.ColorPoint3D;
import boofcv.struct.FastQueue;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import org.openkinect.freenect.Resolution;

import java.nio.ByteBuffer;

/**
 * @author Peter Abeles
 */
public class UtilOpenKinect {

	public static String PATH_TO_SHARED_LIBRARY = "/home/pja/libfreenect/build/lib";

	public static final int FREENECT_DEPTH_MM_MAX_VALUE = 10000;
	public static final int FREENECT_DEPTH_MM_NO_VALUE = 0;

	public static int getWidth( Resolution resolution ) {
		if( resolution == Resolution.LOW )
			return 320;
		else if( resolution == Resolution.MEDIUM )
			return 640;
		else if( resolution == Resolution.HIGH )
			return 1280;

		throw new IllegalArgumentException("Unknown resolution "+resolution);
	}

	public static int getHeight( Resolution resolution ) {
		if( resolution == Resolution.LOW )
			return 240;
		else if( resolution == Resolution.MEDIUM )
			return 480;
		else if( resolution == Resolution.HIGH )
			return 1024;

		throw new IllegalArgumentException("Unknown resolution "+resolution);
	}

	public static void depthTo3D( IntrinsicParameters param , ImageUInt16 depth , FastQueue<ColorPoint3D> cloud ) {
		cloud.reset();


	}

	/**
	 * Converts data in a ByteBuffer into a 16bit depth image
	 * @param input Input buffer
	 * @param output Output depth image
	 */
	public static void bufferDepthToU16( ByteBuffer input , ImageUInt16 output ) {
		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				output.data[indexOut] = (short)((input.get(indexIn++) & 0xFF) | ((input.get(indexIn++) & 0xFF) << 8 ));
			}
		}
	}

	/**
	 * Converts data array into a 16bit depth image
	 * @param input Input data
	 * @param output Output depth image
	 */
	public static void bufferDepthToU16( byte[] input , ImageUInt16 output ) {
		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				output.data[indexOut] = (short)((input[indexIn++] & 0xFF) | ((input[indexIn++] & 0xFF) << 8 ));
			}
		}
	}

	/**
	 * Converts ByteBuffer that contains RGB data into a 3-channel MultiSpectral image
	 * @param input Input buffer
	 * @param output Output depth image
	 */
	public static void bufferRgbToMsU8( ByteBuffer input , MultiSpectral<ImageUInt8> output ) {
		ImageUInt8 band0 = output.getBand(0);
		ImageUInt8 band1 = output.getBand(1);
		ImageUInt8 band2 = output.getBand(2);

		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				band2.data[indexOut] = input.get(indexIn++);
				band1.data[indexOut] = input.get(indexIn++);
				band0.data[indexOut] = input.get(indexIn++);
			}
		}
	}

	/**
	 * Converts byte array that contains RGB data into a 3-channel MultiSpectral image
	 * @param input Input array
	 * @param output Output depth image
	 */
	public static void bufferRgbToMsU8( byte []input , MultiSpectral<ImageUInt8> output ) {
		ImageUInt8 band0 = output.getBand(0);
		ImageUInt8 band1 = output.getBand(1);
		ImageUInt8 band2 = output.getBand(2);

		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				band2.data[indexOut] = input[indexIn++];
				band1.data[indexOut] = input[indexIn++];
				band0.data[indexOut] = input[indexIn++];
			}
		}
	}
}
