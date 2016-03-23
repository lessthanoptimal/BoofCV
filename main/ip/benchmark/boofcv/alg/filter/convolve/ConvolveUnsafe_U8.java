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

package boofcv.alg.filter.convolve;

import boofcv.struct.convolve.Kernel2D_I32;
import boofcv.struct.image.GrayI8;
import boofcv.struct.image.GrayU8;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Experimental code demonstrating how convolution can be used by applying unsafe array operations.
 * On my computer this doesn't seem to produce faster results
 *
 * Thanks to Palo Marton for this suggestion and providing example code.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("deprecation")
public class ConvolveUnsafe_U8 {

	static final int ARRAY_INT_BASE_OFFSET = 0;//Unsafe.ARRAY_INT_BASE_OFFSET
	static final int ARRAY_BYTE_BASE_OFFSET = 0; //Unsafe.ARRAY_BYTE_BASE_OFFSET

	static final Unsafe UNSAFE = getUnsafe();

	private static Unsafe getUnsafe() {
		try {
			Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			return (sun.misc.Unsafe) field.get(null);
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	public static boolean convolve(Kernel2D_I32 kernel ,
								   GrayU8 image, GrayI8 dest , int divisor ) {
		switch( kernel.width ) {
			case 3:
				convolve3(kernel,image,dest,divisor);
				break;

			case 5:
				convolve5(kernel,image,dest,divisor);
				break;

			default:
				return false;
		}
		return true;
	}


	public static void convolve3(Kernel2D_I32 kernel, GrayU8 src,
								 GrayI8 dest, int divisor) {
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		final int kernelRadius = kernel.getRadius();
		final int totalRow[] = new int[width];

		for (int y = kernelRadius; y < height - kernelRadius; y++) {

			// first time through the value needs to be set
			int k1 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 0 * 4);
			int k2 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 1 * 4);
			int k3 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 2 * 4);

			int p1, p2, p3;

			int indexSrcRow = src.startIndex + (y - kernelRadius) * src.stride
					- kernelRadius;
			{
				int indexSrc = indexSrcRow + kernelRadius;
				p1 = (dataSrc[indexSrc++] & 0xFF);
				p2 = (dataSrc[indexSrc++] & 0xFF);
				p3 = (dataSrc[indexSrc] & 0xFF);
			}

			for (int x = kernelRadius + 1; x < width - kernelRadius; x++) {
				int indexSrc = indexSrcRow + x;
				p1 = p2;
				p2 = p3;
				p3 = UNSAFE.getByte(dataSrc, ARRAY_BYTE_BASE_OFFSET
						+ indexSrc + 2) & 0xFF;

				int total = 0;
				total += p1 * k1;
				total += p2 * k2;
				total += p3 * k3;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < 3; i++) {
				indexSrcRow = src.startIndex + (y + i - kernelRadius)
						* src.stride - kernelRadius;

				int ki = ARRAY_INT_BASE_OFFSET + i * 3 * 4;
				k1 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k2 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k3 = UNSAFE.getInt(kernel.data, ki);

				{
					int indexSrc = indexSrcRow + kernelRadius;
					p1 = (dataSrc[indexSrc++] & 0xFF);
					p2 = (dataSrc[indexSrc++] & 0xFF);
					p3 = (dataSrc[indexSrc] & 0xFF);
				}
				for (int x = kernelRadius + 1; x < width - kernelRadius; x++) {
					int indexSrc = indexSrcRow + x;
					p1 = p2;
					p2 = p3;
					p3 = UNSAFE.getByte(dataSrc, ARRAY_BYTE_BASE_OFFSET
							+ indexSrc + 2) & 0xFF;

					int total = 0;
					total += p1 * k1;
					total += p2 * k2;
					total += p3 * k3;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y * dest.stride + kernelRadius;
			for (int x = kernelRadius; x < width - kernelRadius; x++) {
				dataDst[indexDst++] = (byte) (totalRow[x] / divisor);
			}
		}
	}

	public static void convolve5(Kernel2D_I32 kernel, GrayU8 src,
								 GrayI8 dest, int divisor) {
		final byte[] dataSrc = src.data;
		final byte[] dataDst = dest.data;

		final int width = src.getWidth();
		final int height = src.getHeight();

		final int kernelRadius = kernel.getRadius();
		final int totalRow[] = new int[width];

		for (int y = kernelRadius; y < height - kernelRadius; y++) {

			// first time through the value needs to be set
			int k1 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 0 * 4);
			int k2 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 1 * 4);
			int k3 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 2 * 4);
			int k4 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 3 * 4);
			int k5 = UNSAFE.getInt(kernel.data,
					ARRAY_INT_BASE_OFFSET + 4 * 4);

			int p1, p2, p3, p4, p5;

			int indexSrcRow = src.startIndex + (y - kernelRadius) * src.stride
					- kernelRadius;
			{
				int indexSrc = indexSrcRow + kernelRadius;
				p1 = (dataSrc[indexSrc++] & 0xFF);
				p2 = (dataSrc[indexSrc++] & 0xFF);
				p3 = (dataSrc[indexSrc++] & 0xFF);
				p4 = (dataSrc[indexSrc++] & 0xFF);
				p5 = (dataSrc[indexSrc] & 0xFF);
			}
			for (int x = kernelRadius + 1; x < width - kernelRadius; x++) {
				int indexSrc = indexSrcRow + x;
				p1 = p2;
				p2 = p3;
				p3 = p4;
				p4 = p5;
				p5 = UNSAFE.getByte(dataSrc, ARRAY_BYTE_BASE_OFFSET
						+ indexSrc + 4) & 0xFF;

				int total = 0;
				total += p1 * k1;
				total += p2 * k2;
				total += p3 * k3;
				total += p4 * k4;
				total += p5 * k5;

				totalRow[x] = total;
			}

			// rest of the convolution rows are an addition
			for (int i = 1; i < 5; i++) {
				indexSrcRow = src.startIndex + (y + i - kernelRadius)
						* src.stride - kernelRadius;

				int ki = ARRAY_INT_BASE_OFFSET + i * 5 * 4;
				k1 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k2 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k3 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k4 = UNSAFE.getInt(kernel.data, ki);
				ki += 4;
				k5 = UNSAFE.getInt(kernel.data, ki);

				{
					int indexSrc = indexSrcRow + kernelRadius;
					p1 = (dataSrc[indexSrc++] & 0xFF);
					p2 = (dataSrc[indexSrc++] & 0xFF);
					p3 = (dataSrc[indexSrc++] & 0xFF);
					p4 = (dataSrc[indexSrc++] & 0xFF);
					p5 = (dataSrc[indexSrc] & 0xFF);
				}
				for (int x = kernelRadius + 1; x < width - kernelRadius; x++) {
					int indexSrc = indexSrcRow + x;
					p1 = p2;
					p2 = p3;
					p3 = p4;
					p4 = p5;
					p5 = UNSAFE.getByte(dataSrc, ARRAY_BYTE_BASE_OFFSET
							+ indexSrc + 4) & 0xFF;

					int total = 0;
					total += p1 * k1;
					total += p2 * k2;
					total += p3 * k3;
					total += p4 * k4;
					total += p5 * k5;

					totalRow[x] += total;
				}
			}
			int indexDst = dest.startIndex + y * dest.stride + kernelRadius;
			for (int x = kernelRadius; x < width - kernelRadius; x++) {
				dataDst[indexDst++] = (byte) (totalRow[x] / divisor);
			}
		}
	}
}
