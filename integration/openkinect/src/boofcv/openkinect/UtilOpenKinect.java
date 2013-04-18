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

import boofcv.alg.distort.PixelToNormalized_F64;
import boofcv.struct.FastQueue;
import boofcv.struct.FastQueueArray_I32;
import boofcv.struct.GrowQueue_I8;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageUInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.openkinect.freenect.Resolution;

import java.io.*;
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

	/**
	 * Creates a point cloud from a depth image.
	 * @param param Intrinsic camera parameters for depth image
	 * @param depth depth image.  each value is in millimeters.
	 * @param cloud Output point cloud
	 */
	public static void depthTo3D( IntrinsicParameters param , ImageUInt16 depth , FastQueue<Point3D_F64> cloud ) {
		cloud.reset();

		PixelToNormalized_F64 p2n = new PixelToNormalized_F64();
		p2n.set(param.fx,param.fy,param.skew,param.cx,param.cy);

		Point2D_F64 n = new Point2D_F64();

		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ ) {
				int mm = depth.data[index++] & 0xFFFF;

				// skip pixels with no depth information
				if( mm == 0 )
					continue;

				// this could all be precomputed to speed it up
				p2n.compute(x,y,n);

				Point3D_F64 p = cloud.grow();
				p.z = mm;
				p.x = n.x*p.z;
				p.y = n.y*p.z;
			}
		}
	}

	/**
	 * Creates a point cloud from a depth image and saves the color information.  The depth and color images are
	 * assumed to be aligned.
	 * @param param Intrinsic camera parameters for depth image
	 * @param depth depth image.  each value is in millimeters.
	 * @param rgb Color image that's aligned to the depth.
	 * @param cloud Output point cloud
	 * @param cloudColor Output color for each point in the cloud
	 */
	public static void depthTo3D( IntrinsicParameters param , MultiSpectral<ImageUInt8> rgb , ImageUInt16 depth ,
								  FastQueue<Point3D_F64> cloud , FastQueueArray_I32 cloudColor ) {
		cloud.reset();
		cloudColor.reset();

		PixelToNormalized_F64 p2n = new PixelToNormalized_F64();
		p2n.set(param.fx,param.fy,param.skew,param.cx,param.cy);

		Point2D_F64 n = new Point2D_F64();

		ImageUInt8 colorR = rgb.getBand(0);
		ImageUInt8 colorG = rgb.getBand(1);
		ImageUInt8 colorB = rgb.getBand(2);

		for( int y = 0; y < depth.height; y++ ) {
			int index = depth.startIndex + y*depth.stride;
			for( int x = 0; x < depth.width; x++ ) {
				int mm = depth.data[index++] & 0xFFFF;

				// skip pixels with no depth information
				if( mm == 0 )
					continue;

				// this could all be precomputed to speed it up
				p2n.compute(x,y,n);

				Point3D_F64 p = cloud.grow();
				p.z = mm;
				p.x = n.x*p.z;
				p.y = n.y*p.z;

				int color[] = cloudColor.grow();

				color[0] = colorR.unsafe_get(x,y);
				color[1] = colorG.unsafe_get(x,y);
				color[2] = colorB.unsafe_get(x,y);
			}
		}
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
				band0.data[indexOut] = input.get(indexIn++);
				band1.data[indexOut] = input.get(indexIn++);
				band2.data[indexOut] = input.get(indexIn++);
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
				band0.data[indexOut] = input[indexIn++];
				band1.data[indexOut] = input[indexIn++];
				band2.data[indexOut] = input[indexIn++];
			}
		}
	}

	public static void savePPM( MultiSpectral<ImageUInt8> rgb , String fileName , byte[] buffer ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("P6\n%d %d\n255\n", rgb.width, rgb.height);
		os.write(header.getBytes());

		ImageUInt8 band0 = rgb.getBand(0);
		ImageUInt8 band1 = rgb.getBand(1);
		ImageUInt8 band2 = rgb.getBand(2);

		int indexOut = 0;
		for( int y = 0; y < rgb.height; y++ ) {
			int index = rgb.startIndex + y*rgb.stride;
			for( int x = 0; x < rgb.width; x++ , index++) {
				buffer[indexOut++] = band0.data[index];
				buffer[indexOut++] = band1.data[index];
				buffer[indexOut++] = band2.data[index];
			}
		}

		os.write(buffer,0,rgb.width*rgb.height*3);

		os.close();
	}

	public static void saveDepth( ImageUInt16 depth , String fileName , byte[] buffer ) throws IOException {
		File out = new File(fileName);
		DataOutputStream os = new DataOutputStream(new FileOutputStream(out));

		String header = String.format("%d %d\n", depth.width, depth.height);
		os.write(header.getBytes());

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

	public static void parseDepth( String fileName , ImageUInt16 depth , GrowQueue_I8 data ) throws IOException {
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
