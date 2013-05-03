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

package boofcv.factory.interpolate;

import boofcv.alg.interpolate.InterpolatePixel;
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

	public static <T extends ImageSingleBand> InterpolatePixel<T>
	createPixel(double min, double max, TypeInterpolate type, Class<T> imageType)
	{
		switch( type ) {
			case NEAREST_NEIGHBOR:
				return nearestNeighborPixel(imageType);

			case BILINEAR:
				return bilinearPixel(imageType);

			case BICUBIC:
				return bicubic(0.5f, (float)min, (float)max, imageType);

			case POLYNOMIAL4:
				return polynomial(4,min,max,imageType);
		}
		throw new IllegalArgumentException("Add type: "+type);
	}

	public static <T extends ImageSingleBand> InterpolatePixel<T> bilinearPixel( T image ) {

		InterpolatePixel<T> ret = bilinearPixel((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageSingleBand> InterpolatePixel<T> bilinearPixel(Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new ImplBilinearPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new ImplBilinearPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new ImplBilinearPixel_S16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixel<T>)new ImplBilinearPixel_S32();
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

	public static <T extends ImageSingleBand> InterpolatePixel<T> nearestNeighborPixel( Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_S16();
		else if( type == ImageUInt16.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_U16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_S32();
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

	public static <T extends ImageSingleBand> InterpolatePixel<T> bicubic(float param, float min , float max ,Class<T> type) {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(param);
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new ImplInterpolatePixelConvolution_F32(kernel,min,max);
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new ImplInterpolatePixelConvolution_U8(kernel,min,max);
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new ImplInterpolatePixelConvolution_S16(kernel,min,max);
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageSingleBand> InterpolatePixel<T> polynomial( int maxDegree , double min , double max , Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new ImplPolynomialPixel_F32(maxDegree,(float)min,(float)max);
		else if( ImageInteger.class.isAssignableFrom(type) ) {
			return (InterpolatePixel<T>)new ImplPolynomialPixel_I(maxDegree,(float)min,(float)max);
		} else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}
}
