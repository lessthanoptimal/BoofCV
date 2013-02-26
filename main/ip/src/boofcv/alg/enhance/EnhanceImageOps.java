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
import boofcv.alg.enhance.impl.ImplEnhanceHistogram;
import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.*;

/**
 * <p>
 * Operations for improving the visibility of images.
 * </p>
 *
 * <p>
 * See [1] for a discussion of algorithms found in this class.
 * </p>
 *
 * <p>
 * [1] R. C. Gonzalez, R. E. Woods, "Digitial Image Processing" 2nd Ed. 2002
 * </p>
 *
 * @author Peter Abeles
 */
// TODO Add laplacian enhancement?
public class EnhanceImageOps {

	/**
	 * Computes a transformation table which will equalize the provided histogram.  An equalized histogram spreads
	 * the 'weight' across the whole spectrum of values.  Often used to make dim images easier for people to see.
	 *
	 * @param histogram Input image histogram.
	 * @param transform Output transformation table.
	 */
	public static void equalize( int histogram[] , int transform[] ) {

		int sum = 0;
		for( int i = 0; i < histogram.length; i++ ) {
			transform[i] = sum += histogram[i];
		}

		int maxValue = histogram.length-1;

		for( int i = 0; i < histogram.length; i++ ) {
			transform[i] = (transform[i]*maxValue)/sum;
		}
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( ImageUInt8 input , int transform[] , ImageUInt8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceHistogram.applyTransform(input,transform,output);
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( ImageUInt16 input , int transform[] , ImageUInt16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceHistogram.applyTransform(input,transform,output);
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( ImageSInt8 input , int transform[] , int minValue, ImageSInt8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceHistogram.applyTransform(input,transform,minValue,output);
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( ImageSInt16 input , int transform[] , int minValue, ImageSInt16 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceHistogram.applyTransform(input,transform,minValue,output);
	}

	/**
	 * Applies the transformation table to the provided input image.
	 *
	 * @param input Input image.
	 * @param minValue Minimum possible pixel value.
	 * @param transform Input transformation table.
	 * @param output Output image.
	 */
	public static void applyTransform( ImageSInt32 input , int transform[] , int minValue, ImageSInt32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceHistogram.applyTransform(input,transform,minValue,output);
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
	public static void equalizeLocal( ImageUInt8 input , int radius , ImageUInt8 output ,
									  int histogram[] , int transform[] ) {

		InputSanityCheck.checkSameShape(input, output);

		int width = radius*2+1;

		// use more efficient algorithms if possible
		if( input.width >= width && input.height >= width ) {
			ImplEnhanceHistogram.equalizeLocalInner(input,radius,output,histogram);

			// top border
			ImplEnhanceHistogram.equalizeLocalRow(input,radius,0,output,histogram,transform);
			// bottom border
			ImplEnhanceHistogram.equalizeLocalRow(input,radius,input.height-radius,output,histogram,transform);

			// left border
			ImplEnhanceHistogram.equalizeLocalCol(input,radius,0,output,histogram,transform);
			// right border
			ImplEnhanceHistogram.equalizeLocalCol(input,radius,input.width-radius,output,histogram,transform);

		} else if( input.width < width && input.height < width ) {
			// the local region is larger than the image.  just use the full image algorithm
			ImageStatistics.histogram(input,histogram);
			equalize(histogram,transform);
			applyTransform(input,transform,output);
		} else {
			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,output,transform);
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
	public static void equalizeLocal( ImageUInt16 input , int radius , ImageUInt16 output ,
									  int histogram[] , int transform[] ) {

		InputSanityCheck.checkSameShape(input, output);

		int width = radius*2+1;

		// use more efficient algorithms if possible
		if( input.width >= width && input.height >= width ) {
			ImplEnhanceHistogram.equalizeLocalInner(input,radius,output,histogram);

			// top border
			ImplEnhanceHistogram.equalizeLocalRow(input,radius,0,output,histogram,transform);
			// bottom border
			ImplEnhanceHistogram.equalizeLocalRow(input,radius,input.height-radius,output,histogram,transform);

			// left border
			ImplEnhanceHistogram.equalizeLocalCol(input,radius,0,output,histogram,transform);
			// right border
			ImplEnhanceHistogram.equalizeLocalCol(input,radius,input.width-radius,output,histogram,transform);

		} else if( input.width < width && input.height < width ) {
			// the local region is larger than the image.  just use the full image algorithm
			ImageStatistics.histogram(input,histogram);
			equalize(histogram,transform);
			applyTransform(input,transform,output);
		} else {
			ImplEnhanceHistogram.equalizeLocalNaive(input,radius,output,transform);
		}
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen4( ImageUInt8 input , ImageUInt8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceFilter.sharpenInner4(input,output,0,255);
		ImplEnhanceFilter.sharpenBorder4(input,output,0,255);
	}

	/**
	 * Applies a Laplacian-4 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen4( ImageFloat32 input , ImageFloat32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceFilter.sharpenInner4(input,output,0,255);
		ImplEnhanceFilter.sharpenBorder4(input, output, 0, 255);
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen8( ImageUInt8 input , ImageUInt8 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceFilter.sharpenInner8(input,output,0,255);
		ImplEnhanceFilter.sharpenBorder8(input, output, 0, 255);
	}

	/**
	 * Applies a Laplacian-8 based sharpen filter to the image.
	 *
	 * @param input Input image.
	 * @param output Output image.
	 */
	public static void sharpen8( ImageFloat32 input , ImageFloat32 output ) {
		InputSanityCheck.checkSameShape(input, output);

		ImplEnhanceFilter.sharpenInner8(input,output,0,255);
		ImplEnhanceFilter.sharpenBorder8(input, output, 0, 255);
	}
}
