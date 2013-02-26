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

package boofcv.alg.filter.convolve.noborder;

import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel1D_I32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.*;


/**
 * <p>
 * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.
 * </p>
 * 
 * <p>
 * NOTE: This code was automatically generated using {@link GenerateConvolveImageStandard}.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveImageStandard {

	public static void horizontal( Kernel1D_F32 kernel ,
								  ImageFloat32 image, ImageFloat32 dest,
								  boolean includeBorder) {
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				float total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical( Kernel1D_F32 kernel,
								 ImageFloat32 image, ImageFloat32 dest,
								 boolean includeBorder)
	{
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				float total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void convolve( Kernel2D_F32 kernel , ImageFloat32 src , ImageFloat32 dest )
	{
		final float[] dataKernel = kernel.data;
		final float[] dataSrc = src.data;
		final float[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				float total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageUInt8 image, ImageInt16 dest,
								  boolean includeBorder) {
		final byte[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageUInt8 image, ImageInt16 dest,
								 boolean includeBorder)
	{
		final byte[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageUInt8 src , ImageInt16 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageUInt8 image, ImageSInt32 dest,
								  boolean includeBorder) {
		final byte[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageUInt8 image, ImageSInt32 dest,
								 boolean includeBorder)
	{
		final byte[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageUInt8 src , ImageSInt32 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageSInt16 image, ImageInt16 dest,
								  boolean includeBorder) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageSInt16 image, ImageInt16 dest,
								 boolean includeBorder)
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageSInt16 src , ImageInt16 dest )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageUInt8 image, ImageInt8 dest, int divisor,
								  boolean includeBorder) {
		final byte[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (byte)(total/divisor);
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageUInt8 image, ImageInt8 dest, int divisor,
								 boolean includeBorder)
	{
		final byte[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (byte)(total/divisor);
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageUInt8 src , ImageInt8 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (byte)(total/divisor);
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageSInt16 image, ImageInt16 dest, int divisor,
								  boolean includeBorder) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = (short)(total/divisor);
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageSInt16 image, ImageInt16 dest, int divisor,
								 boolean includeBorder)
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (short)(total/divisor);
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageSInt16 src , ImageInt16 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)(total/divisor);
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageSInt32 image, ImageSInt32 dest,
								  boolean includeBorder) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageSInt32 image, ImageSInt32 dest,
								 boolean includeBorder)
	{
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageSInt32 src , ImageSInt32 dest )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal( Kernel1D_I32 kernel ,
								  ImageSInt32 image, ImageSInt32 dest, int divisor,
								  boolean includeBorder) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int yBorder = includeBorder ? 0 : radius;

		final int width = image.getWidth();
		final int height = image.getHeight()-yBorder;

		for( int i = yBorder; i < height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+radius;
			int j = image.startIndex + i*image.stride - radius;
			final int jEnd = j+width-radius;

			for( j += radius; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = (total/divisor);
			}
		}
	}

	public static void vertical( Kernel1D_I32 kernel,
								 ImageSInt32 image, ImageSInt32 dest, int divisor,
								 boolean includeBorder)
	{
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int radius = kernel.getRadius();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-radius;

		final int xBorder = includeBorder ? 0 : radius;

		for( int y = radius; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride+xBorder;
			int i = image.startIndex + (y-radius)*image.stride;
			final int iEnd = i+imgWidth-xBorder;

			for( i += xBorder; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (total/divisor);
			}
		}
	}

	public static void convolve( Kernel2D_I32 kernel , ImageSInt32 src , ImageSInt32 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int kernelRadius = kernel.width/2;

		for( int y = kernelRadius; y < height-kernelRadius; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+kernelRadius;
			for( int x = kernelRadius; x < width-kernelRadius; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = -kernelRadius; ki <= kernelRadius; ki++ ) {
					int indexSrc = src.startIndex+(y+ki)*src.stride+ x;
					for( int kj = -kernelRadius; kj <= kernelRadius; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (total/divisor);
			}
		}
	}

}
