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

package boofcv.core.image;

import boofcv.alg.misc.GImageMiscOps;
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
@SuppressWarnings("unchecked")
public class GConvertImage {

	/**
	 * <p>
	 * Converts one type of between two types of images using a default method.  Both are the same image type
	 * then a simple type cast if performed at the pixel level.  If the input is multi-band and the output
	 * is single band then it will average the bands.  If input is single band and output is multi-band
	 * then the single band is copied into each of the other bands.
	 * </p>
	 *
	 * <p>
	 * In some cases a temporary image will be created to store intermediate results.  If this is an issue
	 * you will need to create a specialized conversion algorithm.
	 * </p>
	 *
	 * @param input Input image which is being converted. Not modified.
	 * @param output (Optional) The output image.  If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static void convert( ImageBase input , ImageBase output ) {

		if( input instanceof ImageSingleBand ) {
			ImageSingleBand sb = (ImageSingleBand)input;
			if( output instanceof ImageSingleBand ) {
				if (input.getClass() == output.getClass()) {
					output.setTo(input);
				} else {
					try {
						Method m = ConvertImage.class.getMethod("convert", input.getClass(), output.getClass());
						m.invoke(null, input, output);
					} catch (Exception e) {
						throw new IllegalArgumentException("Unknown conversion");
					}
				}
			} else if( output instanceof MultiSpectral ) {
				MultiSpectral ms = (MultiSpectral)output;
				for (int i = 0; i < ms.getNumBands(); i++) {
					convert(input,ms.getBand(i));
				}
			} else if( output instanceof ImageInterleaved ) {
				ImageInterleaved il = (ImageInterleaved)output;
				for (int i = 0; i < il.getNumBands(); i++) {
					GImageMiscOps.insertBand(sb, i, il);
				}
			}
		} else if( input instanceof ImageInterleaved && output instanceof ImageInterleaved )  {
			if( input.getClass() == output.getClass() ) {
				output.setTo(input);
			} else {
				try {
					Method m = ConvertImage.class.getMethod("convert", input.getClass(), output.getClass());
					m.invoke(null, input, output);
				} catch (Exception e) {
					throw new IllegalArgumentException("Unknown conversion");
				}
			}
		} else if( input instanceof MultiSpectral && output instanceof ImageSingleBand )  {
			MultiSpectral mi = (MultiSpectral)input;
			ImageSingleBand so = (ImageSingleBand)output;

			if( mi.getImageType().getDataType() != so.getDataType() ) {
				int w = output.width;
				int h = output.height;
				ImageSingleBand tmp = GeneralizedImageOps.createSingleBand(mi.getImageType().getDataType(),w,h);
				average(mi,tmp);
				convert(tmp,so);
			} else {
				average(mi,so);
			}
		} else if( input instanceof MultiSpectral && output instanceof ImageInterleaved )  {
			try {
				Method m = ConvertImage.class.getMethod("convert", input.getClass(), output.getClass());
				m.invoke(null, input, output);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unknown conversion");
			}
		} else if( input instanceof MultiSpectral && output instanceof MultiSpectral ) {
			MultiSpectral mi = (MultiSpectral) input;
			MultiSpectral mo = (MultiSpectral) output;

			if (mi.getBandType() == mo.getBandType()) {
				mo.setTo(mi);
			} else {
				for (int i = 0; i < mi.getNumBands(); i++) {
					convert(mi.getBand(i), mo.getBand(i));
				}
			}
		} else if( input instanceof ImageInterleaved && output instanceof MultiSpectral )  {
			try {
				Method m = ConvertImage.class.getMethod("convert", input.getClass(), output.getClass());
				m.invoke(null, input, output);
			} catch (Exception e) {
				throw new IllegalArgumentException("Unknown conversion");
			}
		} else if( input instanceof ImageInterleaved && output instanceof ImageSingleBand )  {
			ImageInterleaved mb = (ImageInterleaved)input;
			ImageSingleBand so = (ImageSingleBand)output;

			if( mb.getImageType().getDataType() != so.getDataType() ) {
				int w = output.width;
				int h = output.height;
				ImageSingleBand tmp = GeneralizedImageOps.createSingleBand(mb.getImageType().getDataType(),w,h);
				average(mb,tmp);
				convert(tmp,so);
			} else {
				average(mb,so);
			}
		} else {
			String nameInput = input.getClass().getSimpleName();
			String nameOutput = output.getClass().getSimpleName();
			throw new IllegalArgumentException("Don't know how to convert between input types. "+nameInput+" "+nameOutput);
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
		Class type = input.getBandType();
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

	/**
	 * Converts a {@link MultiSpectral} into a {@link ImageSingleBand} by computing the average value of each pixel
	 * across all the bands.
	 *
	 * @param input Input MultiSpectral image that is being converted. Not modified.
	 * @param output (Optional) The single band output image.  If null a new image is created. Modified.
	 * @return Converted image.
	 */
	public static <T extends ImageSingleBand>T average( ImageInterleaved input , T output ) {
		ImageDataType type = input.getImageType().getDataType();
		if( type == ImageDataType.U8) {
			return (T)ConvertImage.average((InterleavedU8)input,(ImageUInt8)output);
		} else if( type == ImageDataType.S8) {
			return (T)ConvertImage.average((InterleavedS8)input,(ImageSInt8)output);
		} else if( type == ImageDataType.U16 ) {
			return (T)ConvertImage.average((InterleavedU16)input,(ImageUInt16)output);
		} else if( type == ImageDataType.S16 ) {
			return (T)ConvertImage.average((InterleavedS16)input,(ImageSInt16)output);
		} else if( type == ImageDataType.S32 ) {
			return (T)ConvertImage.average((InterleavedS32)input,(ImageSInt32)output);
		} else if( type == ImageDataType.S64 ) {
			return (T)ConvertImage.average((InterleavedS64)input,(ImageSInt64)output);
		} else if( type == ImageDataType.F32 ) {
			return (T)ConvertImage.average((InterleavedF32)input,(ImageFloat32)output);
		} else if( type == ImageDataType.F64 ) {
			return (T)ConvertImage.average((InterleavedF64)input,(ImageFloat64)output);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + type);
		}
	}

	/**
	 * Converts pixel values in the input image into an integer values from 0 to numValues.
	 * @param input Input image
	 * @param min minimum input pixel value, inclusive
	 * @param max maximum input pixel value, inclusive
	 * @param numValues Number of possible pixel values in output image
	 * @param output (Optional) Storage for the output image.  Can be null.
	 * @return The converted output image.
	 */
	public static ImageUInt8 convert(ImageSingleBand input , double min , double max , int numValues , ImageUInt8 output )
	{
		// see if it can use the faster straight forward convert
		if( min == 0 && max == 255 && numValues == 256 ) {
			if( output == null )
				output = new ImageUInt8(input.width,input.height);
			convert(input,output);
			return output;
		}

		ImageDataType type = input.getImageType().getDataType();
		if( type == ImageDataType.U8) {
			return ConvertImage.convert((ImageUInt8)input,(int)min,(int)max,numValues,output);
		} else if( type == ImageDataType.S8) {
			return ConvertImage.convert((ImageSInt8)input,(int)min,(int)max,numValues,output);
		} else if( type == ImageDataType.U16 ) {
			return ConvertImage.convert((ImageUInt16)input,(int)min,(int)max,numValues,output);
		} else if( type == ImageDataType.S16 ) {
			return ConvertImage.convert((ImageSInt16)input,(int)min,(int)max,numValues,output);
		} else if( type == ImageDataType.S32 ) {
			return ConvertImage.convert((ImageSInt32)input,(int)min,(int)max,numValues,output);
		} else if( type == ImageDataType.S64 ) {
			return ConvertImage.convert((ImageSInt64)input,(long)min,(long)max,numValues,output);
		} else if( type == ImageDataType.F32 ) {
			return ConvertImage.convert((ImageFloat32)input,(float)min,(float)max,numValues,output);
		} else if( type == ImageDataType.F64 ) {
			return ConvertImage.convert((ImageFloat64)input,min,max,numValues,output);
		} else {
			throw new IllegalArgumentException("Unknown image type: " + type);
		}
	}
}
