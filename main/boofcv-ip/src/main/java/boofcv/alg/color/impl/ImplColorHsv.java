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
import boofcv.struct.image.Planar;

import static boofcv.alg.color.ColorHsv.PI2_F32;
import static boofcv.alg.color.ColorHsv.d60_F32;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Low level implementation of function for converting HSV images.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplColorHsv {
	public static void hsvToRgb_F32(Planar<GrayF32> hsv , Planar<GrayF32> rgb ) {
		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);

		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,hsv.height,row->{
		for( int row = 0; row < hsv.height; row++ ) {
			int indexHsv = hsv.startIndex + row*hsv.stride;
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int endRgb = indexRgb+hsv.width;

			for( ; indexRgb < endRgb; indexHsv++ , indexRgb++) {
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
		//CONCURRENT_ABOVE });
	}

	/**
	 * Converts an image from RGB into HSV. Pixels must have a value within the range of [0,1].
	 *
	 * @param rgb (Input) Image in RGB format
	 * @param hsv (Output) Image in HSV format
	 */
	public static void rgbToHsv_F32(Planar<GrayF32> rgb , Planar<GrayF32> hsv ) {
		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);
		GrayF32 V = hsv.getBand(2);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0,hsv.height,row->{
		for( int row = 0; row < hsv.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indexHsv = hsv.startIndex + row*hsv.stride;
			int endRgb = indexRgb+hsv.width;

			for( ; indexRgb < endRgb; indexHsv++ , indexRgb++) {

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
		//CONCURRENT_ABOVE });
	}
}
