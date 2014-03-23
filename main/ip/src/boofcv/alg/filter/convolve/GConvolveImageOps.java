/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.struct.convolve.*;
import boofcv.struct.image.*;

/**
 * Image type agnostic convolution functions
 */
public class GConvolveImageOps {

	/**
	 * Performs a horizontal 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D, B extends ImageBorder<T>>
	void horizontal(K kernel, T input, T output , B border ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveWithBorder.horizontal((Kernel1D_F32)kernel,(ImageFloat32)input,(ImageFloat32)output,(ImageBorder_F32)border);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveWithBorder.horizontal((Kernel1D_I32)kernel,(ImageUInt8)input,(ImageInt16)output,(ImageBorder_I32)border);
			else
				ConvolveWithBorder.horizontal((Kernel1D_I32)kernel,(ImageUInt8)input,(ImageSInt32)output,(ImageBorder_I32)border);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveWithBorder.horizontal((Kernel1D_I32)kernel,(ImageSInt16)input,(ImageInt16)output,(ImageBorder_I32)border);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D, B extends ImageBorder<T>>
	void vertical(K kernel, T input, T output , B border ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveWithBorder.vertical((Kernel1D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output, (ImageBorder_F32) border);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveWithBorder.vertical((Kernel1D_I32) kernel, (ImageUInt8) input, (ImageInt16) output, (ImageBorder_I32) border);
			else
				ConvolveWithBorder.vertical((Kernel1D_I32) kernel, (ImageUInt8) input, (ImageSInt32) output, (ImageBorder_I32) border);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveWithBorder.vertical((Kernel1D_I32) kernel, (ImageSInt16) input, (ImageInt16) output, (ImageBorder_I32) border);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a 2D convolution across the image.  Borders are handled as specified by the 'border'
	 * parameter.
	 *
	 * @param input The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 * @param border How the image borders are handled.
	 */
	public static <T extends ImageSingleBand, K extends Kernel2D, B extends ImageBorder<T>>
	void convolve(K kernel, T input, T output , B border ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveWithBorder.convolve((Kernel2D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output, (ImageBorder_F32) border);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveWithBorder.convolve((Kernel2D_I32) kernel, (ImageUInt8) input, (ImageInt16) output, (ImageBorder_I32) border);
			else
				ConvolveWithBorder.convolve((Kernel2D_I32) kernel, (ImageUInt8) input, (ImageSInt32) output, (ImageBorder_I32) border);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveWithBorder.convolve((Kernel2D_I32) kernel, (ImageSInt16) input, (ImageInt16) output, (ImageBorder_I32) border);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D>
	void horizontal(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveImageNoBorder.horizontal((Kernel1D_F32)kernel,(ImageFloat32)input,(ImageFloat32)output);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveImageNoBorder.horizontal((Kernel1D_I32)kernel,(ImageUInt8)input,(ImageInt16)output);
			else
				ConvolveImageNoBorder.horizontal((Kernel1D_I32)kernel,(ImageUInt8)input,(ImageSInt32)output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveImageNoBorder.horizontal((Kernel1D_I32)kernel,(ImageSInt16)input,(ImageInt16)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a horizontal 1D convolution across the image.  The horizontal border is not processed.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D>
	void vertical(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveImageNoBorder.vertical((Kernel1D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveImageNoBorder.vertical((Kernel1D_I32) kernel, (ImageUInt8) input, (ImageInt16) output);
			else
				ConvolveImageNoBorder.vertical((Kernel1D_I32) kernel, (ImageUInt8) input, (ImageSInt32) output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveImageNoBorder.vertical((Kernel1D_I32) kernel, (ImageSInt16) input, (ImageInt16) output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a 2D convolution across the image.  The image's borders are not processed.
	 *
	 * @param kernel A square kernel that will be convolved across the source image
	 * @param input  The source image that is to be convolved
	 * @param output   The results of the convolution
	 */
	public static <T extends ImageSingleBand, K extends Kernel2D>
	void convolve(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveImageNoBorder.convolve((Kernel2D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output);
		} else if( input instanceof ImageUInt8 ) {
			if( ImageInt16.class.isAssignableFrom(output.getClass()) )
				ConvolveImageNoBorder.convolve((Kernel2D_I32) kernel, (ImageUInt8) input, (ImageInt16) output);
			else
				ConvolveImageNoBorder.convolve((Kernel2D_I32) kernel, (ImageUInt8) input, (ImageSInt32) output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveImageNoBorder.convolve((Kernel2D_I32) kernel, (ImageSInt16) input, (ImageInt16) output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a horizontal 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D>
	void horizontalNormalized(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveNormalized.horizontal((Kernel1D_F32)kernel,(ImageFloat32)input,(ImageFloat32)output);
		} else if( input instanceof ImageUInt8 ) {
			ConvolveNormalized.horizontal((Kernel1D_I32)kernel,(ImageUInt8)input,(ImageInt8)output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveNormalized.horizontal((Kernel1D_I32)kernel,(ImageSInt16)input,(ImageInt16)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a vertical 1D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static <T extends ImageSingleBand, K extends Kernel1D>
	void verticalNormalized(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveNormalized.vertical((Kernel1D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output);
		} else if( input instanceof ImageUInt8 ) {
			ConvolveNormalized.vertical((Kernel1D_I32) kernel, (ImageUInt8) input, (ImageInt8) output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveNormalized.vertical((Kernel1D_I32) kernel, (ImageSInt16) input, (ImageInt16) output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}

	/**
	 * Performs a 2D convolution across the image while re-normalizing the kernel depending on its
	 * overlap with the image.
	 *
	 * @param input	 The original image. Not modified.
	 * @param output	 Where the resulting image is written to. Modified.
	 * @param kernel The kernel that is being convolved. Not modified.
	 */
	public static <T extends ImageSingleBand, K extends Kernel2D>
	void convolveNormalized(K kernel, T input, T output ) {
		if( input instanceof ImageFloat32 ) {
			ConvolveNormalized.convolve((Kernel2D_F32) kernel, (ImageFloat32) input, (ImageFloat32) output);
		} else if( input instanceof ImageUInt8 ) {
			ConvolveNormalized.convolve((Kernel2D_I32) kernel, (ImageUInt8) input, (ImageInt8) output);
		} else if( input instanceof ImageSInt16 ) {
			ConvolveNormalized.convolve((Kernel2D_I32) kernel, (ImageSInt16) input, (ImageInt16) output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+input.getClass().getName());
		}
	}
}
