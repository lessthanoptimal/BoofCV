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
import boofcv.struct.image.Planar;

/**
 * <p>
 * Color conversion between RGB and HSV color spaces.  HSV stands for Hue-Saturation-Value.  "Hue" has a range of [0,2*PI]
 * and "Saturation" has a range of [0,1], the two together represent the color.  While "Value" has the same range as the
 * input pixels and represents how light/dark the color is. Original algorithm taken from [1] and modified slightly.
 * </p>
 *
 * <p>
 * NOTE: The hue is represented in radians instead of degrees, as is often done.<br>
 * NOTE: Hue will be set to NaN if it is undefined.  It is undefined when chroma is zero, which happens when the input
 * color is a pure gray (e.g. same value across all color bands).
 * </p>
 *
 * <p> RGB to HSV:</pr>
 * <pre>
 * min = min(r,g,b)
 * max = max(r,g,b)
 * delta = max-min  // this is the chroma
 * value = max
 *
 * if( max != 0 )
 *   saturation = delta/max
 * else
 *   saturation = 0;
 *   hue = NaN
 *
 * if( r == max )
 *   hue = (g-b)/delta
 * else if( g == max )
 *   hue = 2 + (b-r)/delta
 * else
 *   hue = 4 + (r-g)/delta
 *
 * hue *= 60.0*PI/180.0
 * if( hue < 0 )
 *   hue += 2.0*PI
 *
 * </pre>
 *
 * <p>
 * [1] http://www.cs.rit.edu/~ncs/color/t_convert.html
 * </p>
 *
 * @author Peter Abeles
 */
/*
 Developer Notes:


 The number of comparisons to find min/max can be reduced by one using the following code:

 // Maximum and Minimum values
 float min,max;
 if( r > g ) {
 	if( r > b ) { max = r; } else { max = b; }
 	if( g < b ) { min = g; } else { min = b; }
 } else {
 	if( g > b ) { max = g; } else { max = b; }
 	if( r < b ) { min = r; } else { min = b; }
 }

 This doesn't seem to improve the runtime noticeably and makes the code uglier.
  */
public class ColorHsv {

	// 60 degrees in radians
	public static final double d60_F64 = 60.0*Math.PI/180.0;
	public static final float d60_F32 = (float)d60_F64;

	// 360 degrees in radians
	public static final double PI2_F64 = 2*Math.PI;
	public static final float PI2_F32 = (float)PI2_F64;

	/**
	 * Convert HSV color into RGB color
	 *
	 * @param h Hue [0,2*PI]
	 * @param s Saturation [0,1]
	 * @param v Value
	 * @param rgb (Output) RGB value
	 */
	public static void hsvToRgb( double h , double s , double v , double []rgb ) {
		if( s == 0 ) {
			rgb[0] = v;
			rgb[1] = v;
			rgb[2] = v;
			return;
		}
		h /= d60_F64;
		int h_int = (int)h;
		double remainder = h - h_int;
		double p = v * ( 1 - s );
		double q = v * ( 1 - s * remainder );
		double t = v * ( 1 - s * ( 1 - remainder ) );

		if( h_int < 1 ) {
			rgb[0] = v;
			rgb[1] = t;
			rgb[2] = p;
		} else if( h_int < 2 ) {
			rgb[0] = q;
			rgb[1] = v;
			rgb[2] = p;
		} else if( h_int < 3 ) {
			rgb[0] = p;
			rgb[1] = v;
			rgb[2] = t;
		} else if( h_int < 4 ) {
			rgb[0] = p;
			rgb[1] = q;
			rgb[2] = v;
		} else if( h_int < 5 ) {
			rgb[0] = t;
			rgb[1] = p;
			rgb[2] = v;
		} else {
			rgb[0] = v;
			rgb[1] = p;
			rgb[2] = q;
		}
	}

	/**
	 * Convert HSV color into RGB color
	 *
	 * @param h Hue [0,2*PI]
	 * @param s Saturation [0,1]
	 * @param v Value
	 * @param rgb (Output) RGB value
	 */
	public static void hsvToRgb( float h , float s , float v , float []rgb ) {
		if( s == 0 ) {
			rgb[0] = v;
			rgb[1] = v;
			rgb[2] = v;
			return;
		}
		h /= d60_F32;
		int h_int = (int)h;
		float remainder = h - h_int;
		float p = v * ( 1 - s );
		float q = v * ( 1 - s * remainder );
		float t = v * ( 1 - s * ( 1 - remainder ) );

		if( h_int < 1 ) {
			rgb[0] = v;
			rgb[1] = t;
			rgb[2] = p;
		} else if( h_int < 2 ) {
			rgb[0] = q;
			rgb[1] = v;
			rgb[2] = p;
		} else if( h_int < 3 ) {
			rgb[0] = p;
			rgb[1] = v;
			rgb[2] = t;
		} else if( h_int < 4 ) {
			rgb[0] = p;
			rgb[1] = q;
			rgb[2] = v;
		} else if( h_int < 5 ) {
			rgb[0] = t;
			rgb[1] = p;
			rgb[2] = v;
		} else {
			rgb[0] = v;
			rgb[1] = p;
			rgb[2] = q;
		}
	}

