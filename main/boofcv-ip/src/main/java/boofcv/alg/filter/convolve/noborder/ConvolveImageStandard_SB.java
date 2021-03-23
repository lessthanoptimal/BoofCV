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

package boofcv.alg.filter.convolve.noborder;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;
import java.util.Arrays;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;


/**
 * <p>
 * Standard algorithms with no fancy optimization for convolving 1D and 2D kernels across an image.
 * </p>
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvolveImageStandard_SB</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.noborder.GenerateConvolveImageStandard_SB")
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ConvolveImageStandard_SB {

	public static void horizontal( Kernel1D_F32 kernel, GrayF32 src, GrayF32 dst ) {
		final float[] dataSrc = src.data;
		final float[] dataDst = dst.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				float total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F32 kernel, GrayF32 src, GrayF32 dst ) {
		final float[] dataSrc = src.data;
		final float[] dataDst = dst.data;
		final float[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (float)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				float kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += ((dataSrc[i])*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_F32 kernel, GrayF32 src, GrayF32 dest ) {
		final float[] dataKernel = kernel.data;
		final float[] dataSrc = src.data;
		final float[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				float total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj])*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_F64 kernel, GrayF64 src, GrayF64 dst ) {
		final double[] dataSrc = src.data;
		final double[] dataDst = dst.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				double total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F64 kernel, GrayF64 src, GrayF64 dst ) {
		final double[] dataSrc = src.data;
		final double[] dataDst = dst.data;
		final double[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (double)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				double kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += ((dataSrc[i])*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_F64 kernel, GrayF64 src, GrayF64 dest ) {
		final double[] dataKernel = kernel.data;
		final double[] dataSrc = src.data;
		final double[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				double total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj])*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 src, GrayI16 dst ) {
		final byte[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 src, GrayI16 dst ) {
		final byte[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (short)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				int kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += (short)((dataSrc[i] & 0xFF)*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 src, GrayI16 dest ) {
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj] & 0xFF)*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 src, GrayS32 dst ) {
		final byte[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 src, GrayS32 dst ) {
		final byte[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (int)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				int kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += ((dataSrc[i] & 0xFF)*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 src, GrayS32 dest ) {
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj] & 0xFF)*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayU16 src, GrayI8 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++] & 0xFFFF)*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (byte)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (short)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				int kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += (short)((dataSrc[i])*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest ) {
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj])*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 src, GrayI8 dst , int divisor ) {
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayU8 src, GrayI8 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++] & 0xFF)*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (byte)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 src, GrayI8 dest, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataKernel = kernel.data;
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height - offsetR,kernel.width, workspaces, (work,y0,y1) -> {
		final int y0 = offsetL, y1 = height - offsetR;
		int totalRow[] = BoofMiscOps.checkDeclare(work, src.width, false);
		for (int y = y0; y < y1; y++) {
			int indexSrcRow = src.startIndex + (y - offsetL)*src.stride - offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < kernel.width; i++) {
				indexSrcRow = src.startIndex + (y + i - offsetL)*src.stride - offsetL;
				int indexKer = i*kernel.width;

				for (int x = offsetL; x < width - offsetR; x++) {
					int indexSrc = indexSrcRow + x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] & 0xFF)*dataKernel[indexKer + k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				dataDst[indexDst++] = (byte)((totalRow[x] + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_INLINE });
	}
	public static void horizontal( Kernel1D_S32 kernel, GrayS16 src, GrayI16 dst , int divisor ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayS16 src, GrayI16 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++])*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (short)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS16 src, GrayI16 dest, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height - offsetR,kernel.width, workspaces, (work,y0,y1) -> {
		final int y0 = offsetL, y1 = height - offsetR;
		int totalRow[] = BoofMiscOps.checkDeclare(work, src.width, false);
		for (int y = y0; y < y1; y++) {
			int indexSrcRow = src.startIndex + (y - offsetL)*src.stride - offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++])*dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < kernel.width; i++) {
				indexSrcRow = src.startIndex + (y + i - offsetL)*src.stride - offsetL;
				int indexKer = i*kernel.width;

				for (int x = offsetL; x < width - offsetR; x++) {
					int indexSrc = indexSrcRow + x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++])*dataKernel[indexKer + k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				dataDst[indexDst++] = (short)((totalRow[x] + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_INLINE });
	}
	public static void horizontal( Kernel1D_S32 kernel, GrayU16 src, GrayI16 dst ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU16 src, GrayI16 dst ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (short)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				int kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += (short)((dataSrc[i] & 0xFFFF)*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU16 src, GrayI16 dest ) {
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj] & 0xFFFF)*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = (short)total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU16 src, GrayI16 dst , int divisor ) {
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayU16 src, GrayI16 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++] & 0xFFFF)*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (short)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU16 src, GrayI16 dest, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataKernel = kernel.data;
		final short[] dataSrc = src.data;
		final short[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height - offsetR,kernel.width, workspaces, (work,y0,y1) -> {
		final int y0 = offsetL, y1 = height - offsetR;
		int totalRow[] = BoofMiscOps.checkDeclare(work, src.width, false);
		for (int y = y0; y < y1; y++) {
			int indexSrcRow = src.startIndex + (y - offsetL)*src.stride - offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)*dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < kernel.width; i++) {
				indexSrcRow = src.startIndex + (y + i - offsetL)*src.stride - offsetL;
				int indexKer = i*kernel.width;

				for (int x = offsetL; x < width - offsetR; x++) {
					int indexSrc = indexSrcRow + x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++] & 0xFFFF)*dataKernel[indexKer + k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				dataDst[indexDst++] = (short)((totalRow[x] + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_INLINE });
	}
	public static void vertical( Kernel1D_S32 kernel,
								 GrayS32 src, GrayI16 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataSrc = src.data;
		final short[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++])*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (short)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS32 src, GrayS32 dst ) {
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS32 src, GrayS32 dst ) {
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offset, yEnd, y -> {
		for (int y = offset; y < yEnd; y++) {
			final int indexDstStart = dst.startIndex + y*dst.stride;
			Arrays.fill(dataDst, indexDstStart, indexDstStart + imgWidth, (int)0);

			for (int k = 0; k < kernelWidth; k++) {
				final int iStart = src.startIndex + (y - offset + k)*src.stride;
				final int iEnd = iStart + imgWidth;
				int indexDst = indexDstStart;
				int kernelValue = dataKer[k];
				for (int i = iStart; i < iEnd; i++) {
					dataDst[indexDst++] += ((dataSrc[i])*kernelValue);
				}
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS32 src, GrayS32 dest ) {
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetL, height - offsetR, y -> {
		for (int y = offsetL; y < height - offsetR; y++) {
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int indexKer = 0;
				for (int ki = 0; ki < kernel.width; ki++) {
					int indexSrc = src.startIndex + (y + ki - offsetL)*src.stride + x - offsetL;
					for (int kj = 0; kj < kernel.width; kj++) {
						total += (dataSrc[indexSrc + kj])*dataKernel[indexKer++];
					}
				}
				dataDst[indexDst++] = total;
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS32 src, GrayS32 dst , int divisor ) {
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;

		final int width = src.getWidth();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, src.height, i -> {
		for (int i = 0; i < src.height; i++) {
			int indexDst = dst.startIndex + i*dst.stride + offset;
			int j = src.startIndex + i*src.stride;
			final int jEnd = j + width - (kernelWidth - 1);

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j;
				for (int k = 0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDst++] = ((total + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel,
								 GrayS32 src, GrayS32 dst, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataSrc = src.data;
		final int[] dataDst = dst.data;
		final int[] dataKer = kernel.data;

		final int offset = kernel.getOffset();
		final int kernelWidth = kernel.getWidth();
		final int halfDivisor = divisor/2;
		final double divisionHack = 1.0/divisor; // WTF integer division is slower than converting to a float??

		final int imgWidth = dst.getWidth();
		final int imgHeight = dst.getHeight();
		final int yEnd = imgHeight - (kernelWidth - offset - 1);

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offset, yEnd, workspaces, (work, y0, y1)->{
		final int y0 = offset, y1 = yEnd;
		int[] totalRow = BoofMiscOps.checkDeclare(work, imgWidth, true);
		for (int y = y0; y < y1; y++) {
			for (int k = 0; k < kernelWidth; k++) {
				final int kernelValue = dataKer[k];
				int indexSrc = src.startIndex + (y - offset + k)*src.stride;
				for (int i = 0; i < imgWidth; i++) {
					totalRow[i] += ((dataSrc[indexSrc++])*kernelValue);
				}
			}

			int indexDst = dst.startIndex + y*dst.stride;
			for (int i = 0; i < imgWidth; i++) {
				dataDst[indexDst++] = (int)((totalRow[i] + halfDivisor)*divisionHack);
			}
			Arrays.fill(totalRow,0,imgWidth,0);
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS32 src, GrayS32 dest, int divisor, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataKernel = kernel.data;
		final int[] dataSrc = src.data;
		final int[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();
		final int halfDivisor = divisor/2;

		int offsetL = kernel.offset;
		int offsetR = kernel.width - kernel.offset - 1;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(offsetL, height - offsetR,kernel.width, workspaces, (work,y0,y1) -> {
		final int y0 = offsetL, y1 = height - offsetR;
		int totalRow[] = BoofMiscOps.checkDeclare(work, src.width, false);
		for (int y = y0; y < y1; y++) {
			int indexSrcRow = src.startIndex + (y - offsetL)*src.stride - offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				int indexSrc = indexSrcRow + x;

				int total = 0;
				for (int k = 0; k < kernel.width; k++) {
					total += (dataSrc[indexSrc++])*dataKernel[k];
				}
				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < kernel.width; i++) {
				indexSrcRow = src.startIndex + (y + i - offsetL)*src.stride - offsetL;
				int indexKer = i*kernel.width;

				for (int x = offsetL; x < width - offsetR; x++) {
					int indexSrc = indexSrcRow + x;

					int total = 0;
					for (int k = 0; k < kernel.width; k++) {
						total += (dataSrc[indexSrc++])*dataKernel[indexKer + k];
					}

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y*dest.stride + offsetL;
			for (int x = offsetL; x < width - offsetR; x++) {
				dataDst[indexDst++] = ((totalRow[x] + halfDivisor)/divisor);
			}
		}
		//CONCURRENT_INLINE });
	}
}
