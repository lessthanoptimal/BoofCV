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

package boofcv.alg.color;

import boofcv.alg.InputSanityCheck;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * <p>
 * Color conversion between YUV and RGB.  YUV is color encoding which takes in account a human perception model and
 * is commonly used in cameras/video with analog signals. Y component encodes luma (B&W gray scale) and UV
 * encodes color. Unfortunately there are a ton of different ways to encode YUV amd coefficients can vary depending
 * on the source.  YCbCr is also commonly referred to as YUV, even though it is a different format.
 * </p>
 *
 * <p>
 * Sources for equations:
 * <ul>
 *     <li>http://en.wikipedia.org/wiki/YUV</li>
 *     <li>http://software.intel.com/sites/products/documentation/hpc/ipp/ippi/ippi_ch6/ch6_color_models.html</li>
 * </ul>
 * </p>
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
	 * Conversion from YUV to RGB using suggested integer approximation.  Y values have a range of 16 to 235.
	 * U,V have a full range of 0 to 255.
	 */
	public static void rgbToYuv( int r , int g , int b , int yuv[] ) {
		int y =   66*r + 129*g +  25*b;
		int u =  -38*r -  74*g + 112*b;
		int v =  112*r -  94*g -  18*b;

		yuv[0] = ((y + 128) >> 8) + 16;
		yuv[1] = ((u + 128) >> 8) + 128;
		yuv[2] = ((v + 128) >> 8) + 128;
	}

	/**
	 * Convert a 3-channel {@link MultiSpectral} image from YUV into RGB.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param yuv (Output) YUV encoded image
	 */
	public static void yuvToRgb_F32( MultiSpectral<ImageFloat32> yuv , MultiSpectral<ImageFloat32> rgb ) {

		InputSanityCheck.checkSameShape(yuv,rgb);

		ImageFloat32 Y = yuv.getBand(0);
		ImageFloat32 U = yuv.getBand(1);
		ImageFloat32 V = yuv.getBand(2);

		ImageFloat32 R = rgb.getBand(0);
		ImageFloat32 G = rgb.getBand(1);
		ImageFloat32 B = rgb.getBand(2);

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
	 * Convert a 3-channel {@link MultiSpectral} image from RGB into YUV.
	 *
	 * @param rgb (Input) RGB encoded image
	 * @param yuv (Output) YUV encoded image
	 */
	public static void rgbToYuv_F32( MultiSpectral<ImageFloat32> rgb , MultiSpectral<ImageFloat32> yuv ) {

		InputSanityCheck.checkSameShape(yuv,rgb);

		ImageFloat32 R = rgb.getBand(0);
		ImageFloat32 G = rgb.getBand(1);
		ImageFloat32 B = rgb.getBand(2);

		ImageFloat32 Y = yuv.getBand(0);
		ImageFloat32 U = yuv.getBand(1);
		ImageFloat32 V = yuv.getBand(2);

		for( int row = 0; row < yuv.height; row++ ) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < yuv.width; col++ , indexYuv++ , indexRgb++) {
				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				float y = 0.299f*r + 0.587f*g + 0.114f*b;

				Y.data[indexRgb] = y;
				U.data[indexRgb] = 0.492f*(b-y);
				V.data[indexRgb] = 0.877f*(r-y);
			}
		}
	}

	/**
	 * Conversion from YUV to RGB based on NTSC standard.
	 *
	 * @param yuv YUV encoded 8-bit image
	 * @param rgb RGB encoded 8-bit image
	 */
	public static void yuvToRgb_U8( MultiSpectral<ImageUInt8> yuv , MultiSpectral<ImageUInt8> rgb ) {

		ImageUInt8 Y = yuv.getBand(0);
		ImageUInt8 U = yuv.getBand(1);
		ImageUInt8 V = yuv.getBand(2);

		ImageUInt8 R = rgb.getBand(0);
		ImageUInt8 G = rgb.getBand(1);
		ImageUInt8 B = rgb.getBand(2);

		for( int row = 0; row < yuv.height; row++ ) {
			int indexYuv = yuv.startIndex + row*yuv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < yuv.width; col++ , indexYuv++ , indexRgb++) {
				int c = (Y.data[indexYuv]&0xFF) - 16;
				int d = (U.data[indexYuv]&0xFF) - 128;
				int e = (V.data[indexYuv]&0xFF) - 128;

				int r = (298*c + 409*e + 128) >> 8;
				int g = (298*c - 100*d - 208*e + 128) >> 8;
				int b = (298*c + 516*d + 128) >> 8;

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
