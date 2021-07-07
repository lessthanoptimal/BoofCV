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

package boofcv.io.image.impl;

import boofcv.io.image.ConvertRaster;
import boofcv.struct.image.*;

import java.awt.image.*;

import static boofcv.io.image.ConvertRaster.*;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * Routines for converting to and from {@link BufferedImage} that use its internal
 * raster for better performance.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("Duplicates")
public class ImplConvertRaster {

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(DataBufferUShort buffer , WritableRaster src, GrayI16 dst) {
		short[] srcData = buffer.getData();

		int numBands = src.getNumBands();

		int size = dst.getWidth() * dst.getHeight();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);
		int srcStrideDiff = srcStride-src.getNumDataElements()*dst.width;

		if (numBands == 1) {
			if (dst.startIndex == 0 && dst.width == dst.stride && srcStrideDiff == 0 && srcOffset == 0 )
				System.arraycopy(srcData, 0, dst.data, 0, size);
			else {
				//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
				for (int y = 0; y < dst.height; y++) {
					int indexDst = dst.startIndex + dst.stride * y;
					int indexSrc = srcOffset + srcStride * y;

					System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
				}
				//CONCURRENT_ABOVE });
			}
		} else {
			throw new RuntimeException("Only single band images are currently support for 16bit");
		}
	}

	public static void from_4BU8_to_U8(byte[] srcData, int srcStride, int srcOffset, GrayU8 dst) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			for (; indexDst < indexDstEnd; indexDst++) {
				indexSrc++;
				int r = srcData[indexSrc++] & 0xFF;
				int g = srcData[indexSrc++] & 0xFF;
				int b = srcData[indexSrc++] & 0xFF;

				int ave = (r + g + b) / 3;

				dst.data[indexDst] = (byte) ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_1BU8_to_U8(byte[] srcData, int size, int srcStride, int srcOffset, int srcStrideDiff, GrayU8 dst) {
		if (dst.startIndex == 0 && dst.width == dst.stride && srcStrideDiff == 0 && srcOffset == 0 )
			System.arraycopy(srcData, 0, dst.data, 0, size);
		else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexSrc = srcOffset + srcStride * y;

				System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void from_3BU8_to_U8(byte[] srcData, int srcStride, int srcOffset, GrayU8 dst) {

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			while ( indexDst < indexDstEnd ) {
				int r = srcData[indexSrc++] & 0xFF;
				int g = srcData[indexSrc++] & 0xFF;
				int b = srcData[indexSrc++] & 0xFF;

				int ave = (r + g + b) / 3;

				dst.data[indexDst++] = (byte) ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_4BU8_to_F32(byte[] srcData, int srcStride, int srcOffset, GrayF32 dst) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			while (indexDst < indexDstEnd) {
				indexSrc++;
				int r = srcData[indexSrc++] & 0xFF;
				int g = srcData[indexSrc++] & 0xFF;
				int b = srcData[indexSrc++] & 0xFF;

				float ave = (r + g + b) / 3.0f;

				dst.data[indexDst++] = ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_1BU8_to_F32(byte[] srcData, int srcStride, int srcOffset, GrayF32 dst) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			int indexSrc = srcOffset + srcStride * y;

			while ( indexDst < indexDstEnd) {
				dst.data[indexDst++] = srcData[indexSrc++] & 0xFF;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_3BU8_to_F32(byte[] srcData, int srcStride, int srcOffset, GrayF32 dst) {
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			while ( indexDst < indexDstEnd) {
				int r = srcData[indexSrc++] & 0xFF;
				int g = srcData[indexSrc++] & 0xFF;
				int b = srcData[indexSrc++] & 0xFF;

				float ave = (r + g + b) / 3.0f;

				dst.data[indexDst++] = ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToInterleaved(DataBufferByte buffer, WritableRaster src, InterleavedF32 dst) {
		byte[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		int length = dst.width*dst.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + length;
			while( indexDst < indexDstEnd) {
				dst.data[indexDst++] = srcData[indexSrc++] & 0xFF;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void bufferedToInterleaved(DataBufferByte buffer, WritableRaster src, InterleavedU8 dst) {
		byte[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		int length = dst.width*dst.numBands;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y*dst.stride;
			int indexSrc = srcOffset + y*srcStride;

			System.arraycopy(srcData,indexSrc,dst.data,indexDst,length);
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_4BU8_to_PLF32(byte[] srcData, int srcStride, int srcOffset, Planar<GrayF32> dst) {
		float[] band1 = dst.getBand(0).data;
		float[] band2 = dst.getBand(1).data;
		float[] band3 = dst.getBand(2).data;
		float[] band4 = dst.getBand(3).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			for (; indexDst < indexDstEnd; indexDst++) {
				band1[indexDst] = srcData[indexSrc++] & 0xFF;
				band2[indexDst] = srcData[indexSrc++] & 0xFF;
				band3[indexDst] = srcData[indexSrc++] & 0xFF;
				band4[indexDst] = srcData[indexSrc++] & 0xFF;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_1BU8_to_PLF32(byte[] srcData, int srcStride, int srcOffset, Planar<GrayF32> dst) {
		float[] data = dst.getBand(0).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;

			for (; indexDst < indexDstEnd; indexDst++) {
				data[indexDst] = srcData[indexSrc++] & 0xFF;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_3BU8_to_PLF32(byte[] srcData, int srcStride, int srcOffset, Planar<GrayF32> dst) {
		float[] band1 = dst.getBand(0).data;
		float[] band2 = dst.getBand(1).data;
		float[] band3 = dst.getBand(2).data;


		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			for (; indexDst < indexDstEnd; indexDst++) {
				band1[indexDst] = srcData[indexSrc++] & 0xFF;
				band2[indexDst] = srcData[indexSrc++] & 0xFF;
				band3[indexDst] = srcData[indexSrc++] & 0xFF;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_4BU8_to_PLU8(byte[] srcData, int srcStride, int srcOffset, Planar<GrayU8> dst) {
		byte[] band1 = dst.getBand(0).data;
		byte[] band2 = dst.getBand(1).data;
		byte[] band3 = dst.getBand(2).data;
		byte[] band4 = dst.getBand(3).data;


		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			for (; indexDst < indexDstEnd; indexDst++) {
				band1[indexDst] = srcData[indexSrc++];
				band2[indexDst] = srcData[indexSrc++];
				band3[indexDst] = srcData[indexSrc++];
				band4[indexDst] = srcData[indexSrc++];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_1BU8_to_PLU8(byte[] srcData, int srcStride, int srcOffset, Planar<GrayU8> dst) {
		byte dstData[] = dst.getBand(0).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;

			System.arraycopy(srcData, indexSrc, dstData, indexDst, dst.width);
		}
		//CONCURRENT_ABOVE });
	}

	public static void from_3BU8_to_PLU8(byte[] srcData, int srcStride, int srcOffset, Planar<GrayU8> dst) {
		byte[] band1 = dst.getBand(0).data;
		byte[] band2 = dst.getBand(1).data;
		byte[] band3 = dst.getBand(2).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + dst.stride * y;
			int indexDstEnd = indexDst + dst.width;
			for (; indexDst < indexDstEnd; indexDst++) {
				band1[indexDst] = srcData[indexSrc++];
				band2[indexDst] = srcData[indexSrc++];
				band3[indexDst] = srcData[indexSrc++];
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(DataBufferInt buffer, WritableRaster src, GrayU8 dst) {
		int[] srcData = buffer.getData();

		byte[] data = dst.data;

		int srcStride = stride(src);
		int srcOffset = getOffset(src);


		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + y * dst.stride;
			for (int x = 0; x < dst.width; x++) {

				int rgb = srcData[indexSrc++];

				int r = (rgb >>> 16) & 0xFF;
				int g = (rgb >>> 8) & 0xFF;
				int b = rgb & 0xFF;

				int ave = (r + g + b) / 3;

				data[indexDst++] = (byte) ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(DataBufferInt buffer, WritableRaster src, GrayF32 dst) {
		int[] srcData = buffer.getData();

		float[] data = dst.data;

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
		for (int y = 0; y < dst.height; y++) {
			int indexSrc = srcOffset + y*srcStride;
			int indexDst = dst.startIndex + y * dst.stride;
			for (int x = 0; x < dst.width; x++) {

				int rgb = srcData[indexSrc++];

				int r = (rgb >>> 16) & 0xFF;
				int g = (rgb >>> 8) & 0xFF;
				int b = rgb & 0xFF;

				float ave = (r + g + b) / 3.0f;

				data[indexDst++] = ave;
			}
		}
		//CONCURRENT_ABOVE });
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToPlanar_U8(DataBufferInt buffer, WritableRaster src, Planar<GrayU8> dst) {
		int[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		int numBands = src.getNumBands();
		byte[] data1 = dst.getBand(0).data;
		byte[] data2 = dst.getBand(1).data;
		byte[] data3 = dst.getBand(2).data;

		if( numBands == 3 ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (byte) (rgb >>> 16);
					data2[indexDst] = (byte) (rgb >>> 8);
					data3[indexDst] = (byte) rgb;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( numBands == 4 ) {
			byte[] data4 = dst.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (byte) (rgb >>> 24);
					data2[indexDst] = (byte) (rgb >>> 16);
					data3[indexDst] = (byte) (rgb >>> 8);
					data4[indexDst] = (byte) rgb;
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToPlanar_F32(DataBufferInt buffer, WritableRaster src, Planar<GrayF32> dst) {
		int[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		float[] data1 = dst.getBand(0).data;
		float[] data2 = dst.getBand(1).data;
		float[] data3 = dst.getBand(2).data;

		int numBands = src.getNumBands();

		if( numBands == 3 ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (rgb >>> 16) & 0xFF;
					data2[indexDst] = (rgb >>> 8) & 0xFF;
					data3[indexDst] = rgb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( numBands == 4 ) {
			float[] data4 = dst.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (rgb >>> 24) & 0xFF;
					data2[indexDst] = (rgb >>> 16) & 0xFF;
					data3[indexDst] = (rgb >>> 8) & 0xFF;
					data4[indexDst] = rgb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToInterleaved(DataBufferInt buffer, WritableRaster src, InterleavedU8 dst) {
		int[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		int numBands = src.getNumBands();
		if( numBands == 3 ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++) {

					int rgb = srcData[indexSrc++];

					dst.data[indexDst++] = (byte) (rgb >>> 16);
					dst.data[indexDst++] = (byte) (rgb >>> 8);
					dst.data[indexDst++] = (byte) rgb;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( numBands == 4 ) {

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++) {

					int rgb = srcData[indexSrc++];

					dst.data[indexDst++] = (byte) (rgb >>> 24);
					dst.data[indexDst++] = (byte) (rgb >>> 16);
					dst.data[indexDst++] = (byte) (rgb >>> 8);
					dst.data[indexDst++] = (byte) rgb;
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void bufferedToInterleaved(DataBufferInt buffer, WritableRaster src, InterleavedF32 dst ) {
		int[] srcData = buffer.getData();

		int srcStride = stride(src);
		int srcOffset = getOffset(src);

		int numBands = src.getNumBands();

		if( numBands == 3 ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++) {

					int rgb = srcData[indexSrc++];

					dst.data[indexDst++] = (rgb >>> 16) & 0xFF;
					dst.data[indexDst++] = (rgb >>> 8) & 0xFF;
					dst.data[indexDst++] = rgb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( numBands == 4 ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, dst.height, y -> {
			for (int y = 0; y < dst.height; y++) {
				int indexSrc = srcOffset + y*srcStride;
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++) {

					int rgb = srcData[indexSrc++];

					dst.data[indexDst++] = (rgb >>> 24) & 0xFF;
					dst.data[indexDst++] = (rgb >>> 16) & 0xFF;
					dst.data[indexDst++] = (rgb >>> 8) & 0xFF;
					dst.data[indexDst++] = rgb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void bufferedToGray(BufferedImage src, byte[] dstData, int dstStartIndex , int dstStride ) {

		int width = src.getWidth();
		int height = src.getHeight();

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image. See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();

			//CONCURRENT_REMOVE_BELOW
			int hack[] = new int[1];

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				//CONCURRENT_INLINE int hack[] = new int[1];
				int index = dstStartIndex + y * dstStride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					dstData[index++] = (byte) hack[0];
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dstStartIndex + y * dstStride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					dstData[index++] = (byte) ((((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) / 3);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an 16bit intensity image using the
	 * BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working
	 * directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToGray(BufferedImage src, GrayI16 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		short[] data = dst.data;

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY || src.getType() == BufferedImage.TYPE_USHORT_GRAY ) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image. See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			//CONCURRENT_REMOVE_BELOW
			int hack[] = new int[1];

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				//CONCURRENT_INLINE int hack[] = new int[1];
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					data[index++] = (short) hack[0];
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			// this will be totally garbage. just here so that some unit test will pass

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					data[index++] = (short) ((((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) / 3);
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void bufferedToGray(BufferedImage src, float[] data, int dstStartIndex , int dstStride ) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image. See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			//CONCURRENT_REMOVE_BELOW
			float hack[] = new float[1];

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				//CONCURRENT_INLINE int hack[] = new int[1];
				int index = dstStartIndex + y * dstStride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					data[index++] = hack[0];
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dstStartIndex + y * dstStride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					int r = (argb >>> 16) & 0xFF;
					int g = (argb >>> 8) & 0xFF;
					int b = argb & 0xFF;

					float ave = (r + g + b) / 3.0f;

					data[index++] = ave;
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an planar image using the BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToPlanar_U8(BufferedImage src, Planar<GrayU8> dst) {

		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {
			byte[] band1 = dst.getBand(0).data;
			byte[] band2 = dst.getBand(1).data;
			byte[] band3 = dst.getBand(2).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++, index++) {
					int argb = src.getRGB(x, y);

					band1[index] = (byte) (argb >>> 16);
					band2[index] = (byte) (argb >>> 8);
					band3[index] = (byte) argb;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			bufferedToGray(src, dst.getBand(0).data,dst.startIndex,dst.stride);
			GrayU8 band1 = dst.getBand(0);
			for (int i = 1; i < dst.getNumBands(); i++) {
				dst.getBand(i).setTo(band1);
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an planar image using the BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToPlanar_F32(BufferedImage src, Planar<GrayF32> dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {
			final float[] band1 = dst.getBand(0).data;
			final float[] band2 = dst.getBand(1).data;
			final float[] band3 = dst.getBand(2).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++, index++) {
					int argb = src.getRGB(x, y);

					band1[index] = (argb >>> 16) & 0xFF;
					band2[index] = (argb >>> 8) & 0xFF;
					band3[index] = argb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (dst.getNumBands() == 4) {
			final float[] band1 = dst.getBand(0).data;
			final float[] band2 = dst.getBand(1).data;
			final float[] band3 = dst.getBand(2).data;
			final float[] band4 = dst.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++, index++) {
					int argb = src.getRGB(x, y);

					band1[index] = (argb >>> 24) & 0xFF;
					band2[index] = (argb >>> 16) & 0xFF;
					band3[index] = (argb >>> 8) & 0xFF;
					band4[index] = argb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( dst.getNumBands() == 1 ){
			bufferedToGray(src, dst.getBand(0).data, dst.startIndex, dst.stride);
		} else {
			throw new IllegalArgumentException("Unsupported number of input bands");
		}
	}

	public static void bufferedToInterleaved(BufferedImage src, InterleavedF32 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					dst.data[indexDst++] = (argb >>> 16) & 0xFF;
					dst.data[indexDst++] = (argb >>> 8) & 0xFF;
					dst.data[indexDst++] = argb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (dst.getNumBands() == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					dst.data[indexDst++] = (argb >>> 24) & 0xFF;
					dst.data[indexDst++] = (argb >>> 16) & 0xFF;
					dst.data[indexDst++] = (argb >>> 8) & 0xFF;
					dst.data[indexDst++] = argb & 0xFF;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( dst.getNumBands() == 1 ){
			bufferedToGray(src, dst.data, dst.startIndex, dst.stride);
		} else {
			throw new IllegalArgumentException("Unsupported number of input bands");
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an planar image using the BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToInterleaved(BufferedImage src, InterleavedU8 dst) {

		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					dst.data[indexDst++] = (byte) (argb >>> 16);
					dst.data[indexDst++] = (byte) (argb >>> 8);
					dst.data[indexDst++] = (byte) argb;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( dst.getNumBands() == 1 ){
			ImplConvertRaster.bufferedToGray(src, dst.data,dst.startIndex,dst.stride);
		} else {
			throw new IllegalArgumentException("Unsupported number of input bands");
		}
	}

	public static void grayToBuffered(GrayU8 src, DataBufferByte buffer , WritableRaster dst) {

		final byte[] srcData = src.data;
		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 1) {
			if (src.startIndex == 0 && src.width == src.stride) {
				System.arraycopy(srcData, 0, dstData, 0, size);
			} else {
				//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.startIndex + src.stride * y;
					int indexDst = src.width * y;

					System.arraycopy(srcData, indexSrc, dstData, indexDst, src.width);
				}
				//CONCURRENT_ABOVE });
			}
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayI16 src, DataBufferByte buffer , WritableRaster dst) {

		final short[] srcData = src.data;
		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 1) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) srcData[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayF32 src, DataBufferByte buffer , WritableRaster dst) {

		final float[] srcData = src.data;
		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 1) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) srcData[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayI16 src, DataBufferUShort buffer , WritableRaster dst) {

		final short[] srcData = src.data;
		final short[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					short val = srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 1) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = srcData[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					short val = srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, DataBufferByte buffer , WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			final byte[] band1 = src.getBand(0).data;
			final byte[] band2 = src.getBand(1).data;
			final byte[] band3 = src.getBand(2).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = band1[indexSrc];
					dstData[indexDst++] = band2[indexSrc];
					dstData[indexDst++] = band3[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			final byte[] band1 = src.getBand(0).data;
			final byte[] band2 = src.getBand(1).data;
			final byte[] band3 = src.getBand(2).data;
			final byte[] band4 = src.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = band1[indexSrc];
					dstData[indexDst++] = band2[indexSrc];
					dstData[indexDst++] = band3[indexSrc];
					dstData[indexDst++] = band4[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			byte bands[][] = new byte[numBands][];
			for (int i = 0; i < numBands; i++) {
				bands[i] = src.getBand(i).data;
			}

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					for (int i = 0; i < numBands; i++)
						dstData[indexDst++] = bands[i][indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void planarToBuffered_F32(Planar<GrayF32> src, DataBufferByte buffer , WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			final float[] band1 = src.getBand(0).data;
			final float[] band2 = src.getBand(1).data;
			final float[] band3 = src.getBand(2).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) band1[indexSrc];
					dstData[indexDst++] = (byte) band2[indexSrc];
					dstData[indexDst++] = (byte) band3[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			final float[] band1 = src.getBand(0).data;
			final float[] band2 = src.getBand(1).data;
			final float[] band3 = src.getBand(2).data;
			final float[] band4 = src.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) band1[indexSrc];
					dstData[indexDst++] = (byte) band2[indexSrc];
					dstData[indexDst++] = (byte) band3[indexSrc];
					dstData[indexDst++] = (byte) band4[indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			float bands[][] = new float[numBands][];
			for (int i = 0; i < numBands; i++) {
				bands[i] = src.getBand(i).data;
			}

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;
				int indexDst = y*src.width*numBands;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					for (int i = 0; i < numBands; i++)
						dstData[indexDst++] = (byte) bands[i][indexSrc];
				}
			}
			//CONCURRENT_ABOVE });
		}
	}

	public static void grayToBuffered(GrayU8 src, DataBufferInt buffer, WritableRaster dst) {

		final byte[] srcData = src.data;
		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayI16 src, DataBufferInt buffer, WritableRaster dst) {
		final short[] srcData = src.data;
		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayF32 src, DataBufferInt buffer, WritableRaster dst) {
		final float[] srcData = src.data;
		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, DataBufferInt buffer, WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		final byte[] band1 = src.getBand(0).data;
		final byte[] band2 = src.getBand(1).data;
		final byte[] band3 = src.getBand(2).data;

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = band1[indexSrc] & 0xFF;
					int c2 = band2[indexSrc] & 0xFF;
					int c3 = band3[indexSrc] & 0xFF;

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			final byte[] band4 = src.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = band1[indexSrc] & 0xFF;
					int c2 = band2[indexSrc] & 0xFF;
					int c3 = band3[indexSrc] & 0xFF;
					int c4 = band4[indexSrc] & 0xFF;

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void planarToBuffered_F32(Planar<GrayF32> src, DataBufferInt buffer, WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();

		final float[] band1 = src.getBand(0).data;
		final float[] band2 = src.getBand(1).data;
		final float[] band3 = src.getBand(2).data;

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = (int) band1[indexSrc];
					int c2 = (int) band2[indexSrc];
					int c3 = (int) band3[indexSrc];

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			final float[] band4 = src.getBand(3).data;

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = y*src.width;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = (int) band1[indexSrc];
					int c2 = (int) band2[indexSrc];
					int c3 = (int) band3[indexSrc];
					int c4 = (int) band4[indexSrc];

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(GrayU8 src, BufferedImage dst) {

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		byte[] data = src.data;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = data[indexSrc++] & 0xFF;

				int rgb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, rgb);
			}
		}
		//CONCURRENT_ABOVE });

	}

	public static void grayToBuffered(GrayI16 src, BufferedImage dst) {

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		short[] data = src.data;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int) data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void grayToBuffered(GrayF32 src, BufferedImage dst) {
		final int width = dst.getWidth();
		final int height = dst.getHeight();

		float[] data = src.data;
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int) data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void planarToBuffered_U8(Planar<GrayU8> src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		byte[] band1 = src.getBand(0).data;
		byte[] band2 = src.getBand(1).data;
		byte[] band3 = src.getBand(2).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++, indexSrc++) {
				int c1 = band1[indexSrc] & 0xFF;
				int c2 = band2[indexSrc] & 0xFF;
				int c3 = band3[indexSrc] & 0xFF;

				int argb = c1 << 16 | c2 << 8 | c3;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void planarToBuffered_F32(Planar<GrayF32> src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		float[] band1 = src.getBand(0).data;
		float[] band2 = src.getBand(1).data;
		float[] band3 = src.getBand(2).data;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++, indexSrc++) {
				int c1 = (int) band1[indexSrc];
				int c2 = (int) band2[indexSrc];
				int c3 = (int) band3[indexSrc];

				int argb = c1 << 16 | c2 << 8 | c3;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void interleavedToBuffered(InterleavedU8 src, DataBufferInt buffer, WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();
		int dstStride = stride(dst);
		int dstOffset = getOffset(dst);

		if (numBands == 3) {

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = dstOffset + y*dstStride;

				for (int x = 0; x < src.width; x++) {
					int c1 = src.data[indexSrc++] & 0xFF;
					int c2 = src.data[indexSrc++] & 0xFF;
					int c3 = src.data[indexSrc++] & 0xFF;

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {

			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = dstOffset + y*dstStride;

				for (int x = 0; x < src.width; x++) {
					int c1 = src.data[indexSrc++] & 0xFF;
					int c2 = src.data[indexSrc++] & 0xFF;
					int c3 = src.data[indexSrc++] & 0xFF;
					int c4 = src.data[indexSrc++] & 0xFF;

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void interleavedToBuffered(InterleavedU8 src, DataBufferByte buffer , WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();
		final int length = src.width*numBands;

		int dstStride = stride(dst);
		int dstOffset = getOffset(dst);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
		for (int y = 0; y < src.height; y++) {
			int indexSrc = src.startIndex + src.stride * y;
			int indexDst = dstOffset + dstStride*y;

			System.arraycopy(src.data,indexSrc,dstData,indexDst,length);
		}
		//CONCURRENT_ABOVE });
	}

	public static void interleavedToBuffered( InterleavedU8 src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++ ) {
				int c1 = src.data[indexSrc++] & 0xFF;
				int c2 = src.data[indexSrc++] & 0xFF;
				int c3 = src.data[indexSrc++] & 0xFF;

				int argb = c1 << 16 | c2 << 8 | c3;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void interleavedToBuffered(InterleavedF32 src, DataBufferInt buffer, WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();
		int dstStride = stride(dst);
		int dstOffset = getOffset(dst);

		if (numBands == 3) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = dstOffset + y*dstStride;

				for (int x = 0; x < src.width; x++) {
					int c1 = (int)src.data[indexSrc++];
					int c2 = (int)src.data[indexSrc++];
					int c3 = (int)src.data[indexSrc++];

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if (numBands == 4) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;
				int indexDst = dstOffset + y*dstStride;

				for (int x = 0; x < src.width; x++) {
					int c1 = (int)src.data[indexSrc++];
					int c2 = (int)src.data[indexSrc++];
					int c3 = (int)src.data[indexSrc++];
					int c4 = (int)src.data[indexSrc++];

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void interleavedToBuffered(InterleavedF32 src, DataBufferByte buffer , WritableRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = buffer.getData();

		final int numBands = dst.getNumBands();
		final int length = src.width*numBands;

		int dstStride = stride(dst);
		int dstOffset = getOffset(dst);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, y -> {
		for (int y = 0; y < src.height; y++) {
			int indexSrc = src.startIndex + src.stride * y;
			int indexDst = dstOffset + dstStride*y;
			int indexSrcEnd = indexSrc+length;

			while( indexSrc < indexSrcEnd ) {
				dstData[indexDst++] = (byte)src.data[indexSrc++];
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void interleavedToBuffered( InterleavedF32 src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++ ) {
				int c1 = (int)src.data[indexSrc++];
				int c2 = (int)src.data[indexSrc++];
				int c3 = (int)src.data[indexSrc++];

				int argb = c1 << 16 | c2 << 8 | c3;

				dst.setRGB(x, y, argb);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void orderBandsIntoRGB( InterleavedU8 image , BufferedImage input ) {
		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {
				//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
				for (int y = 0; y < image.height; y++) {
					int index = image.startIndex + y*image.stride;
					int indexEnd = index + image.width*3;

					while( index < indexEnd ) {
						byte tmp = image.data[index+2];
						image.data[index+2] = image.data[index];
						image.data[index] = tmp;

						index += 3;
					}
				}
				//CONCURRENT_ABOVE });

			} else if( image.getNumBands() == 4 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {

					//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							byte tmp = image.data[index];
							image.data[index] = image.data[index+1];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = image.data[index+3];
							image.data[index+3] = tmp;

							index += 4;
						}
					}
					//CONCURRENT_ABOVE });
				} else if( bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {

					//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							byte tmp1 = image.data[index+1];
							byte tmp0 = image.data[index];
							image.data[index] = image.data[index+3];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = tmp1;
							image.data[index+3] = tmp0;

							index += 4;
						}
					}
					//CONCURRENT_ABOVE });
				}
			}
		}
	}

	public static void orderBandsIntoRGB( InterleavedF32 image , BufferedImage input ) {
		boolean swap = swapBandOrder(input);

		// Output formats are: RGB and RGBA

		if( swap ) {
			if( image.getNumBands() == 3 ) {

				//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
				for (int y = 0; y < image.height; y++) {
					int index = image.startIndex + y*image.stride;
					int indexEnd = index + image.width*3;

					while( index < indexEnd ) {
						float tmp = image.data[index+2];
						image.data[index+2] = image.data[index];
						image.data[index] = tmp;

						index += 3;
					}
				}
				//CONCURRENT_ABOVE });

			} else if( image.getNumBands() == 4 ) {
				int bufferedImageType = input.getType();
				if( bufferedImageType == BufferedImage.TYPE_INT_ARGB ) {

					//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							float tmp = image.data[index];
							image.data[index] = image.data[index+1];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = image.data[index+3];
							image.data[index+3] = tmp;

							index += 4;
						}
					}
					//CONCURRENT_ABOVE });
				} else if( bufferedImageType == BufferedImage.TYPE_4BYTE_ABGR ) {

					//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, y -> {
					for (int y = 0; y < image.height; y++) {
						int index = image.startIndex + y*image.stride;
						int indexEnd = index + image.width*3;

						while( index < indexEnd ) {
							float tmp1 = image.data[index+1];
							float tmp0 = image.data[index];
							image.data[index] = image.data[index+3];
							image.data[index+1] = image.data[index+2];
							image.data[index+2] = tmp1;
							image.data[index+3] = tmp0;

							index += 4;
						}
					}
					//CONCURRENT_ABOVE });
				}
			}
		}
	}

	/**
	 * The image the BufferedImage was created from had RGB or RGBA color order. This swaps the bytes around
	 * to put it into the expected local format
	 */
	public static void orderBandsBufferedFromRGB( DataBufferByte buffer , WritableRaster raster , int type ) {
		int height = raster.getHeight();
		int width = raster.getWidth();
		int stride = ConvertRaster.stride(raster);
		int offset = ConvertRaster.getOffset(raster);
		byte data[] = buffer.getData();

		if( BufferedImage.TYPE_3BYTE_BGR == type ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++) {
					byte tmp = data[index];
					data[index] = data[index+2];
					data[index+2] = tmp;
					index += 3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( BufferedImage.TYPE_4BYTE_ABGR == type ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++) {
					byte tmp0 = data[index];
					byte tmp1 = data[index+1];
					data[index] = data[index+3];
					data[index+1] = data[index+2];
					data[index+2] = tmp1;
					data[index+3] = tmp0;
					index += 4;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new IllegalArgumentException("Unsupported buffered image type");
		}
	}

	/**
	 * The image the BufferedImage was created from had RGB or RGBA color order. This swaps the bytes around
	 * to put it into the expected local format
	 */
	public static void orderBandsBufferedFromRGB( DataBufferInt buffer, WritableRaster raster , int type ) {
		if( BufferedImage.TYPE_INT_RGB == type )
			return;

		int height = raster.getHeight();
		int width = raster.getWidth();
		int stride = ConvertRaster.stride(raster);
		int offset = ConvertRaster.getOffset(raster);
		int data[] = buffer.getData();

		if( BufferedImage.TYPE_INT_BGR == type ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++, index++) {
					int tmp = data[index];
					int c1 = tmp & 0xFF;
					int c2 = (tmp >> 8) & 0xFF;
					int c3 = (tmp >> 16) & 0xFF;

					data[index] = c1 << 16 | c2 << 8 | c3;
				}
			}
			//CONCURRENT_ABOVE });
		} else if( BufferedImage.TYPE_INT_ARGB == type ) {
			//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
			for (int y = 0; y < height; y++) {
				int index = offset + y*stride;
				for (int x = 0; x < width; x++, index++) {
					int tmp = data[index];
					int c1 = tmp & 0xFF;
					int c2 = (tmp >> 8) & 0xFF;
					int c3 = (tmp >> 16) & 0xFF;
					int c4 = (tmp >> 24) & 0xFF;

					data[index] = c1 << 24 | c4 << 16 | c3 << 8 | c2;
				}
			}
			//CONCURRENT_ABOVE });
		} else {
			throw new IllegalArgumentException("Unsupported buffered image type");
		}
	}
}
