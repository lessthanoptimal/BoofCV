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

package boofcv.alg.color;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

/**
 * <p>
 * Color conversion between YUV and RGB, and YCbCr and RGB.  YUV is color encoding which takes in account a human
 * perception model and is commonly used in cameras/video with analog signals. YCbCr is the digital equivalent
 * color space of YUV that is scaled and offset.  YCbCr is also commonly referred to as YUV, even though it is a
 * different format.
 * </p>
 *
 * <p>
 * Y component encodes luma (B&W gray scale) and U,V encodes color. The same is true for Y,Cb,Cr, respectively.
 * In the functions below, if the input is floating point then YUV is used, otherwise YCbCr is used.  How
 * these color formats are encoded varies widely. For YCbCr, Y is "defined to have a nominal 8-bit range of 16-235,
 * Cb and Cr are defined to have a nominal range of 16-240", see [2].
 * </p>
 *
 * <b>Equations:</b>
 * <pre>
 * Y =  0.299*R + 0.587*G + 0.114*B
 * U = -0.147*R - 0.289*G + 0.436*B = 0.492*(B - Y)
 * V =  0.615*R - 0.515*G - 0.100*B = 0.877*(R - Y)</pre>
 * <p>Source: [1] and [2].</p>
 *
 * <b>Citations:</b>
 * <ol>
 *     <li>http://software.intel.com/sites/products/documentation/hpc/ipp/ippi/ippi_ch6/ch6_color_models.html</li>
 *     <li>Keith Jack, "Video Demystified" 5th ed 2007</li>
 * </ol>
 *
 * @author Peter Abeles
 */
public class ColorYuv {

	/**
	 * Conversion from RGB to YUV using same equations as Intel IPP.
	 */
	public static void rgbToYuv( double r , double g , double b , double yuv[] ) {
		double y = yuv[0] = 0.299*r + 0.587*g + 0.114*b;
		yuv[1] = 0.492*(b-y);
		yuv[2] = 0.877*(r-y);
	}

	/**
	 * Conversion from RGB to YUV using same equations as Intel IPP.
	 */
	public static void rgbToYuv( float r , float g , float b , float yuv[] ) {
		float y = yuv[0] = 0.299f*r + 0.587f*g + 0.114f*b;
		yuv[1] = 0.492f*(b-y);
		yuv[2] = 0.877f*(r-y);
	}

	/**
	 * Conversion from YUV to RGB using same equations as Intel IPP.
	 */
	public static void yuvToRgb( double y , double u , double v , double rgb[] ) {
		rgb[0] = y + 1.13983*v;
		rgb[1] = y - 0.39465*u - 0.58060*v;
		rgb[2] = y + 2.032*u;
	}


	/**
	 * Conversion from YUV to RGB using same equations as Intel IPP.
	 */
	public static void yuvToRgb( float y , float u , float v , float rgb[] ) {
		rgb[0] = y + 1.13983f*v;
		rgb[1] = y - 0.39465f*u - 0.58060f*v;
		rgb[2] = y + 2.032f*u;
	}
	/**
	 * Conversion from RGB to YCbCr.  See [Jack07].
	 *
	 * @param r Red [0 to 255]
	 * @param g Green [0 to 255]
	 * @param b Blue [0 to 255]
	 */
	public static void rgbToYCbCr( int r , int g , int b , byte yuv[] ) {
		// multiply coefficients in book by 1024, which is 2^10
		yuv[0] = (byte)((( 187*r + 629*g + 63*b ) >> 10) + 16);
		yuv[1] = (byte)(((-103*r - 346*g + 450*b) >> 10) + 128);
		yuv[2] = (byte)((( 450*r - 409*g - 41*b ) >> 10) + 128);
	}

