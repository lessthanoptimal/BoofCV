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

package boofcv.core.image;

import boofcv.struct.image.*;

import java.lang.reflect.Method;

/**
 * <p>
 * Generalized functions for converting between different image types. Numerical values do not change or are closely
 * approximated in these functions. If an output image is not specified then a new instance is declared and returned.
 * </p>
 *
 * @author Peter Abeles
 */
public class GConvertImage {

	/**
	 * <p>
	 * Converts one type of {@link ImageSingleBand} into a different type by typecasting each pixel.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image.  If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static void convert( ImageSingleBand input , ImageSingleBand output ) {
		if( input.getClass() == output.getClass() ) {
			output.setTo(input);
		} else {
			try {
				Method m = ConvertImage.class.getMethod("convert",input.getClass(),output.getClass());
				m.invoke(null,input,output);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unknown conversion");
			}
		}
	}

	/**
	 * Converts a {@link MultiSpectral} into a {@link ImageSingleBand} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input MultiSpectral image that is being converted. Not modified.
	 * @param output (Optional) The single band output image.  If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static <T extends ImageSingleBand>T average( MultiSpectral<T> input , T output ) {
		Class type = input.getType();
		if( type == ImageUInt8.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageUInt8>)input,(ImageUInt8)output);
		} else if( type == ImageSInt8.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageSInt8>)input,(ImageSInt8)output);
		} else if( type == ImageUInt16.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageUInt16>)input,(ImageUInt16)output);
		} else if( type == ImageSInt16.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageSInt16>)input,(ImageSInt16)output);
		} else if( type == ImageSInt32.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageSInt32>)input,(ImageSInt32)output);
		} else if( type == ImageSInt64.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageSInt64>)input,(ImageSInt64)output);
		} else if( type == ImageFloat32.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageFloat32>)input,(ImageFloat32)output);
		} else if( type == ImageFloat64.class ) {
			return (T)ConvertImage.average((MultiSpectral<ImageFloat64>)input,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: "+type.getSimpleName());
		}
	}
}
