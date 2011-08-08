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

package gecv.alg.transform.ii;

import gecv.alg.InputSanityCheck;
import gecv.alg.transform.ii.impl.ImplIntegralImageOps;
import gecv.struct.ImageRectangle;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;


/**
 * @author Peter Abeles
 */
public class IntegralImageOps {

	public static ImageFloat32 transform( ImageFloat32 input , ImageFloat32 transformed ) {
		transformed = InputSanityCheck.checkDeclare(input,transformed);

		ImplIntegralImageOps.process(input,transformed);

		return transformed;
	}

	public static ImageSInt32 transform( ImageUInt8 input , ImageSInt32 transformed ) {
		transformed = InputSanityCheck.checkDeclare(input,transformed,ImageSInt32.class);

		ImplIntegralImageOps.process(input,transformed);

		return transformed;
	}

	/**
	 * General code for convolving a box filter across an image using the integral image.
	 *
	 * @param integral Integral image.
	 * @param blocks Block regions which are being convolved across the image.
	 * @param scales What each block is scaled by.
	 * @param output The convolved image.
	 */
	public static ImageFloat32 convolve( ImageFloat32 integral ,
										 ImageRectangle[] blocks , int scales[],
										 ImageFloat32 output )
	{
		output = InputSanityCheck.checkDeclare(integral,output);

		for( int y = 0; y < integral.height; y++ ) {
			for( int x = 0; x < integral.width; x++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*scales[i];
				}
				output.set(x,y,total);
			}
		}

		return output;
	}

	/**
	 * General code for convolving a box filter across an image using the integral image.
	 *
	 * @param integral Integral image.
	 * @param blocks Block regions which are being convolved across the image.
	 * @param scales What each block is scaled by.
	 * @param output The convolved image.
	 */
	public static ImageFloat32 convolve2( ImageFloat32 integral ,
										  ImageRectangle[] blocks , int scales[],
										  ImageFloat32 output )
	{
		output = InputSanityCheck.checkDeclare(integral,output);

		float sf[] = new float[ scales.length ];
		for( int i = 0; i < scales.length; i++ ) {
			sf[i] = scales[i];
		}

		for( int y = 0; y < integral.height; y++ ) {
			for( int x = 0; x < integral.width; x++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*sf[i];
				}
				output.set(x,y,total);
			}
		}

		return output;
	}

	/**
	 * Convolves just the image border.
	 *
	 * @param integral Integral image.
	 * @param blocks Block regions which are being convolved across the image.
	 * @param scales What each block is scaled by.
	 * @param output The convolved image.
	 * @param borderX Size of the image border along the horizontal axis.
	 * @param borderY size of the image border along the vertical axis.
	 */
	public static ImageFloat32 convolveBorder( ImageFloat32 integral ,
											   ImageRectangle[] blocks , int scales[],
											   ImageFloat32 output , int borderX , int borderY )
	{
		output = InputSanityCheck.checkDeclare(integral,output);

		for( int x = 0; x < integral.width; x++ ) {
			for( int y = 0; y < borderY; y++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*scales[i];
				}
				output.set(x,y,total);
			}
			for( int y = integral.height-borderY; y < integral.height; y++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*scales[i];
				}
				output.set(x,y,total);
			}
		}

		int endY = integral.height-borderY;
		for( int y = borderY; y < endY; y++ ) {
			for( int x = 0; x < borderX; x++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*scales[i];
				}
				output.set(x,y,total);
			}
			for( int x = integral.width-borderX; x < integral.width; x++ ) {
				float total = 0;
				for( int i = 0; i < blocks.length; i++ ) {
					ImageRectangle b = blocks[i];
					total += block_zero(integral,x+b.x0,y+b.y0,x+b.x1,y+b.y1)*scales[i];
				}
				output.set(x,y,total);
			}
		}

		return output;
	}

	/**
	 * Convolves a kernel around a single point in the integral image.
	 *
	 * @param integral Input integral image. Not modified.
	 * @param kernel Convolution kernel.
	 * @param x Pixel the convolution is performed at.
	 * @param y Pixel the convolution is performed at.
	 * @return Value of the convolution
	 */
	public static float convolveSparse( ImageFloat32 integral , IntegralKernel kernel , int x , int y )
	{
		float ret = 0;
		int N = kernel.getNumBlocks();

		for( int i = 0; i < N; i++ ) {
			ImageRectangle r = kernel.blocks[i];
			ret += block_zero(integral,x+r.x0,y+r.y0,x+r.x1,y+r.y1)*kernel.scales[i];
		}

		return ret;
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image without bounds checking.  The block is
	 * defined as follows: x0 < x <= x1 and y0 < y < y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static float block_unsafe( ImageFloat32 integral , int x0 , int y0 , int x1 , int y1 )
	{
		float br = integral.data[ integral.startIndex + y1*integral.stride + x1 ];
		float tr = integral.data[ integral.startIndex + y0*integral.stride + x1 ];
		float bl = integral.data[ integral.startIndex + y1*integral.stride + x0 ];
		float tl = integral.data[ integral.startIndex + y0*integral.stride + x0 ];

		return br-tr-bl+tl;
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image and treats pixels outside of the
	 * image as zero.  The block is defined as follows: x0 < x <= x1 and y0 < y < y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static float block_zero( ImageFloat32 integral , int x0 , int y0 , int x1 , int y1 )
	{
		x0 = Math.min(x0,integral.width-1);
		y0 = Math.min(y0,integral.height-1);
		x1 = Math.min(x1,integral.width-1);
		y1 = Math.min(y1,integral.height-1);

		float br=0,tr=0,bl=0,tl=0;

		if( x1 >= 0 && y1 >= 0)
			br = integral.data[ integral.startIndex + y1*integral.stride + x1 ];
		if( y0 >= 0 && x1 >= 0)
			tr = integral.data[ integral.startIndex + y0*integral.stride + x1 ];
		if( x0 >= 0 && y1 >= 0)
			bl = integral.data[ integral.startIndex + y1*integral.stride + x0 ];
		if( x0 >= 0 && y0 >= 0)
			tl = integral.data[ integral.startIndex + y0*integral.stride + x0 ];

		return br-tr-bl+tl;
	}
}
