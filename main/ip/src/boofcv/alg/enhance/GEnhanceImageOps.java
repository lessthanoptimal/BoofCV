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

package boofcv.alg.enhance;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.enhance.impl.ImplEnhanceFilter;
import boofcv.alg.filter.convolve.border.ConvolveJustBorder_General;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.BoofDefaults;
import boofcv.struct.image.*;

/**
 * Weakly typed version of {@link EnhanceImageOps}.
 *
 * @author Peter Abeles
 */
public class GEnhanceImageOps {
	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param minValue Minimum possible pixel value.
	 * @param output Output image.
	 */
	public static <T extends ImageSingleBand>
	void applyTransform( T input , int transform[] , int minValue , T output ) {
		InputSanityCheck.checkSameShape(input, output);

		if( input instanceof ImageUInt8) {
			EnhanceImageOps.applyTransform((ImageUInt8) input, transform, (ImageUInt8) output);
		} else if( input instanceof ImageSInt8) {
			EnhanceImageOps.applyTransform((ImageSInt8)input,transform,minValue,(ImageSInt8)output);
		} else if( input instanceof ImageUInt16) {
			EnhanceImageOps.applyTransform((ImageUInt16)input,transform,(ImageUInt16)output);
		} else if( input instanceof ImageSInt16) {
			EnhanceImageOps.applyTransform((ImageSInt16)input,transform,minValue,(ImageSInt16)output);
		} else if( input instanceof ImageSInt32) {
			EnhanceImageOps.applyTransform((ImageSInt32)input,transform,minValue,(ImageSInt32)output);
		} else {
			throw new IllegalArgumentException("Image type not supported. "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Equalizes the local image histogram on a per pixel basis.
	 *
	 * @param input Input image.
	 * @param radius Radius of square local histogram.
	 * @param output Output image.
	 * @param histogram Storage for image histogram.  Must be large enough to contain all possible values.
	 * @param transform Storage for transformation table.  Must be large enough to contain all possible values.
	 */
	public static <T extends ImageSingleBand>
	void equalizeLocal( T input , int radius , T output ,
						int histogram[] , int transform[] ) {
		if( input instanceof ImageUInt8 ) {
			EnhanceImageOps.equalizeLocal((ImageUInt8)input,radius,(ImageUInt8)output,histogram,transform);
		} else if( input instanceof ImageUInt16 ) {
			EnhanceImageOps.equalizeLocal((ImageUInt16)input,radius,(ImageUInt16)output,histogram,transform);
		} else {
			throw new IllegalArgumentException("Unsupported image type "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static <T extends ImageSingleBand> void sharpen4( T input , T output ) {
		if( input instanceof ImageUInt8) {
			EnhanceImageOps.sharpen4((ImageUInt8) input, (ImageUInt8) output);
		} else if( input instanceof ImageFloat32) {
			EnhanceImageOps.sharpen4((ImageFloat32)input, (ImageFloat32)output);
		} else {
			throw new IllegalArgumentException("Image type not supported. "+input.getClass().getSimpleName());
		}
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static <T extends ImageSingleBand> void sharpen8( T input , T output ) {
		if( input instanceof ImageUInt8) {
			EnhanceImageOps.sharpen8((ImageUInt8) input, (ImageUInt8) output);
		} else if( input instanceof ImageFloat32) {
			EnhanceImageOps.sharpen8((ImageFloat32)input, (ImageFloat32)output);
		} else {
			throw new IllegalArgumentException("Image type not supported. "+input.getClass().getSimpleName());
		}
	}
}
