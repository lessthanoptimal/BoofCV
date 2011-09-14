/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;


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
	public static <I extends ImageBase, II extends ImageBase>
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

	public static <I extends ImageBase, T extends ImageBase>
	T transform( I input , T transformed ) {
		if( input instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.transform((ImageFloat32)input,(ImageFloat32)transformed);
		} else if( input instanceof ImageUInt8) {
			return (T)IntegralImageOps.transform((ImageUInt8)input,(ImageSInt32)transformed);
		} else if( input instanceof ImageSInt32) {
			return (T)IntegralImageOps.transform((ImageSInt32)input,(ImageSInt32)transformed);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	public static <T extends ImageBase>
	T convolve( T integral ,
				IntegralKernel kernel,
				T output ) {
		if( integral instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.convolve((ImageFloat32)integral,kernel,(ImageFloat32)output);
		} else if( integral instanceof ImageSInt32) {
			return (T)IntegralImageOps.convolve((ImageSInt32)integral,kernel,(ImageSInt32)output);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	public static <T extends ImageBase>
	T convolveBorder( T integral ,
					  IntegralKernel kernel,
					  T output , int borderX , int borderY ) {
		if( integral instanceof ImageFloat32 ) {
			return (T)IntegralImageOps.convolveBorder((ImageFloat32)integral,kernel,(ImageFloat32)output,borderX,borderY);
		} else if( integral instanceof ImageSInt32) {
			return (T)IntegralImageOps.convolveBorder((ImageSInt32)integral,kernel,(ImageSInt32)output,borderX,borderY);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}

	public static <T extends ImageBase>
	double convolveSparse( T integral ,
						   IntegralKernel kernel ,
						   int x , int y ) {
		if( integral instanceof ImageFloat32 ) {
			return IntegralImageOps.convolveSparse((ImageFloat32)integral,kernel,x,y);
		} else if( integral instanceof ImageSInt32) {
			return IntegralImageOps.convolveSparse((ImageSInt32)integral,kernel,x,y);
		} else {
			throw new IllegalArgumentException("Unknown input type");
		}
	}
}
