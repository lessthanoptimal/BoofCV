/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import sun.awt.image.ByteInterleavedRaster;
import sun.awt.image.IntegerInterleavedRaster;

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

		if (numBands == 3) {
			int indexSrc = 0;
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
			}
		} else if (numBands == 1) {
			if (dst.startIndex == 0 && dst.width == dst.stride)
				System.arraycopy(srcData, 0, data, 0, size);
			else {
				for (int y = 0; y < dst.height; y++) {
					int indexDst = dst.startIndex + dst.stride * y;
					int indexSrc = dst.width * y;

					System.arraycopy(srcData, indexSrc, dst.data, indexDst, dst.width);
				}
			}
		} else {
			throw new RuntimeException("Write more code here.");
		}
	}

/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(ByteInterleavedRaster src, ImageFloat32 dst) {
		byte[] srcData = src.getDataStorage();

		float[] data = dst.data;

		int numBands = src.getNumBands();

		if (numBands == 3) {
			int indexSrc = 0;
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
			}
		} else if (numBands == 1) {
			for (int y = 0; y < dst.height; y++) {
				int indexDst = dst.startIndex + dst.stride * y;
				int indexDstEnd = indexDst + dst.width;
				int indexSrc = dst.width * y;

				for (; indexDst < indexDstEnd; indexDst++) {
					data[indexDst] = srcData[indexSrc++] & 0xFF;
				}
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

		int indexSrc = 0;
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
		}
	}

	/**
	 * A faster convert that works directly with a specific raster
	 */
	public static void bufferedToGray(IntegerInterleavedRaster src, ImageFloat32 dst) {
		int[] srcData = src.getDataStorage();

		float[] data = dst.data;

		int indexSrc = 0;
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

		if( src.getType() == BufferedImage.TYPE_BYTE_GRAY ) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image.  See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			int hack[] = new int[1];

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y*dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x,y,hack);

					data[index++] = (byte)hack[0];
				}
			}
		} else {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y*dst.stride;
				for (int x = 0; x < width; x++) {
					int argb = src.getRGB(x, y);

					data[index++] = (byte) ((((argb >>> 16) & 0xFF) + ((argb >>> 8) & 0xFF) + (argb & 0xFF)) / 3);
				}
			}
		}
	}

	/**
	 * <p>
	 * Converts a buffered image into an 8bit intensity image using the
	 * BufferedImage's RGB interface.
	 * </p>
	 * <p/>
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

		if( src.getType() == BufferedImage.TYPE_BYTE_GRAY ) {
			// If the buffered image is a gray scale image there is a bug where getRGB distorts
			// the image.  See Bug ID: 5051418 , it has been around since 2004. Fuckers...
			WritableRaster raster = src.getRaster();
			float hack[] = new float[1];

			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y*dst.stride;
				for (int x = 0; x < width; x++) {
					raster.getPixel(x,y,hack);

					data[index++] = hack[0];
				}
			}
		} else {
			for (int y = 0; y < height; y++) {
				int index = dst.startIndex + y*dst.stride;
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

	public static void grayToBuffered(ImageUInt8 src, ByteInterleavedRaster dst) {
//        dst.markDirty();

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
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageSInt16 src, ByteInterleavedRaster dst) {
//        dst.markDirty();

		final short[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte)srcData[indexSrc];

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
					dstData[indexDst++] = (byte)srcData[indexSrc];
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageFloat32 src, ByteInterleavedRaster dst) {
//        dst.markDirty();

		final float[] srcData = src.data;
		final byte[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + src.stride * y;
				int indexSrcEnd = indexSrc + src.width;

				for (; indexSrc < indexSrcEnd; indexSrc++) {
					byte val = (byte)srcData[indexSrc];

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
					dstData[indexDst++] = (byte)srcData[indexSrc];
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}

	}

	public static void grayToBuffered(ImageUInt8 src, IntegerInterleavedRaster dst) {
//        dst.markDirty();

		final byte[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		final int size = src.getWidth() * src.getHeight();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = srcData[indexSrc++] & 0xFF;

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageSInt16 src, IntegerInterleavedRaster dst) {
		final short[] srcData = src.data;
		final int[] dstData = dst.getDataStorage();

		final int numBands = dst.getNumBands();

		if (numBands == 3) {
			int indexDst = 0;
			for (int y = 0; y < src.height; y++) {
				int indexSrc = src.startIndex + y * src.stride;

				for (int x = 0; x < src.width; x++) {
					int v = (int)srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
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
					int v = (int)srcData[indexSrc++];

					dstData[indexDst++] = v << 16 | v << 8 | v;
				}
			}
		} else {
			throw new RuntimeException("Code more here");
		}
	}

	public static void grayToBuffered(ImageUInt8 src, BufferedImage dst) {
//        dst.markDirty();

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

	public static void grayToBuffered(ImageSInt16 src, BufferedImage dst) {
//        dst.markDirty();

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		short[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int)data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
	}

	public static void grayToBuffered(ImageFloat32 src, BufferedImage dst) {
//        dst.markDirty();

		final int width = dst.getWidth();
		final int height = dst.getHeight();

		float[] data = src.data;
		for (int y = 0; y < height; y++) {
			int indexSrc = src.startIndex + src.stride * y;

			for (int x = 0; x < width; x++) {
				int v = (int)data[indexSrc++];

				int argb = v << 16 | v << 8 | v;

				dst.setRGB(x, y, argb);
			}
		}
	}
}
