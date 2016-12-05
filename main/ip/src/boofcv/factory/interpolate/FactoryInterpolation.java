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

package boofcv.factory.interpolate;

import boofcv.abst.filter.interpolate.InterpolatePixel_PL_using_SB;
import boofcv.alg.interpolate.*;
import boofcv.alg.interpolate.impl.*;
import boofcv.alg.interpolate.kernel.BicubicKernel_F32;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
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
	public static <T extends ImageGray> InterpolatePixelS<T>
	createPixelS(double min, double max, InterpolationType type, BorderType borderType , ImageDataType dataType )
	{

		Class t = ImageDataType.typeToSingleClass(dataType);

		return createPixelS(min,max,type,borderType,t);
	}

	public static <T extends ImageBase> InterpolatePixel<T>
	createPixel(double min, double max, InterpolationType type, BorderType borderType, ImageType<T> imageType) {
		switch( imageType.getFamily() ) {
			case GRAY:
			case PLANAR:
				return createPixelS(min, max, type, borderType, imageType.getImageClass());

			case INTERLEAVED:
				return createPixelMB(min,max,type,borderType,(ImageType)imageType);

			default:
				throw new IllegalArgumentException("Unknown family");
		}
	}

	/**
	 * Creates an interpolation class of the specified type for the specified image type.
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Interpolation type
	 * @param borderType Border type. If null then it will not be set here.
	 * @param imageType Type of input image
	 * @return Interpolation
	 */
	public static <T extends ImageGray> InterpolatePixelS<T>
	createPixelS(double min, double max, InterpolationType type, BorderType borderType, Class<T> imageType)
	{
		InterpolatePixelS<T> alg;

		switch( type ) {
			case NEAREST_NEIGHBOR:
				alg = nearestNeighborPixelS(imageType);
				break;

			case BILINEAR:
				return bilinearPixelS(imageType, borderType);

			case BICUBIC:
				alg = bicubicS(-0.5f, (float) min, (float) max, imageType);
				break;

			case POLYNOMIAL4:
				alg = polynomialS(4, min, max, imageType);
				break;

			default:
				throw new IllegalArgumentException("Add type: "+type);
		}

		if( borderType != null )
			alg.setBorder(FactoryImageBorder.single(imageType, borderType));
		return alg;
	}

	/**
	 * Pixel based interpolation on multi-band image
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Interpolation type
	 * @param imageType Type of input image
	 */
	public static <T extends ImageBase> InterpolatePixelMB<T>
	createPixelMB(double min, double max, InterpolationType type, BorderType borderType, ImageType<T> imageType )
	{
		switch (imageType.getFamily()) {

			case PLANAR:
				return (InterpolatePixelMB) createPixelPL(createPixelS(min, max, type, borderType, imageType.getDataType()));

			case GRAY:{
				InterpolatePixelS interpS = createPixelS(min,max,type,borderType,imageType.getImageClass());
				return new InterpolatePixel_S_to_MB(interpS);
			}

			case INTERLEAVED:
				switch( type ) {
					case NEAREST_NEIGHBOR:
						return nearestNeighborPixelMB((ImageType) imageType, borderType);

					case BILINEAR:
						return bilinearPixelMB((ImageType)imageType,borderType);

					default:
						throw new IllegalArgumentException("Interpolate type not yet support for ImageInterleaved");
				}

			default:
				throw new IllegalArgumentException("Add type: "+type);
		}
	}

	/**
	 * Converts a single band interpolation algorithm into a mult-band interpolation for {@link Planar} images.
	 * NOTE: If a specialized interpolation exists you should use that instead of this the specialized code can
	 * reduce the number of calculations.
	 *
	 * @param singleBand Interpolation for a single band.
	 * @param <T> Single band image trype
	 * @return Interpolation for Planar images
	 */
	public static <T extends ImageGray> InterpolatePixelMB<Planar<T>>
	createPixelPL(InterpolatePixelS<T> singleBand) {
		return new InterpolatePixel_PL_using_SB<>(singleBand);
	}

	public static <T extends ImageGray> InterpolatePixelS<T> bilinearPixelS(T image, BorderType borderType) {

		InterpolatePixelS<T> ret = bilinearPixelS((Class) image.getClass(), borderType);
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageGray> InterpolatePixelS<T> bilinearPixelS(Class<T> imageType, BorderType borderType ) {
		InterpolatePixelS<T> alg;

		if( imageType == GrayF32.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_F32();
		else if( imageType == GrayF64.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_F64();
		else if( imageType == GrayU8.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_U8();
		else if( imageType == GrayS16.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_S16();
		else if( imageType == GrayS32.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+ typeName(imageType));

		if( borderType != null )
			alg.setBorder(FactoryImageBorder.single(imageType, borderType));

		return alg;
	}

	public static <T extends ImageMultiBand> InterpolatePixelMB<T> bilinearPixelMB(T image, BorderType borderType) {

		InterpolatePixelMB<T> ret = bilinearPixelMB(image.getImageType(), borderType);
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageMultiBand> InterpolatePixelMB<T> bilinearPixelMB(ImageType<T> imageType, BorderType borderType ) {
		InterpolatePixelMB<T> alg;

		int numBands = imageType.getNumBands();
		if( imageType.getFamily() == ImageType.Family.INTERLEAVED ) {
			switch( imageType.getDataType()) {
				case U8:
					alg = (InterpolatePixelMB<T>)new ImplBilinearPixel_IL_U8(numBands);
					break;

				case S16:
					alg = (InterpolatePixelMB<T>)new ImplBilinearPixel_IL_S16(numBands);
					break;

				case S32:
					alg = (InterpolatePixelMB<T>)new ImplBilinearPixel_IL_S32(numBands);
					break;

				case F32:
					alg = (InterpolatePixelMB<T>)new ImplBilinearPixel_IL_F32(numBands);
					break;

				case F64:
					alg = (InterpolatePixelMB<T>)new ImplBilinearPixel_IL_F64(numBands);
					break;

				default:
					throw new IllegalArgumentException("Add support");
			}

			if( borderType != null )
				alg.setBorder(FactoryImageBorder.interleaved(imageType.getImageClass(), borderType));
		} else {
			throw new IllegalArgumentException("Only interleaved current supported here");
		}

		return alg;
	}

	public static <T extends ImageMultiBand> InterpolatePixelMB<T> nearestNeighborPixelMB(ImageType<T> imageType, BorderType borderType ) {
		InterpolatePixelMB<T> alg;

		if( imageType.getFamily() == ImageType.Family.INTERLEAVED ) {
			switch( imageType.getDataType()) {
				case U8:
					alg = (InterpolatePixelMB<T>)new NearestNeighborPixel_IL_U8();
					break;

				case S16:
					alg = (InterpolatePixelMB<T>)new NearestNeighborPixel_IL_S16();
					break;

				case S32:
					alg = (InterpolatePixelMB<T>)new NearestNeighborPixel_IL_S32();
					break;

				case F32:
					alg = (InterpolatePixelMB<T>)new NearestNeighborPixel_IL_F32();
					break;

				default:
					throw new IllegalArgumentException("Add support");
			}

			if( borderType != null )
				alg.setBorder(FactoryImageBorder.interleaved(imageType.getImageClass(), borderType));
		} else {
			throw new IllegalArgumentException("Only interleaved current supported here");
		}

		return alg;
	}

	private static String typeName(Class type) {
		return type == null ? "null" : type.getName();
	}

	public static <T extends ImageGray> InterpolateRectangle<T> bilinearRectangle(T image ) {

		InterpolateRectangle<T> ret = bilinearRectangle((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageGray> InterpolateRectangle<T> bilinearRectangle(Class<T> type ) {
		if( type == GrayF32.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_F32();
		else if( type == GrayU8.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_U8();
		else if( type == GrayS16.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageGray> InterpolatePixelS<T> nearestNeighborPixelS(Class<T> type) {
		if( type == GrayF32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_F32();
		else if( type == GrayU8.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U8();
		else if( type == GrayS16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S16();
		else if( type == GrayU16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U16();
		else if( type == GrayS32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageGray> InterpolateRectangle<T> nearestNeighborRectangle(Class<?> type ) {
		if( type == GrayF32.class )
			return (InterpolateRectangle<T>)new NearestNeighborRectangle_F32();
//		else if( type == GrayU8.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_U8();
//		else if( type == GrayS16.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageGray> InterpolatePixelS<T> bicubicS(float param, float min, float max, Class<T> type) {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(param);
		if( type == GrayF32.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_F32(kernel,min,max);
		else if( type == GrayU8.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_U8(kernel,min,max);
		else if( type == GrayS16.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_S16(kernel,min,max);
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageGray> InterpolatePixelS<T> polynomialS(int maxDegree, double min, double max, Class<T> type) {
		if( type == GrayF32.class )
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_F32(maxDegree,(float)min,(float)max);
		else if( GrayI.class.isAssignableFrom(type) ) {
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_I(maxDegree,(float)min,(float)max);
		} else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}
}
