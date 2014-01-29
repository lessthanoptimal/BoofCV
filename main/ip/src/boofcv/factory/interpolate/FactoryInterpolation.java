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

package boofcv.factory.interpolate;

import boofcv.abst.filter.interpolate.InterpolatePixel_S_to_MB_MultiSpectral;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.interpolate.impl.*;
import boofcv.alg.interpolate.kernel.BicubicKernel_F32;
import boofcv.struct.image.*;

/**
 * Simplified interface for creating interpolation classes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryInterpolation {

	/**
	 * Returns {@link InterpolatePixelS} of the specified type.
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Type of interpolation.
	 * @param dataType Type of gray-scale image
	 * @return Interpolation for single band image
	 */
	public static <T extends ImageSingleBand> InterpolatePixelS<T>
	createPixelS(double min, double max, TypeInterpolate type, ImageDataType dataType )
	{

		Class t = ImageDataType.typeToSingleClass(dataType);

		return createPixelS(min,max,type,t);
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T>
	createPixelS(double min, double max, TypeInterpolate type, Class<T> imageType)
	{
		switch( type ) {
			case NEAREST_NEIGHBOR:
				return nearestNeighborPixelS(imageType);

			case BILINEAR:
				return bilinearPixelS(imageType);

			case BICUBIC:
				return bicubicS(0.5f, (float) min, (float) max, imageType);

			case POLYNOMIAL4:
				return polynomialS(4, min, max, imageType);
		}
		throw new IllegalArgumentException("Add type: "+type);
	}

	public static <T extends ImageMultiBand> InterpolatePixelMB<T>
	createPixelMB(double min, double max, TypeInterpolate type, ImageType<T> imageType )
	{
		switch (imageType.getFamily()) {

			case MULTI_SPECTRAL:
				return (InterpolatePixelMB)createPixelMB(createPixelS(min,max,type,imageType.getDataType()));

			case SINGLE_BAND:
				throw new IllegalArgumentException("Need to specify a multi-band image type");

			case INTERLEAVED:
				throw new IllegalArgumentException("Not yet supported.  Post a message letting us know you need this." +
						"  Use MultiSpectral instead for now.");

			default:
				throw new IllegalArgumentException("Add type: "+type);
		}
	}

	/**
	 * Converts a single band interpolation algorithm into a mult-band interpolation for {@link MultiSpectral} images.
	 * NOTE: If a specialized interpolation exists you should use that instead of this the specialized code can
	 * reduce the number of calculations.
	 *
	 * @param singleBand Interpolation for a single band.
	 * @param <T> Single band image trype
	 * @return Interpolation for MultiSpectral images
	 */
	public static <T extends ImageSingleBand> InterpolatePixelMB<MultiSpectral<T>>
	createPixelMB( InterpolatePixelS<T> singleBand ) {
		return new InterpolatePixel_S_to_MB_MultiSpectral<T>(singleBand);
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bilinearPixelS(T image) {

		InterpolatePixelS<T> ret = bilinearPixelS((Class) image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bilinearPixelS(Class<T> type) {
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new ImplBilinearPixel_F32();
		if( type == ImageFloat64.class )
			return (InterpolatePixelS<T>)new ImplBilinearPixel_F64();
		else if( type == ImageUInt8.class )
			return (InterpolatePixelS<T>)new ImplBilinearPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixelS<T>)new ImplBilinearPixel_S16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixelS<T>)new ImplBilinearPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> bilinearRectangle( T image ) {

		InterpolateRectangle<T> ret = bilinearRectangle((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> bilinearRectangle( Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_F32();
		else if( type == ImageUInt8.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_U8();
		else if( type == ImageSInt16.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> nearestNeighborPixelS(Class<T> type) {
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S16();
		else if( type == ImageUInt16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> nearestNeighborRectangle( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new NearestNeighborRectangle_F32();
//		else if( type == ImageUInt8.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_U8();
//		else if( type == ImageSInt16.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bicubicS(float param, float min, float max, Class<T> type) {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(param);
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_F32(kernel,min,max);
		else if( type == ImageUInt8.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_U8(kernel,min,max);
		else if( type == ImageSInt16.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_S16(kernel,min,max);
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> polynomialS(int maxDegree, double min, double max, Class<T> type) {
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_F32(maxDegree,(float)min,(float)max);
		else if( ImageInteger.class.isAssignableFrom(type) ) {
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_I(maxDegree,(float)min,(float)max);
		} else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}
}
