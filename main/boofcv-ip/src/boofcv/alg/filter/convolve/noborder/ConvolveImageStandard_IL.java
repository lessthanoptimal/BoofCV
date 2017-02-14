/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
 * NOTE: This code was automatically generated using GenerateConvolveImageStandard_IL.
 * </p>
 * 
 * @author Peter Abeles
 */
@SuppressWarnings({"ForLoopReplaceableByForEach"})
public class ConvolveImageStandard_IL {

	public static void horizontal( Kernel1D_F32 kernel ,
								   InterleavedF32 src, InterleavedF32 dst ) {
		final float[] dataSrc = src.data;
		final float[] dataDst = dst.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					float total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_F32 kernel,
								 InterleavedF32 src, InterleavedF32 dst )
	{
		final float[] dataSrc = src.data;
		final float[] dataDst = dst.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					float total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve( Kernel2D_F32 kernel , InterleavedF32 src , InterleavedF32 dst )
	{
		final float[] dataKernel = kernel.data;
		final float[] dataSrc = src.data;
		final float[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					float total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void horizontal( Kernel1D_F64 kernel ,
								   InterleavedF64 src, InterleavedF64 dst ) {
		final double[] dataSrc = src.data;
		final double[] dataDst = dst.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					double total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_F64 kernel,
								 InterleavedF64 src, InterleavedF64 dst )
	{
		final double[] dataSrc = src.data;
		final double[] dataDst = dst.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					double total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve( Kernel2D_F64 kernel , InterleavedF64 src , InterleavedF64 dst )
	{
		final double[] dataKernel = kernel.data;
		final double[] dataSrc = src.data;
		final double[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					double total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedU8 src, InterleavedI16 dst ) {
		final byte[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] & 0xFF) * dataKer[k];
					}
					dataDst[indexDst++] = (short)total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedU8 src, InterleavedI16 dst )
	{
		final byte[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (short)total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedU8 src , InterleavedI16 dst )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] & 0xFF)* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = (short)total;
				}
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedU8 src, InterleavedS32 dst ) {
		final byte[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] & 0xFF) * dataKer[k];
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedU8 src, InterleavedS32 dst )
	{
		final byte[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedU8 src , InterleavedS32 dst )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final int[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] & 0xFF)* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedU16 src, InterleavedI8 dst , int divisor )
	{
		final short[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] & 0xFFFF)* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedS16 src, InterleavedI16 dst ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = (short)total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedS16 src, InterleavedI16 dst )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (short)total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedS16 src , InterleavedI16 dst )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = (short)total;
				}
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedU8 src, InterleavedI8 dst , int divisor ) {
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] & 0xFF) * dataKer[k];
					}
					dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedU8 src, InterleavedI8 dst , int divisor )
	{
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] & 0xFF)* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedU8 src , InterleavedI8 dst , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] & 0xFF)* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = (byte)((total+halfDivisor)/divisor);
				}
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedS16 src, InterleavedI16 dst , int divisor ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedS16 src, InterleavedI16 dst , int divisor )
	{
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedS16 src , InterleavedI16 dst , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedS32 src, InterleavedI16 dst , int divisor )
	{
		final int[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = (short)((total+halfDivisor)/divisor);
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedS32 src, InterleavedS32 dst ) {
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedS32 src, InterleavedS32 dst )
	{
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = total;
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedS32 src , InterleavedS32 dst )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = total;
				}
			}
		}
	}

	public static void horizontal( Kernel1D_S32 kernel ,
								   InterleavedS32 src, InterleavedS32 dst , int divisor ) {
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int endJ = src.width - (kernelWidth - 1);

		for( int i = 0; i < src.height; i++ ) {
			int indexDst = dst.startIndex + i*dst.stride+offset*numBands;

			for (int j = 0; j < endJ; j++) {
				int indexSrcStart = src.startIndex + i*src.stride + j*numBands;
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;
					int total = 0;
					for (int k = 0; k < kernelWidth; k++, indexSrc += numBands) {
						total += (dataSrc[indexSrc] ) * dataKer[k];
					}
					dataDst[indexDst++] = ((total+halfDivisor)/divisor);
				}
			}
		}
	}

	public static void vertical( Kernel1D_S32 kernel,
								 InterleavedS32 src, InterleavedS32 dst , int divisor )
	{
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();

		final int yEnd = imgHeight-(kernelWidth-offset-1);

		for( int y = offset; y < yEnd; y++ ) {
			int indexDst = dst.startIndex+y*dst.stride;
			int indexSrcStart = src.startIndex+(y-offset)*src.stride;

			for (int x = 0; x < imgWidth; x++) {
				for (int band = 0; band < numBands; band++) {
					int indexSrc = indexSrcStart + band;

					int total = 0;
					for (int k = 0; k < kernelWidth; k++) {
						total += (dataSrc[indexSrc] )* dataKer[k];
						indexSrc += src.stride;
					}
					dataDst[indexDst++] = ((total+halfDivisor)/divisor);
				}
				indexSrcStart += numBands;
			}
		}
	}

	public static void convolve(Kernel2D_S32 kernel , InterleavedS32 src , InterleavedS32 dst , int divisor )
	{
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int numBands = src.getNumBands();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width-kernel.offset-1;

		for( int y = offsetL; y < height-offsetR; y++ ) {
			int indexDst = dst.startIndex + y*dst.stride+offsetL*numBands;
			for( int x = offsetL; x < width-offsetR; x++ ) {
				int indexSrcStart = src.startIndex + (y-offsetL)*src.stride + (x-offsetL)*numBands;

				for (int band = 0; band < numBands; band++) {
					int total = 0;
					int indexKer = 0;
					for( int ki = 0; ki < kernel.width; ki++ ) {
						int indexSrc = indexSrcStart+ki*src.stride + band;
						for( int kj = 0; kj <  kernel.width; kj++ ) {
							total += (dataSrc[indexSrc] )* dataKernel[indexKer++];
							indexSrc += numBands;
						}
					}
					dataDst[indexDst++] = ((total+halfDivisor)/divisor);
				}
			}
		}
	}

}
