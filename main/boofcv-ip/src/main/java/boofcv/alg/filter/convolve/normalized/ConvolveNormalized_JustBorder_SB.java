/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.convolve.normalized;

import boofcv.misc.BoofMiscOps;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;
import pabeles.concurrency.GrowArray;

import javax.annotation.Generated;

//CONCURRENT_INLINE import boofcv.concurrency.BoofConcurrency;

/**
 * <p>
 * Convolves a 1D kernel in the horizontal or vertical direction across an image's border only, while re-normalizing the
 * kernel sum to one. The kernel MUST be smaller than the image.
 * </p>
 * 
 *
 * <p>DO NOT MODIFY. Automatically generated code created by GenerateConvolveNormalized_JustBorder_SB</p>
 *
 * @author Peter Abeles
 */
@Generated("boofcv.alg.filter.convolve.normalized.GenerateConvolveNormalized_JustBorder_SB")
@SuppressWarnings({"ForLoopReplaceableByForEach","Duplicates"})
public class ConvolveNormalized_JustBorder_SB {

	public static void horizontal( Kernel1D_F32 kernel, GrayF32 input, GrayF32 output ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;
		final float[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			float weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				float total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDest++] = (total/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				float total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F32 kernel, GrayF32 input, GrayF32 output, @Nullable GrowArray<DogArray_F32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F32::new);
		final DogArray_F32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;
		final float[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		float[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			float w = dataKer[kStart];
			float weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (totals[x]/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			float w = dataKer[0];
			float weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (totals[x]/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_F32 kernel, GrayF32 input, GrayF32 output ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;
		final float[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				float total = 0;
				float weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						float w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (total/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				float total = 0;
				float weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						float w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				float total = 0;
				float weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						float w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				float total = 0;
				float weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						float w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_F64 kernel, GrayF64 input, GrayF64 output ) {
		final double[] dataSrc = input.data;
		final double[] dataDst = output.data;
		final double[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			double weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				double total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDest++] = (total/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				double total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_F64 kernel, GrayF64 input, GrayF64 output, @Nullable GrowArray<DogArray_F64> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_F64::new);
		final DogArray_F64 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final double[] dataSrc = input.data;
		final double[] dataDst = output.data;
		final double[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		double[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			double w = dataKer[kStart];
			double weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (totals[x]/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			double w = dataKer[0];
			double weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (totals[x]/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_F64 kernel, GrayF64 input, GrayF64 output ) {
		final double[] dataSrc = input.data;
		final double[] dataDst = output.data;
		final double[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				double total = 0;
				double weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						double w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (total/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				double total = 0;
				double weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						double w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				double total = 0;
				double weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						double w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				double total = 0;
				double weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						double w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (total/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU8 input, GrayI8 output ) {
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			int weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKer[k];
				}
				dataDst[indexDest++] = (byte)((total + weight/2)/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++] & 0xFF)*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = (byte)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU8 input, GrayI8 output, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			int w = dataKer[kStart];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++] & 0xFF)*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++] & 0xFF)*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (byte)((totals[x] + weight/2)/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			int w = dataKer[0];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++] & 0xFF)*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++] & 0xFF)*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (byte)((totals[x] + weight/2)/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU8 input, GrayI8 output ) {
		final byte[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFF)*w;
					}
				}

				dataDst[indexDst++] = (byte)((total + weight/2)/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFF)*w;
					}
				}

				dataDst[indexDst++] = (byte)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				int total = 0;
				int weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFF)*w;
					}
				}
				dataDst[indexDst++] = (byte)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFF)*w;
					}
				}
				dataDst[indexDst++] = (byte)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			int weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDest++] = (short)((total + weight/2)/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS16 input, GrayI16 output, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			int w = dataKer[kStart];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (short)((totals[x] + weight/2)/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			int w = dataKer[0];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (short)((totals[x] + weight/2)/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS16 input, GrayI16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				int total = 0;
				int weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayU16 input, GrayI16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			int weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)*dataKer[k];
				}
				dataDst[indexDest++] = (short)((total + weight/2)/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++] & 0xFFFF)*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayU16 input, GrayI16 output, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			int w = dataKer[kStart];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++] & 0xFFFF)*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++] & 0xFFFF)*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (short)((totals[x] + weight/2)/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			int w = dataKer[0];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++] & 0xFFFF)*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++] & 0xFFFF)*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = (short)((totals[x] + weight/2)/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayU16 input, GrayI16 output ) {
		final short[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFFFF)*w;
					}
				}

				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFFFF)*w;
					}
				}

				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				int total = 0;
				int weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFFFF)*w;
					}
				}
				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j] & 0xFFFF)*w;
					}
				}
				dataDst[indexDst++] = (short)((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void horizontal( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output ) {
		final int[] dataSrc = input.data;
		final int[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, input.height, y -> {
		for (int y = 0; y < height; y++) {
			int indexDest = output.startIndex + y*output.stride;
			int j = input.startIndex + y*input.stride;
			final int jStart = j;
			int jEnd = j + offsetL;


			int weight = 0;
			for (int k = kernelWidth - offsetR; k < kernelWidth; k++) {
				weight += dataKer[k];
			}

			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = jStart;
				int kernelIdx0 = kernelWidth - (offsetR + 1 + j - jStart);
				weight += dataKer[kernelIdx0];
				for (int k = kernelIdx0; k < kernelWidth; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				dataDst[indexDest++] = ((total + weight/2)/weight);
			}

			j += width - (offsetL + offsetR);
			indexDest += width - (offsetL + offsetR);

			{
				weight = 0;
				int kEnd = jStart + width - (j - offsetL) + 1;
				for (int k = 0; k < kEnd; k++) {
					weight += dataKer[k];
				}
			}

			jEnd = jStart + width;
			for (; j < jEnd; j++) {
				int total = 0;
				int indexSrc = j - offsetL;
				final int kEnd = jEnd - indexSrc;

				for (int k = 0; k < kEnd; k++) {
					total += (dataSrc[indexSrc++])*dataKer[k];
				}
				weight -= dataKer[kEnd];
				dataDst[indexDest++] = ((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical( Kernel1D_S32 kernel, GrayS32 input, GrayS32 output, @Nullable GrowArray<DogArray_I32> workspaces ) {
		workspaces = BoofMiscOps.checkDeclare(workspaces, DogArray_I32::new);
		final DogArray_I32 work = workspaces.grow(); //CONCURRENT_REMOVE_LINE
		final int[] dataSrc = input.data;
		final int[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - offsetR;

		//CONCURRENT_BELOW BoofConcurrency.loopBlocks(0, input.width, 20, workspaces, (work, x0, x1) -> {
		final int x0 = 0, x1 = input.width;
		final int countX = x1 - x0;
		int[] totals = BoofMiscOps.checkDeclare(work, x1 - x0, false);
		// NOTE: for floating point images this workspace isn't needed. Output image can be used instead

		for (int y = 0; y < offsetL; y++) {
			int indexInRow = input.startIndex + x0;
			final int kStart = offsetL - y;
			int w = dataKer[kStart];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = kStart + 1; k < kernelWidth; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = ((totals[x] + weight/2)/weight);
			}
		}

		for (int y = yEnd; y < imgHeight; y++) {
			int indexInRow = input.startIndex + x0 + (y - offsetL)*input.stride;
			final int kEnd = imgHeight - (y - offsetL);
			int w = dataKer[0];
			int weight = w;
			for (int x = 0, indexIn = indexInRow; x < countX; x++) {
				totals[x] = (dataSrc[indexIn++])*w;
			}
			for (int k = 1; k < kEnd; k++) {
				indexInRow += input.stride;
				w = dataKer[k];
				weight += w;
				for (int x = 0, indexIn = indexInRow; x < countX; x++) {
					totals[x] += (dataSrc[indexIn++])*w;
				}
			}
			int indexOut = output.startIndex + x0 + y*output.stride;
			for (int x = 0; x < countX; x++) {
				dataDst[indexOut++] = ((totals[x] + weight/2)/weight);
			}
		}
		//CONCURRENT_INLINE });
	}

	public static void convolve( Kernel2D_S32 kernel, GrayS32 input, GrayS32 output ) {
		final int[] dataSrc = input.data;
		final int[] dataDst = output.data;
		final int[] dataKer = kernel.data;

		final int kernelWidth = kernel.getWidth();
		final int offsetL = kernel.getOffset();
		final int offsetR = kernelWidth - offsetL - 1;

		final int width = input.getWidth();
		final int height = input.getHeight();

		// convolve across the left and right borders
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, height, y -> {
		for (int y = 0; y < height; y++) {

			int minI = y >= offsetL ? -offsetL : -y;
			int maxI = y < height - offsetR ? offsetR : height - y - 1;

			int indexDst = output.startIndex + y*output.stride;

			for (int x = 0; x < offsetL; x++) {

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -x; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = ((total + weight/2)/weight);
			}

			indexDst = output.startIndex + y* output.stride + width - offsetR;
			for (int x = width - offsetR; x < width; x++) {

				int maxJ = width - x - 1;

				int total = 0;
				int weight = 0;

				for (int i = minI; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= maxJ; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}

				dataDst[indexDst++] = ((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the top border while avoiding convolving the corners again
		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetL, y -> {
		for (int y = 0; y < offsetL; y++) {

			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {

				int total = 0;
				int weight = 0;

				for (int i = -y; i <= offsetR; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = ((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });

		// convolve across the bottom border
		//CONCURRENT_BELOW BoofConcurrency.loopFor(height - offsetR, height, y -> {
		for (int y = height - offsetR; y < height; y++) {

			int maxI = height - y - 1;
			int indexDst = output.startIndex + y*output.stride + offsetL;

			for (int x = offsetL; x < width - offsetR; x++) {
				int total = 0;
				int weight = 0;

				for (int i = -offsetL; i <= maxI; i++) {
					int indexSrc = input.startIndex + (y + i)*input.stride + x;
					int indexKer = (i + offsetL)*kernelWidth;

					for (int j = -offsetL; j <= offsetR; j++) {
						int w = dataKer[indexKer + j + offsetL];
						weight += w;
						total += (dataSrc[indexSrc + j])*w;
					}
				}
				dataDst[indexDst++] = ((total + weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(Kernel1D_S32 kernelX, Kernel1D_S32 kernelY,
								GrayU16 input, GrayI8 output ) {
		final short[] dataSrc = input.data;
		final byte[] dataDst = output.data;
		final int[] dataKer = kernelY.data;

		final int offsetY = kernelY.getOffset();
		final int kernelWidthY = kernelY.getWidth();

		final int offsetX = kernelX.getOffset();
		final int kernelWidthX = kernelX.getWidth();
		final int offsetX1 = kernelWidthX-offsetX-1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - (kernelWidthY - offsetY - 1);

		int tmpSumX = 0;
		for (int k = offsetX; k < kernelWidthX; k++) {
			tmpSumX += kernelX.data[k];
		}
		final int startWeightX = tmpSumX;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetY, y -> {
		for (int y = 0; y < offsetY; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;
			final int iEnd = i + imgWidth;

			int kStart = offsetY - y;

			int weightY = 0;
			for (int k = kStart; k < kernelWidthY; k++) {
				weightY += dataKer[k];
			}
			int weightX = startWeightX;

			for ( int x = 0; i < iEnd; i++, x++ ) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - y*input.stride;
				for (int k = kStart; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]& 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total+weight/2)/weight);
				if( x < offsetX ) {
					weightX += kernelX.data[offsetX-x-1];
				} else if( x >= input.width-(kernelWidthX-offsetX) ) {
					weightX -= kernelX.data[input.width-x+offsetX-1];
				}
			}
		}
		//CONCURRENT_ABOVE });

		//CONCURRENT_BELOW BoofConcurrency.loopFor(yEnd, imgHeight, y -> {
		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;
			final int iEnd = i + imgWidth;

			int kEnd = imgHeight - (y - offsetY);

			int weightY = 0;
			for (int k = 0; k < kEnd; k++) {
				weightY += dataKer[k];
			}
			int weightX = startWeightX;

			for (int x = 0; i < iEnd; i++, x++) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kEnd; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]& 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total+weight/2)/weight);
				if( x < offsetX ) {
					weightX += kernelX.data[offsetX - x - 1];
				} else if (x >= input.width - (kernelWidthX - offsetX)) {
					weightX -= kernelX.data[input.width-x+offsetX-1];
				}
			}
		}
		//CONCURRENT_ABOVE });

		// left and right border
		final int weightY = kernelY.computeSum();
		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetY, yEnd, y -> {
		for (int y = offsetY; y < yEnd; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;

			// left side
			int iEnd = i + offsetY;
			int weightX = startWeightX;
			for (int x = 0; i < iEnd; i++, x++) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]& 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total+weight/2)/weight);
				weightX += kernelX.data[offsetX - x - 1];
			}

			// right side
			int startX = input.width - offsetX1;
			indexDst = output.startIndex + y*output.stride + startX;
			i = input.startIndex + y*input.stride + startX;
			iEnd = input.startIndex + y*input.stride + input.width;
			for (int x = startX; i < iEnd; i++, x++) {
				weightX -= kernelX.data[input.width - x + offsetX];
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc]& 0xFFFF)*dataKer[k];
				}
				dataDst[indexDst++] = (byte)((total+weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

	public static void vertical(Kernel1D_S32 kernelX, Kernel1D_S32 kernelY,
								GrayS32 input, GrayI16 output ) {
		final int[] dataSrc = input.data;
		final short[] dataDst = output.data;
		final int[] dataKer = kernelY.data;

		final int offsetY = kernelY.getOffset();
		final int kernelWidthY = kernelY.getWidth();

		final int offsetX = kernelX.getOffset();
		final int kernelWidthX = kernelX.getWidth();
		final int offsetX1 = kernelWidthX-offsetX-1;

		final int imgWidth = output.getWidth();
		final int imgHeight = output.getHeight();

		final int yEnd = imgHeight - (kernelWidthY - offsetY - 1);

		int tmpSumX = 0;
		for (int k = offsetX; k < kernelWidthX; k++) {
			tmpSumX += kernelX.data[k];
		}
		final int startWeightX = tmpSumX;

		//CONCURRENT_BELOW BoofConcurrency.loopFor(0, offsetY, y -> {
		for (int y = 0; y < offsetY; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;
			final int iEnd = i + imgWidth;

			int kStart = offsetY - y;

			int weightY = 0;
			for (int k = kStart; k < kernelWidthY; k++) {
				weightY += dataKer[k];
			}
			int weightX = startWeightX;

			for ( int x = 0; i < iEnd; i++, x++ ) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - y*input.stride;
				for (int k = kStart; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc])*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+weight/2)/weight);
				if( x < offsetX ) {
					weightX += kernelX.data[offsetX-x-1];
				} else if( x >= input.width-(kernelWidthX-offsetX) ) {
					weightX -= kernelX.data[input.width-x+offsetX-1];
				}
			}
		}
		//CONCURRENT_ABOVE });

		//CONCURRENT_BELOW BoofConcurrency.loopFor(yEnd, imgHeight, y -> {
		for (int y = yEnd; y < imgHeight; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;
			final int iEnd = i + imgWidth;

			int kEnd = imgHeight - (y - offsetY);

			int weightY = 0;
			for (int k = 0; k < kEnd; k++) {
				weightY += dataKer[k];
			}
			int weightX = startWeightX;

			for (int x = 0; i < iEnd; i++, x++) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kEnd; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc])*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+weight/2)/weight);
				if( x < offsetX ) {
					weightX += kernelX.data[offsetX - x - 1];
				} else if (x >= input.width - (kernelWidthX - offsetX)) {
					weightX -= kernelX.data[input.width-x+offsetX-1];
				}
			}
		}
		//CONCURRENT_ABOVE });

		// left and right border
		final int weightY = kernelY.computeSum();
		//CONCURRENT_BELOW BoofConcurrency.loopFor(offsetY, yEnd, y -> {
		for (int y = offsetY; y < yEnd; y++) {
			int indexDst = output.startIndex + y*output.stride;
			int i = input.startIndex + y*input.stride;

			// left side
			int iEnd = i + offsetY;
			int weightX = startWeightX;
			for (int x = 0; i < iEnd; i++, x++) {
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc])*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+weight/2)/weight);
				weightX += kernelX.data[offsetX - x - 1];
			}

			// right side
			int startX = input.width - offsetX1;
			indexDst = output.startIndex + y*output.stride + startX;
			i = input.startIndex + y*input.stride + startX;
			iEnd = input.startIndex + y*input.stride + input.width;
			for (int x = startX; i < iEnd; i++, x++) {
				weightX -= kernelX.data[input.width - x + offsetX];
				int weight = weightX*weightY;
				int total = 0;
				int indexSrc = i - offsetY*input.stride;
				for (int k = 0; k < kernelWidthY; k++, indexSrc += input.stride) {
					total += (dataSrc[indexSrc])*dataKer[k];
				}
				dataDst[indexDst++] = (short)((total+weight/2)/weight);
			}
		}
		//CONCURRENT_ABOVE });
	}

}
