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

import boofcv.struct.image.*;

/**
 * <p>Contains functions related to working with RGB images and converting RGB images to gray-scale using a weighted
 * equation.  The weighted equation is designed to mimic the intensity seen by the human eye.</p>
 *
 * <b>Weighted Equation</b><br>
 * gray = 0.299*r + 0.587*g + 0.114*b
 *
 * @author Peter Abeles
 */
public class ColorRgb {

	public static int rgbToGray_Weighted( int r , int g , int b ) {
		return (int)(0.299*r + 0.587*g + 0.114*b);
	}

	public static float rgbToGray_Weighted( float r , float g , float b ) {
		return 0.299f*r + 0.587f*g + 0.114f*b;
	}

	public static double rgbToGray_Weighted( double r , double g , double b ) {
		return 0.299*r + 0.587*g + 0.114*b;
	}

	public static void rgbToGray_Weighted( ImageMultiBand rgb , ImageGray gray ) {
		if( rgb instanceof Planar ) {
			Planar p = (Planar)rgb;
			if( p.getBandType() == GrayU8.class ) {
				rgbToGray_Weighted_U8(p,(GrayU8)gray);return;
			} else if( p.getBandType() == GrayF32.class ) {
				rgbToGray_Weighted_F32(p,(GrayF32)gray);return;
			} else if( p.getBandType() == GrayF64.class ) {
				rgbToGray_Weighted_F64(p,(GrayF64)gray);return;
			}
		} else if( rgb instanceof ImageInterleaved ) {
			if( rgb instanceof InterleavedU8 ) {
				rgbToGray_Weighted((InterleavedU8)rgb,(GrayU8)gray);return;
			} else if( rgb instanceof InterleavedF32 ) {
				rgbToGray_Weighted((InterleavedF32)rgb,(GrayF32)gray);return;
			} else if( rgb instanceof InterleavedF64 ) {
				rgbToGray_Weighted((InterleavedF64)rgb,(GrayF64)gray);return;
			}
		}
		throw new IllegalArgumentException("Unknown image type");
	}

	public static void rgbToGray_Weighted_U8(Planar<GrayU8> rgb , GrayU8 gray ) {
		GrayU8 R = rgb.getBand(0);
		GrayU8 G = rgb.getBand(1);
		GrayU8 B = rgb.getBand(2);

		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ , indexRgb++) {
				double r = R.data[indexRgb]&0xFF;
				double g = G.data[indexRgb]&0xFF;
				double b = B.data[indexRgb]&0xFF;

				gray.data[indedGra] = (byte)(0.299*r + 0.587*g + 0.114*b);
			}
		}
	}

	public static void rgbToGray_Weighted_F32(Planar<GrayF32> rgb , GrayF32 gray ) {
		GrayF32 R = rgb.getBand(0);
		GrayF32 G = rgb.getBand(1);
		GrayF32 B = rgb.getBand(2);

		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ , indexRgb++) {
				float r = R.data[indexRgb];
				float g = G.data[indexRgb];
				float b = B.data[indexRgb];

				gray.data[indedGra] = 0.299f*r + 0.587f*g + 0.114f*b;
			}
		}
	}

	public static void rgbToGray_Weighted_F64(Planar<GrayF64> rgb , GrayF64 gray ) {
		GrayF64 R = rgb.getBand(0);
		GrayF64 G = rgb.getBand(1);
		GrayF64 B = rgb.getBand(2);

		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ , indexRgb++) {
				double r = R.data[indexRgb];
				double g = G.data[indexRgb];
				double b = B.data[indexRgb];

				gray.data[indedGra] = 0.299*r + 0.587*g + 0.114*b;
			}
		}
	}

	public static void rgbToGray_Weighted(InterleavedU8 rgb , GrayU8 gray ) {
		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ ) {
				double r = rgb.data[indexRgb++]&0xFF;
				double g = rgb.data[indexRgb++]&0xFF;
				double b = rgb.data[indexRgb++]&0xFF;

				gray.data[indedGra] = (byte)(0.299*r + 0.587*g + 0.114*b);
			}
		}
	}

	public static void rgbToGray_Weighted(InterleavedF32 rgb , GrayF32 gray ) {
		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ ) {
				float r = rgb.data[indexRgb++];
				float g = rgb.data[indexRgb++];
				float b = rgb.data[indexRgb++];

				gray.data[indedGra] = 0.299f*r + 0.587f*g + 0.114f*b;
			}
		}
	}

	public static void rgbToGray_Weighted(InterleavedF64 rgb , GrayF64 gray ) {
		for( int row = 0; row < rgb.height; row++ ) {
			int indexRgb = rgb.startIndex + row*rgb.stride;
			int indedGra = gray.startIndex + row*gray.stride;

			for( int col = 0; col < rgb.width; col++ , indedGra++ ) {
				double r = rgb.data[indexRgb++];
				double g = rgb.data[indexRgb++];
				double b = rgb.data[indexRgb++];

				gray.data[indedGra] = 0.299*r + 0.587*g + 0.114*b;
			}
		}
	}

}
