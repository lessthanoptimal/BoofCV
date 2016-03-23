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
package boofcv.alg.filter.convolve.noborder;

import boofcv.struct.convolve.*;
import boofcv.struct.image.*;


/**
 * <p>
 * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.
 * </p>
 * 
 * <p>
 * NOTE: This code was automatically generated using {@link boofcv.alg.filter.convolve.noborder.GenerateConvolveImageStandard}.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveImageStandard {

	public static void horizontal(Kernel1D_F32 kernel ,
								  GrayF32 image, GrayF32 dest ) {
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				float total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical(Kernel1D_F32 kernel,
								GrayF32 image, GrayF32 dest )
	{
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
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

	public static void convolve(Kernel2D_F32 kernel , GrayF32 src , GrayF32 dest )
	{
		final float[] dataKernel = kernel.data;
		final float[] dataSrc = src.data;
		final float[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				float total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal(Kernel1D_F64 kernel ,
								  GrayF64 image, GrayF64 dest ) {
		final double[] dataSrc = image.data;
		final double[] dataDst = dest.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				double total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical(Kernel1D_F64 kernel,
								GrayF64 image, GrayF64 dest )
	{
		final double[] dataSrc = image.data;
		final double[] dataDst = dest.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				double total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void convolve(Kernel2D_F64 kernel , GrayF64 src , GrayF64 dest )
	{
		final double[] dataKernel = kernel.data;
		final double[] dataSrc = src.data;
		final double[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				double total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayU8 image, GrayI16 dest ) {
		final byte[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 image, GrayI16 dest )
	{
		final byte[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
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

	public static void convolve(Kernel2D_I32 kernel , GrayU8 src , GrayI16 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayU8 image, GrayS32 dest ) {
		final byte[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 image, GrayS32 dest )
	{
		final byte[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
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

	public static void convolve(Kernel2D_I32 kernel , GrayU8 src , GrayS32 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayU16 image, GrayI8 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] & 0xFFFF)* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayS16 image, GrayI16 dest ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayS16 image, GrayI16 dest )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
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

	public static void convolve(Kernel2D_I32 kernel , GrayS16 src , GrayI16 dest )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayU8 image, GrayI8 dest , int divisor ) {
		final byte[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFF) * dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayU8 image, GrayI8 dest , int divisor )
	{
		final byte[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void convolve(Kernel2D_I32 kernel , GrayU8 src , GrayI8 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayS16 image, GrayI16 dest , int divisor ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayS16 image, GrayI16 dest , int divisor )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void convolve(Kernel2D_I32 kernel , GrayS16 src , GrayI16 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayS32 image, GrayI16 dest , int divisor )
	{
		final int[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayS32 image, GrayS32 dest ) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayS32 image, GrayS32 dest )
	{
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
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

	public static void convolve(Kernel2D_I32 kernel , GrayS32 src , GrayS32 dest )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
	}

	public static void horizontal(Kernel1D_I32 kernel ,
								  GrayS32 image, GrayS32 dest , int divisor ) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] ) * dataKer[k];
				}
				dataDst[indexDst++] = ((total+halfDivisor)/divisor);
			}
		}
	}

	public static void vertical(Kernel1D_I32 kernel,
								GrayS32 image, GrayS32 dest , int divisor )
	{
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dest.startIndex+y*dest.stride;
			int i = image.startIndex + (y-offset)*image.stride;
			final int iEnd = i+imgWidth;

			for( ; i < iEnd; i++ ) {
				int total = 0;
				int indexSrc = i;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc] )* dataKer[k];
					indexSrc += image.stride;
				}
				dataDst[indexDst++] = ((total+halfDivisor)/divisor);
			}
		}
	}

	public static void convolve(Kernel2D_I32 kernel , GrayS32 src , GrayS32 dest , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj]  )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = ((total+halfDivisor)/divisor);
			}
		}
	}

}
