/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.interpolate;

import gecv.alg.interpolate.impl.*;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

/**
 * Simplified interface for creating interpolation classes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryInterpolation {

	public static <T extends ImageBase> InterpolatePixel<T> bilinearPixel( T image ) {

		InterpolatePixel<T> ret = bilinearPixel(image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageBase> InterpolatePixel<T> bilinearPixel(Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new BilinearPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new BilinearPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new BilinearPixel_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolateRectangle<T> bilinearRectangle( T image ) {

		InterpolateRectangle<T> ret = bilinearRectangle(image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageBase> InterpolateRectangle<T> bilinearRectangle( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_F32();
		else if( type == ImageUInt8.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_U8();
		else if( type == ImageSInt16.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolatePixel<T> nearestNeighborPixel( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixel<T>)new NearestNeighborPixel_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}

	public static <T extends ImageBase> InterpolateRectangle<T> nearestNeighborRectangle( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new NearestNeighborRectangle_F32();
//		else if( type == ImageUInt8.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_U8();
//		else if( type == ImageSInt16.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+type.getName());
	}
}
