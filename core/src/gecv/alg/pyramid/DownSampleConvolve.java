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

package gecv.alg.pyramid;

import gecv.alg.filter.convolve.ConvolveImageNoBorderSparse;
import gecv.alg.filter.convolve.ConvolveNormalizedSparse;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * <p>
 * Down samples and image after first convolving a kernel across it it.  A sparse down sampling is used for efficiency.
 * </p>
 *
 * <p>
 * This is typically done with constructing a Gaussian image pyramid.
 * </p>
 *
 * @author Peter Abeles
 */
public class DownSampleConvolve {

	/**
	 * Down samples an image by convolving the specified kernel across the original image and sampling
	 * every "skip" pixels.
	 *
	 * @param kernel 1D blur convolution kernel.
	 * @param original Image being down sampled. Not modified.
	 * @param downSampled The output down sampled image. Modified.
	 * @param skip How many rows/columns should be skipped.
	 * @param storage Storage array the same width as the kernel.
	 */
	public static void downSample( Kernel1D_F32 kernel ,
								   ImageFloat32 original ,  ImageFloat32 downSampled ,
								   int skip , float storage[] )
	{
		for( int y = 0, destY = 0; destY < downSampled.height; y += skip , destY++ ) {

			int indexDest = downSampled.startIndex + destY*downSampled.stride;
			int indexDestEnd = indexDest + downSampled.width;
			for( int x = 0; indexDest < indexDestEnd; x += skip , indexDest++) {
				float val = ConvolveNormalizedSparse.convolve(kernel,kernel,original,x,y,storage);
				downSampled.data[ indexDest ] = val;
			}
		}
	}

	/**
	 * Down samples an image by convolving the specified kernel across the original image and sampling
	 * every "skip" pixels.  The image's border is ignored.
	 *
	 * @param kernel 1D blur convolution kernel.
	 * @param original Image being down sampled. Not modified.
	 * @param downSampled The output down sampled image. Modified.
	 * @param skip How many rows/columns should be skipped.
	 * @param storage Storage array the same width as the kernel.
	 */
	public static void downSampleNoBorder( Kernel1D_F32 kernel ,
										   ImageFloat32 original ,  ImageFloat32 downSampled ,
										   int skip , float storage[] )
	{
		int radius = kernel.getRadius();
		int border = radius + (radius % skip);
		int borderDown = border/skip;

		for( int y = border, destY = borderDown; destY < downSampled.height-borderDown; y += skip , destY++ ) {

			int indexDest = downSampled.startIndex + destY*downSampled.stride;
			int indexDestEnd = indexDest + downSampled.width;
			indexDest += borderDown;
			indexDestEnd -= borderDown;
			for( int x = border; indexDest < indexDestEnd; x += skip , indexDest++) {
				float val = ConvolveImageNoBorderSparse.convolve(kernel,kernel,original,x,y,storage);
				downSampled.data[ indexDest ] = val;
			}
		}
	}

	/**
	 * Down samples an image by convolving the specified kernel across the original image and sampling
	 * every "skip" pixels.
	 *
	 * @param kernel 1D blur convolution kernel.
	 * @param original Image being down sampled. Not modified.
	 * @param downSampled The output down sampled image. Modified.
	 * @param skip How many rows/columns should be skipped.
	 * @param storage Storage array the same width as the kernel.
	 */
	public static void downSample( Kernel1D_I32 kernel ,
								   ImageUInt8 original ,  ImageUInt8 downSampled ,
								   int skip , int storage[] )
	{
		for( int y = 0, destY = 0; destY < downSampled.height; y += skip , destY++ ) {

			int indexDest = downSampled.startIndex + destY*downSampled.stride;
			int indexDestEnd = indexDest + downSampled.width;
			for( int x = 0; indexDest < indexDestEnd; x += skip , indexDest++) {
				int val = ConvolveNormalizedSparse.convolve(kernel,kernel,original,x,y,storage);
				downSampled.data[ indexDest ] = (byte)val;
			}
		}
	}

	/**
	 * Down samples an image by convolving the specified kernel across the original image and sampling
	 * every "skip" pixels.
	 *
	 * @param kernel 1D blur convolution kernel.
	 * @param original Image being down sampled. Not modified.
	 * @param downSampled The output down sampled image. Modified.
	 * @param skip How many rows/columns should be skipped.
	 * @param storage Storage array the same width as the kernel.
	 */
	public static void downSample( Kernel1D_I32 kernel ,
								   ImageSInt16 original ,  ImageSInt16 downSampled ,
								   int skip , int storage[] )
	{
		for( int y = 0, destY = 0; destY < downSampled.height; y += skip , destY++ ) {

			int indexDest = downSampled.startIndex + destY*downSampled.stride;
			int indexDestEnd = indexDest + downSampled.width;
			for( int x = 0; indexDest < indexDestEnd; x += skip , indexDest++) {
				int val = ConvolveNormalizedSparse.convolve(kernel,kernel,original,x,y,storage);
				downSampled.data[ indexDest ] = (short)val;
			}
		}
	}
}