	/**
	 * Convert RGB color into HSV color
	 *
	 * @param r red
	 * @param g green
	 * @param b blue
	 * @param hsv (Output) HSV value.
	 */
	public static void rgbToHsv( double r , double g , double b , double []hsv ) {
		// Maximum value
		double max = r > g ? ( r > b ? r : b) : ( g > b ? g : b );
		// Minimum value
		double min = r < g ? ( r < b ? r : b) : ( g < b ? g : b );
		double delta = max - min;

		hsv[2] = max;

		if( max != 0 )
			hsv[1] = delta / max;
		else {
			hsv[0] = Double.NaN;
			hsv[1] = 0;
			return;
		}

		double h;
		if( r == max )
			h = ( g - b ) / delta;
		else if( g == max )
			h = 2 + ( b - r ) / delta;
		else
			h = 4 + ( r - g ) / delta;

		h *= d60_F64;
		if( h < 0 )
			h += PI2_F64;

		hsv[0] = h;
	}

	/**
	 * Convert RGB color into HSV color
	 *
	 * @param r red
	 * @param g green
	 * @param b blue
	 * @param hsv (Output) HSV value.
	 */
	public static void rgbToHsv( float r , float g , float b , float []hsv ) {
		// Maximum value
		float max = r > g ? ( r > b ? r : b) : ( g > b ? g : b );
		// Minimum value
		float min = r < g ? ( r < b ? r : b) : ( g < b ? g : b );
		float delta = max - min;

		hsv[2] = max;

		if( max != 0 )
			hsv[1] = delta / max;
		else {
			hsv[0] = Float.NaN;
			hsv[1] = 0;
			return;
		}

		float h;
		if( r == max )
			h = ( g - b ) / delta;
		else if( g == max )
			h = 2 + ( b - r ) / delta;
		else
			h = 4 + ( r - g ) / delta;

		h *= d60_F32;
		if( h < 0 )
			h += PI2_F32;

		hsv[0] = h;
	}

	/**
	 * Converts an image from HSV into RGB.
	 *
	 * @param hsv (Input) Image in HSV format
	 * @param rgb (Output) Image in RGB format
	 */
	public static void hsvToRgb_F32(Planar<GrayF32> hsv , Planar<GrayF32> rgb ) {

		InputSanityCheck.checkSameShape(hsv, rgb);

		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		for( int row = 0; row < hsv.height; row++ ) {
			int indexHsv = hsv.startIndex + row*hsv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;

			for( int col = 0; col < hsv.width; col++ , indexHsv++ , indexRgb++) {
				float h = H.data[indexHsv];
				float s = S.data[indexHsv];
				float v = V.data[indexHsv];

				if( s == 0 ) {
					R.data[indexRgb] = v;
					G.data[indexRgb] = v;
					B.data[indexRgb] = v;
					continue;
				}
				h /= d60_F32;
				int h_int = (int)h;
				float remainder = h - h_int;
				float p = v * ( 1 - s );
				float q = v * ( 1 - s * remainder );
				float t = v * ( 1 - s * ( 1 - remainder ) );

				if( h_int < 1 ) {
					R.data[indexRgb] = v;
					G.data[indexRgb] = t;
					B.data[indexRgb] = p;
				} else if( h_int < 2 ) {
					R.data[indexRgb] = q;
					G.data[indexRgb] = v;
					B.data[indexRgb] = p;
				} else if( h_int < 3 ) {
					R.data[indexRgb] = p;
					G.data[indexRgb] = v;
					B.data[indexRgb] = t;
				} else if( h_int < 4 ) {
					R.data[indexRgb] = p;
					G.data[indexRgb] = q;
					B.data[indexRgb] = v;
				} else if( h_int < 5 ) {
					R.data[indexRgb] = t;
					G.data[indexRgb] = p;
					B.data[indexRgb] = v;
				} else {
					R.data[indexRgb] = v;
					G.data[indexRgb] = p;
					B.data[indexRgb] = q;
				}
			}
		}
	}

	/**
	 * Converts an image from RGB into HSV.  Pixels must have a value within the range of [0,1].
	 *
	 * @param rgb (Input) Image in RGB format
	 * @param hsv (Output) Image in HSV format
	 */
	public static void rgbToHsv_F32(Planar<GrayF32> rgb , Planar<GrayF32> hsv ) {

		InputSanityCheck.checkSameShape(rgb, hsv);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);

		for( int row = 0; row < hsv.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indexHsv = hsv.startIndex + row*hsv.stride;

			for( int col = 0; col < hsv.width; col++ , indexHsv++ , indexRgb++) {

				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				float max = r > g ? ( r > b ? r : b) : ( g > b ? g : b );
				float min = r < g ? ( r < b ? r : b) : ( g < b ? g : b );

				float delta = max - min;

				V.data[indexHsv] = max;

				if( max != 0 )
					S.data[indexHsv] = delta / max;
				else {
					H.data[indexHsv] = Float.NaN;
					S.data[indexHsv] = 0;
					continue;
				}

				float h;
				if( r == max )
					h = ( g - b ) / delta;
				else if( g == max )
					h = 2 + ( b - r ) / delta;
				else
					h = 4 + ( r - g ) / delta;

				h *= d60_F32;
				if( h < 0 )
					h += PI2_F32;

				H.data[indexHsv] = h;
			}
		}
	}
}
