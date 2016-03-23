/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.GrayU16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.ddogleg.struct.GrowQueue_I8;
import org.openkinect.freenect.Resolution;

import java.io.*;
import java.nio.ByteBuffer;

/**
 * @author Peter Abeles
 */
public class UtilOpenKinect {

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

	/**
	 * Converts data in a ByteBuffer into a 16bit depth image
	 * @param input Input buffer
	 * @param output Output depth image
	 */
	public static void bufferDepthToU16( ByteBuffer input , GrayU16 output ) {
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
	public static void bufferDepthToU16( byte[] input , GrayU16 output ) {
		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				output.data[indexOut] = (short)((input[indexIn++] & 0xFF) | ((input[indexIn++] & 0xFF) << 8 ));
			}
		}
	}

	/**
	 * Converts ByteBuffer that contains RGB data into a 3-channel Planar image
	 * @param input Input buffer
	 * @param output Output depth image
	 */
	public static void bufferRgbToMsU8( ByteBuffer input , Planar<GrayU8> output ) {
		GrayU8 band0 = output.getBand(0);
		GrayU8 band1 = output.getBand(1);
		GrayU8 band2 = output.getBand(2);

		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				band0.data[indexOut] = input.get(indexIn++);
				band1.data[indexOut] = input.get(indexIn++);
				band2.data[indexOut] = input.get(indexIn++);
			}
		}
	}

	/**
	 * Converts byte array that contains RGB data into a 3-channel Planar image
	 * @param input Input array
	 * @param output Output depth image
	 */
	public static void bufferRgbToMsU8( byte []input , Planar<GrayU8> output ) {
		GrayU8 band0 = output.getBand(0);
		GrayU8 band1 = output.getBand(1);
		GrayU8 band2 = output.getBand(2);

		int indexIn = 0;
		for( int y = 0; y < output.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				band0.data[indexOut] = input[indexIn++];
				band1.data[indexOut] = input[indexIn++];
				band2.data[indexOut] = input[indexIn++];
			}
		}
	}

	public static void saveDepth( GrayU16 depth , String fileName , GrowQueue_I8 data ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("%d %d\n", depth.width, depth.height);
		os.write(header.getBytes());

		data.resize(depth.width*depth.height*2);
		byte[] buffer = data.data;

		int indexOut = 0;
		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ , index++) {
				int pixel = depth.data[index];
				buffer[indexOut++] = (byte)(pixel&0xFF);
				buffer[indexOut++] = (byte)((pixel>>8) & 0xFF);
			}
		}
		os.write(buffer,0,depth.width*depth.height*2);
		os.close();
	}

	public static void parseDepth( String fileName , GrayU16 depth , GrowQueue_I8 data ) throws IOException {
		DataInputStream in = new DataInputStream(new FileInputStream(fileName));

		String s[] = readLine(in).split(" ");
		int w = Integer.parseInt(s[0]);
		int h = Integer.parseInt(s[1]);

		int length = w*h*2;
		if( data == null )
			data = new GrowQueue_I8(length);
		else
			data.resize(length);

		in.read(data.data,0,length);

		depth.reshape(w,h);
		UtilOpenKinect.bufferDepthToU16(data.data, depth);
	}

	private static String readLine( DataInputStream in ) throws IOException {
		String s = "";
		while( true ) {
			int b = in.read();

			if( b == '\n' )
				return s;
			else
				s += (char)b;
		}
	}
}
