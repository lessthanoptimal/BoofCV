package boofcv.processing;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import processing.core.PImage;

/**
 * Functions for converting between BoofCV and Processing image data types
 *
 * @author Peter Abeles
 */
public class ConvertProcessing {

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

				output.pixels[indexInput] = 0xFF << 24 | value << 16 | value << 8 | value ;
			}
		}
	}

	public static void convert_U8_RGB( ImageUInt8 input , PImage output ) {

		int indexOutput = 0;
		for( int y = 0; y < input.height; y++ ) {
			int indexInput = input.startIndex + y*input.stride;
			for( int x = 0; x < input.width; x++ ,indexOutput++,indexInput++) {
				int value = input.data[indexInput]&0xFF;

				output.pixels[indexInput] = 0xFF << 24 | value << 16 | value << 8 | value ;
			}
		}
	}
}
