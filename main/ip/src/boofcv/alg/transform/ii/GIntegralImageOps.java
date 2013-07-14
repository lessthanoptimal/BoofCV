/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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
	public static <I extends ImageSingleBand, II extends ImageSingleBand>
	Class<II> getIntegralType( Class<I> inputType ) {
		if( inputType == ImageFloat32.class ) {
			return (Class<II>)ImageFloat32.class;
		} else if( inputType == ImageUInt8.class ){
			return (Class<II>)ImageSInt32.class;
		} else if( inputType == ImageSInt32.class ){
			return (Class<II>)ImageSInt32.class;
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
	public static <I extends ImageSingleBand, T extends ImageSingleBand>
	T transform( I input , T transformed ) {
		if( input instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.transform((ImageFloat32)input,(ImageFloat32)transformed);
		} else if( input instanceof ImageFloat64) {
			return (T)IntegralImageOps.transform((ImageFloat64)input,(ImageFloat64)transformed);
		} else if( input instanceof ImageUInt8) {
			return (T)IntegralImageOps.transform((ImageUInt8)input,(ImageSInt32)transformed);
		} else if( input instanceof ImageSInt32) {
			return (T)IntegralImageOps.transform((ImageSInt32)input,(ImageSInt32)transformed);
		} else if( input instanceof ImageSInt64) {
			return (T)IntegralImageOps.transform((ImageSInt64)input,(ImageSInt64)transformed);
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
	public static <T extends ImageSingleBand>
	T convolve( T integral ,
				IntegralKernel kernel,
				T output ) {
		if( integral instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.convolve((ImageFloat32)integral,kernel,(ImageFloat32)output);
		} else if( integral instanceof ImageFloat64) {
			return (T)IntegralImageOps.convolve((ImageFloat64)integral,kernel,(ImageFloat64)output);
		} else if( integral instanceof ImageSInt32) {
			return (T)IntegralImageOps.convolve((ImageSInt32)integral,kernel,(ImageSInt32)output);
		} else if( integral instanceof ImageSInt64) {
			return (T)IntegralImageOps.convolve((ImageSInt64)integral,kernel,(ImageSInt64)output);
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
	public static <T extends ImageSingleBand>
	T convolveBorder( T integral ,
					  IntegralKernel kernel,
					  T output , int borderX , int borderY ) {
		if( integral instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.convolveBorder((ImageFloat32)integral,kernel,(ImageFloat32)output,borderX,borderY);
		} else if( integral instanceof ImageFloat64) {
			return (T)IntegralImageOps.convolveBorder((ImageFloat64)integral,kernel,(ImageFloat64)output,borderX,borderY);
		} else if( integral instanceof ImageSInt32) {
			return (T)IntegralImageOps.convolveBorder((ImageSInt32)integral,kernel,(ImageSInt32)output,borderX,borderY);
		} else if( integral instanceof ImageSInt64) {
			return (T)IntegralImageOps.convolveBorder((ImageSInt64)integral,kernel,(ImageSInt64)output,borderX,borderY);
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
	public static <T extends ImageSingleBand>
	double convolveSparse( T integral ,
						   IntegralKernel kernel ,
						   int x , int y ) {
		if( integral instanceof ImageFloat32 ) {
			return IntegralImageOps.convolveSparse((ImageFloat32)integral,kernel,x,y);
		} else if( integral instanceof ImageFloat64) {
			return IntegralImageOps.convolveSparse((ImageFloat64)integral,kernel,x,y);
		} else if( integral instanceof ImageSInt32) {
			return IntegralImageOps.convolveSparse((ImageSInt32)integral,kernel,x,y);
		} else if( integral instanceof ImageSInt64) {
			return IntegralImageOps.convolveSparse((ImageSInt64)integral,kernel,x,y);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image and treats pixels outside of the
	 * image as zero.  The block is defined as follows: x0 < x <= x1 and y0 < y <= y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static <T extends ImageSingleBand>
	double block_zero( T integral , int x0 , int y0 , int x1 , int y1 )
	{
		if( integral instanceof ImageFloat32 ) {
			return IntegralImageOps.block_zero((ImageFloat32)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageFloat64) {
			return IntegralImageOps.block_zero((ImageFloat64)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageSInt32) {
			return IntegralImageOps.block_zero((ImageSInt32)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageSInt64) {
			return IntegralImageOps.block_zero((ImageSInt64)integral,x0,y0,x1,y1);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	/**
	 * <p>
	 * Computes the value of a block inside an integral image without bounds checking.  The block is
	 * defined as follows: x0 < x <= x1 and y0 < y <= y1.
	 * </p>
	 *
	 * @param integral Integral image.
	 * @param x0 Lower bound of the block.  Exclusive.
	 * @param y0 Lower bound of the block.  Exclusive.
	 * @param x1 Upper bound of the block.  Inclusive.
	 * @param y1 Upper bound of the block.  Inclusive.
	 * @return Value inside the block.
	 */
	public static <T extends ImageSingleBand>
	double block_unsafe( T integral , int x0 , int y0 , int x1 , int y1 )
	{
		if( integral instanceof ImageFloat32 ) {
			return IntegralImageOps.block_unsafe((ImageFloat32)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageFloat64) {
			return IntegralImageOps.block_unsafe((ImageFloat64)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageSInt32) {
			return IntegralImageOps.block_unsafe((ImageSInt32)integral,x0,y0,x1,y1);
		} else if( integral instanceof ImageSInt64) {
			return IntegralImageOps.block_unsafe((ImageSInt64)integral,x0,y0,x1,y1);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}
}