	/**
	 * Conversion from YCbCr to RGB.  See [Jack07].
	 *
	 * @param y Y [0 to 255]
	 * @param cb Cb [0 to 255]
	 * @param cr Cr [0 to 255]
	 */
	public static void ycbcrToRgb( int y , int cb , int cr , byte rgb[] ) {
		// multiply coefficients in book by 1024, which is 2^10
		y = 1191*(y - 16);
		if( y < 0 ) y = 0;
		cb -= 128;
		cr -= 128;

		int r = (y + 1836*cr) >> 10;
		int g = (y - 547*cr - 218*cb) >> 10;
		int b = (y + 2165*cb) >> 10;

		if( r < 0 ) r = 0;
		else if( r > 255 ) r = 255;
		if( g < 0 ) g = 0;
		else if( g > 255 ) g = 255;
		if( b < 0 ) b = 0;
		else if( b > 255 ) b = 255;

		rgb[0] = (byte)r;
		rgb[1] = (byte)g;
		rgb[2] = (byte)b;
	}

	/**
	 * Convert a 3-channel {@link Planar} image from YUV into RGB.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param yuv (Output) YUV encoded image
	 */
	public static void yuvToRgb_F32(Planar<GrayF32> yuv , Planar<GrayF32> rgb ) {

		InputSanityCheck.checkSameShape(yuv,rgb);

		GrayF32 Y = yuv.getBand(0);
		GrayF32 U = yuv.getBand(1);
		GrayF32 V = yuv.getBand(2);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		for( int row = 0; row < yuv.height; row++ ) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < yuv.width; col++ , indexYuv++ , indexRgb++) {
				float y = Y.data[indexYuv];
				float u = U.data[indexYuv];
				float v = V.data[indexYuv];

				R.data[indexRgb] = y + 1.13983f*v;
				G.data[indexRgb] = y - 0.39465f*u - 0.58060f*v;
				B.data[indexRgb] = y + 2.032f*u;
			}
		}
	}

	/**
	 * Convert a 3-channel {@link Planar} image from RGB into YUV.
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param yuv (Output) YUV encoded image
	 */
	public static void rgbToYuv_F32(Planar<GrayF32> rgb , Planar<GrayF32> yuv ) {

		InputSanityCheck.checkSameShape(yuv,rgb);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 Y = yuv.getBand(0);
		GrayF32 U = yuv.getBand(1);
		GrayF32 V = yuv.getBand(2);

		for( int row = 0; row < yuv.height; row++ ) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < yuv.width; col++ , indexYuv++ , indexRgb++) {
				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				float y = 0.299f*r + 0.587f*g + 0.114f*b;

				Y.data[indexYuv] = y;
				U.data[indexYuv] = 0.492f*(b-y);
				V.data[indexYuv] = 0.877f*(r-y);
			}
		}
	}

	/**
	 * Conversion from YCbCr to RGB.
	 *
	 * NOTE: Input and output image can be the same instance.
	 *
	 * @param yuv YCbCr encoded 8-bit image
	 * @param rgb RGB encoded 8-bit image
	 */
	public static void ycbcrToRgb_U8(Planar<GrayU8> yuv , Planar<GrayU8> rgb ) {

		GrayU8 Y = yuv.getBand(0);
		GrayU8 U = yuv.getBand(1);
		GrayU8 V = yuv.getBand(2);

		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		for( int row = 0; row < yuv.height; row++ ) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < yuv.width; col++ , indexYuv++ , indexRgb++) {
				int y = 1191*((Y.data[indexYuv]&0xFF) - 16);
				int cb = (U.data[indexYuv]&0xFF) - 128;
				int cr = (V.data[indexYuv]&0xFF) - 128;

				if( y < 0 ) y = 0;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

				if( r < 0 ) r = 0;
				else if( r > 255 ) r = 255;
				if( g < 0 ) g = 0;
				else if( g > 255 ) g = 255;
				if( b < 0 ) b = 0;
				else if( b > 255 ) b = 255;

				R.data[indexRgb] = (byte)r;
				G.data[indexRgb] = (byte)g;
				B.data[indexRgb] = (byte)b;
			}
		}
	}

}
