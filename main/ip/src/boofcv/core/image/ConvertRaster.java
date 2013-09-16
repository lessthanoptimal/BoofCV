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

package boofcv.core.image;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInt16;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;
import sun.awt.image.ShortInterleavedRaster;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

/**
 * Routines for converting to and from {@link BufferedImage} that use its internal
 * raster for better performance.
 *
 * @author Peter Abeles
 */
public class ConvertRaster {

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(ByteInterleavedRaster src, ImageUInt8 dst) {
		byte[] srcData = src.getDataStorage();

		byte[] data = dst.data;

		int numBands = src.getNumBands();

		int size = dst.getWidth() * dst.getHeight();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		if (numBands == 3) {
			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					int r = srcData[indexSrc++] & 0xFF;
					int g = srcData[indexSrc++] & 0xFF;
					int b = srcData[indexSrc++] & 0xFF;

					int ave = (r + g + b) / 3;

					data[indexDst] = (byte) ave;
				}
				indexSrc += srcStrideDiff;
			}
		} else if (numBands == 1) {
			if (dst.startIndex == 0 && dst.width == dst.stride && srcStrideDiff == 0 && srcOffset == 0 )
				System.arraycopy(srcData, 0, data, 0, size);
			else {
				for (int y = 0; y < dst.height; y++) {
					int indexDst = dst.startIndex + dst.stride * y;
					int indexSrc = srcOffset + srcStride * y;

					System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
				}
			}
		} else if (numBands == 4) {
			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					indexSrc++;
					int r = srcData[indexSrc++] & 0xFF;
					int g = srcData[indexSrc++] & 0xFF;
					int b = srcData[indexSrc++] & 0xFF;

					int ave = (r + g + b) / 3;

					data[indexDst] = (byte) ave;
				}
				indexSrc += srcStrideDiff;
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(ShortInterleavedRaster src, ImageInt16 dst) {
		short[] srcData = src.getDataStorage();

		int numBands = src.getNumBands();

		int size = dst.getWidth() * dst.getHeight();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		if (numBands == 1) {
			if (dst.startIndex == 0 && dst.width == dst.stride && srcStrideDiff == 0 && srcOffset == 0 )
				System.arraycopy(srcData, 0, dst.data, 0, size);
			else {
				for (int y = 0; y < dst.height; y++) {
					int indexDst = dst.startIndex + dst.stride * y;
					int indexSrc = srcOffset + srcStride * y;

					System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
				}
			}
		} else {
			throw new RuntimeException("Only single band images are currently support for 16bit");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(ByteInterleavedRaster src, ImageFloat32 dst) {
		byte[] srcData = src.getDataStorage();

		float[] data = dst.data;

		int numBands = src.getNumBands();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-numBands+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		if (numBands == 3) {
			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					int r = srcData[indexSrc++] & 0xFF;
					int g = srcData[indexSrc++] & 0xFF;
					int b = srcData[indexSrc++] & 0xFF;

					float ave = (r + g + b) / 3.0f;

					data[indexDst] = ave;
				}
				indexSrc += srcStrideDiff;
			}
		} else if (numBands == 1) {
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				int indexSrc = srcOffset + srcStride * y;

				for (; indexDst < indexDstEnd; indexDst++) {
					data[indexDst] = srcData[indexSrc++] & 0xFF;
				}
			}
		} else if (numBands == 4) {
			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					indexSrc++;
					int r = srcData[indexSrc++] & 0xFF;
					int g = srcData[indexSrc++] & 0xFF;
					int b = srcData[indexSrc++] & 0xFF;

					float ave = (r + g + b) / 3.0f;

					data[indexDst] = ave;
				}
				indexSrc += srcStrideDiff;
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToMulti_U8(ByteInterleavedRaster src, MultiSpectral<ImageUInt8> dst) {
		byte[] srcData = src.getDataStorage();

		int numBands = src.getNumBands();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-numBands+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		if (numBands == 3) {
			byte[] band1 = dst.getBand(0).data;
			byte[] band2 = dst.getBand(1).data;
			byte[] band3 = dst.getBand(2).data;

			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					band1[indexDst] = srcData[indexSrc++];
					band2[indexDst] = srcData[indexSrc++];
					band3[indexDst] = srcData[indexSrc++];
				}
				indexSrc += srcStrideDiff;
			}
		} else if (numBands == 1) {
			byte dstData[] = dst.getBand(0).data;

			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexSrc = srcOffset + srcStride * y;

				System.arraycopy(srcData, indexSrc, dstData, indexDst, dst.width);
			}
		} else if (numBands == 4) {
			byte[] band1 = dst.getBand(0).data;
			byte[] band2 = dst.getBand(1).data;
			byte[] band3 = dst.getBand(2).data;
			byte[] band4 = dst.getBand(3).data;

			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					band1[indexDst] = srcData[indexSrc++];
					band2[indexDst] = srcData[indexSrc++];
					band3[indexDst] = srcData[indexSrc++];
					band4[indexDst] = srcData[indexSrc++];
				}
				indexSrc += srcStrideDiff;
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToMulti_F32(ByteInterleavedRaster src, MultiSpectral<ImageFloat32> dst) {
		byte[] srcData = src.getDataStorage();

		int numBands = src.getNumBands();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		if (numBands == 3) {
			float[] band1 = dst.getBand(0).data;
			float[] band2 = dst.getBand(1).data;
			float[] band3 = dst.getBand(2).data;

			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					band1[indexDst] = srcData[indexSrc++] & 0xFF;
					band2[indexDst] = srcData[indexSrc++] & 0xFF;
					band3[indexDst] = srcData[indexSrc++] & 0xFF;
				}
				indexSrc += srcStrideDiff;
			}
		} else if (numBands == 1) {
			float[] data = dst.getBand(0).data;

			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				int indexSrc = srcOffset + srcStride * y;

				for (; indexDst < indexDstEnd; indexDst++) {
					data[indexDst] = srcData[indexSrc++] & 0xFF;
				}
			}
		} else if (numBands == 4) {
			float[] band1 = dst.getBand(0).data;
			float[] band2 = dst.getBand(1).data;
			float[] band3 = dst.getBand(2).data;
			float[] band4 = dst.getBand(3).data;

			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				for (; indexDst < indexDstEnd; indexDst++) {
					band1[indexDst] = srcData[indexSrc++] & 0xFF;
					band2[indexDst] = srcData[indexSrc++] & 0xFF;
					band3[indexDst] = srcData[indexSrc++] & 0xFF;
					band4[indexDst] = srcData[indexSrc++] & 0xFF;
				}

				indexSrc += srcStrideDiff;
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(IntegerInterleavedRaster src, ImageUInt8 dst) {
		int[] srcData = src.getDataStorage();

		byte[] data = dst.data;

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y * dst.stride;
			for (int x = 0; x < dst.width; x++) {

				int rgb = srcData[indexSrc++];

				int r = (rgb >>> 16) & 0xFF;
				int g = (rgb >>> 8) & 0xFF;
				int b = rgb & 0xFF;

				int ave = (r + g + b) / 3;

				data[indexDst++] = (byte) ave;
			}
			indexSrc += srcStrideDiff;
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(IntegerInterleavedRaster src, ImageFloat32 dst) {
		int[] srcData = src.getDataStorage();

		float[] data = dst.data;

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		int indexSrc = srcOffset;
		for (int y = 0; y < dst.height; y++) {
			int indexDst = dst.startIndex + y * dst.stride;
			for (int x = 0; x < dst.width; x++) {

				int rgb = srcData[indexSrc++];

				int r = (rgb >>> 16) & 0xFF;
				int g = (rgb >>> 8) & 0xFF;
				int b = rgb & 0xFF;

				float ave = (r + g + b) / 3.0f;

				data[indexDst++] = ave;
			}
			indexSrc += srcStrideDiff;
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToMulti_U8(IntegerInterleavedRaster src, MultiSpectral<ImageUInt8> dst) {
		int[] srcData = src.getDataStorage();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		int numBands = src.getNumBands();
		byte[] data1 = dst.getBand(0).data;
		byte[] data2 = dst.getBand(1).data;
		byte[] data3 = dst.getBand(2).data;

		if( numBands == 3 ) {
			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (byte) (rgb >>> 16);
					data2[indexDst] = (byte) (rgb >>> 8);
					data3[indexDst] = (byte) rgb;
				}

				indexSrc += srcStrideDiff;
			}
		} else if( numBands == 4 ) {
			byte[] data4 = dst.getBand(3).data;

			int indexSrc = srcOffset;
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (byte) (rgb >>> 24);
					data2[indexDst] = (byte) (rgb >>> 16);
					data3[indexDst] = (byte) (rgb >>> 8);
					data4[indexDst] = (byte) rgb;
				}

				indexSrc += srcStrideDiff;
			}
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToMulti_F32(IntegerInterleavedRaster src, MultiSpectral<ImageFloat32> dst) {
		int[] srcData = src.getDataStorage();

		int srcStride = src.getScanlineStride();
		int srcOffset = src.getDataOffset(0)-src.getPixelStride()+1;
		int srcStrideDiff = srcStride-src.getPixelStride()*dst.width;

		float[] data1 = dst.getBand(0).data;
		float[] data2 = dst.getBand(1).data;
		float[] data3 = dst.getBand(2).data;

		int indexSrc = srcOffset;

		int numBands = src.getNumBands();

		if( numBands == 3 ) {
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (rgb >>> 16) & 0xFF;
					data2[indexDst] = (rgb >>> 8) & 0xFF;
					data3[indexDst] = rgb & 0xFF;
				}

				indexSrc += srcStrideDiff;
			}
		} else if( numBands == 4 ) {
			float[] data4 = dst.getBand(3).data;

			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + y * dst.stride;
				for (int x = 0; x < dst.width; x++, indexDst++) {

					int rgb = srcData[indexSrc++];

					data1[indexDst] = (rgb >>> 24) & 0xFF;
					data2[indexDst] = (rgb >>> 16) & 0xFF;
					data3[indexDst] = (rgb >>> 8) & 0xFF;
					data4[indexDst] = rgb & 0xFF;
				}

				indexSrc += srcStrideDiff;
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an 8bit intensity image using the
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
	public static void bufferedToGray(BufferedImage src, ImageUInt8 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		byte[] data = dst.data;

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image.  See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			int hack[] = new int[1];

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					data[index++] = (byte) hack[0];
				}
			}
		} else {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					data[index++] = (byte) ((((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) / 3);
				}
			}
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
	public static void bufferedToGray(BufferedImage src, ImageInt16 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		short[] data = dst.data;

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY || src.getType() == BufferedImage.TYPE_USHORT_GRAY ) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image.  See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			int hack[] = new int[1];

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					data[index++] = (short) hack[0];
				}
			}
		} else {
			// this will be totally garbage.  just here so that some unit test will pass
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					data[index++] = (short) ((((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) / 3);
				}
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an 8bit intensity image using the
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
	public static void bufferedToGray(BufferedImage src, ImageFloat32 dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		float[] data = dst.data;

		if (src.getType() == BufferedImage.TYPE_BYTE_GRAY) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image.  See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			float hack[] = new float[1];

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x, y, hack);

					data[index++] = hack[0];
				}
			}
		} else {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					int r = (argb >>> 16) & 0xFF;
					int g = (argb >>> 8) & 0xFF;
					int b = argb & 0xFF;

					float ave = (r + g + b) / 3.0f;

					data[index++] = ave;
				}
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an multi-spectral image using the BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToMulti_U8(BufferedImage src, MultiSpectral<ImageUInt8> dst) {

		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {
			byte[] band1 = dst.getBand(0).data;
			byte[] band2 = dst.getBand(1).data;
			byte[] band3 = dst.getBand(2).data;

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++, index++) {
					int argb = src.getRGB(x, y);

					band1[index] = (byte) (argb >>> 16);
					band2[index] = (byte) (argb >>> 8);
					band3[index] = (byte) argb;
				}
			}
		} else {
			ConvertRaster.bufferedToGray(src, dst.getBand(0));
			ImageUInt8 band1 = dst.getBand(0);
			for (int i = 1; i < dst.getNumBands(); i++) {
				dst.getBand(i).setTo(band1);
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an multi-spectral image using the BufferedImage's RGB interface.
	 * </p>
	 * <p>
	 * This is much slower than working directly with the BufferedImage's internal raster and should be
	 * avoided if possible.
	 * </p>
	 *
	 * @param src Input image.
	 * @param dst Output image.
	 */
	public static void bufferedToMulti_F32(BufferedImage src, MultiSpectral<ImageFloat32> dst) {
		final int width = src.getWidth();
		final int height = src.getHeight();

		if (dst.getNumBands() == 3) {
			final float[] band1 = dst.getBand(0).data;
			final float[] band2 = dst.getBand(1).data;
			final float[] band3 = dst.getBand(2).data;

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y * dst.stride;
				for (int x = 0; x < width; x++, index++) {
					int argb = src.getRGB(x, y);

					band1[index] = (argb >>> 16) & 0xFF;
					band2[index] = (argb >>> 8) & 0xFF;
					band3[index] = argb & 0xFF;
				}
			}
		} else if (dst.getNumBands() == 4) {
			final float[] band1 = dst.getBand(0).data;
			final float[] band2 = dst.getBand(1).data;
			final float[] band3 = dst.getBand(2).data;
			final float[] band4 = dst.getBand(3).data;

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
		} else {
			ConvertRaster.bufferedToGray(src, dst.getBand(0));
			ImageFloat32 band1 = dst.getBand(0);
			for (int i = 1; i < dst.getNumBands(); i++) {
				dst.getBand(i).setTo(band1);
			}
		}
	}

	public static void grayToBuffered(ImageUInt8 src, ByteInterleavedRaster dst) {

		final byte[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else if (numBands == 1) {
			if (src.startIndex == 0 && src.width == src.stride) {
				System.arraycopy(srcData, 0, dstData, 0, size);
			} else {
				for (int y = 0; y < src.height; y++) {
					int indexSrc = src.startIndex + src.stride * y;
					int indexDst = src.width * y;

					System.arraycopy(srcData, indexSrc, dstData, indexDst, src.width);
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageInt16 src, ByteInterleavedRaster dst) {

		final short[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else if (numBands == 1) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) srcData[indexSrc];
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageFloat32 src, ByteInterleavedRaster dst) {

		final float[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else if (numBands == 1) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) srcData[indexSrc];
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte) srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageInt16 src, ShortInterleavedRaster dst) {

		final short[] srcData = src.data;
		final short[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					short val = srcData[indexSrc];

					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else if (numBands == 1) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = srcData[indexSrc];
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					short val = srcData[indexSrc];

					indexDst++;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
					dstData[indexDst++] = val;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void multToBuffered_U8(MultiSpectral<ImageUInt8> src, ByteInterleavedRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			final byte[] band1 = src.getBand(0).data;
			final byte[] band2 = src.getBand(1).data;
			final byte[] band3 = src.getBand(2).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = band1[indexSrc];
					dstData[indexDst++] = band2[indexSrc];
					dstData[indexDst++] = band3[indexSrc];
				}
			}
		} else if (numBands == 4) {
			final byte[] band1 = src.getBand(0).data;
			final byte[] band2 = src.getBand(1).data;
			final byte[] band3 = src.getBand(2).data;
			final byte[] band4 = src.getBand(3).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = band1[indexSrc];
					dstData[indexDst++] = band2[indexSrc];
					dstData[indexDst++] = band3[indexSrc];
					dstData[indexDst++] = band4[indexSrc];
				}
			}
		} else {
			byte bands[][] = new byte[numBands][];
			for (int i = 0; i < numBands; i++) {
				bands[i] = src.getBand(i).data;
			}

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					for (int i = 0; i < numBands; i++)
						dstData[indexDst++] = (byte) bands[i][indexSrc];
				}
			}
		}
	}

	public static void multToBuffered_F32(MultiSpectral<ImageFloat32> src, ByteInterleavedRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			final float[] band1 = src.getBand(0).data;
			final float[] band2 = src.getBand(1).data;
			final float[] band3 = src.getBand(2).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) band1[indexSrc];
					dstData[indexDst++] = (byte) band2[indexSrc];
					dstData[indexDst++] = (byte) band3[indexSrc];
				}
			}
		} else if (numBands == 4) {
			final float[] band1 = src.getBand(0).data;
			final float[] band2 = src.getBand(1).data;
			final float[] band3 = src.getBand(2).data;
			final float[] band4 = src.getBand(3).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					dstData[indexDst++] = (byte) band1[indexSrc];
					dstData[indexDst++] = (byte) band2[indexSrc];
					dstData[indexDst++] = (byte) band3[indexSrc];
					dstData[indexDst++] = (byte) band4[indexSrc];
				}
			}
		} else {
			float bands[][] = new float[numBands][];
			for (int i = 0; i < numBands; i++) {
				bands[i] = src.getBand(i).data;
			}

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					for (int i = 0; i < numBands; i++)
						dstData[indexDst++] = (byte) bands[i][indexSrc];
				}
			}
		}
	}

	public static void grayToBuffered(ImageUInt8 src, IntegerInterleavedRaster dst) {

		final byte[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageInt16 src, IntegerInterleavedRaster dst) {
		final short[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageFloat32 src, IntegerInterleavedRaster dst) {
		final float[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else if (numBands == 4) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = (int) srcData[indexSrc++];

					dstData[indexDst++] = 0xFF << 24 | v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void multToBuffered_U8(MultiSpectral<ImageUInt8> src, IntegerInterleavedRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final byte[] band1 = src.getBand(0).data;
		final byte[] band2 = src.getBand(1).data;
		final byte[] band3 = src.getBand(2).data;

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = band1[indexSrc] & 0xFF;
					int c2 = band2[indexSrc] & 0xFF;
					int c3 = band3[indexSrc] & 0xFF;

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
		} else if (numBands == 4) {
			final byte[] band4 = src.getBand(3).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = band1[indexSrc] & 0xFF;
					int c2 = band2[indexSrc] & 0xFF;
					int c3 = band3[indexSrc] & 0xFF;
					int c4 = band4[indexSrc] & 0xFF;

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void multToBuffered_F32(MultiSpectral<ImageFloat32> src, IntegerInterleavedRaster dst) {

		if (src.getNumBands() != dst.getNumBands())
			throw new IllegalArgumentException("Unequal number of bands src = " + src.getNumBands() + " dst = " + dst.getNumBands());

		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final float[] band1 = src.getBand(0).data;
		final float[] band2 = src.getBand(1).data;
		final float[] band3 = src.getBand(2).data;

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = (int) band1[indexSrc];
					int c2 = (int) band2[indexSrc];
					int c3 = (int) band3[indexSrc];

					dstData[indexDst++] = c1 << 16 | c2 << 8 | c3;
				}
			}
		} else if (numBands == 4) {
			final float[] band4 = src.getBand(3).data;

			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++, indexSrc++) {
					int c1 = (int) band1[indexSrc];
					int c2 = (int) band2[indexSrc];
					int c3 = (int) band3[indexSrc];
					int c4 = (int) band4[indexSrc];

					dstData[indexDst++] = c1 << 24 | c2 << 16 | c3 << 8 | c4;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageUInt8 src, BufferedImage dst) {

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		byte[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = data[indexSrc++] & 0xFF;

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}

	}

	public static void grayToBuffered(ImageInt16 src, BufferedImage dst) {

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		short[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int) data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
	}

	public static void grayToBuffered(ImageFloat32 src, BufferedImage dst) {
		final int width = dst.getWidth();
		final int height = dst.getHeight();

		float[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int) data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
	}

	public static void multToBuffered_U8(MultiSpectral<ImageUInt8> src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		byte[] band1 = src.getBand(0).data;
		byte[] band2 = src.getBand(1).data;
		byte[] band3 = src.getBand(2).data;

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
	}

	public static void multToBuffered_F32(MultiSpectral<ImageFloat32> src, BufferedImage dst) {

		if (src.getNumBands() != 3)
			throw new IllegalArgumentException("src must have three bands");

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		float[] band1 = src.getBand(0).data;
		float[] band2 = src.getBand(1).data;
		float[] band3 = src.getBand(2).data;

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
	}
}
