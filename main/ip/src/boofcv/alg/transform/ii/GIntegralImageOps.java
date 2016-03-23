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

package boofcv.alg.transform.ii;

import boofcv.struct.image.*;


/**
 * Provides a mechanism to call {@link IntegralImageOps} with unknown types at compile time.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class GIntegralImageOps {

	/**
	 * Given the input image, return the type of image the integral image should be.
	 */
	public static <I extends ImageGray, II extends ImageGray>
	Class<II> getIntegralType( Class<I> inputType ) {
		if( inputType == GrayF32.class ) {
			return (Class<II>)GrayF32.class;
		} else if( inputType == GrayU8.class ){
			return (Class<II>)GrayS32.class;
		} else if( inputType == GrayS32.class ){
			return (Class<II>)GrayS32.class;
		} else {
			throw new IllegalArgumentException("Unknown input image type: "+inputType.getSimpleName());
		}
	}

	/**
	 * Converts a regular image into an integral image.
	 *
	 * @param input Regular image. Not modified.
	 * @param transformed Integral image. If null a new image will be created. Modified.
	 * @return Integral image.
	 */
	public static <I extends ImageGray, T extends ImageGray>
	T transform( I input , T transformed ) {
		if( input instanceof GrayF32) {
			return (T)IntegralImageOps.transform((GrayF32)input,(GrayF32)transformed);
		} else if( input instanceof GrayF64) {
			return (T)IntegralImageOps.transform((GrayF64)input,(GrayF64)transformed);
		} else if( input instanceof GrayU8) {
			return (T)IntegralImageOps.transform((GrayU8)input,(GrayS32)transformed);
		} else if( input instanceof GrayS32) {
			return (T)IntegralImageOps.transform((GrayS32)input,(GrayS32)transformed);
		} else if( input instanceof GrayS64) {
			return (T)IntegralImageOps.transform((GrayS64)input,(GrayS64)transformed);
		} else {
			throw new IllegalArgumentException("Unknown input type: "+input.getClass().getSimpleName());
		}
	}

	/**
	 * General code for convolving a box filter across an image using the integral image.
	 *
	 * @param integral Integral image.
	 * @param kernel Convolution kernel.
	 * @param output The convolved image. If null a new image will be declared and returned. Modified.
	 * @return Convolved image.
	 */
	public static <T extends ImageGray>
	T convolve( T integral ,
				IntegralKernel kernel,
				T output ) {
		if( integral instanceof GrayF32) {
			return (T)IntegralImageOps.convolve((GrayF32)integral,kernel,(GrayF32)output);
		} else if( integral instanceof GrayF64) {
			return (T)IntegralImageOps.convolve((GrayF64)integral,kernel,(GrayF64)output);
		} else if( integral instanceof GrayS32) {
			return (T)IntegralImageOps.convolve((GrayS32)integral,kernel,(GrayS32)output);
		} else if( integral instanceof GrayS64) {
			return (T)IntegralImageOps.convolve((GrayS64)integral,kernel,(GrayS64)output);
		} else {
			throw new IllegalArgumentException("Unknown input type: "+integral.getClass().getSimpleName());
		}
	}

	/**
	 * Convolves the kernel only across the image's border.
	 *
	 * @param integral Integral image. Not modified.
	 * @param kernel Convolution kernel.
	 * @param output The convolved image. If null a new image will be created. Modified.
	 * @param borderX Size of the image border along the horizontal axis.
	 * @param borderY size of the image border along the vertical axis.
	 */
	public static <T extends ImageGray>
	T convolveBorder( T integral ,
					  IntegralKernel kernel,
					  T output , int borderX , int borderY ) {
		if( integral instanceof GrayF32) {
			return (T)IntegralImageOps.convolveBorder((GrayF32)integral,kernel,(GrayF32)output,borderX,borderY);
		} else if( integral instanceof GrayF64) {
			return (T)IntegralImageOps.convolveBorder((GrayF64)integral,kernel,(GrayF64)output,borderX,borderY);
		} else if( integral instanceof GrayS32) {
			return (T)IntegralImageOps.convolveBorder((GrayS32)integral,kernel,(GrayS32)output,borderX,borderY);
		} else if( integral instanceof GrayS64) {
			return (T)IntegralImageOps.convolveBorder((GrayS64)integral,kernel,(GrayS64)output,borderX,borderY);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
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
	public static <T extends ImageGray>
	double convolveSparse( T integral ,
						   IntegralKernel kernel ,
						   int x , int y ) {
		if( integral instanceof GrayF32) {
			return IntegralImageOps.convolveSparse((GrayF32)integral,kernel,x,y);
		} else if( integral instanceof GrayF64) {
			return IntegralImageOps.convolveSparse((GrayF64)integral,kernel,x,y);
		} else if( integral instanceof GrayS32) {
			return IntegralImageOps.convolveSparse((GrayS32)integral,kernel,x,y);
		} else if( integral instanceof GrayS64) {
			return IntegralImageOps.convolveSparse((GrayS64)integral,kernel,x,y);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image and treats pixels outside of the
	 * image as zero.  The block is defined as follows: x0 &lt; x &le; x1 and y0 &lt; y &le; y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static <T extends ImageGray>
	double block_zero( T integral , int x0 , int y0 , int x1 , int y1 )
	{
		if( integral instanceof GrayF32) {
			return IntegralImageOps.block_zero((GrayF32)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayF64) {
			return IntegralImageOps.block_zero((GrayF64)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayS32) {
			return IntegralImageOps.block_zero((GrayS32)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayS64) {
			return IntegralImageOps.block_zero((GrayS64)integral,x0,y0,x1,y1);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image without bounds checking.  The block is
	 * defined as follows: x0 &lt; x &le; x1 and y0 &lt; y &le; y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static <T extends ImageGray>
	double block_unsafe( T integral , int x0 , int y0 , int x1 , int y1 )
	{
		if( integral instanceof GrayF32) {
			return IntegralImageOps.block_unsafe((GrayF32)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayF64) {
			return IntegralImageOps.block_unsafe((GrayF64)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayS32) {
			return IntegralImageOps.block_unsafe((GrayS32)integral,x0,y0,x1,y1);
		} else if( integral instanceof GrayS64) {
			return IntegralImageOps.block_unsafe((GrayS64)integral,x0,y0,x1,y1);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}
}
