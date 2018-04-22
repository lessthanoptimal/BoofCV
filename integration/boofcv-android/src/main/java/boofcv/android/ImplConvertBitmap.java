/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.android;

import android.graphics.Bitmap;
import boofcv.struct.image.*;

/**
 * Low level implementations of Bitmap conversion routines. Contains functions
 * which are not being used for benchmarking purposes.
 * 
 * When converting into 565 format a lookup table is used.  The equations used to compute
 * the table round instead of flooring to minimize error.  I believe that this is what the Android
 * library does too, without looking at the code.
 * 
 * @author Peter Abeles
 * 
 */
public class ImplConvertBitmap {

	// storages values used to convert to 565 format
	private static int table5[] = new int[ 256 ];
	private static int table6[] = new int[ 256 ];
	

	static {
		for( int i = 0; i < table5.length; i++ ) {
			// minimize error by rounding instead of flooring
			table5[i] = (int)Math.round(i*0x1F/255.0);
			table6[i] = (int)Math.round(i*0x3F/255.0);
		}
	}

	public static void bitmapToGrayRGB(Bitmap input, GrayU8 output) {
		final int h = output.height;
		final int w = output.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexDst = output.startIndex + y * output.stride;

			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);

			for (int x = 0; x < w; x++) {
				int rgb = pixels[indexSrc++];

				int value = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
				output.data[indexDst++] = (byte) value;
			}
		}
	}
	
	public static void bitmapToGrayRGB(Bitmap input, GrayF32 output) {
		final int h = output.height;
		final int w = output.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int index = output.startIndex + y * output.stride;
			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);
			for (int x = 0; x < w; x++) {
				int rgb = pixels[indexSrc++];

				float value = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3f;
				output.data[index++] = value;
			}
		}
	}
	
	public static void bitmapToPlanarRGB_U8(Bitmap input, Planar<GrayU8> output) {
		final int h = output.height;
		final int w = output.width;
		
		GrayU8 R = output.getBand(0);
		GrayU8 G = output.getBand(1);
		GrayU8 B = output.getBand(2);
		GrayU8 A = output.getBand(3);

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);
			for (int x = 0; x < w; x++, indexDst++ ) {
				int rgb = pixels[indexSrc++];

				A.data[indexDst] = (byte)(rgb >> 24);
				R.data[indexDst] = (byte)((rgb >> 16) & 0xFF);
				G.data[indexDst] = (byte)((rgb >> 8) & 0xFF);
				B.data[indexDst] = (byte)(rgb & 0xFF);
			}
		}
	}

	public static void bitmapToInterleaved(Bitmap input, InterleavedU8 output) {
		final int h = output.height;
		final int w = output.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);
			for (int x = 0; x < w; x++) {
				int rgb = pixels[indexSrc++];

				output.data[indexDst++] = (byte)(rgb >> 24);
				output.data[indexDst++] = (byte)((rgb >> 16) & 0xFF);
				output.data[indexDst++] = (byte)((rgb >> 8) & 0xFF);
				output.data[indexDst++] = (byte)(rgb & 0xFF);
			}
		}
	}
	
	public static void bitmapToPlanarRGB_F32(Bitmap input, Planar<GrayF32> output) {
		final int h = output.height;
		final int w = output.width;
		
		GrayF32 R = output.getBand(0);
		GrayF32 G = output.getBand(1);
		GrayF32 B = output.getBand(2);
		GrayF32 A = output.getBand(3);

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);
			for (int x = 0; x < w; x++, indexDst++ ) {
				int rgb = pixels[indexSrc++];

				A.data[indexDst] = (rgb >> 24) & 0xFF;
				R.data[indexDst] = (rgb >> 16) & 0xFF;
				G.data[indexDst] = (rgb >> 8) & 0xFF;
				B.data[indexDst] = rgb & 0xFF;
			}
		}
	}

	public static void bitmapToInterleaved(Bitmap input, InterleavedF32 output) {
		final int h = output.height;
		final int w = output.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexDst = output.startIndex + y * output.stride;
			int indexSrc = 0;
			input.getPixels(pixels, 0,  w, 0, y, w, 1);
			for (int x = 0; x < w; x++ ) {
				int rgb = pixels[indexSrc++];

				output.data[indexDst++] = (rgb >> 24) & 0xFF;
				output.data[indexDst++] = (rgb >> 16) & 0xFF;
				output.data[indexDst++] = (rgb >> 8) & 0xFF;
				output.data[indexDst++] = rgb & 0xFF;
			}
		}
	}

	public static void grayToBitmapRGB(GrayU8 input, Bitmap output) {
		final int h = input.height;
		final int w = input.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexSrc = input.startIndex + y * input.stride;
			int indexDst = 0;
			for (int x = 0; x < w; x++) {
				int gray = input.data[indexSrc++] & 0xFF;

				pixels[indexDst++] = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
			}
			output.setPixels(pixels,0,w,0,y,w,1);
		}
	}
	
	public static void grayToBitmapRGB(GrayF32 input, Bitmap output) {
		final int h = input.height;
		final int w = input.width;

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexSrc = input.startIndex + y * input.stride;
			int indexDst = 0;
			for (int x = 0; x < w; x++) {
				int gray = (int)input.data[indexSrc++];

				pixels[indexDst++] = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
			}
			output.setPixels(pixels,0,w,0,y,w,1);
		}
	}
	
	public static void planarToBitmapRGB_U8(Planar<GrayU8> input, Bitmap output) {
		final int h = input.height;
		final int w = input.width;

		GrayU8 R = input.getBand(0);
		GrayU8 G = input.getBand(1);
		GrayU8 B = input.getBand(2);
		GrayU8 A = input.getBand(3);

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexSrc = input.startIndex + y * input.stride;
			int indexDst = 0;
			for (int x = 0; x < w; x++,indexSrc++) {
				pixels[indexDst++] = (A.data[indexSrc] & 0xFF) << 24 | (R.data[indexSrc] & 0xFF) << 16 | (G.data[indexSrc] & 0xFF) << 8 | (B.data[indexSrc] & 0xFF);
			}
			output.setPixels(pixels,0,w,0,y,w,1);
		}
	}

	public static void planarToBitmapRGB_F32(Planar<GrayF32> input, Bitmap output) {
		final int h = input.height;
		final int w = input.width;

		GrayF32 R = input.getBand(0);
		GrayF32 G = input.getBand(1);
		GrayF32 B = input.getBand(2);
		GrayF32 A = input.getBand(3);

		// read one row at a time instead of the entire image to reduce memory requirements
		int[] pixels = new int[w];

		for (int y = 0; y < h; y++) {
			int indexSrc = input.startIndex + y * input.stride;
			int indexDst = 0;
			for (int x = 0; x < w; x++,indexSrc++) {

				int r = (int)R.data[indexSrc];
				int g = (int)G.data[indexSrc];
				int b = (int)B.data[indexSrc];
				int a = (int)A.data[indexSrc];

				pixels[indexDst++] = a << 24 | r << 16 | g << 8 | b;
			}
			output.setPixels(pixels,0,w,0,y,w,1);
		}
	}
	
	public static void arrayToGray( int input[], Bitmap.Config config, GrayU8 output ) {
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;
		
		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++) {
				while (indexDst < end) {
					int rgb = input[indexSrc++];

					int value = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
					output.data[indexDst++] = (byte) value;
				}
			}
			break;

		default:
			throw new RuntimeException("Image type not yet supported: "+config);
		}
	}
	
	public static void arrayToGray(int input[], Bitmap.Config config, GrayF32 output )
	{
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;
		
		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++) {
				while (indexDst < end) {
					int rgb = input[indexSrc++];

					int value = (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
					output.data[indexDst++] = value;
				}
			}
			break;

		default:
			throw new RuntimeException("Image type not yet supported: "+config);
		}
	}
	
	public static void arrayToPlanar_U8(int input[], Bitmap.Config config , Planar<GrayU8> output ) {
		final int h = output.height;
		final int w = output.width;

		GrayU8 R = output.getBand(0);
		GrayU8 G = output.getBand(1);
		GrayU8 B = output.getBand(2);
		GrayU8 A = output.getBand(3);
		
		int indexSrc = 0;
		
		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++) {
				while (indexDst < end) {
					int rgb = input[indexSrc++];

					A.data[indexDst] = (byte)(rgb >> 24);
					B.data[indexDst] = (byte)(rgb >> 16);
					G.data[indexDst] = (byte)(rgb >> 8);
					R.data[indexDst] = (byte)rgb;
					indexDst++;
				}
			}
			break;

		default:
			throw new RuntimeException("Image type not yet supported: "+config);
		}
	}

	public static void arrayToInterleaved(int input[], Bitmap.Config config , InterleavedU8 output ) {
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;

		switch (config) {
			case ARGB_8888:
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w*output.numBands;
					// for (int x = 0; x < w; x++) {
					while (indexDst < end) {
						int rgb = input[indexSrc++];

						output.data[indexDst++] = (byte)(rgb >> 24);
						output.data[indexDst++] = (byte)(rgb >> 16);
						output.data[indexDst++] = (byte)(rgb >> 8);
						output.data[indexDst++] = (byte)rgb;
					}
				}
				break;

			default:
				throw new RuntimeException("Image type not yet supported: "+config);
		}
	}
	
	public static void arrayToPlanar_F32(int input[], Bitmap.Config config , Planar<GrayF32> output ) {
		final int h = output.height;
		final int w = output.width;

		GrayF32 R = output.getBand(0);
		GrayF32 G = output.getBand(1);
		GrayF32 B = output.getBand(2);
		GrayF32 A = output.getBand(3);
		
		int indexSrc = 0;
		
		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++) {
				while (indexDst < end) {
					int rgb = input[indexSrc++];

					A.data[indexDst] = (rgb >> 24) & 0xFF;
					B.data[indexDst] = (rgb >> 16) & 0xFF;
					G.data[indexDst] = (rgb >> 8) & 0xFF;
					R.data[indexDst] = rgb & 0xFF;
					indexDst++;
				}
			}
			break;

		default:
			throw new RuntimeException("Image type not yet supported: "+config);
		}
	}

	public static void arrayToInterleaved(int input[], Bitmap.Config config , InterleavedF32 output ) {
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;

		switch (config) {
			case ARGB_8888:
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w*output.numBands;
					// for (int x = 0; x < w; x++) {
					while (indexDst < end) {
						int rgb = input[indexSrc++];

						output.data[indexDst++] = (rgb >> 24) & 0xFF;
						output.data[indexDst++] = (rgb >> 16) & 0xFF;
						output.data[indexDst++] = (rgb >> 8) & 0xFF;
						output.data[indexDst++] = rgb & 0xFF;
					}
				}
				break;

			default:
				throw new RuntimeException("Image type not yet supported: "+config);
		}
	}
	
	public static void arrayToGray(byte array[], Bitmap.Config config , GrayU8 output) {
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;

		switch (config) {
		case ARGB_8888: {
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++, indexSrc++) {
				while (indexDst < end) {
					int value = ((array[indexSrc++] & 0xFF)
							+ (array[indexSrc++] & 0xFF) + (array[indexSrc++] & 0xFF)) / 3;
					output.data[indexDst++] = (byte) value;
					indexSrc++;// skip over alpha channel
				}
			}
		}
			break;

		case RGB_565:{
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				for (int x = 0; x < w; x++) {

					int value = (array[indexSrc++] & 0xFF)
							| ((array[indexSrc++] & 0xFF) << 8 );
					
					int r = (value >> 11)*256/32;
					int g = ((value & 0x07E0) >> 5)*256/64;
					int b = (value & 0x001F)*256/32;

					output.data[indexDst++] = (byte) ((r + g + b) / 3);
				}
			}}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}

	}
	
	public static void arrayToGray(byte array[], Bitmap.Config config , GrayF32 output) {
		final int h = output.height;
		final int w = output.width;

		int indexSrc = 0;

		switch (config) {
		case ARGB_8888: {
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				int end = indexDst + w;
				// for (int x = 0; x < w; x++, indexSrc++) {
				while (indexDst < end) {
					float value = ((array[indexSrc++] & 0xFF) + (array[indexSrc++] & 0xFF) + (array[indexSrc++] & 0xFF)) / 3f;
					output.data[indexDst++] = value;
					indexSrc++;// skip over alpha channel
				}
			}
		}
		break;

		case RGB_565:{
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				for (int x = 0; x < w; x++) {

					int value = (array[indexSrc++] & 0xFF)
							| ((array[indexSrc++] & 0xFF) << 8 );
					
					int r = (value >> 11)*256/32;
					int g = ((value & 0x07E0) >> 5)*256/64;
					int b = (value & 0x001F)*256/32;

					output.data[indexDst++] = (r + g + b) / 3;
				}
			}}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}
	
	public static void arrayToPlanar_U8(byte []input, Bitmap.Config config, Planar<GrayU8> output) {
		final int h = output.height;
		final int w = output.width;
		
		GrayU8 R = output.getBand(0);
		GrayU8 G = output.getBand(1);
		GrayU8 B = output.getBand(2);

		int indexSrc = 0;

		switch (config) {
		case ARGB_8888: {
			if( output.getNumBands() == 4 ) {
				GrayU8 A = output.getBand(3);
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w;
					// for (int x = 0; x < w; x++, indexSrc++) {
					while (indexDst < end) {
						R.data[indexDst] = input[indexSrc++];
						G.data[indexDst] = input[indexSrc++];
						B.data[indexDst] = input[indexSrc++];
						A.data[indexDst] = input[indexSrc++];

						indexDst++;
					}
				}
			} else if( output.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w;
					// for (int x = 0; x < w; x++, indexSrc++) {
					while (indexDst < end) {
						R.data[indexDst] = input[indexSrc++];
						G.data[indexDst] = input[indexSrc++];
						B.data[indexDst] = input[indexSrc++];
						indexSrc++;
						indexDst++;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected 3 or 4 bands in output");
			}
		}
			break;

		case RGB_565:{
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				for (int x = 0; x < w; x++,indexDst++) {

					int value = (input[indexSrc++] & 0xFF)
							| ((input[indexSrc++] & 0xFF) << 8 );
					
					int r = (value >> 11)*0xFF/0x1F;
					int g = ((value >> 5) & 0x3F )*0xFF/0x3F;
					int b = (value & 0x1F)*0xFF/0x1F;
					
					B.data[indexDst] = (byte)b;
					G.data[indexDst] = (byte)g;
					R.data[indexDst] = (byte)r;
				}
			}}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}

	}
	
	public static void arrayToPlanar_F32(byte []input, Bitmap.Config config, Planar<GrayF32> output) {
		final int h = output.height;
		final int w = output.width;
		
		GrayF32 R = output.getBand(0);
		GrayF32 G = output.getBand(1);
		GrayF32 B = output.getBand(2);

		int indexSrc = 0;

		switch (config) {
		case ARGB_8888: {
			if( output.getNumBands() == 4 ) {
				GrayF32 A = output.getBand(3);
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w;
					// for (int x = 0; x < w; x++, indexSrc++) {
					while (indexDst < end) {
						R.data[indexDst] = input[indexSrc++] & 0xFF;
						G.data[indexDst] = input[indexSrc++] & 0xFF;
						B.data[indexDst] = input[indexSrc++] & 0xFF;
						A.data[indexDst] = input[indexSrc++] & 0xFF;

						indexDst++;
					}
				}
			} else if( output.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexDst = output.startIndex + y * output.stride;
					int end = indexDst + w;
					// for (int x = 0; x < w; x++, indexSrc++) {
					while (indexDst < end) {
						R.data[indexDst] = input[indexSrc++] & 0xFF;
						G.data[indexDst] = input[indexSrc++] & 0xFF;
						B.data[indexDst] = input[indexSrc++] & 0xFF;
						indexSrc++;
						indexDst++;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected 3 or 4 bands in output");
			}
		}
			break;

		case RGB_565:{
			for (int y = 0; y < h; y++) {
				int indexDst = output.startIndex + y * output.stride;
				for (int x = 0; x < w; x++,indexDst++) {

					int value = (input[indexSrc++] & 0xFF)
							| ((input[indexSrc++] & 0xFF) << 8 );
					
					int r = (value >> 11)*0xFF/0x1F;
					int g = ((value >> 5) & 0x3F )*0xFF/0x3F;
					int b = (value & 0x1F)*0xFF/0x1F;
					
					B.data[indexDst] = b & 0xFF;
					G.data[indexDst] = g & 0xFF;
					R.data[indexDst] = r & 0xFF;
				}
			}}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}

	}

	public static void grayToArray(GrayU8 input, byte []output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		int indexDst = 0;

		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexSrc = input.startIndex + y * input.stride;
				for (int x = 0; x < w; x++) {
					int value = input.data[indexSrc++] & 0xFF;

					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) 0xFF;
				}
			}
			break;

		case RGB_565:
			for (int y = 0; y < h; y++) {
				int indexSrc = input.startIndex + y * input.stride;
				for (int x = 0; x < w; x++) {
					int value = input.data[indexSrc++] & 0xFF;
					int value5 = table5[value];
					int value6 = table6[value];

					int rgb565 = (value5 << 11) | (value6 << 5) | value5;
					output[indexDst++] = (byte) rgb565;
					output[indexDst++] = (byte) (rgb565 >> 8);
				}
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}

	}
	
	public static void grayToArray(GrayF32 input, byte[] output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		int indexDst = 0;

		switch (config) {
		case ARGB_8888:
			for (int y = 0; y < h; y++) {
				int indexSrc = input.startIndex + y * input.stride;
				for (int x = 0; x < w; x++) {
					int value = (int)input.data[indexSrc++];
					
					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) value;
					output[indexDst++] = (byte) 0xFF;
				}
			}
			break;

		case RGB_565:
			for (int y = 0; y < h; y++) {
				int indexSrc = input.startIndex + y * input.stride;
				for (int x = 0; x < w; x++) {
					int value = (int)input.data[indexSrc++];
					int value5 = table5[value];
					int value6 = table6[value];

					int rgb565 = (value5 << 11) | (value6 << 5) | value5;
					output[indexDst++] = (byte) rgb565;
					output[indexDst++] = (byte) (rgb565 >> 8);
				}
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}
	
	public static void planarToArray_U8(Planar<GrayU8> input, byte[] output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		GrayU8 R = input.getBand(0);
		GrayU8 G = input.getBand(1);
		GrayU8 B = input.getBand(2);

		int indexDst = 0;

		switch (config) {
			case ARGB_8888:
			if( input.getNumBands() == 4 ) {
				GrayU8 A = input.getBand(3);
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++,indexSrc++) {
						output[indexDst++] = R.data[indexSrc];
						output[indexDst++] = G.data[indexSrc];
						output[indexDst++] = B.data[indexSrc];
						output[indexDst++] = A.data[indexSrc];
					}
				}
			} else if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++,indexSrc++) {
						output[indexDst++] = R.data[indexSrc];
						output[indexDst++] = G.data[indexSrc];
						output[indexDst++] = B.data[indexSrc];
						output[indexDst++] = (byte)0xFF;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 or 4 bands");
			}
			break;

		case RGB_565:
			if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++, indexSrc++) {
						int r = R.data[indexSrc] & 0xFF;
						int g = G.data[indexSrc] & 0xFF;
						int b = B.data[indexSrc] & 0xFF;

						int valueR = table5[r];
						int valueG = table6[g];
						int valueB = table5[b];

						int rgb565 = (valueR << 11) | (valueG << 5) | valueB;

						output[indexDst++] = (byte) rgb565;
						output[indexDst++] = (byte) (rgb565 >> 8);
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 bands");
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}
	
	public static void planarToArray_F32(Planar<GrayF32> input, byte[] output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		GrayF32 R = input.getBand(0);
		GrayF32 G = input.getBand(1);
		GrayF32 B = input.getBand(2);

		int indexDst = 0;

		switch (config) {
		case ARGB_8888:
			if( input.getNumBands() == 4 ) {
				GrayF32 A = input.getBand(3);
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++,indexSrc++) {
						output[indexDst++] = (byte)R.data[indexSrc];
						output[indexDst++] = (byte)G.data[indexSrc];
						output[indexDst++] = (byte)B.data[indexSrc];
						output[indexDst++] = (byte)A.data[indexSrc];
					}
				}
			} else if( input.getNumBands() == 3 ){
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++,indexSrc++) {
						output[indexDst++] = (byte)R.data[indexSrc];
						output[indexDst++] = (byte)G.data[indexSrc];
						output[indexDst++] = (byte)B.data[indexSrc];
						output[indexDst++] = (byte)255;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 or 4 bands");
			}
			break;

		case RGB_565:
			if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++, indexSrc++) {
						int r = (int) R.data[indexSrc];
						int g = (int) G.data[indexSrc];
						int b = (int) B.data[indexSrc];

						int valueR = table5[r];
						int valueG = table6[g];
						int valueB = table5[b];

						int rgb565 = (valueR << 11) | (valueG << 5) | valueB;

						output[indexDst++] = (byte) rgb565;
						output[indexDst++] = (byte) (rgb565 >> 8);
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 bands");
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}

	public static void interleavedToArray(InterleavedU8 input, byte[] output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		int indexDst = 0;

		switch (config) {
			case ARGB_8888:
			if( input.getNumBands() == 4 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = input.data[indexSrc++];
					}
				}
			} else if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = input.data[indexSrc++];
						output[indexDst++] = (byte)0xFF;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 or 4 bands");
			}
			break;

		case RGB_565:
			if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						int r = input.data[indexSrc++] & 0xFF;
						int g = input.data[indexSrc++] & 0xFF;
						int b = input.data[indexSrc++] & 0xFF;

						int valueR = table5[r];
						int valueG = table6[g];
						int valueB = table5[b];

						int rgb565 = (valueR << 11) | (valueG << 5) | valueB;

						output[indexDst++] = (byte) rgb565;
						output[indexDst++] = (byte) (rgb565 >> 8);
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 bands");
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}

	public static void interleavedToArray(InterleavedF32 input, byte[] output , Bitmap.Config config ) {
		final int h = input.height;
		final int w = input.width;

		int indexDst = 0;

		switch (config) {
			case ARGB_8888:
			if( input.getNumBands() == 4 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)input.data[indexSrc++];
					}
				}
			} else if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)input.data[indexSrc++];
						output[indexDst++] = (byte)0xFF;
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 or 4 bands");
			}
			break;

		case RGB_565:
			if( input.getNumBands() == 3 ) {
				for (int y = 0; y < h; y++) {
					int indexSrc = input.startIndex + y * input.stride;
					for (int x = 0; x < w; x++) {
						int r = (int)input.data[indexSrc++];
						int g = (int)input.data[indexSrc++];
						int b = (int)input.data[indexSrc++];

						int valueR = table5[r];
						int valueG = table6[g];
						int valueB = table5[b];

						int rgb565 = (valueR << 11) | (valueG << 5) | valueB;

						output[indexDst++] = (byte) rgb565;
						output[indexDst++] = (byte) (rgb565 >> 8);
					}
				}
			} else {
				throw new IllegalArgumentException("Expected input to have 3 bands");
			}
			break;

		case ALPHA_8:
			throw new RuntimeException("ALPHA_8 seems to have some weired internal format and is not currently supported");

		case ARGB_4444:
			throw new RuntimeException("Isn't 4444 deprecated?");
		}
	}

	public static void interleavedYuvToArgb8888(InterleavedU8 input, byte[] output)
	{
		final int width = input.width;
		final int height = input.height;

		int indexDst = 0;
		for( int row = 0; row < height; row++ ) {
			int indexSrc = input.startIndex + input.stride*row;

			for( int col = 0; col < width; col++ ) {
				int y = 1191*((input.data[indexSrc++] & 0xFF) - 16);
				int cr = (input.data[ indexSrc++ ] & 0xFF) - 128;
				int cb = (input.data[ indexSrc++] & 0xFF) - 128;

//				if( y < 0 ) y = 0;
				y = ((y >>> 31)^1)*y;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

//				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
//				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
//				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				r *= ((r >>> 31)^1);
				g *= ((g >>> 31)^1);
				b *= ((b >>> 31)^1);

				// The bitwise code below isn't faster than than the if statement below
//				r |= (((255-r) >>> 31)*0xFF);
//				g |= (((255-g) >>> 31)*0xFF);
//				b |= (((255-b) >>> 31)*0xFF);

				if( r > 255 ) r = 255;
				if( g > 255 ) g = 255;
				if( b > 255 ) b = 255;

				output[indexDst++] = (byte)r;
				output[indexDst++] = (byte)g;
				output[indexDst++] = (byte)b;
				output[indexDst++] = (byte)0xFF;
			}
		}
	}

	public static void interleavedYuvToRGB565(InterleavedU8 input, byte[] output)
	{
		final int width = input.width;
		final int height = input.height;

		int indexDst = 0;
		for( int row = 0; row < height; row++ ) {
			int indexSrc = input.startIndex + input.stride*row;

			for( int col = 0; col < width; col++ ) {
				int y = 1191*((input.data[indexSrc++] & 0xFF) - 16);
				int cr = (input.data[ indexSrc++ ] & 0xFF) - 128;
				int cb = (input.data[ indexSrc++] & 0xFF) - 128;

//				if( y < 0 ) y = 0;
				y = ((y >>> 31)^1)*y;

				int r = (y + 1836*cr) >> 10;
				int g = (y - 547*cr - 218*cb) >> 10;
				int b = (y + 2165*cb) >> 10;

//				if( r < 0 ) r = 0; else if( r > 255 ) r = 255;
//				if( g < 0 ) g = 0; else if( g > 255 ) g = 255;
//				if( b < 0 ) b = 0; else if( b > 255 ) b = 255;

				r *= ((r >>> 31)^1);
				g *= ((g >>> 31)^1);
				b *= ((b >>> 31)^1);

				// The bitwise code below isn't faster than than the if statement below
//				r |= (((255-r) >>> 31)*0xFF);
//				g |= (((255-g) >>> 31)*0xFF);
//				b |= (((255-b) >>> 31)*0xFF);

				if( r > 255 ) r = 255;
				if( g > 255 ) g = 255;
				if( b > 255 ) b = 255;

				output[indexDst++] = (byte)table5[r];
				output[indexDst++] = (byte)table6[g];
				output[indexDst++] = (byte)table5[b];
			}
		}
	}
}
