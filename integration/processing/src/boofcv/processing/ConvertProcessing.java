/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import processing.core.PImage;

/**
 * Functions for converting between BoofCV and Processing image data types
 *
 * @author Peter Abeles
 */
public class ConvertProcessing {

	public static void convertFromRGB( PImage input , ImageBase output ) {
		if( output instanceof ImageUInt8 ) {
			convert_RGB_U8(input,(ImageUInt8)output);
		} else if( output instanceof ImageFloat32) {
			convert_RGB_F32(input, (ImageFloat32) output);
		} else if( output instanceof MultiSpectral ) {
			Class bandType = ((MultiSpectral)output).getBandType();
			if( bandType == ImageFloat32.class ) {
				convert_RGB_MSF32(input,(MultiSpectral)output);
			} else if( bandType == ImageUInt8.class ) {
				convert_RGB_MSU8(input, (MultiSpectral) output);
			}
		}
	}

	public static void convert_RGB_F32( PImage input , ImageFloat32 output ) {

		int indexInput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < input.width; x++ ,indexInput++,indexOut++) {
				int value = input.pixels[indexInput];

				int r = ( value >> 16 ) & 0xFF;
				int g = ( value >> 8 ) & 0xFF;
				int b = value & 0xFF;

				output.data[indexOut] = (r+g+b)/3.0f;
			}
		}
	}

	public static void convert_RGB_U8( PImage input , ImageUInt8 output ) {

		int indexInput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < input.width; x++ ,indexInput++,indexOut++) {
				int value = input.pixels[indexInput];

				int r = ( value >> 16 ) & 0xFF;
				int g = ( value >> 8 ) & 0xFF;
				int b = value & 0xFF;

				output.data[indexOut] = (byte)((r+g+b)/3);
			}
		}
	}

	public static void convert_F32_RGB( ImageFloat32 input , PImage output ) {

		int indexOutput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int value = (int)input.data[indexInput];

				output.pixels[indexOutput] = 0xFF << 24 | value << 16 | value << 8 | value ;
			}
		}
	}

	public static void convert_U8_RGB( ImageUInt8 input , PImage output ) {

		int indexOutput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int value = input.data[indexInput]&0xFF;

				output.pixels[indexOutput] = 0xFF << 24 | value << 16 | value << 8 | value ;
			}
		}
	}

	public static void convert_MSF32_RGB( MultiSpectral<ImageFloat32> input , PImage output ) {

		ImageFloat32 red = input.getBand(0);
		ImageFloat32 green = input.getBand(1);
		ImageFloat32 blue = input.getBand(2);

		int indexOutput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int r = (int)red.data[indexInput];
				int g = (int)green.data[indexInput];
				int b = (int)blue.data[indexInput];

				output.pixels[indexOutput] = 0xFF << 24 | r << 16 | g << 8 | b ;
			}
		}
	}

	public static void convert_MSU8_RGB( MultiSpectral<ImageUInt8> input , PImage output ) {

		ImageUInt8 red = input.getBand(0);
		ImageUInt8 green = input.getBand(1);
		ImageUInt8 blue = input.getBand(2);

		int indexOutput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int r = (red.data[indexInput]&0xFF);
				int g = (green.data[indexInput]&0xFF);
				int b = (blue.data[indexInput]&0xFF);

				output.pixels[indexOutput] = 0xFF << 24 | r << 16 | g << 8 | b;
			}
		}
	}

	public static void convert_RGB_MSF32( PImage input , MultiSpectral<ImageFloat32> output ) {

		ImageFloat32 red = output.getBand(0);
		ImageFloat32 green = output.getBand(1);
		ImageFloat32 blue = output.getBand(2);

		int indexInput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexOutput = output.startIndex + y*output.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int value = input.pixels[indexInput];

				red.data[indexOutput] = (value>>16)&0xFF;
				green.data[indexOutput] = (value>>8)&0xFF;
				blue.data[indexOutput] = value&0xFF;
			}
		}
	}

	public static void convert_RGB_MSU8( PImage input , MultiSpectral<ImageUInt8> output ) {

		ImageUInt8 red = output.getBand(0);
		ImageUInt8 green = output.getBand(1);
		ImageUInt8 blue = output.getBand(2);

		int indexInput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexOutput = output.startIndex + y*output.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int value = input.pixels[indexInput];

				red.data[indexOutput] = (byte)(value>>16);
				green.data[indexOutput] = (byte)(value>>8);
				blue.data[indexOutput] = (byte)value;
			}
		}
	}
}
