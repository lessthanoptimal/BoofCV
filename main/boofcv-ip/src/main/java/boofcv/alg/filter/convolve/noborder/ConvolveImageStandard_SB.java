/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.concurrency.IWorkArrays;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;

import javax.annotation.Generated;
//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;


/**
 * <p>
 * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.
 * </p>
 * 
 * <p>
 * NOTE: This code was automatically generated using GenerateConvolveImageStandard_SB.
 * </p>
 * 
 * @author Peter Abeles
 */
@Generated({"boofcv.alg.filter.convolve.noborder.GenerateConvolveImageStandard_SB"})
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ConvolveImageStandard_SB {

	public static void horizontal( Kernel1D_F32 kernel ,
								  GrayF32 image, GrayF32 dest ) {
		final float[] dataSrc = image.data;
		final float[] dataDst = dest.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_F32 kernel , GrayF32 src , GrayF32 dest )
	{
		final float[] dataKernel = kernel.data;
		final float[] dataSrc = src.data;
		final float[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_F64 kernel ,
								  GrayF64 image, GrayF64 dest ) {
		final double[] dataSrc = image.data;
		final double[] dataDst = dest.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F64 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_F64 kernel , GrayF64 src , GrayF64 dest )
	{
		final double[] dataKernel = kernel.data;
		final double[] dataSrc = src.data;
		final double[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayU8 image, GrayI16 dest ) {
		final byte[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayU8 src , GrayI16 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayU8 image, GrayS32 dest ) {
		final byte[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayU8 src , GrayS32 dest )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayS16 image, GrayI16 dest ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayS16 src , GrayI16 dest )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayU8 image, GrayI8 dest , int divisor ) {
		final byte[] dataSrc = image.data;
		final byte[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayU8 src , GrayI8 dest , int divisor , IWorkArrays work)
	{
		if( work == null ) {
			work = new IWorkArrays(src.width);
		} else {
			work.reset(src.width);
		}
		final IWorkArrays _work = work;
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height-offsetR,kernel.width, (y0,y1) -> {
		final int y0 = offsetL, y1 = height-offsetR;
		int totalRow[] = _work.pop();
		for( int y = y0; y < y1; y++ ) {
			int indexSrcRow = src.startIndex+(y-offsetL)*src.stride-offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)* dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < kernel.width; i++ ) {
				indexSrcRow = src.startIndex+(y+i-offsetL)*src.stride-offsetL;
				int indexKer = i*kernel.width;

				for( int x = offsetL; x < width-offsetR; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] & 0xFF)* dataKernel[indexKer+k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				dataDst[indexDst++] = (byte)((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		_work.recycle(totalRow);
		//CONCURRENT_INLINE });
	}
	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayS16 image, GrayI16 dest , int divisor ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayS16 src , GrayI16 dest , int divisor , IWorkArrays work)
	{
		if( work == null ) {
			work = new IWorkArrays(src.width);
		} else {
			work.reset(src.width);
		}
		final IWorkArrays _work = work;
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height-offsetR,kernel.width, (y0,y1) -> {
		final int y0 = offsetL, y1 = height-offsetR;
		int totalRow[] = _work.pop();
		for( int y = y0; y < y1; y++ ) {
			int indexSrcRow = src.startIndex+(y-offsetL)*src.stride-offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] )* dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < kernel.width; i++ ) {
				indexSrcRow = src.startIndex+(y+i-offsetL)*src.stride-offsetL;
				int indexKer = i*kernel.width;

				for( int x = offsetL; x < width-offsetR; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] )* dataKernel[indexKer+k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				dataDst[indexDst++] = (short)((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		_work.recycle(totalRow);
		//CONCURRENT_INLINE });
	}
	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayU16 image, GrayI16 dest ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFFFF) * dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayU16 image, GrayI16 dest )
	{
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dest.getWidth();
		final int imgHeight = dest.getHeight();
		final int yEnd = imgHeight-(kernelWidth-offset-1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayU16 src , GrayI16 dest )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int total = 0;
				int indexKer = 0;
				for( int ki = 0; ki < kernel.width; ki++ ) {
					int indexSrc = src.startIndex + (y+ki-offsetL)*src.stride + x-offsetL;
					for( int kj = 0; kj <  kernel.width; kj++ ) {
						total += (dataSrc[indexSrc+kj] & 0xFFFF )* dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayU16 image, GrayI16 dest , int divisor ) {
		final short[] dataSrc = image.data;
		final short[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
		for( int i = 0; i < image.height; i++ ) {
			int indexDst = dest.startIndex + i*dest.stride+offset;
			int j = image.startIndex + i*image.stride;
			final int jEnd = j+width-(kernelWidth-1);

			for( ; j < jEnd; j++ ) {
				int total = 0;
				int indexSrc = j;
				for( int k = 0; k < kernelWidth; k++ ) {
					total += (dataSrc[indexSrc++] & 0xFFFF) * dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayU16 image, GrayI16 dest , int divisor )
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
				dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayU16 src , GrayI16 dest , int divisor , IWorkArrays work)
	{
		if( work == null ) {
			work = new IWorkArrays(src.width);
		} else {
			work.reset(src.width);
		}
		final IWorkArrays _work = work;
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height-offsetR,kernel.width, (y0,y1) -> {
		final int y0 = offsetL, y1 = height-offsetR;
		int totalRow[] = _work.pop();
		for( int y = y0; y < y1; y++ ) {
			int indexSrcRow = src.startIndex+(y-offsetL)*src.stride-offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)* dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < kernel.width; i++ ) {
				indexSrcRow = src.startIndex+(y+i-offsetL)*src.stride-offsetL;
				int indexKer = i*kernel.width;

				for( int x = offsetL; x < width-offsetR; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] & 0xFFFF)* dataKernel[indexKer+k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				dataDst[indexDst++] = (short)((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		_work.recycle(totalRow);
		//CONCURRENT_INLINE });
	}
	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayS32 image, GrayS32 dest ) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayS32 src , GrayS32 dest )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height-offsetR, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								  GrayS32 image, GrayS32 dest , int divisor ) {
		final int[] dataSrc = image.data;
		final int[] dataDst = dest.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = image.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, image.height, i -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
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

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
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
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel , GrayS32 src , GrayS32 dest , int divisor , IWorkArrays work)
	{
		if( work == null ) {
			work = new IWorkArrays(src.width);
		} else {
			work.reset(src.width);
		}
		final IWorkArrays _work = work;
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height-offsetR,kernel.width, (y0,y1) -> {
		final int y0 = offsetL, y1 = height-offsetR;
		int totalRow[] = _work.pop();
		for( int y = y0; y < y1; y++ ) {
			int indexSrcRow = src.startIndex+(y-offsetL)*src.stride-offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] )* dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < kernel.width; i++ ) {
				indexSrcRow = src.startIndex+(y+i-offsetL)*src.stride-offsetL;
				int indexKer = i*kernel.width;

				for( int x = offsetL; x < width-offsetR; x++ ) {
					int indexSrc = indexSrcRow+x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] )* dataKernel[indexKer+k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride+offsetL;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				dataDst[indexDst++] = ((totalRow[x]+halfDivisor)/ divisor);
			}
		}
		_work.recycle(totalRow);
		//CONCURRENT_INLINE });
	}
}
