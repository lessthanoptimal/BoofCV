/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.convolve.down;

import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.image.ImageFloat32;


/**
 * <p>
 * Unrolls the convolution kernel to improve runtime performance by reducing array accesses.
 * </p>
 *
 * @author Peter Abeles
 */
public class ConvolveDownNoBorderUnrolled_F32_F32 {

	public static boolean horizontal( Kernel1D_F32 kernel , ImageFloat32 image, ImageFloat32 dest ,
									  int skip ) {
		switch( kernel.width ) {
			case 3:
				horizontal3(kernel,image,dest,skip);
				break;

			default:
				return false;
		}
		return true;
	}

	public static boolean vertical( Kernel1D_F32 kernel , ImageFloat32 image, ImageFloat32 dest ,
									int skip ) {
		switch( kernel.width ) {
			case 3:
				vertical3(kernel,image,dest,skip);
				break;

			default:
				return false;
		}
		return true;
	}

	public static boolean convolve( Kernel2D_F32 kernel , ImageFloat32 image, ImageFloat32 dest ,
									int skip ) {
		switch( kernel.width ) {
			case 3:
				convolve3(kernel,image,dest,skip);
				break;

			default:
				return false;
		}
		return true;
	}

	public static void horizontal3( Kernel1D_F32 kernel ,
									ImageFloat32 input, ImageFloat32 output ,
									int skip ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;

		final float k1 = kernel.data[0];
		final float k2 = kernel.data[1];
		final float k3 = kernel.data[2];

		final int radius = kernel.getRadius();

		final int width = input.getWidth();
		final int height = input.getHeight();

		final int offsetX = radius <= skip ? skip : radius + radius % skip;

		for( int i = 0; i < height; i++ ) {
			int indexDst = output.startIndex + i*output.stride + offsetX/skip;
			int j = input.startIndex+ i*input.stride;
			final int jEnd = j+width-radius;

			for( j += offsetX; j < jEnd; j += skip ) {
				int indexSrc = j-radius;

				float total = (dataSrc[indexSrc++] ) * k1;
				total += (dataSrc[indexSrc++] ) * k2;
				total += (dataSrc[indexSrc] ) * k3;

				dataDst[indexDst++] = total;
			}
		}
	}

	public static void vertical3( Kernel1D_F32 kernel,
								  ImageFloat32 input, ImageFloat32 output,
								  int skip ) {
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;

		final float k1 = kernel.data[0];
		final float k2 = kernel.data[1];
		final float k3 = kernel.data[2];

		final int radius = kernel.getRadius();

		final int imgWidth = input.getWidth();
		final int imgHeight = input.getHeight();

		final int yEnd = imgHeight-radius;

		final int offsetY = radius <= skip ? skip : radius + radius % skip;

		for( int y = offsetY; y < yEnd; y += skip ) {
			int indexDst = output.startIndex + (y/skip)*output.stride;
			int i = input.startIndex + y*input.stride;
			final int iEnd = i + imgWidth;

			for( ; i < iEnd; i++ ) {
				int indexSrc = i-radius*input.stride;
				float total = (dataSrc[indexSrc] )*k1;
				indexSrc += input.stride;
				total += (dataSrc[indexSrc] )*k2;
				indexSrc += input.stride;
				total += (dataSrc[indexSrc] )*k3;

				dataDst[indexDst++] = total;
			}
		}
	}

	public static void convolve3( Kernel2D_F32 kernel ,
								 ImageFloat32 input , ImageFloat32 output , int skip )
	{
		final float[] dataSrc = input.data;
		final float[] dataDst = output.data;

		final int width = input.getWidth();
		final int height = input.getHeight();

		final int radius = kernel.getRadius();

		final int offset = radius <= skip ? skip : radius + radius % skip;

		for( int y = offset; y < height-radius; y += skip) {

			// first time through the value needs to be set
			float k1 = kernel.data[0];
			float k2 = kernel.data[1];
			float k3 = kernel.data[2];

			int indexDst = output.startIndex + (y/skip)*output.stride + offset/skip;
			int indexSrcRow = input.startIndex + (y-radius)*input.stride - radius;
			for( int x = offset; x < width-radius; x += skip ) {
				int indexSrc = indexSrcRow + x;

				float total = 0;
				total += (dataSrc[indexSrc++] )* k1;
				total += (dataSrc[indexSrc++] )* k2;
				total += (dataSrc[indexSrc] )* k3;

				dataDst[indexDst++] = total;
			}

			// rest of the convolution rows are an addition
			for( int i = 1; i < 3; i++ ) {
				indexDst = output.startIndex + (y/skip)*output.stride + offset/skip;
				indexSrcRow = input.startIndex + (y+i-radius)*input.stride - radius;

				k1 = kernel.data[i*3 + 0];
				k2 = kernel.data[i*3 + 1];
				k3 = kernel.data[i*3 + 2];

				for( int x = offset; x < width-radius; x += skip ) {
					int indexSrc = indexSrcRow+x;

					float total = (dataSrc[indexSrc++] )* k1;
					total += (dataSrc[indexSrc++] )* k2;
					total += (dataSrc[indexSrc] )* k3;

					dataDst[indexDst++] += total;
				}
			}
		}
	}
}
